/*
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.odoo.base.res.services;

import odoo.ODomain;
import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.odoo.base.res.ResPartner;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.OUser;
import com.odoo.support.contact.OContact;
import com.odoo.support.service.OService;

public class ContactSyncService extends OService {

	public static final String TAG = "com.odoo.base.res.services.ContactSyncService";

	@Override
	public void performSync(Context context, OUser user, Account account,
			Bundle extras, String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		try {
			ResPartner db = new ResPartner(context);
			OSyncHelper odoo = db.getSyncHelper();
			OContact contact = new OContact(context, user);
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(context);
			boolean syncServerContacts = settings.getBoolean(
					"server_contact_sync", false);
			if (syncServerContacts && odoo != null) {
				Log.v(TAG, "Contact sync with server");
				int company_id = Integer.parseInt(user.getCompany_id());
				ODomain domain = new ODomain();
				domain.add("company_id", "=", company_id);
				odoo.syncWithServer(domain);
			}
			contact.createContacts(db.select());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Service getService() {
		return this;
	}
}
