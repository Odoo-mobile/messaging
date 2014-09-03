package com.odoo.support.listview;

import odoo.controls.OForm;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.orm.ODataRow;

public class OCursorListAdapter extends CursorAdapter {

	private Integer mLayout = null;
	private LayoutInflater mInflater = null;
	private OnViewCreateListener mOnViewCreateListener = null;

	public OCursorListAdapter(Context context, Cursor c, int layout) {
		super(context, c, false);
		mLayout = layout;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final ODataRow row = new ODataRow();
		for (String col : cursor.getColumnNames()) {
			row.put(col, getValue(cursor, col));
		}
		if (mOnViewCreateListener != null) {
			mOnViewCreateListener.onViewCreated(view, cursor,
					cursor.getPosition());
		}
		OForm form = (OForm) view;
		form.initForm(row);
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup) {
		getCursor().moveToPosition(position);
		Cursor cursor = getCursor();
		view = newView(mContext, cursor, (ViewGroup) view);
		return super.getView(position, view, viewGroup);
	}

	private Object getValue(Cursor c, String column) {
		Object value = false;
		int index = c.getColumnIndex(column);
		switch (c.getType(index)) {
		case Cursor.FIELD_TYPE_NULL:
			value = false;
			break;
		case Cursor.FIELD_TYPE_BLOB:
		case Cursor.FIELD_TYPE_STRING:
			value = c.getString(index);
			break;
		case Cursor.FIELD_TYPE_FLOAT:
			value = c.getFloat(index);
			break;
		case Cursor.FIELD_TYPE_INTEGER:
			value = c.getInt(index);
			break;
		}
		return value;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		return mInflater.inflate(mLayout, viewGroup, false);
	}

	public int getResource() {
		return mLayout;
	}

	public View inflate(int resource, ViewGroup viewGroup) {
		return mInflater.inflate(resource, viewGroup, false);
	}

	public void setOnViewCreateListener(OnViewCreateListener viewCreateListener) {
		mOnViewCreateListener = viewCreateListener;
	}

	public interface OnViewCreateListener {
		public void onViewCreated(View view, Cursor cr, int position);
	}
}
