package com.odoo.addons.mail.services;

import java.util.List;

import odoo.ODomain;

import org.json.JSONArray;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.models.MailNotification;
import com.odoo.auth.OdooAccountManager;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.OUser;
import com.odoo.support.service.OService;

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

			if (mdb.getSyncHelper().syncDataLimit(10).syncWithServer(domain)) {
				// if (updateStarredOnServer(context, user,
				// mdb.getSyncHelper())) {
				// if (updateReadUnreadOnServer(context, user,
				// mdb.getSyncHelper())) {
				// if (sendMails(context, user, mdb.getSyncHelper())) {
				if (updateOldMessages(context, user, mdb.ids())) {
					if (user.getAndroidName().equals(account.name)) {
						context.sendBroadcast(intent);
					}
					// }
					// }
					// }
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// private Boolean sendMails(Context context, OUser user, OSyncHelper
	// helper) {
	// helper.callMethod(method, args)
	// helper.callMethod(method, args, context)
	// helper.callMethod(method, args, context, kwargs)
	// return true;
	// }

	// private Boolean updateStarredOnServer(Context context, OUser user,
	// OSyncHelper helper) {
	// JSONArray mIds_st = new JSONArray();
	// JSONArray mIds_sf = new JSONArray();
	// OArguments args_st = new OArguments();
	// OArguments args_sf = new OArguments();
	// for (ODataRow row : mdb.select("starred = ? and is_dirty = ?",
	// new String[] { "true", "true" })) {
	// mIds_st.put(row.getInt("id"));
	// }
	// for (ODataRow rows : mdb.select("starred = ? and is_dirty = ?",
	// new String[] { "false", "true" })) {
	// mIds_sf.put(rows.getInt("id"));
	// }
	//
	// args_st.add(mIds_st);
	// args_st.add(true);
	// args_sf.add(mIds_sf);
	// args_sf.add(false);
	// boolean response = (Boolean) helper.callMethod("set_message_starred",
	// args_st, null);
	// response = (Boolean) helper.callMethod("set_message_starred", args_sf,
	// null);
	//
	// if (response)
	// return true;
	// return true;
	// }

	// private Boolean updateReadUnReadStarredtoLocal(Context context, OUser
	// user,
	// OSyncHelper helper) {
	// return false;
	// }

//	private Boolean updateReadUnreadOnServer(Context context, OUser user,
//			OSyncHelper helper) {
//		JSONArray mIds_rt = new JSONArray();
//		JSONArray mIds_rf = new JSONArray();
//		OArguments args_rt = new OArguments();
//		OArguments args_rf = new OArguments();
//		for (ODataRow row : mdb.select("is_dirty = ? and to_read = ?",
//				new String[] { "true", "true" })) {
//			mIds_rt.put(row.getInt("id"));
//			// if (row.getInt("parent_id") != 0)
//			// mIds_rt.put(row.getInt("id"));
//
//			OLog.log("To_read = " + row.getBoolean("to_read"));
//		}
//		for (ODataRow row : mdb.select("is_dirty = ? and to_read = ?",
//				new String[] { "true", "false" })) {
//			mIds_rf.put(row.getInt("id"));
//			// if (row.getInt("parent_id") != 0)
//			// mIds_rf.put(row.getInt("id"));
//			OLog.log("To_read = " + row.getBoolean("to_read"));
//		}
//		args_rt.add(mIds_rt);
//		args_rt.add(true);
//		args_rf.add(mIds_rf);
//		args_rf.add(false);
//		boolean response = (Boolean) helper.callMethod("set_message_read",
//				args_rt, null);
//		response = (Boolean) helper.callMethod("set_message_read", args_rf,
//				null);
//		OLog.log("Read UnRead Response == " + response);
//		if (response)
//			return true;
//		return false;
	// }

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
