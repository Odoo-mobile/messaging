package com.odoo.addons.mail.services;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.auth.OdooAccountManager;
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
			if (mdb.getSyncHelper().syncWithServer()) {
				if (user.getAndroidName().equals(account.name)) {
					context.sendBroadcast(intent);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
