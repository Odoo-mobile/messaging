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
 * Created on 25/2/15 11:49 AM
 */
package com.odoo.base.addons.mail;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.odoo.addons.mail.models.MailNotification;
import com.odoo.addons.mail.providers.MailProvider;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.base.addons.res.ResUsers;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.StringUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import odoo.ODomain;

public class MailMessage extends OModel {
    public static final String TAG = MailMessage.class.getSimpleName();
    public static final String AUTHORITY = "com.odoo.messaging.base.addons.mail.mail_message";
    private Context mContext;
    private MailNotification notification;

    OColumn author_id = new OColumn("Author", ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn email_from = new OColumn("Email From", OVarchar.class).setDefaultValue("false");
    OColumn subject = new OColumn("Subject", OVarchar.class).setSize(100);
    OColumn body = new OColumn("Body", OText.class);
    OColumn date = new OColumn("Date", ODateTime.class);
    OColumn record_name = new OColumn("Record Name", OVarchar.class).setSize(150);
    OColumn model = new OColumn("Model", OVarchar.class).setDefaultValue("false");
    OColumn res_id = new OColumn("Resource Id", OInteger.class).setDefaultValue(0);
    OColumn attachment_ids = new OColumn("Attachments", IrAttachment.class,
            OColumn.RelationType.ManyToMany);
    OColumn type = new OColumn("Type", OVarchar.class);

    OColumn partner_ids = new OColumn("To", ResPartner.class, OColumn.RelationType.ManyToMany);
    OColumn notified_partner_ids = new OColumn("Notified partners", ResPartner.class,
            OColumn.RelationType.ManyToMany);
    OColumn parent_id = new OColumn("Parent", MailMessage.class, OColumn.RelationType.ManyToOne);
    OColumn child_ids = new OColumn("Child", MailMessage.class, OColumn.RelationType.OneToMany)
            .setRelatedColumn("parent_id");

    OColumn notification_ids = new OColumn("Notifications", MailNotification.class,
            OColumn.RelationType.OneToMany).setRelatedColumn("message_id");
    OColumn vote_user_ids = new OColumn("Voters", ResUsers.class, OColumn.RelationType.ManyToMany);

    @Odoo.Functional(method = "authorName", depends = {"author_id", "email_from"}, store = true)
    OColumn author_name = new OColumn("Author Name", OVarchar.class).setLocalColumn();

    @Odoo.Functional(method = "messageTitle", depends = {"record_name", "subject", "type"}, store = true)
    OColumn message_title = new OColumn("Title", OVarchar.class).setSize(100).setLocalColumn();
    @Odoo.Functional(method = "shortBody", depends = {"body"}, store = true)
    OColumn short_body = new OColumn("Short Body", OVarchar.class).setSize(200).setLocalColumn();

    @Odoo.Functional(method = "storeToRead", depends = {"notification_ids"}, store = true)
    OColumn to_read = new OColumn("To Read", OBoolean.class).setDefaultValue(true);

    @Odoo.Functional(method = "storeStarred", depends = {"notification_ids"}, store = true)
    OColumn starred = new OColumn("Starred", OBoolean.class).setDefaultValue(false);

    @Odoo.Functional(method = "storeToMe", depends = {"partner_ids"}, store = true)
    OColumn to_me = new OColumn("To Me", OBoolean.class).setDefaultValue(false).setLocalColumn();

    public MailMessage(Context context, OUser user) {
        super(context, "mail.message", user);
        mContext = context;
        notification = new MailNotification(context, user);
    }

    @Override
    public ODomain defaultDomain() {
        Integer partner_id = getUser().getPartner_id();
        ODomain domain = new ODomain();
        domain.add("|");
        domain.add("partner_ids", "in", new JSONArray().put(partner_id));
        domain.add("|");
        domain.add("notified_partner_ids", "in", new JSONArray().put(partner_id));
        domain.add("author_id", "=", partner_id);
        return domain;
    }

    @Override
    public boolean checkForWriteDate() {
        return false;
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    public Uri sortedUri() {
        return uri().buildUpon().appendPath(MailProvider.KEY_SORTED_MESSAGES).build();
    }

    public Boolean storeToRead(OValues vals) {
        try {
            JSONArray ids = (JSONArray) vals.get("notification_ids");
            if (ids.length() > 0) {
                ODataRow noti = notification.browse(notification.selectRowId(ids.getInt(0)));
                if (noti != null)
                    return (noti.contains("is_read")) ? !noti
                            .getBoolean("is_read") : !noti.getBoolean("read");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vals.getBoolean("to_read");
    }

    public Boolean storeStarred(OValues vals) {
        try {
            JSONArray ids = (JSONArray) vals.get("notification_ids");
            if (ids.length() > 0) {
                ODataRow noti = notification.browse(notification.selectRowId(ids.getInt(0)));
                if (noti != null)
                    return noti.getBoolean("starred");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vals.getBoolean("starred");
    }

    public Boolean storeToMe(OValues values) {
        try {
            JSONArray ids = (JSONArray) values.get("partner_ids");
            if (ids.length() > 0) {
                for (int i = 0; i < ids.length(); i++) {
                    if (ids.getInt(i) == getUser().getPartner_id()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String messageTitle(OValues row) {
        String title = "false";
        if (!row.getString("record_name").equals("false"))
            title = row.getString("record_name");
        if (!title.equals("false") && !row.getString("subject").equals("false"))
            title += ": " + row.getString("subject");
        if (title.equals("false") && !row.getString("subject").equals("false"))
            title = row.getString("subject");
        if (title.equals("false"))
            title = StringUtils.capitalizeString(row.getString("type"));

        return title;
    }

    public String shortBody(OValues row) {
        String body = StringUtils.htmlToString(row.getString("body"));
        int end = (body.length() > 100) ? 100 : body.length();
        return body.substring(0, end);
    }

    public String authorName(OValues values) {
        try {
            if (!values.getString("author_id").equals("false")) {
                JSONArray author_id = new JSONArray(values.getString("author_id"));
                return author_id.getString(1);
            } else {
                return values.getString("email_from");
            }
        } catch (Exception e) {

        }
        return "";
    }

    public String getAuthorImage(int author_id) {
        String image = "false";
        ResPartner partner = (ResPartner) createInstance(ResPartner.class);
        Cursor cr = mContext.getContentResolver().query(partner.uri().withAppendedPath(partner.uri(),
                        author_id + ""),
                new String[]{"image_small"}, null, null, null);
        if (cr.moveToFirst()) {
            image = cr.getString(cr.getColumnIndex("image_small"));
        }
        cr.close();
        return image;
    }

    public List<Integer> getServerIds(String model, int res_server_id) {
        List<Integer> ids = new ArrayList<>();
        for (ODataRow row : select(new String[]{}, "model = ? and res_id = ?",
                new String[]{model, res_server_id + ""})) {
            ids.add(row.getInt("id"));
        }
        return ids;
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

}
