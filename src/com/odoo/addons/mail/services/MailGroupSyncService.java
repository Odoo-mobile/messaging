package com.odoo.addons.mail.services;

import odoo.ODomain;
import odoo.Odoo;

import org.json.JSONArray;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.auth.OdooAccountManager;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.OUser;
import com.odoo.support.service.OService;
import com.odoo.util.logger.OLog;

public class MailGroupSyncService extends OService {
	public static final String TAG = MailGroupSyncService.class.getSimpleName();

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		OLog.log(TAG + " MailGroupSyncService Start");
		OUser user = OUser.current(context);
		Intent intent = new Intent();
		intent.setAction(SyncFinishReceiver.SYNC_FINISH);
		MailGroup mdb = new MailGroup(context);
		mdb.setUser(OdooAccountManager.getAccountDetail(context, account.name));
		OSyncHelper oe = mdb.getSyncHelper();

		if (oe != null && oe.syncWithServer() == true) {
			MailFollowers follower = new MailFollowers(context);
			ODomain domain = new ODomain();

			domain.add("partner_id", "=", user.getPartner_id());
			domain.add("res_model", "=", mdb.getModelName());
			Odoo.DEBUG = true;
			if (follower.getSyncHelper().syncWithServer(domain, true)) {
				Odoo.DEBUG = false;
				JSONArray group_ids = new JSONArray();
				for (ODataRow grp : follower.select(
						"res_model = ? AND partner_id = ?",
						new Object[] { mdb.getModelName(),
								user.getPartner_id() + "" })) {
					group_ids.put(grp.getInt("id"));
				}
				Bundle messageBundle = new Bundle();
				messageBundle.putString("group_ids", group_ids.toString());
				messageBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL,
						true);
				messageBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED,
						true);
				ContentResolver.requestSync(account, MailProvider.AUTHORITY,
						messageBundle);
			}

		}
		if (OdooAccountManager.current_user.getAndroidName().equals(
				account.name)) {
			context.sendBroadcast(intent);
		}

	}

}
