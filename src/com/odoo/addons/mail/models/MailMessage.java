package com.odoo.addons.mail.models;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.ODomain;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OHtml;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.util.ODate;

public class MailMessage extends OModel {
	private Context mContext = null;

	OColumn type = new OColumn("Type", OInteger.class).setDefault("email");
	OColumn email_from = new OColumn("Email", OVarchar.class, 64)
			.setDefault("false");
	OColumn author_id = new OColumn("Author", ResPartner.class,
			RelationType.ManyToOne);
	OColumn partner_ids = new OColumn("To", ResPartner.class,
			RelationType.ManyToMany).setRequired(true);
	OColumn notified_partner_ids = new OColumn("Notified Partners",
			ResPartner.class, RelationType.ManyToMany);
	OColumn attachment_ids = new OColumn("Attachments", IrAttachment.class,
			RelationType.ManyToMany);
	OColumn parent_id = new OColumn("Parent", MailMessage.class,
			RelationType.ManyToOne).setDefault(0);
	OColumn child_ids = new OColumn("Childs", MailMessage.class,
			RelationType.OneToMany).setRelatedColumn("parent_id")
			.setLocalColumn();
	OColumn model = new OColumn("Model", OVarchar.class, 64)
			.setDefault("false");
	OColumn res_id = new OColumn("Resource ID", OInteger.class).setDefault(0);
	OColumn record_name = new OColumn("Record name", OText.class)
			.setDefault("false");
	OColumn notification_ids = new OColumn("Notifications",
			MailNotification.class, RelationType.OneToMany)
			.setRelatedColumn("message_id");
	OColumn subject = new OColumn("Subject", OVarchar.class, 100).setDefault(
			"false").setRequired(true);
	OColumn date = new OColumn("Date", ODateTime.class).setParsePattern(
			ODate.DEFAULT_FORMAT).setDefault(
			ODate.getUTCDate(ODate.DEFAULT_FORMAT));
	OColumn body = new OColumn("Body", OHtml.class).setDefault("").setRequired(
			true);
	OColumn vote_user_ids = new OColumn("Voters", ResUsers.class,
			RelationType.ManyToMany);

	@Odoo.Functional(method = "getToRead", store = true, depends = { "notification_ids" })
	OColumn to_read = new OColumn("To Read", OBoolean.class).setDefault(true);
	@Odoo.Functional(method = "getStarred", store = true, depends = { "notification_ids" })
	OColumn starred = new OColumn("Starred", OBoolean.class).setDefault(false);

	// Functional Fields
	@Odoo.Functional(method = "getMessageTitle")
	OColumn message_title = new OColumn("Title");
	@Odoo.Functional(method = "getChildCount")
	OColumn childs_count = new OColumn("Childs");
	@Odoo.Functional(method = "getAuthorName")
	OColumn author_name = new OColumn("Author", OVarchar.class);
	@Odoo.Functional(method = "hasVoted")
	OColumn has_voted = new OColumn("Has voted", OVarchar.class);
	@Odoo.Functional(method = "getVoteCounter")
	OColumn vote_counter = new OColumn("Votes", OInteger.class);
	@Odoo.Functional(method = "getPartnersName")
	OColumn partners_name = new OColumn("Partners", OVarchar.class);

	private List<Integer> mNewCreateIds = new ArrayList<Integer>();
	private MailNotification notification = null;

	public MailMessage(Context context) {
		super(context, "mail.message");
		mContext = context;
		notification = new MailNotification(mContext);
		write_date.setDefault(false);
		create_date.setDefault(false);
	}

	public Integer author_id() {
		return new ResPartner(mContext).selectRowId(user().getPartner_id());
	}

	@Override
	public Boolean checkForWriteDate() {
		return true;
	}

	public Boolean getValueofReadUnReadField(int id) {
		boolean read = false;
		ODataRow row = select(id);
		read = row.getBoolean("to_read");
		return read;
	}

	@Override
	public Boolean checkForLocalUpdate() {
		return false;
	}

	@Override
	public Boolean checkForLocalLatestUpdate() {
		return false;
	}

	@Override
	public Boolean canCreateOnServer() {
		return false;
	}

	@Override
	public Boolean canDeleteFromLocal() {
		return false;
	}

	@Override
	public Boolean canDeleteFromServer() {
		return false;
	}

	@Override
	public Boolean canUpdateToServer() {
		return false;
	}

	public Boolean getToRead(OValues vals) {
		ODataRow noti = notification.select(vals.getInt(notification_ids
				.getName()));
		return (noti.contains("is_read")) ? !noti.getBoolean("is_read") : !noti
				.getBoolean("read");
	}

	public Boolean getStarred(OValues vals) {
		ODataRow noti = notification.select(vals.getInt(notification_ids
				.getName()));
		return noti.getBoolean("starred");
	}

