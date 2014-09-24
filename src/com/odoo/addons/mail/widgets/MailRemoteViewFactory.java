package com.odoo.addons.mail.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.ODataRow;
import com.odoo.support.OUser;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.util.HTMLHelper;
import com.odoo.util.ODate;
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

		ODataRow row = (ODataRow) mMailListItems.get(position);

		String to_read = row.getString("to_read");

		if (to_read.equals("1")) {
			mView.setTextColor(R.id.txvMessageSubject, Color.BLACK);
			mView.setTextColor(R.id.txvMessageFrom, Color.BLACK);
		} else {
			mView.setTextColor(R.id.txvMessageSubject,
					Color.parseColor("#414141"));
			mView.setTextColor(R.id.txvMessageFrom, Color.parseColor("#414141"));
		}
		String starred = row.getString("starred");
		if (starred.equals("1"))
			mView.setImageViewResource(R.id.imgMessageStarred,
					starred_drawables[0]); // Color.parseColor("#FF8800")
		else
			mView.setImageViewResource(R.id.imgMessageStarred,
					starred_drawables[0]); // Color.parseColor("#aaaaaa")

		String subject = row.getString("subject");
		if (subject.equals("false")) {
			subject = row.getString("type");
		}
		if (!row.getString("record_name").equals("false"))
			subject = row.getString("record_name");
		mView.setTextViewText(R.id.txvMessageSubject, subject);

		if (row.getInt("childs") > 0) {
			mView.setViewVisibility(R.id.txvChilds, View.VISIBLE);
			mView.setTextViewText(R.id.txvChilds, row.getString("childs")
					+ " reply");
		} else
			mView.setViewVisibility(R.id.txvChilds, View.GONE);

		mView.setTextViewText(R.id.txvMessageBody,
				HTMLHelper.htmlToString(row.getString("body")));
		String date = row.getString("date");
		mView.setTextViewText(R.id.txvMessageDate, ODate.getDate(mContext,
				date, TimeZone.getDefault().getID(), "MMM dd,  hh:mm a"));
		mView.setTextColor(R.id.txvMessageDate, Color.parseColor("#414141"));

		String from = row.getString("email_from");
		if (from.equals("false")) {
			ODataRow author_id = row.getM2ORecord("author_id").browse();
			if (author_id != null)
				from = row.getM2ORecord("author_id").browse().getString("name");
		}

		mView.setTextViewText(R.id.txvMessageFrom, from);
		// mView.setViewVisibility(R.id.txvMessageTag, View.GONE);

		final Intent fillInIntent = new Intent();
		fillInIntent.setAction(MailWidget.ACTION_MESSAGE_WIDGET_CALL);
		final Bundle bundle = new Bundle();
		bundle.putInt(WidgetHelper.EXTRA_WIDGET_DATA_VALUE, row.getInt("id"));
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
		HashMap<String, Object> whereMap = getWhere(mFilter);
		String where = (String) whereMap.get("where");
		String[] whereArgs = (String[]) whereMap.get("whereArgs");
		List<ODataRow> messages = message.select(where, whereArgs, null, null,
				"date DESC");
		for (ODataRow row : messages) {
			boolean isParent = true;
			// Get parent id of the Mail
			ODataRow rows = row.getM2ORecord("parent_id").browse();
			int key = 0;
			if (rows != null)
				key = rows.getInt("id");
			if (key != 0) {
				isParent = false;
			}
			// if (key == 0) {
			// key = row.getInt("id");
			// } else {
			// isParent = false;
			// }
			if (!parents.containsKey(key)) {
				// Fetching row parent message
				ODataRow newRow = null;

				if (isParent) {
					newRow = row;
				} else {
					newRow = message.select(key);
				}
				int childs = message.count("parent_id = ? ", new String[] { key
						+ "" });
				newRow.put("childs", childs);
				parents.put(key + "", null);
				mMailListItems.add(newRow);
			}
		}

	}

	public HashMap<String, Object> getWhere(String type) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		String where = null;
		String[] whereArgs = null;
		if (type != null) {
			if (type.equals("inbox")) {
				where = "to_read = ? AND starred = ?";
				whereArgs = new String[] { "1", "0" }; // true , false
			}
			if (type.equals("to-do")) {
				where = "to_read = ? AND starred = ?";
				whereArgs = new String[] { "1", "1" }; // true , true
			}
			if (type.equals("to:me")) {
				where = "res_id = ? AND to_read = ?";
				whereArgs = new String[] { "0", "1" }; // 0 , true
			}
		}
		map.put("where", where);
		map.put("whereArgs", whereArgs);
		return map;
	}

	@Override
	public void onDataSetChanged() {
		onCreate();
	}

	@Override
	public void onDestroy() {
	}
}
