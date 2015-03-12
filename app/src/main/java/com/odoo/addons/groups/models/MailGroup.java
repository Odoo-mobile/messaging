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
 * Created on 12/3/15 2:39 PM
 */
package com.odoo.addons.groups.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.mail.models.MailFollowers;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBlob;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.JSONUtils;

import org.json.JSONArray;

import java.util.List;

public class MailGroup extends OModel {
    public static final String TAG = MailGroup.class.getSimpleName();
    public static final String AUTHORITY = "com.odoo.addons.groups.models.mail_group";
    OColumn name = new OColumn("Name", OVarchar.class);
    OColumn image = new OColumn("Image", OBlob.class).setDefaultValue("false");
    OColumn description = new OColumn("Description", OText.class);
    OColumn message_follower_ids = new OColumn("Followers", MailFollowers.class,
            OColumn.RelationType.ManyToMany);

    @Odoo.Functional(method = "hasFollowed", depends = {"message_follower_ids"}, store = true)
    OColumn has_followed = new OColumn("Followed", OBoolean.class)
            .setDefaultValue("false").setLocalColumn();

    public MailGroup(Context context, OUser user) {
        super(context, "mail.group", user);
    }

    public String hasFollowed(OValues vals) {
        List<Integer> ids = JSONUtils.toList((JSONArray) vals
                .get("message_follower_ids"));
        if (ids.indexOf(getUser().getPartner_id()) > -1) {
            return "true";
        }
        return "false";
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
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
