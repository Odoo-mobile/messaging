package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnListRowViewClickListener;
import odoo.controls.OList.OnRowClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Mail extends BaseFragment implements OnPullListener,
		BeforeListRowCreateListener, OnListRowViewClickListener,
		OnRowClickListener {

	public static final String TAG = Mail.class.getSimpleName();
	public static final String KEY = "fragment_mail";
	private View mView = null;
	private OList mListControl = null;
	private List<ODataRow> mListRecords = new ArrayList<ODataRow>();
	private MessagesLoader mMessageLoader = null;
	private Boolean mSynced = false;
	private OETouchListener mTouchListener = null;

	public enum Type {
		Inbox, ToMe, ToDo, Archives, Outbox
	}

	private Type mType = Type.Inbox;
	private int[] background_resources = new int[] {
			R.drawable.message_listview_bg_toread_selector,
			R.drawable.message_listview_bg_tonotread_selector };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		scope = new AppScope(this);
		mTouchListener = scope.main().getTouchAttacher();
		initType();
		return inflater.inflate(R.layout.mail, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init();
	}

	private void initType() {
		Bundle bundle = getArguments();
		if (bundle.containsKey(KEY)) {
			mType = Type.valueOf(bundle.getString(KEY));
		}
	}

	private void init() {
		mListControl = (OList) mView.findViewById(R.id.lstMeesages);
		mTouchListener.setPullableView(mListControl, this);
		mListControl
				.setOnListRowViewClickListener(R.id.img_starred_mlist, this);
		mListControl.setBeforeListRowCreateListener(this);
		mListControl.setOnRowClickListener(this);
		mMessageLoader = new MessagesLoader(mType);
		mMessageLoader.execute();

	}

	private HashMap<String, Object> getWhere(Type type) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		String where = null;
		String[] whereArgs = null;
		switch (type) {
		case Inbox:
			where = "to_read = ? AND starred = ?";
			whereArgs = new String[] { "true", "false" };
			break;
		case ToMe:
			where = "to_read = ? AND starred = ? ";
			whereArgs = new String[] { "true", "false" };
			break;
		case ToDo:
			where = "to_read = ? AND starred = ?";
			whereArgs = new String[] { "true", "true" };
			break;
		case Outbox:
			where = "id = ?";
			whereArgs = new String[] { "0" };
			break;
		default:
			break;
		}
		map.put("where", where);
		map.put("whereArgs", whereArgs);
		return map;
	}

	public class MessagesLoader extends AsyncTask<Void, Void, Boolean> {

		Type messageType = null;
		Boolean mSyncing = false;

		public MessagesLoader(Type type) {
			messageType = type;
			mView.findViewById(R.id.loadingProgress)
					.setVisibility(View.VISIBLE);
			if (db().isEmptyTable() && !mSynced) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				mSyncing = true;
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			scope.main().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mListRecords.clear();
					LinkedHashMap<String, ODataRow> mParentList = new LinkedHashMap<String, ODataRow>();
					HashMap<String, Object> map = getWhere(messageType);
					String where = (String) map.get("where");
					String whereArgs[] = (String[]) map.get("whereArgs");
					for (ODataRow row : db().select(where, whereArgs, null,
							null, "date DESC")) {
						ODataRow parent = row.getM2ORecord("parent_id")
								.browse();
						if (parent != null) {
							// Child
							if (!mParentList.containsKey("key_"
									+ parent.getString("id"))) {
								parent.put("body", row.getString("body"));
								parent.put("date", row.getString("date"));
								parent.put("to_read", row.getBoolean("to_read"));
								mParentList.put(
										"key_" + parent.getString("id"), parent);

							}
						} else { // parent
							if (!mParentList.containsKey("key_"
									+ row.getString("id"))) {
								mParentList.put("key_" + row.getString("id"),
										row);
							}
						}
					}
					for (String k : mParentList.keySet()) {
						mListRecords.add(mParentList.get(k));
					}
				}
			});
			return true;

		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (!mSyncing)
				mView.findViewById(R.id.loadingProgress).setVisibility(
						View.GONE);
			mMessageLoader = null;
			mListControl.initListControl(mListRecords);
		}

	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Messaging", true));
		menu.add(new DrawerItem(TAG, "Inbox", 0, 0, object(Type.Inbox)));
		menu.add(new DrawerItem(TAG, "To: me", 0, 0, object(Type.ToMe)));
		menu.add(new DrawerItem(TAG, "To-do", 0, 0, object(Type.ToDo)));
		menu.add(new DrawerItem(TAG, "Archives", 0, 0, object(Type.Archives)));
		menu.add(new DrawerItem(TAG, "Outbox", 0, 0, object(Type.Outbox)));
		return menu;
	}

	private Fragment object(Type type) {
		Mail mail = new Mail();
		Bundle bundle = new Bundle();
		bundle.putString(KEY, type.toString());
		mail.setArguments(bundle);
		return mail;
	}

	@Override
	public void onResume() {
		super.onResume();
		scope.main().registerReceiver(mSyncFinishReceiver,
				new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
	}

	@Override
	public void onPause() {
		super.onPause();
		scope.main().unregisterReceiver(mSyncFinishReceiver);
	}

	SyncFinishReceiver mSyncFinishReceiver = new SyncFinishReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			super.onReceive(context, intent);
			mTouchListener.setPullComplete();
			scope.main().refreshDrawer(TAG);
			mListRecords.clear();
			if (mMessageLoader != null)
				mMessageLoader.cancel(true);
			mMessageLoader = new MessagesLoader(mType);
			mMessageLoader.execute();
		}
	};

	@Override
	public void onPullStarted(View arg) {
		if (inNetwork()) {
			scope.main().requestSync(MailProvider.AUTHORITY);
		} else {
			mTouchListener.setPullComplete();
		}
	}

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		ImageView imgStarred = (ImageView) view
				.findViewById(R.id.img_starred_mlist);
		boolean is_fav = row.getBoolean("starred");
		imgStarred.setColorFilter((is_fav) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));

		// Check for to_read selector
		boolean to_read = row.getBoolean("to_read");
		view.setBackgroundResource(background_resources[(to_read) ? 1 : 0]);
	}

	@Override
	public void onRowViewClick(ViewGroup view_group, View view, int position,
			ODataRow row) {
		MailMessage mail = new MailMessage(getActivity());
		if (view.getId() == R.id.img_starred_mlist) {
			ImageView imgStarred = (ImageView) view;
			boolean is_fav = row.getBoolean("starred");
			imgStarred.setColorFilter((!is_fav) ? Color.parseColor("#FF8800")
					: Color.parseColor("#aaaaaa"));
			OValues values = new OValues();
			values.put("starred", !is_fav);
			mail.update(values, row.getInt(OColumn.ROW_ID));
			row.put("starred", !is_fav);
			mListRecords.remove(position);
			mListRecords.add(position, row);
		}
	}

	@Override
	public void onRowItemClick(int position, View view, ODataRow row) {
		OLog.log(row.toString());
	}
}
