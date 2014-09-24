package com.odoo.addons.mail.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.ODataRow;
import com.odoo.support.OUser;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.util.logger.OLog;
import com.odoo.widgets.WidgetHelper;
import com.openerp.R;

public class MailRemoteViewFactory implements RemoteViewsFactory {

	public static final String TAG = "com.odoo.addons.mail.widgets.MailRemoteViewFactory";
	private Context mContext = null;
	private int mAppWidgetId = -1;

	private int[] starred_drawables = new int[] { R.drawable.ic_action_starred };
	private List<Object> mMailListItems = new ArrayList<Object>();
	private OCursorListAdapter mAdapter = null;
	String mFilter = "inbox";

	public MailRemoteViewFactory(Context context, Intent intent) {
		Log.d(TAG, "MessageRemoteViewFactory->constructor()");
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		mFilter = intent.getExtras().getString(
				AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
	}

	@Override
	public int getCount() {
		return mMailListItems.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public RemoteViews getLoadingView() {
		RemoteViews mView = new RemoteViews(mContext.getPackageName(),
				R.layout.listview_data_loading_progress);
		return mView;
	}

	@Override
	public RemoteViews getViewAt(int position) {
		Log.d(TAG, "getViewAt()");

		RemoteViews mView = new RemoteViews(mContext.getPackageName(),
				R.layout.widget_mail_item_layout);

		// ODataRow row = (ODataRow) mMailListItems.get(position);
		OLog.log("position = " + position);

		Cursor c = (Cursor) mMailListItems.get(position);
		// String to_read = row.getString("to_read");
		// int to_read = c.getInt(c.getColumnIndex("to_read"));
		// if (to_read == 1) {
		// mView.setTextColor(R.id.txvMessageSubject, Color.BLACK);
		// mView.setTextColor(R.id.txvMessageFrom, Color.BLACK);
		// } else {
		// mView.setTextColor(R.id.txvMessageSubject,
		// Color.parseColor("#414141"));
		// mView.setTextColor(R.id.txvMessageFrom, Color.parseColor("#414141"));
		// }
		// // String starred = row.getString("starred");
		// int starred = c.getInt(c.getColumnIndex("starred"));
		// if (starred == 1)
		// mView.setImageViewResource(R.id.imgMessageStarred,
		// starred_drawables[0]); // Color.parseColor("#FF8800")
		// else
		// mView.setImageViewResource(R.id.imgMessageStarred,
		// starred_drawables[0]); // Color.parseColor("#aaaaaa")
		//
		// // String subject = row.getString("subject");
		// String subject = c.getString(c.getColumnIndex("subject"));
		// if (subject.equals("false")) {
		// // subject = row.getString("type");
		// subject = c.getString(c.getColumnIndex("type"));
		// }
		// // if (!row.getString("record_name").equals("false"))
		// if (!c.getString(c.getColumnIndex("record_name")).equals("false"))
		// subject = c.getString(c.getColumnIndex("record_name"));
		// // subject = row.getString("record_name");
		// mView.setTextViewText(R.id.txvMessageSubject, subject);
		//
		// // if (row.getInt("childs") > 0) {
		// if (c.getInt(c.getColumnIndex("childs")) > 0) {
		// mView.setViewVisibility(R.id.txvChilds, View.VISIBLE);
		// // mView.setTextViewText(R.id.txvChilds, row.getString("childs")
		// // + " reply");
		// mView.setTextViewText(R.id.txvChilds,
		// c.getString(c.getColumnIndex("childs")) + " reply");
		// } else
		// mView.setViewVisibility(R.id.txvChilds, View.GONE);
		//
		// // mView.setTextViewText(R.id.txvMessageBody,
		// // HTMLHelper.htmlToString(row.getString("body")));
		// // String date = row.getString("date");
		// mView.setTextViewText(R.id.txvMessageBody,
		// HTMLHelper.htmlToString(c.getString(c.getColumnIndex("body"))));
		// String date = c.getString(c.getColumnIndex("date"));
		// mView.setTextViewText(R.id.txvMessageDate, ODate.getDate(mContext,
		// date, TimeZone.getDefault().getID(), "MMM dd,  hh:mm a"));
		// mView.setTextColor(R.id.txvMessageDate, Color.parseColor("#414141"));
		//
		// // String from = row.getString("email_from");
		// // if (from.equals("false")) {
		// // ODataRow author_id = row.getM2ORecord("author_id").browse();
		// // if (author_id != null)
		// // from = row.getM2ORecord("author_id").browse().getString("name");
		// // }
		// String from = c.getString(c.getColumnIndex("email_from"));
		// // if (from.equals("false")) {
		// // ODataRow author_id =
		// // c.getM2ORecord(c.getColumnIndex("author_id")).browse();
		// // if (author_id != null)
		// // from =
		// //
		// c.getM2ORecord(c.getColumnIndex("author_id")).browse().getString("name");
		// // }
		// mView.setTextViewText(R.id.txvMessageFrom, from);
		// // mView.setViewVisibility(R.id.txvMessageTag, View.GONE);

		final Intent fillInIntent = new Intent();
		fillInIntent.setAction(MailWidget.ACTION_MESSAGE_WIDGET_CALL);
		final Bundle bundle = new Bundle();
		// bundle.putInt(WidgetHelper.EXTRA_WIDGET_DATA_VALUE,
		// row.getInt("id"));
		bundle.putInt(WidgetHelper.EXTRA_WIDGET_DATA_VALUE,
				c.getInt(c.getColumnIndex("id")));
		fillInIntent.putExtras(bundle);
		mView.setOnClickFillInIntent(R.id.messageListViewItem, fillInIntent);
		return mView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onCreate() {
		if (OUser.current(mContext) == null)
			return;
		mMailListItems.clear();
		HashMap<String, ODataRow> parents = new HashMap<String, ODataRow>();
		MailMessage message = new MailMessage(mContext);
		String where = "to_read = ? AND starred = ? AND parent_id = ?";
		String[] whereArgs = new String[] { "1", "0", "0" };
		// HashMap<String, Object> whereMap = getWhere(mFilter);
		// String where = (String) whereMap.get("where");
		// String[] whereArgs = (String[]) whereMap.get("whereArgs");
		// List<ODataRow> messages = message.select(where, whereArgs, null,
		// null,
		// "date DESC");
		Cursor c = mContext.getContentResolver().query(
				message.uri(),
				new String[] { "message_title", "author_name",
						"author_id.image_small", "total_childs", "parent_id",
						"date", "to_read", "body", "starred" }, where,
				whereArgs, null);
		OLog.log("cursor" + c.toString());
		// ODataRow newRow = null;
		while (c.moveToNext()) {
			// OLog.log(c.getString(c.getColumnIndex("body")));
			// OLog.log(c.getString(c.getColumnIndex("subject")));
			// OLog.log(c.getInt(c.getColumnIndex("type")) + "");
			// OLog.log(c.getString(c.getColumnIndex("message_title")));
			// newRow = c.toString();
			mMailListItems.add(c);
		}
		OLog.log("List items " + mMailListItems.toString());
		// SQLiteDatabase db = message.getReadableDatabase();
		// ContentResolver cr=
		// OLog.log("Cursor == "+c);
		// for (ODataRow row : messages) {
		// boolean isParent = true;
		// // Get parent id of the Mail
		// ODataRow rows = row.getM2ORecord("parent_id").browse();
		// int key = 0;
		// if (rows != null)
		// key = rows.getInt("id");
		// if (key != 0) {
		// isParent = false;
		// }
		// if (!parents.containsKey(key)) {
		// // Fetching row parent message
		// ODataRow newRow = null;
		//
		// if (isParent) {
		// newRow = row;
		// } else {
		// newRow = message.select(key);
		// }
		// int childs = message.count("parent_id = ? ", new String[] { key
		// + "" });
		// newRow.put("childs", childs);
		// parents.put(key + "", null);
		// mMailListItems.add(newRow);
		// }
		// }

	}

	// public HashMap<String, Object> getWhere(String type) {
	// HashMap<String, Object> map = new HashMap<String, Object>();
	// String where = null;
	// String[] whereArgs = null;
	// if (type != null) {
	// if (type.equals("inbox")) {
	// where = "to_read = ? AND starred = ?";
	// whereArgs = new String[] { "1", "0" };
	// }
	// if (type.equals("to-do")) {
	// where = "to_read = ? AND starred = ?";
	// whereArgs = new String[] { "1", "1" };
	// }
	// if (type.equals("to:me")) {
	// where = "res_id = ? AND to_read = ?";
	// whereArgs = new String[] { "0", "1" };
	// }
	// }
	// map.put("where", where);
	// map.put("whereArgs", whereArgs);
	// return map;
	// }

	@Override
	public void onDataSetChanged() {
		onCreate();
	}

	@Override
	public void onDestroy() {
	}
}
