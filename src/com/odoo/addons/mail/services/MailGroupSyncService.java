package com.odoo.addons.mail.services;

import android.os.Bundle;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.support.OUser;
import com.odoo.support.service.OSyncAdapter;
import com.odoo.support.service.OSyncService;
import com.odoo.util.logger.OLog;

public class MailGroupSyncService extends OSyncService {
	public static final String TAG = MailGroupSyncService.class.getSimpleName();

	public static final String KEY_GROUP_IDS = "group_ids";

	@Override
	public OSyncAdapter getSyncAdapter() {
		return new OSyncAdapter(getApplicationContext(), new MailGroup(
				getApplicationContext()), this, true).syncDataLimit(30);
	}

	@Override
	public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
		OLog.log("Perform sync Groups");
	}

	// @Override
	// public Service getService() {
	// return this;
	// }
	//
	// @Override
	// public void performSync(Context context, OUser user, Account account,
	// Bundle extras, String authority, ContentProviderClient provider,
	// SyncResult syncResult) {
	// Intent intent = new Intent();
	// intent.setAction(SyncFinishReceiver.SYNC_FINISH);
	// MailGroup mailGroup = new MailGroup(context);
	// mailGroup.setUser(user);
	// if (mailGroup.getSyncHelper().syncWithServer()) {
	//
	// MailFollowers follower = new MailFollowers(context);
	// ODomain domain = new ODomain();
	// domain.add("partner_id", "=", user.getPartner_id());
	// domain.add("res_model", "=", mailGroup.getModelName());
	// if (follower.getSyncHelper().syncWithServer(domain, false)) {
	// JSONArray group_ids = new JSONArray();
	// for (ODataRow grp : follower.select(
	// "res_model = ? AND partner_id = ?", new Object[] {
	// mailGroup.getModelName(),
	// user.getPartner_id() + "" })) {
	// group_ids.put(grp.getInt("id"));
	// }
	// Bundle messageBundle = new Bundle();
	// messageBundle.putString(KEY_GROUP_IDS, group_ids.toString());
	// messageBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL,
	// true);
	// messageBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED,
	// true);
	// ContentResolver.requestSync(account, MailProvider.AUTHORITY,
	// messageBundle);
	// }
	// }
	// if (OUser.current(context).getAndroidName().equals(account.name)) {
	// context.sendBroadcast(intent);
	// }
	//
	// }

}
