package com.odoo.util.notification;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.odoo.R;

public class NotificationBuilder {
	private Context mContext;
	private Builder mNotificationBuilder = null;
	private PendingIntent mNotificationResultIntent = null;
	private NotificationManager mNotificationManager = null;
	private String title, text, bigText;
	private boolean mOnGoing = false, mAutoCancel = true;
	private Intent resultIntent = null;
	private int icon = R.drawable.ic_odoo_o;
	private List<NotificationAction> mActions = new ArrayList<NotificationBuilder.NotificationAction>();

	public NotificationBuilder(Context context) {
		mContext = context;
	}

	public NotificationBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public NotificationBuilder setText(String text) {
		this.text = text;
		return this;
	}

	public NotificationBuilder setIcon(int res_id) {
		icon = res_id;
		return this;
	}

	public NotificationBuilder setBigText(String bigText) {
		this.bigText = bigText;
		return this;
	}

	public NotificationBuilder setOngoing(boolean onGoing) {
		mOnGoing = onGoing;
		return this;
	}

	public NotificationBuilder setAutoCancel(boolean autoCancel) {
		mAutoCancel = autoCancel;
		return this;
	}

	public NotificationBuilder addAction(NotificationAction action) {
		mActions.add(action);
		return this;
	}

	private void init() {
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationBuilder = new NotificationCompat.Builder(mContext);
		mNotificationBuilder.setContentTitle(title);
		mNotificationBuilder.setContentText(text);
		mNotificationBuilder.setSmallIcon(icon);
		mNotificationBuilder.setAutoCancel(mAutoCancel);
		mNotificationBuilder.setOngoing(mOnGoing);
		if (bigText != null) {
			NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
			notiStyle.setBigContentTitle(title);
			notiStyle.setSummaryText(text);
			notiStyle.bigText(bigText);
			mNotificationBuilder.setStyle(notiStyle);
		}
		setSoundForNotification();
		setVibrateForNotification();
	}

	private void setSoundForNotification() {
		mNotificationBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000,
				1000 });
	}

	private void setVibrateForNotification() {
		Uri uri = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		mNotificationBuilder.setSound(uri);
	}

	public NotificationBuilder setResultIntent(Intent intent) {
		resultIntent = intent;
		return this;
	}

	public NotificationBuilder build() {
		init();
		if (resultIntent != null) {
			_setResultIntent();
		}
		return this;
	}

	private void _setResultIntent() {
		mNotificationResultIntent = PendingIntent.getActivity(mContext, 0,
				resultIntent, PendingIntent.FLAG_UPDATE_CURRENT
						| Notification.FLAG_AUTO_CANCEL);
		mNotificationBuilder.setDefaults(Notification.DEFAULT_ALL);
		mNotificationBuilder.setContentIntent(mNotificationResultIntent);
	}

	public void show() {
		mNotificationManager.notify(0, mNotificationBuilder.build());
	}

	public class NotificationAction {
		private int icon;
		private int requestCode;
		private String title;
		private String action;
		private Bundle extras;

		public NotificationAction(int icon, String title, int requestCode,
				String action, Bundle extras) {
			super();
			this.icon = icon;
			this.title = title;
			this.requestCode = requestCode;
			this.action = action;
			this.extras = extras;
		}

		public int getIcon() {
			return icon;
		}

		public void setIcon(int icon) {
			this.icon = icon;
		}

		public int getRequestCode() {
			return requestCode;
		}

		public void setRequestCode(int requestCode) {
			this.requestCode = requestCode;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public Bundle getExtras() {
			return extras;
		}

		public void setExtras(Bundle extras) {
			this.extras = extras;
		}

	}
}
