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
 * Created on 12/3/15 5:03 PM
 */
package com.odoo.addons.mail.models;

import android.content.Context;

import com.odoo.base.addons.mail.MailMessage;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.support.OUser;

import odoo.ODomain;

public class MailNotification extends OModel {
    public static final String TAG = MailNotification.class.getSimpleName();

    @Odoo.api.v7
    OColumn read = new OColumn("Read", OBoolean.class).setDefaultValue(false);

    @Odoo.api.v8
    @Odoo.api.v9alpha
    OColumn is_read = new OColumn("Is Read", OBoolean.class).setDefaultValue(true);

    OColumn starred = new OColumn("Starred", OBoolean.class).setDefaultValue(false);
    OColumn partner_id = new OColumn("Partner", ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn message_id = new OColumn("Message", MailMessage.class, OColumn.RelationType.ManyToOne);


    public MailNotification(Context context, OUser user) {
        super(context, "mail.notification", user);
        makeCreateWriteDateLocal();
    }

    @Override
    public ODomain defaultDomain() {
        ODomain domain = new ODomain();
        domain.add("partner_id", "=", getUser().getPartner_id());
        return domain;
    }

    @Override
    public boolean allowCreateRecordOnServer() {
        return false;
    }

    @Override
    public boolean allowDeleteRecordOnServer() {
        return false;
    }

    @Override
    public boolean allowUpdateRecordOnServer() {
        return false;
    }

    @Override
    public boolean checkForCreateDate() {
        return false;
    }

    @Override
    public boolean checkForWriteDate() {
        return false;
    }
}
