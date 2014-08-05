package com.odoo.addons.mail.services;

import java.util.List;

import odoo.OArguments;
import odoo.ODomain;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.odoo.MainActivity;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.models.MailNotification;
import com.odoo.auth.OdooAccountManager;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.OUser;
import com.odoo.support.service.OService;
import com.odoo.util.OENotificationHelper;
import com.openerp.R;

public class MailSyncService extends OService {
	public static final String TAG = MailSyncService.class.getSimpleName();
	Context mContext = null;
	MailMessage mdb = null;

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.d(TAG, "MailSyncService->Start");
		mContext = context;
		OUser user = OdooAccountManager.getAccountDetail(context, account.name);
		Intent intent = new Intent();
		intent.setAction(SyncFinishReceiver.SYNC_FINISH);
		try {
			mdb = new MailMessage(context);
			mdb.setUser(user);
			ODomain domain = new ODomain();
			if (extras.containsKey(MailGroupSyncService.KEY_GROUP_IDS)) {
				JSONArray group_ids = new JSONArray(
						extras.getString(MailGroupSyncService.KEY_GROUP_IDS));
				domain.add("res_id", "in", group_ids);
				domain.add("model", "=", "mail.group");
			}

			// #1 : Sync new messages
			// #2 : Starred local to server
			// #3 : read/unread local to server
			// #4 : update read/unread/starred server to local
			// #5 : send mails

			boolean showNotification = true;
			ActivityManager am = (ActivityManager) context
					.getSystemService(ACTIVITY_SERVICE);
			List<ActivityManager.RunningTaskInfo> taskInfo = am
					.getRunningTasks(1);
			ComponentName componentInfo = taskInfo.get(0).topActivity;
			if (componentInfo.getPackageName().equalsIgnoreCase("com.openerp")) {
				showNotification = false;
			}
			if (sendMails(context, user, mdb, mdb.getSyncHelper())) {
				if (mdb.getSyncHelper().syncDataLimit(30)
						.syncWithServer(domain)) {
					if (showNotification && mdb.newMessageIds().size() > 0) {
						int newTotal = mdb.newMessageIds().size();
						OENotificationHelper mNotification = new OENotificationHelper();
						Intent mainActiivty = new Intent(context,
								MainActivity.class);
						mNotification.setResultIntent(mainActiivty, context);
						mNotification.showNotification(context, newTotal
								+ " new messages", newTotal
								+ " new message received (Odoo)", authority,
								R.drawable.ic_odoo_o);
					}
					// if (updateStarredOnServer(context, user,
					// mdb.getSyncHelper())) {
					// if (updateReadUnreadOnServer(context, user,
					// mdb.getSyncHelper())) {
					if (updateOldMessages(context, user, mdb.ids())) {
						if (user.getAndroidName().equals(account.name)) {
							context.sendBroadcast(intent);
						}
					}
					// }
					// }
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Boolean sendMails(Context context, OUser user, MailMessage mails,
			OSyncHelper helper) {
		for (ODataRow mail : mails.select("id = ?", new Object[] { 0 })) {
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
				arguments.put("res_id", 0);
				arguments.put("record_name", false);
				JSONArray partner_ids = new JSONArray();
				JSONArray p_ids = new JSONArray();
				for (ODataRow partner : mail.getM2MRecord("partner_ids")
						.browseEach()) {
					p_ids.put(partner.getInt("id"));

				}
				partner_ids.put(6);
				partner_ids.put(false);
				partner_ids.put(p_ids);
				arguments.put("partner_ids", new JSONArray().put(partner_ids));
				arguments.put("template_id", false);

				JSONObject kwargs = new JSONObject();
				kwargs.put("context", helper.getContext(new JSONObject()));

				OArguments args = new OArguments();
				args.add(arguments);
				String model = "mail.compose.message";
				// Creating compose message
				Object message_id = helper.callMethod(model, "create", args,
						null, kwargs);
				// sending mail
				args = new OArguments();
				args.add(new JSONArray().put(message_id));
				args.add(helper.getContext(null));
				helper.callMethod(model, "send_mail", args, null, null);
				mails.delete(mail.getInt(OColumn.ROW_ID));
			} catch (Exception e) {
				Log.e(TAG, "sendMails():" + e.getMessage());
			}
		}
		return true;
	}

	private Boolean updateOldMessages(Context context, OUser user,
			List<Integer> ids) {
		try {
			ODomain domain = new ODomain();
			domain.add("message_id", "in", ids);
			domain.add("partner_id", "=", user.getPartner_id());
			MailNotification mailNotification = new MailNotification(context);
			MailMessage message = new MailMessage(context);
			if (mailNotification.getSyncHelper().syncWithServer(domain, false)) {
				for (Integer id : ids) {
					int row_id = message.selectRowId(id);
					List<ODataRow> notifications = mailNotification.select(
							"message_id = ?", new Object[] { row_id });
					if (notifications.size() > 0) {
						OValues vals = new OValues();
						ODataRow noti = notifications.get(0);
						vals.put(
								"to_read",
								(noti.contains("read")) ? !noti
										.getBoolean("read") : !noti
										.getBoolean("is_read"));
						vals.put("starred", noti.get("starred"));
						message.update(vals, row_id);
					}
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
