package com.odoo.addons.mail.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.odoo.addons.mail.Mail;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.OColumn;
import com.odoo.support.OUser;
import com.odoo.util.Base64Helper;
import com.odoo.util.ODate;
import com.odoo.widgets.WidgetHelper;
import com.odoo.R;

public class MailRemoteViewFactory implements RemoteViewsFactory {

	public static final String TAG = "com.odoo.addons.mail.widgets.MailRemoteViewFactory";
	private Context mContext = null;
	private Cursor mCursor;
	private String mFilter = "";
	private String selection;
	private String[] args;
	private int[] background_resources = new int[] { Color.WHITE,
			Color.parseColor("#ebebeb") };

	@SuppressLint("InlinedApi")
	public MailRemoteViewFactory(Context context, Intent intent) {
		Log.d(TAG, "MessageRemoteViewFactory->constructor()");
		mContext = context;
		mFilter = intent.getExtras().getString(
				AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
	}

	@Override
	public int getCount() {
		return mCursor.getCount();
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
		RemoteViews mView = new RemoteViews(mContext.getPackageName(),
				R.layout.widget_mail_item_layout);
		mCursor.moveToPosition(position);
		// Updating views
		int to_read = mCursor.getInt(mCursor.getColumnIndex("to_read"));
		mView.setInt(R.id.mail_row, "setBackgroundColor",
				background_resources[to_read]);

		int is_fav = mCursor.getInt(mCursor.getColumnIndex("starred"));
		mView.setInt(
				R.id.img_starred_mlist,
				"setColorFilter",
				(is_fav == 1) ? Color.parseColor("#FF8800") : Color
						.parseColor("#aaaaaa"));

		int replies = Integer.parseInt(mCursor.getString(mCursor
				.getColumnIndex("total_childs")));
		String childs = "";
		if (replies > 0) {
			childs = replies + " replies";
		}
		mView.setTextViewText(R.id.total_childs, childs);
		mView.setTextViewText(R.id.message_title,
				mCursor.getString(mCursor.getColumnIndex("message_title")));
		mView.setTextViewText(R.id.author_name,
				mCursor.getString(mCursor.getColumnIndex("author_name")));
		mView.setTextViewText(R.id.message_short_body,
				mCursor.getString(mCursor.getColumnIndex("short_body")));
		mView.setTextViewText(R.id.mail_date, ODate.getDate(mContext, mCursor
				.getString(mCursor.getColumnIndex("date")), TimeZone
				.getDefault().getID(), "MMM dd"));
		String base64 = mCursor.getString(mCursor
				.getColumnIndex("author_id_image_small"));
		Bitmap bitmap = null;
		if (!base64.equals("false")) {
			bitmap = Base64Helper.getBitmapImage(mContext, base64);
		} else {
			bitmap = BitmapFactory.decodeResource(mContext.getResources(),
					R.drawable.avatar);
		}
		bitmap = Base64Helper.getRoundedCornerBitmap(mContext, bitmap, true);
		mView.setBitmap(R.id.author_image, "setImageBitmap", bitmap);

		// onClick intent call
		final Intent fillInIntent = new Intent();
		fillInIntent.setAction(MailWidget.ACTION_MESSAGE_WIDGET_CALL);
		final Bundle bundle = new Bundle();
		bundle.putInt(WidgetHelper.EXTRA_WIDGET_DATA_VALUE,
				mCursor.getInt(mCursor.getColumnIndex(OColumn.ROW_ID)));
		fillInIntent.putExtras(bundle);
		mView.setOnClickFillInIntent(R.id.mail_row_clickable, fillInIntent);
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
		MailMessage message = new MailMessage(mContext);
		Mail.Type mType = Mail.Type.Inbox;
		if (mFilter.equals(MailWidgetConfigure.KEY_TOME)) {
			mType = Mail.Type.ToMe;
		}
		if (mFilter.equals(MailWidgetConfigure.KEY_TODO)) {
			mType = Mail.Type.ToDo;
		}
		if (mFilter.equals(MailWidgetConfigure.KEY_ARCHIVE)) {
			mType = Mail.Type.Archives;
		}
		createSelection(mType);
		mCursor = mContext.getContentResolver().query(
				message.mailUri(),
				new String[] { "message_title", "author_name", "parent_id",
						"author_id.image_small", "total_childs", "date",
						"to_read", "short_body", "starred" }, selection, args,
				null);

	}

	private void createSelection(Mail.Type mType) {
		selection = " ";
		List<String> argsList = new ArrayList<String>();
		switch (mType) {
		case Inbox:
			selection += " to_read = ? and starred = ? and id != ?";
			argsList.add("1");
			argsList.add("0");
			argsList.add("0");
			break;
		case ToMe:
			selection += " to_read = ? and starred = ? and res_id = ?";
			argsList.add("1");
			argsList.add("0");
			argsList.add("0");
			break;
		case ToDo:
			selection += " to_read = ? and starred = ?";
			argsList.add("1");
			argsList.add("1");
			break;
		case Outbox:
			selection += " id = ?";
			argsList.add("0");
			break;
		case Archives:
			// Load all mails expect out box
			selection += " id != ?";
			argsList.add("0");
			break;
		default:
			break;
		}
		args = argsList.toArray(new String[argsList.size()]);
	}

	@Override
	public void onDataSetChanged() {
		onCreate();
	}

	@Override
	public void onDestroy() {
		mCursor.close();
	}

}
