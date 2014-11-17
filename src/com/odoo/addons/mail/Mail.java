package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import odoo.controls.OField;
import odoo.controls.fab.FloatingActionButton;
import odoo.controls.undobar.UndoBar;
import odoo.controls.undobar.UndoBar.UndoBarListener;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.odoo.OSwipeListener.SwipeCallbacks;
import com.odoo.OTouchListener;
import com.odoo.R;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.sql.OQuery;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.AsyncTaskListener;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.OnSearchViewChangeListener;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.BeforeBindUpdateData;
import com.odoo.support.listview.OCursorListAdapter.OnRowViewClickListener;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;

public class Mail extends BaseFragment implements OnRefreshListener,
		LoaderManager.LoaderCallbacks<Cursor>, SyncStatusObserverListener,
		OnItemClickListener, OnSearchViewChangeListener, OnViewBindListener,
		OnClickListener, OnRowViewClickListener, SwipeCallbacks,
		UndoBarListener, BeforeBindUpdateData {

	public static final String TAG = Mail.class.getSimpleName();
	public static final String KEY = "fragment_mail";
	public static final String KEY_MESSAGE_ID = "mail_id";
	public static final Integer REQUEST_COMPOSE_MAIL = 234;
	private View mView = null;
	private MailMessage db = null;
	private ListView mailList = null;
	private OCursorListAdapter mAdapter;
	private String mCurFilter = null;
	private Type mType = Type.Inbox;
	private String selection = null;
	private String[] args;
	private OTouchListener mTouch;
	private FloatingActionButton mFab;
	private int lastSwipedMail = -1;

	public enum Type {
		Inbox, ToMe, ToDo, Archives, Outbox, Group
	}

	private int[] background_resources = new int[] {
			R.drawable.message_listview_bg_toread_selector,
			R.drawable.message_listview_bg_tonotread_selector };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		setHasSyncStatusObserver(TAG, this, db());
		scope = new AppScope(this);
		initType();
		return inflater.inflate(R.layout.mail, container, false);
	}

	private void initType() {
		Bundle bundle = getArguments();
		if (bundle.containsKey(KEY)) {
			mType = Type.valueOf(bundle.getString(KEY));
			createSelection();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init(mView);
	}

	private void init(View view) {
		mTouch = scope.main().getTouchAttacher();
		setHasSwipeRefreshView(view, R.id.swipe_container, this);
		mFab = (FloatingActionButton) view.findViewById(R.id.fabbutton);
		mailList = (ListView) view.findViewById(R.id.mail_list_view);
		mAdapter = new OCursorListAdapter(getActivity(), null,
				R.layout.mail_list_item);
		mAdapter.setOnViewBindListener(this);
		mAdapter.setBeforeBindUpdateData(this);
		mailList.setAdapter(mAdapter);
		mailList.setOnItemClickListener(this);
		mailList.setEmptyView(mView.findViewById(R.id.loadingProgress));
		if (getPref().getBoolean("archive_with_swipe", false)) {
			if (mType != Type.Archives)
				mTouch.setSwipeableView(mailList, this);
		}
		mFab.listenTo(mailList);
		mFab.setOnClickListener(this);
		mAdapter.setOnRowViewClickListener(R.id.img_starred_mlist, this);
		OControls.setVisible(view, R.id.loadingProgress);
		OControls.setGone(view, R.id.emptyView);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		Resources res = context.getResources();
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, res.getString(R.string.drawer_inbox),
				count_total(context, Type.Inbox), R.drawable.ic_action_inbox,
				object(Type.Inbox)));
		menu.add(new DrawerItem(TAG, res.getString(R.string.drawer_tome),
				count_total(context, Type.ToMe), R.drawable.ic_action_user,
				object(Type.ToMe)));
		menu.add(new DrawerItem(TAG, res.getString(R.string.drawer_todo),
				count_total(context, Type.ToDo),
				R.drawable.ic_action_clipboard, object(Type.ToDo)));
		menu.add(new DrawerItem(TAG, res.getString(R.string.drawer_archives),
				0, R.drawable.ic_action_briefcase, object(Type.Archives)));
		menu.add(new DrawerItem(TAG, res.getString(R.string.drawer_outbox),
				count_total(context, Type.Outbox),
				R.drawable.ic_action_unsent_mail, object(Type.Outbox)));
		return menu;
	}

	private int count_total(Context context, Type key) {
		if (db == null)
			db = new MailMessage(context);
		OQuery q = db.browse().columns("id");
		q = setSelection(context, q, key);
		return q.fetch().size();
	}

	private OQuery setSelection(Context context, OQuery query, Type type) {
		switch (type) {
		case Inbox:
			query.addWhere("to_read", "=", 1);
			query.addWhere("starred", "=", 0);
			query.addWhere("id", "!=", 0);
			break;
		case ToMe:
			query.addWhere("to_read", "=", 1);
			query.addWhere("starred", "=", 0);
			query.addWhere("res_id", "=", 0);
			break;
		case ToDo:
			query.addWhere("to_read", "=", 1);
			query.addWhere("starred", "=", 1);
			break;
		case Outbox:
			query.addWhere("id", "=", 0);
			break;
		case Group:
			Integer group_id = getArguments().getInt(Groups.KEY);
			query.addWhere("res_id", "=", group_id);
			query.addWhere("model", "=", "mail.group");
			break;
		case Archives:
			query.addWhere("id", "!=", 0);
			break;
		default:
			break;
		}
		return query;
	}

	public Fragment object(Type type) {
		Mail mail = new Mail();
		Bundle bundle = new Bundle();
		bundle.putString(KEY, type.toString());
		mail.setArguments(bundle);
		return mail;
	}

	@Override
	public void onRefresh() {
		if (app().inNetwork()) {
			scope.main().requestSync(MailProvider.AUTHORITY);
		} else {
			hideRefreshingProgress();
			Toast.makeText(getActivity(), _s(R.string.no_connection),
					Toast.LENGTH_LONG).show();
		}
	}

	private void createSelection() {
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
		case Group:
			selection += " res_id = ? and model = ?";
			argsList.add(getArguments().getInt(Groups.KEY) + "");
			argsList.add("mail.group");
			break;
		}
		args = argsList.toArray(new String[argsList.size()]);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		if (db().isEmptyTable()) {
			scope.main().requestSync(MailProvider.AUTHORITY);
			setSwipeRefreshing(true);
		}
		List<String> argsList = new ArrayList<String>();
		createSelection();
		if (mCurFilter != null) {
			argsList.addAll(Arrays.asList(args));
			selection += " and (author_name like ? or message_title like ?)";
			argsList.add(mCurFilter + "%");
			argsList.add("%" + mCurFilter + "%");
			args = argsList.toArray(new String[argsList.size()]);
		}
		Uri uri = ((MailMessage) db()).mailUri();
		return new CursorLoader(getActivity(), uri, new String[] {
				"message_title", "author_name", "parent_id", "author_id",
				"total_childs", "date", "to_read", "short_body", "starred" },
				selection, args, "date DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
		toggleEmptyView((cursor.getCount() == 0));
		OControls.setGone(mView, R.id.loadingProgress);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onStatusChange(Boolean refreshing) {
		if (!refreshing) {
			hideRefreshingProgress();
		} else
			setSwipeRefreshing(true);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Cursor cr = (Cursor) mAdapter.getItem(position);
		int _id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
		int record_id = cr.getInt(cr.getColumnIndex("id"));
		MailDetail mDetail = new MailDetail();
		Bundle bundle = new Bundle();
		bundle.putInt(OColumn.ROW_ID, _id);
		bundle.putInt("id", record_id);
		mDetail.setArguments(bundle);
		startFragment(mDetail, true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_mail, menu);
		setHasSearchView(this, menu, R.id.menu_mail_search);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.menu_mail_create:
		// Intent i = new Intent(getActivity(), ComposeMail.class);
		// startActivityForResult(i, REQUEST_COMPOSE_MAIL);
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_COMPOSE_MAIL
				&& resultCode == Activity.RESULT_OK) {
			if (inNetwork()) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				Toast.makeText(getActivity(), _s(R.string.message_sent),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getActivity(), _s(R.string.message_cant_sent),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public boolean onSearchViewTextChange(String newFilter) {
		if (mCurFilter == null && newFilter == null)
			return true;
		if (mCurFilter != null && mCurFilter.equals(newFilter))
			return true;

		mCurFilter = newFilter;
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override
	public void onSearchViewClose() {
		// Do Nothing...
	}

	@Override
	public void onViewBind(View view, Cursor cr, ODataRow row) {
		// Setting background as per to_read
		int to_read = cr.getInt(cr.getColumnIndex("to_read"));
		view.setBackgroundResource(background_resources[to_read]);
		// Setting starred color
		ImageView imgStarred = (ImageView) view
				.findViewById(R.id.img_starred_mlist);

		int is_fav = 0;
		is_fav = cr.getInt(cr.getColumnIndex("starred"));
		imgStarred.setColorFilter((is_fav == 1) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));
		OField totalChilds = (OField) view.findViewById(R.id.totalChilds);
		int replies = 0;
		String total_childs = cr.getString(cr.getColumnIndex("total_childs"));
		replies = Integer.parseInt(total_childs);
		String childs = "";
		if (replies > 0) {
			childs = replies + " replies";
		}
		totalChilds.setText(childs);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.fabbutton) {
			Intent i = new Intent(getActivity(), ComposeMail.class);
			startActivityForResult(i, REQUEST_COMPOSE_MAIL);
		}
		if (v.getId() == R.id.checkForNewData) {
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					onRefresh();
				}
			}, 250);
		}
	}

	public void toggleEmptyView(boolean show) {
		OControls.toggleViewVisibility(mView, R.id.emptyView, show);
		if (show)
			setEmptyMessage();
	}

	private void setEmptyMessage() {
		int icon = 0;
		int str = R.string.label_no_records_found;
		switch (mType) {
		case Inbox:
			icon = R.drawable.ic_action_inbox;
			str = R.string.message_inbox_all_read;
			break;
		case ToMe:
			icon = R.drawable.ic_action_user;
			str = R.string.message_tome_all_read;
			break;
		case ToDo:
			icon = R.drawable.ic_action_clipboard;
			str = R.string.message_todo_all_read;
			break;
		case Outbox:
			icon = R.drawable.ic_action_unsent_mail;
			str = R.string.message_no_outbox_message;
			break;
		case Archives:
			icon = R.drawable.ic_action_briefcase;
			break;
		case Group:
			icon = R.drawable.ic_action_social_group;
			str = R.string.message_no_group_message;
			break;
		}
		OControls.setImage(mView, R.id.emptyListIcon, icon);
		OControls.setText(mView, R.id.emptyListMessage, _s(str));
		if (mType != Type.Outbox)
			mView.findViewById(R.id.checkForNewData).setOnClickListener(this);
		else
			mView.findViewById(R.id.checkForNewData).setVisibility(View.GONE);
	}

	private void restartLoader() {
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onRowViewClick(int position, Cursor cursor, View view,
			View parent) {
		final Cursor c = cursor;
		switch (view.getId()) {
		case R.id.img_starred_mlist:
			String starred = "";
			starred = c.getString(c.getColumnIndex("starred"));
			final boolean is_fav = !starred.equals("1");
			if (inNetwork()) {
				ImageView imgStarred = (ImageView) view;
				imgStarred.setColorFilter((is_fav) ? Color
						.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
				// markAsTodo
				scope.main().newBackgroundTask(new AsyncTaskListener() {

					@Override
					public Object onPerformTask() {
						MailMessage mail = (MailMessage) db();
						mail.markAsTodo(c, is_fav);
						return null;
					}

					@Override
					public void onFinish(Object result) {
						getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								restartLoader();
							}
						});
					}
				}).execute();
			} else {
				Toast.makeText(getActivity(), _s(R.string.no_connection),
						Toast.LENGTH_SHORT).show();
			}
			break;

		default:
			break;
		}
	}

	@Override
	public boolean canSwipe(int position) {
		return true;
	}

	@Override
	public void onSwipe(View view, int[] positions) {
		for (int pos : positions) {
			Cursor cr = mAdapter.getCursor();
			cr.moveToPosition(pos);
			lastSwipedMail = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
			toggleMailToRead(lastSwipedMail, false);
			showUndoBar();
		}
	}

	private void showUndoBar() {
		if (getPref().getBoolean("mail_show_undo_archive", true)) {
			UndoBar undoBar = new UndoBar(getActivity());
			undoBar.setMessage("Mail archived");
			undoBar.setDuration(7000);
			undoBar.setListener(this);
			undoBar.show(true);
		} else {
			updateArchiveOnServer();
		}
	}

	@Override
	public void onHide() {
		if (lastSwipedMail != -1) {
			updateArchiveOnServer();
			lastSwipedMail = -1;
		}
	}

	private void updateArchiveOnServer() {
		if (inNetwork()) {
			MailMessage mail = (MailMessage) db();
			mail.markMailReadUnread(lastSwipedMail, false);
		}
	}

	@Override
	public void onUndo(Parcelable token) {
		toggleMailToRead(lastSwipedMail, true);
		lastSwipedMail = -1;
	}

	private void toggleMailToRead(int mailId, boolean to_read) {
		ContentValues values = new ContentValues();
		values.put("to_read", (to_read) ? 1 : 0);
		if (!inNetwork())
			values.put("is_dirty", 1);
		String selection = OColumn.ROW_ID + " = ? or parent_id = ?";
		String[] args = new String[] { mailId + "", mailId + "" };
		if (to_read) {
			selection = OColumn.ROW_ID + " = ?";
			args = new String[] { mailId + "" };
		}
		getActivity().getContentResolver().update(db().uri(), values,
				selection, args);
	}

	@Override
	public ODataRow updateDataRow(Cursor cr) {
		String author_image = "false";
		ODataRow row = new ODataRow();
		if (!cr.getString(cr.getColumnIndex("author_id")).equals("false")) {
			ODataRow partner = new ResPartner(getActivity()).select(cr
					.getInt(cr.getColumnIndex("author_id")));
			if (partner != null) {
				author_image = partner.getString("image_small");
			}
		}
		row.put("author_id_image_small", author_image);
		return row;
	}
}
