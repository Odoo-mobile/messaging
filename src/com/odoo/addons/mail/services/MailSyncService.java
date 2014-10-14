package com.odoo.addons.mail.services;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.ODomain;
import odoo.Odoo;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.odoo.App;
import com.odoo.MainActivity;
import com.odoo.R;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.base.ir.Attachments;
import com.odoo.base.res.ResPartner;
import com.odoo.base.res.ResUsers;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OFieldsHelper;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.support.OUser;
import com.odoo.support.service.OSyncAdapter;
import com.odoo.support.service.OSyncFinishListener;
import com.odoo.support.service.OSyncService;
import com.odoo.util.JSONUtils;
import com.odoo.util.notification.NotificationBuilder;

public class MailSyncService extends OSyncService implements
		OSyncFinishListener {
	public static final String TAG = MailSyncService.class.getSimpleName();

	@Override
	public OSyncAdapter getSyncAdapter() {
		return new OSyncAdapter(getApplicationContext(), new MailMessage(
				getApplicationContext()), this, true);
	}

	@Override
	public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
		Context mContext = getApplicationContext();
		try {
			MailMessage mdb = new MailMessage(mContext);
			mdb.setUser(user);
			if (sendMails(mContext, user, mdb, mdb.getSyncHelper())) {
				updateMails(user);
			}
			ODomain domain = new ODomain();
			if (extras.containsKey(MailGroupSyncService.KEY_GROUP_IDS)) {
				JSONArray group_ids = new JSONArray(
						extras.getString(MailGroupSyncService.KEY_GROUP_IDS));
				domain.add("res_id", "in", group_ids);
				domain.add("model", "=", "mail.group");
			}
			adapter.syncDataLimit(30).onSyncFinish(this).setDomain(domain);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Boolean sendMails(Context context, OUser user, MailMessage mails,
			OSyncHelper helper) {
		for (ODataRow mail : mails.select("id = ?", new Object[] { 0 })) {
			try {
				JSONArray partner_ids = new JSONArray();
				JSONArray p_ids = new JSONArray();
				List<ODataRow> partners = mail.getM2MRecord("partner_ids")
						.browseEach();
				p_ids = JSONUtils.toArray(getPartnersServerId(partners));
				partner_ids.put(6);
				partner_ids.put(false);
				partner_ids.put(p_ids);
				// Attachments
				List<Integer> attachments = getAttachmentIds(mail);
				Object attachment_ids = false;
				if (attachments.size() > 0) {
					JSONArray attachmentIds = new JSONArray();
					attachmentIds.put(6);
					attachmentIds.put(false);
					attachmentIds.put(JSONUtils.toArray(attachments));
					attachment_ids = new JSONArray().put(attachmentIds);
				}
				if ((Integer) mail.getM2ORecord("parent_id").getId() == 0) {
					_sendMail(mail, partner_ids, attachment_ids, helper, mails);
				} else {
					JSONArray attachments_reply = new JSONArray();
					if (!attachment_ids.toString().equals("false")) {
						attachments_reply = new JSONArray(JSONUtils.toArray(
								attachments).toString());
					}
					ODataRow parent = mail.getM2ORecord("parent_id").browse();
					sendReply(context, parent, mail, partner_ids,
							attachments_reply, helper, mails);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "send Mails()" + e.getMessage());
			}
		}
		return true;
	}

	private void _sendMail(ODataRow mail, JSONArray partner_ids,
			Object attachment_ids, OSyncHelper helper, MailMessage mails) {
		try {

			JSONObject arguments = new JSONObject();
			arguments.put("composition_mode", "comment");
			arguments.put("model", false);
			arguments.put("parent_id", false);
			arguments.put("subject", mail.getString("subject"));
			arguments.put("body", mail.getString("body"));
			arguments.put("post", true);
			arguments.put("notify", false);
			arguments.put("same_thread", true);
			arguments.put("use_active_domain", false);
			arguments.put("reply_to", false);
			arguments.put("res_id", false);
			arguments.put("record_name", false);
			arguments.put("partner_ids", new JSONArray().put(partner_ids));
			arguments.put("template_id", false);
			arguments.put("attachment_ids", attachment_ids);

			JSONObject kwargs = new JSONObject();
			kwargs.put("context", helper.getContext(new JSONObject()));
			OArguments args = new OArguments();
			args.add(arguments);
			String model = "mail.compose.message";
			// Creating compose message
			Object message_id = helper.callMethod(model, "create", args, null,
					kwargs);
			// sending mail
			args = new OArguments();
			args.add(new JSONArray().put(message_id));
			args.add(helper.getContext(null));
			helper.callMethod(model, "send_mail", args, null, null);
			mails.resolver().delete(mail.getInt(OColumn.ROW_ID));
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "_sendMail() : " + e.getMessage());
		}
	}

	private void sendReply(Context context, ODataRow parent, ODataRow mail,
			JSONArray partner_ids, Object attachment_ids, OSyncHelper helper,
			MailMessage mails) {
		try {
			// sending reply
			String model = (parent.getString("model").equals("false")) ? "mail.thread"
					: parent.getString("model");
			String method = "message_post";
			Object res_model = (parent.getString("model").equals("false")) ? false
					: parent.getString("model");
			Object res_id = (parent.getInt("res_id") == 0) ? false : parent
					.getInt("res_id");

			JSONObject jContext = new JSONObject();
			jContext.put("default_model", res_model);
			jContext.put("default_res_id", res_id);
			jContext.put("default_parent_id", parent.getInt("id"));
			jContext.put("mail_post_autofollow", true);
			jContext.put("mail_post_autofollow_partner_ids", new JSONArray());
			JSONObject kwargs = new JSONObject();
			kwargs.put("context", jContext);
			kwargs.put("subject", mail.getString("subject"));
			kwargs.put("body", mail.getString("body"));
			kwargs.put("parent_id", parent.getInt("id"));
			kwargs.put("attachment_ids", attachment_ids);
			kwargs.put("partner_ids", new JSONArray().put(partner_ids));

			OArguments args = new OArguments();
			args.add(new JSONArray().put(res_id));
			Integer messageId = (Integer) helper.callMethod(model, method,
					args, null, kwargs);
			OValues vals = new OValues();
			vals.put(OColumn.ROW_ID, mail.getInt(OColumn.ROW_ID));
			vals.put("id", messageId);
			mails.resolver().update(vals.getInt(OColumn.ROW_ID), vals);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "send Reply()" + e.getMessage());
		}
	}

	private List<Integer> getAttachmentIds(ODataRow row) {
		List<Integer> ids = new ArrayList<Integer>();
		try {
			Attachments helper = new Attachments(getApplicationContext());
			for (ODataRow attachment : row.getM2MRecord("attachment_ids")
					.browseEach()) {
				int id = helper.pushToServer(attachment);
				if (id != 0) {
					ids.add(id);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ids;
	}

	private Boolean updateMails(OUser user) {
		_updateUpStream(user);
		_updateDownStream(user);
		return true;
	}

	private void _updateUpStream(OUser user) {
		Context context = getApplicationContext();
		MailMessage mail = new MailMessage(context);
		Cursor cr = context.getContentResolver().query(mail.uri(),
				new String[] { "id", OColumn.ROW_ID, "to_read", "parent_id" },
				"is_dirty = ? or is_dirty = ?", new String[] { "1", "true" },
				null);
		List<Integer> finished = new ArrayList<Integer>();
		if (cr.moveToFirst()) {
			do {
				int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
				int sync_id = parent_id;
				if (parent_id == 0) {
					sync_id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
				}
				if (finished.indexOf(sync_id) <= -1) {
					Boolean to_read = (cr.getInt(cr.getColumnIndex("to_read")) == 1) ? true
							: false;
					mail.markMailReadUnread(sync_id, to_read);
					finished.add(sync_id);
				}
			} while (cr.moveToNext());
		}
		cr.close();
	}

	private void _updateDownStream(OUser user) {
		Context context = getApplicationContext();
		App app = (App) context;
		MailMessage mail = new MailMessage(context);
		ResUsers users = new ResUsers(context);
		try {
			Odoo odoo = app.getOdoo();
			ODomain domain = new ODomain();
			domain.add("id", "in", new JSONArray(mail.ids().toString()));
			JSONObject fields = new JSONObject();
			fields.accumulate("fields", "to_read");
			fields.accumulate("fields", "starred");
			fields.accumulate("fields", "vote_user_ids");
			JSONArray result = odoo.search_read(mail.getModelName(), fields,
					domain.get()).getJSONArray("records");
			for (int i = 0; i < result.length(); i++) {
				JSONObject row = result.getJSONObject(i);
				int id = row.getInt("id");
				int to_read = (row.getBoolean("to_read")) ? 1 : 0;
				int starred = (row.getBoolean("starred")) ? 1 : 0;
				List<Integer> vote_user_ids = JSONUtils.toList(row
						.getJSONArray("vote_user_ids"));
				int row_id = mail.selectRowId(id);
				OValues values = new OValues();
				values.put("to_read", to_read);
				values.put("starred", starred);
				List<Integer> local_vote_user_ids = new ArrayList<Integer>();
				for (Integer user_id : vote_user_ids) {
					OValues vals = new OValues();
					vals.put("id", user_id);
					local_vote_user_ids.add(users.createORReplace(vals));
				}
				values.put("vote_user_ids", local_vote_user_ids.toString());
				mail.resolver().update(row_id, values);
			}
			OFieldsHelper userFields = new OFieldsHelper(new String[] {
					"login", "name" });
			JSONArray user_result = odoo.search_read(users.getModelName(),
					userFields.get(), null).getJSONArray("records");
			List<Integer> local_uids = new ArrayList<Integer>();
			for (ODataRow rows : users.select()) {
				local_uids.add(rows.getInt("id"));
			}
			for (int i = 0; i < user_result.length(); i++) {
				JSONObject row = user_result.getJSONObject(i);
				if (!local_uids.contains(row.getInt("id"))) {
					OValues values = new OValues();
					values.put("id", row.getInt("id"));
					values.put("name", row.getString("name"));
					values.put("login", row.getString("login"));
					values.put("is_dirty", false);
					users.createORReplace(values);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public OSyncAdapter performSync(SyncResult syncResult) {
		App app = (App) getApplicationContext();
		int newTotal = (int) syncResult.stats.numInserts;
		newTotal = (newTotal != 0) ? newTotal / 2 : newTotal;
		if (!app.appOnTop() && newTotal > 0) {
			NotificationBuilder notification = new NotificationBuilder(
					getApplicationContext());
			notification.setAutoCancel(true);
			notification.setIcon(R.drawable.ic_odoo_o);
			notification.setTitle(newTotal + " unread messages");
			notification.setText(newTotal + " new message received (Odoo)");
			Intent rIntent = new Intent(getApplicationContext(),
					MainActivity.class);
			notification.setResultIntent(rIntent);
			notification.build().show();
		}
		return null;
	}

	public List<Integer> getPartnersServerId(List<ODataRow> rows) {
		List<Integer> pIds = new ArrayList<Integer>();

		for (ODataRow row : rows) {
			int server_id = row.getInt("id");
			if (server_id == 0) {
				ODomain domain = new ODomain();
				domain.add("email", "ilike", row.getString("email"));
				server_id = getPartnerId(domain, row);
			}
			pIds.add(server_id);
		}

		return pIds;
	}

	public int getPartnerId(ODomain domain, ODataRow row) {
		int server_id = 0;
		try {
			Context context = getApplicationContext();
			ResPartner partner = new ResPartner(context);
			App app = (App) context;
			Odoo odoo = app.getOdoo();
			JSONObject fields = new JSONObject();
			fields.accumulate("fields", "email");
			JSONObject result = odoo.search_read(partner.getModelName(),
					fields, domain.get());
			JSONArray records = result.getJSONArray("records");
			if (records.length() > 0) {
				JSONObject record = records.getJSONObject(0);
				server_id = record.getInt("id");
				OValues vals = new OValues();
				vals.put("id", server_id);
				partner.resolver().update(row.getInt(OColumn.ROW_ID), vals);
			} else {
				// Creating partner on server
				server_id = partner.getSyncHelper().create(partner, row);
				Log.v(TAG, "partner created on server " + server_id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return server_id;
	}
}
