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
 * Created on 12/3/15 5:17 PM
 */
package com.odoo.addons.mail.providers;

import android.database.Cursor;
import android.net.Uri;

import com.odoo.base.addons.mail.MailMessage;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.provider.BaseModelProvider;
import com.odoo.core.utils.OCursorUtils;
import com.odoo.core.utils.OListUtils;
import com.odoo.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MailProvider extends BaseModelProvider {
    public static final String TAG = MailProvider.class.getSimpleName();
    public static final int SORTED_MESSAGES = 234;
    public static final String KEY_SORTED_MESSAGES = "sorted_messages";

    @Override
    public boolean onCreate() {
        String path = new MailMessage(getContext(), null).getModelName()
                .toLowerCase(Locale.getDefault());
        matcher.addURI(authority(), path + "/" + KEY_SORTED_MESSAGES, SORTED_MESSAGES);
        return super.onCreate();
    }

    @Override
    public void setModel(Uri uri) {
        super.setModel(uri);
        mModel = new MailMessage(getContext(), getUser(uri));
    }

    @Override
    public String authority() {
        return MailMessage.AUTHORITY;
    }

    @Override
    public Cursor query(Uri uri, String[] base_projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        int match = matcher.match(uri);
        if (match != SORTED_MESSAGES)
            return super.query(uri, base_projection, selection, selectionArgs, sortOrder);
        else {
            // Sorting messages by parent with latest child
            MailMessage mail = new MailMessage(getContext(), getUser(uri));

            String query = "SELECT max(" + OColumn.ROW_ID + ") as id, res_id, model from " + mail.getTableName();
            query += " where " + selection;
            query += " group by model, res_id having model != 'false' and model !='mail.group'";
            HashSet<Integer> recordIds = new HashSet<>();
            Cursor idCR = mail.executeQuery(query, selectionArgs);
            if (idCR.moveToFirst()) {
                do {
                    ODataRow idRow = OCursorUtils.toDatarow(idCR);
                    int message_id = idRow.getInt("id");
                    recordIds.add(message_id);
                } while (idCR.moveToNext());
            }
            query = "SELECT max(" + OColumn.ROW_ID + ") as id, parent_id from " + mail.getTableName();
            query += " where " + selection + " and parent_id != '0'";
            query += " group by parent_id, res_id having model ='false' or model = 'mail.group'";

            idCR = mail.executeQuery(query, selectionArgs);
            List<Integer> parent_ids = new ArrayList<>();
            if (idCR.moveToFirst()) {
                do {
                    ODataRow idRow = OCursorUtils.toDatarow(idCR);
                    int message_id = idRow.getInt("id");
                    parent_ids.add(idRow.getInt("parent_id"));
                    recordIds.add(message_id);
                } while (idCR.moveToNext());
            }
            query = "SELECT max(" + OColumn.ROW_ID + ") as id, parent_id from " + mail.getTableName();
            query += " where " + selection + " and " + OColumn.ROW_ID + " not in(" +
                    StringUtils.repeat("?, ", parent_ids.size() - 1) + " ?)";
            query += " and parent_id = '0' group by id, parent_id ";
            List<String> parentArgs = new ArrayList<>();
            parentArgs.addAll(Arrays.asList(selectionArgs));
            parentArgs.addAll(Arrays.asList(OListUtils.toStringList(
                    new ArrayList<Integer>(parent_ids)).toArray(new String[parent_ids.size()])));
            idCR = mail.executeQuery(query, parentArgs.toArray(new String[parentArgs.size()]));
//            idCR = mail.executeQuery(query, selectionArgs);
            if (idCR.moveToFirst()) {
                do {
                    ODataRow idRow = OCursorUtils.toDatarow(idCR);
                    int message_id = idRow.getInt("id");
                    recordIds.add(message_id);
                } while (idCR.moveToNext());
            }

            selection = "" + OColumn.ROW_ID + " IN (" + StringUtils.repeat("?, ", recordIds.size() - 1) + " ?)";
            List<String> arguments = new ArrayList<>();
//            arguments.addAll(Arrays.asList(selectionArgs));
            arguments.addAll(Arrays.asList(OListUtils.toStringList(new ArrayList<>(recordIds))
                    .toArray(new String[recordIds.size()])));
            return super.query(mail.uri(), base_projection, selection, arguments.toArray(new String[arguments.size()])
                    , sortOrder);
//            MatrixCursor data = new MatrixCursor(cr.getColumnNames());
//            if (cr.moveToFirst()) {
//                do {
//                    int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
//                    int _id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
//                    int res_id = cr.getInt(cr.getColumnIndex("res_id"));
//                    String model = cr.getString(cr.getColumnIndex("model"));
//                    // General mail threads
//                    if (parent_id != 0 && !parent_list.contains("parent_" + parent_id)) {
////                        OLog.log(">>> #1" + model + " : " + parent_id);
//                        // Child of message creating parent record
//                        Cursor parent = query(Uri.withAppendedPath(mail.uri(), parent_id + "")
//                                , null, null, null, null);
//                        if (parent.moveToFirst()) {
//                            List<String> values = new ArrayList<>();
//                            for (String col : parent.getColumnNames()) {
//                                if (col.equals("short_body") || col.equals("date")) {
//                                    values.add(OCursorUtils.cursorValue(col, cr) + "");
//                                } else {
//                                    values.add(OCursorUtils.cursorValue(col, parent) + "");
//                                }
//                            }
//                            data.addRow(values);
//                        }
//                        parent_list.add("parent_" + parent.getInt(parent.getColumnIndex(OColumn.ROW_ID)));
////                    } else if (res_id != 0) {
////                        OLog.log(">>> #2 with res id and model" + model + " : " + res_id);
////                        // Checks for chatter message thread
////                        Cursor chatterCR = query(mail.uri(), null, "res_id = ? and model = ?",
////                                new String[]{res_id + "", model}, "date desc");
////                        chatterCR.moveToFirst();
////                        int mail_id = chatterCR.getInt(chatterCR.getColumnIndex(OColumn.ROW_ID));
////                        if (!parent_list.contains("parent_" + mail_id)) {
////                            data.addRow(OCursorUtils.valuesToList(chatterCR));
////                            parent_list.add("parent_" + mail_id);
////                        }
////                    } else if
//                    } else if (!parent_list.contains("parent_" + _id)) {
//                        data.addRow(OCursorUtils.valuesToList(cr));
//                        parent_list.add("parent_" + _id);
//                    }
//                } while (cr.moveToNext());
//            }
//            data.setNotificationUri(getContext().getContentResolver(), uri);
//            return data;
        }
    }
}