	public boolean markAsTodo(ODataRow row, Boolean todo_state) {
		try {
			OArguments args = new OArguments();
			args.add(new JSONArray().put(row.getInt("id")));
			args.add(todo_state);
			args.add(true);
			getSyncHelper().callMethod("set_message_starred", args, null);
			OValues values = new OValues();
			values.put("starred", todo_state);
			// updating local record
			update(values, row.getInt(OColumn.ROW_ID));
			// updating mail notification
			values = new OValues();
			values.put("starred", todo_state);
			new MailNotification(mContext).update(values, "message_id = ?",
					new Object[] { row.getInt(OColumn.ROW_ID) });
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int create(OValues values) {
		int newId = super.create(values);
		mNewCreateIds.add(newId);
		return newId;
	}

	public List<Integer> newMessageIds() {
		return mNewCreateIds;
	}

	public Integer sendQuickReply(String subject, String body, Integer parent_id) {
		OValues vals = new OValues();
		vals.put("subject", subject);
		vals.put("body", body);
		vals.put("parent_id", parent_id);
		vals.put("author_id", author_id());
		vals.put("date", ODate.getUTCDate(ODate.DEFAULT_FORMAT));
		List<Integer> p_ids = new ArrayList<Integer>();
		for (ODataRow partner : select(parent_id).getM2MRecord("partner_ids")
				.browseEach()) {
			p_ids.add(partner.getInt(OColumn.ROW_ID));
		}
		vals.put("partner_ids", p_ids);
		Integer replyId = create(vals);
		// Creating notification for record
		OValues nVals = new OValues();
		nVals.put("partner_id", author_id());
		nVals.put("message_id", replyId);
		notification.create(nVals);
		return replyId;
	}

	public boolean markAsRead(ODataRow row, Boolean is_read) {
		try {
			List<Integer> mIds = new ArrayList<Integer>();
			List<ODataRow> childs = new ArrayList<ODataRow>();
			Object default_model = false;
			Object default_res_id = false;
			ODataRow parent = ((Integer) row.getM2ORecord("parent_id").getId() == 0) ? row
					: row.getM2ORecord("parent_id").browse();
			default_model = parent.get("model");
			default_res_id = parent.get("res_id");
			mIds.add(parent.getInt("id"));
			childs.addAll(parent.getO2MRecord("child_ids").browseEach());
			for (ODataRow child : childs) {
				mIds.add(child.getInt("id"));
			}
			JSONObject newContext = new JSONObject();
			newContext.put("default_parent_id", parent.getInt("id"));
			newContext.put("default_model", default_model);
			newContext.put("default_res_id", default_res_id);

			OArguments args = new OArguments();
			args.add(new JSONArray(mIds.toString()));
			args.add(is_read);
			args.add(true);
			args.add(newContext);
			Integer updated = (Integer) getSyncHelper().callMethod(
					"set_message_read", args, null);
			if (updated > 0) {

				OValues values = new OValues();
				values.put("to_read", !is_read);
				// updating local record
				for (Integer id : mIds)
					update(values, selectRowId(id));
				// updating mail notification
				values = new OValues();
				values.put("is_read", is_read);
				for (Integer id : mIds)
					new MailNotification(mContext).update(values,
							"message_id = ?", new Object[] { selectRowId(id) });
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getPartnersName(ODataRow row) {
		String partners = "to ";
		List<String> partners_name = new ArrayList<String>();
		for (ODataRow p : row.getM2MRecord("partner_ids").browseEach()) {
			partners_name.add(p.getString("name"));
		}
		return partners + TextUtils.join(", ", partners_name);
	}

	public String getVoteCounter(ODataRow row) {
		int votes = row.getM2MRecord(vote_user_ids.getName()).browseEach()
				.size();
		if (votes > 0)
			return votes + "";
		return "";
	}

	public Boolean hasVoted(ODataRow row) {
		for (ODataRow r : row.getM2MRecord("vote_user_ids").browseEach()) {
			if (r.getInt("id") == user().getUser_id()) {
				return true;
			}
		}
		return false;
	}

	public String getMessageTitle(ODataRow row) {
		String title = "false";
		if (!row.getString("record_name").equals("false"))
			title = row.getString("record_name");
		if (!title.equals("false") && !row.getString("subject").equals("false"))
			title += ": " + row.getString("subject");
		if (title.equals("false") && !row.getString("subject").equals("false"))
			title = row.getString("subject");
		if (title.equals("false"))
			title = "comment";
		return title;
	}

	public String getChildCount(ODataRow row) {
		List<ODataRow> childs = row.getO2MRecord("child_ids").browseEach();
		return (childs.size() > 0) ? childs.size() + " replies" : " ";
	}

	public String getAuthorName(ODataRow row) {
		String author_name = null;
		ODataRow author = row.getM2ORecord("author_id").browse();
		if (author != null) {
			author_name = author.getString("name");
		} else {
			author_name = row.getString("email_from");
		}
		return author_name;
	}

	@Override
	public ODomain defaultDomain() {
		Integer user_id = user().getUser_id();
		List<Integer> parent_ids = new ArrayList<Integer>();
		for (ODataRow row : select("parent_id = ?", new Object[] { "0" })) {
			parent_ids.add(row.getInt("id"));
		}
		ODomain domain = new ODomain();
		if (parent_ids.size() > 0) {
			domain.add("|");
			domain.add("id", "child_of", parent_ids);
		}
		domain.add("|");
		domain.add("partner_ids.user_ids", "in", new JSONArray().put(user_id));
		domain.add("|");
		domain.add("notification_ids.partner_id.user_ids", "in",
				new JSONArray().put(user_id));
		domain.add("author_id.user_ids", "in", new JSONArray().put(user_id));
		if (!isEmptyTable()) {
			domain.add("|");
			domain.add("date", ">", getSyncHelper().getLastSyncDate(this));
		}
		return domain;
	}

	public static class ResUsers extends OModel {

		OColumn name = new OColumn("Name", OVarchar.class, 64);

		public ResUsers(Context context) {
			super(context, "res.users");
		}

	}
}
