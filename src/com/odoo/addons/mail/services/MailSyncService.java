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

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.d(TAG, "MailSyncService->Start");
		OUser user = OdooAccountManager.getAccountDetail(context, account.name);
		Intent intent = new Intent();
		intent.setAction(SyncFinishReceiver.SYNC_FINISH);
		try {
			MailMessage mdb = new MailMessage(context);
			mdb.setUser(user);
			ODomain domain = new ODomain();
			if (extras.containsKey(MailGroupSyncService.KEY_GROUP_IDS)) {
				JSONArray group_ids = new JSONArray(
						extras.getString(MailGroupSyncService.KEY_GROUP_IDS));
				domain.add("res_id", "in", group_ids);
				domain.add("model", "=", "mail.group");
			}
			if (mdb.getSyncHelper().syncDataLimit(10).syncWithServer(domain)) {
				if (updateOldMessages(context, user, mdb.ids())) {
					if (user.getAndroidName().equals(account.name)) {
						context.sendBroadcast(intent);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
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
