/**
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
 * Created on 12/3/15 5:18 PM
 */
package com.odoo.addons.mail.services;

import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.odoo.addons.groups.Groups;
import com.odoo.addons.mail.models.MailNotification;
import com.odoo.base.addons.mail.MailMessage;
import com.odoo.core.service.ISyncFinishListener;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;

import odoo.ODomain;

public class MailSyncService extends OSyncService implements ISyncFinishListener {
    public static final String TAG = MailSyncService.class.getSimpleName();
    public static final String KEY_FILTER_TOREAD = "filter_to_read";
    public static final String KEY_FILTER_STARRED = "filter_starred";
    public static final String KEY_FILTER_GROUP = "filter_group";
    public static final String KEY_FILTER_TOME = "filter_tome";
    private Bundle mailBundle = null;

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, MailMessage.class, this, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        if (adapter.getModel().getModelName().equals("mail.message")) {
            mailBundle = extras;
            adapter.syncDataLimit(30);
            if (extras != null) {
                ODomain domain = new ODomain();
                if (extras.containsKey(KEY_FILTER_TOREAD)) {
                    domain.add("to_read", "=", true);
                }
                if (extras.containsKey(KEY_FILTER_TOME)) {
                    domain.add("partner_ids", "in", user.getPartner_id());
                }
                if (extras.containsKey(KEY_FILTER_STARRED)) {
                    domain.add("starred", "=", true);
                }
                if (extras.containsKey(KEY_FILTER_GROUP)) {
                    domain.add("res_id", "=", extras.getInt(Groups.KEY_GROUP_ID));
                    domain.add("model", "=", "mail.group");
                }
                adapter.setDomain(domain);
            }
            adapter.onSyncFinish(this);
        } else {
            // Mail notification domain
            MailMessage mails = new MailMessage(getApplicationContext(), user);
            ODomain domain = new ODomain();
            domain.add("partner_id", "=", user.getPartner_id());
            if (mailBundle != null) {
                if (mailBundle.containsKey(KEY_FILTER_TOREAD)) {
                    domain.add("message_id", "in", mails.getServerIds(
                            "to_read = ?", new String[]{"true"}
                    ));
                }
                if (mailBundle.containsKey(KEY_FILTER_TOME)) {
                    domain.add("partner_id", "=", user.getCompany_id());
                }
                if (mailBundle.containsKey(KEY_FILTER_STARRED)) {
                    domain.add("message_id", "in", mails.getServerIds(
                            "starred = ?", new String[]{"true"}
                    ));
                }
                if (mailBundle.containsKey(KEY_FILTER_GROUP)) {
                    domain.add("message_id.res_id", "=", mailBundle.getInt(Groups.KEY_GROUP_ID));
                    domain.add("message_id.model", "=", "mail.group");
                }
            }
            adapter.setDomain(domain);
        }
    }

    @Override
    public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
        return new OSyncAdapter(getApplicationContext(), MailNotification.class, this, true);
    }
}
