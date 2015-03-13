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
 * Created on 1/1/15 12:39 PM
 */
package com.odoo.core.utils;

import android.database.Cursor;

import com.odoo.core.orm.ODataRow;

import java.util.ArrayList;
import java.util.List;

public class OCursorUtils {
    public static final String TAG = OCursorUtils.class.getSimpleName();

    public static ODataRow toDatarow(Cursor cr) {
        ODataRow row = new ODataRow();
        for (String col : cr.getColumnNames()) {
            row.put(col, OCursorUtils.cursorValue(col, cr));
        }
        return row;
    }

    public static List<String> valuesToList(ODataRow row) {
        List<String> values = new ArrayList<>();
        for (String col : row.keys()) {
            values.add(row.getString(col));
        }
        return values;
    }

    public static List<String> valuesToList(Cursor cr) {
        List<String> values = new ArrayList<>();
        for (String col : cr.getColumnNames()) {
            values.add(OCursorUtils.cursorValue(col, cr) + "");
        }
        return values;
    }

    public static Object cursorValue(String column, Cursor cr) {
        Object value = false;
        int index = cr.getColumnIndex(column);
        switch (cr.getType(index)) {
            case Cursor.FIELD_TYPE_NULL:
                value = false;
                break;
            case Cursor.FIELD_TYPE_STRING:
                value = cr.getString(index);
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                value = cr.getInt(index);
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                value = cr.getFloat(index);
                break;
            case Cursor.FIELD_TYPE_BLOB:
                value = cr.getBlob(index);
                break;
        }
        return value;
    }
}
