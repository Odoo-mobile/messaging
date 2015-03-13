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
import android.database.MatrixCursor;
import android.net.Uri;

import com.odoo.base.addons.mail.MailMessage;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.provider.BaseModelProvider;
import com.odoo.core.utils.OCursorUtils;

import java.util.ArrayList;
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
            List<String> parent_list = new ArrayList<>();
            // Sorting messages by parent with latest child
            MailMessage mail = new MailMessage(getContext(), getUser(uri));
            Cursor cr = super.query(mail.uri(), base_projection, selection, selectionArgs, sortOrder);
            MatrixCursor data = new MatrixCursor(cr.getColumnNames());
            if (cr.moveToFirst()) {
                do {
                    int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
                    int _id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
                    int res_id = cr.getInt(cr.getColumnIndex("res_id"));
                    if (res_id != 0) {
                        // Record mail threads
                        Cursor recordThread = query(mail.uri(),
                                null, "res_id = ?", new String[]{res_id + ""}, "date desc");
                        if (recordThread.getCount() > 0) {
                            recordThread.moveToFirst();
                            String body = recordThread.getString(recordThread.getColumnIndex("short_body"));
                            String date = recordThread.getString(recordThread.getColumnIndex("date"));
                            recordThread.moveToLast();
                            int thread_id = recordThread.getInt(recordThread.getColumnIndex(OColumn.ROW_ID));
                            if (!parent_list.contains("parent_" + thread_id)) {
                                List<String> values = new ArrayList<>();
                                for (String col : recordThread.getColumnNames()) {
                                    if (col.equals("short_body")) {
                                        values.add(body);
                                    } else if (col.equals("date")) {
                                        values.add(date);
                                    } else {
                                        values.add(OCursorUtils.cursorValue(col, recordThread) + "");
                                    }
                                }
                                data.addRow(values);
                                parent_list.add("parent_" + thread_id);
                            }
                        }
                    } else {
                        // General mail threads
                        if (parent_id != 0 && !parent_list.contains("parent_" + parent_id)) {
                            // Child of message creating parent record
                            Cursor parent = query(Uri.withAppendedPath(mail.uri(), parent_id + "")
                                    , null, null, null, null);
                            if (parent.moveToFirst()) {
                                List<String> values = new ArrayList<>();
                                for (String col : parent.getColumnNames()) {
                                    if (col.equals("short_body") || col.equals("date")) {
                                        values.add(OCursorUtils.cursorValue(col, cr) + "");
                                    } else {
                                        values.add(OCursorUtils.cursorValue(col, parent) + "");
                                    }
                                }
                                data.addRow(values);
                            }
                            parent_list.add("parent_" + parent.getInt(parent.getColumnIndex(OColumn.ROW_ID)));
                        } else if (!parent_list.contains("parent_" + _id)) {
                            data.addRow(OCursorUtils.valuesToList(cr));
                            parent_list.add("parent_" + _id);
                        }
                    }
                } while (cr.moveToNext());
            }
            data.setNotificationUri(getContext().getContentResolver(), uri);
            return data;
        }
    }
}
