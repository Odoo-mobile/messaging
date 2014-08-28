package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widgets.SwipeRefreshLayout.OnRefreshListener;

import com.odoo.addons.mail.Mail.Type;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.orm.OColumn;
import com.odoo.orm.sql.OQuery;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.OnSearchViewChangeListener;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.R;

public class MailLoader extends BaseFragment implements OnRefreshListener,
		LoaderManager.LoaderCallbacks<Cursor>, SyncStatusObserverListener,
		OnItemClickListener, OnSearchViewChangeListener {

	public static final String TAG = MailLoader.class.getSimpleName();
	public static final String KEY = "fragment_mail";
	private View mView = null;
	private MailMessage db = null;
	private ListView mailList = null;
	private OCursorListAdapter mAdapter;
	private String mCurFilter = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		setHasSyncStatusObserver(TAG, this, db());
		scope = new AppScope(this);
		return inflater.inflate(R.layout.mail_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasSwipeRefreshView(view, R.id.swipe_container, this);
		mView = view;
		mailList = (ListView) view.findViewById(R.id.mail_list_view);
		mAdapter = new OCursorListAdapter(getActivity(), null,
				R.layout.mail_list_item);
		mailList.setAdapter(mAdapter);
		mailList.setOnItemClickListener(this);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Inbox", count_total(context, Type.Inbox),
				R.drawable.ic_action_inbox, object(Type.Inbox)));
		menu.add(new DrawerItem(TAG, "To: me", count_total(context, Type.ToMe),
				R.drawable.ic_action_user, object(Type.ToMe)));
		menu.add(new DrawerItem(TAG, "To-do", count_total(context, Type.ToDo),
				R.drawable.ic_action_clipboard, object(Type.ToDo)));
		menu.add(new DrawerItem(TAG, "Archives", 0,
				R.drawable.ic_action_briefcase, object(Type.Archives)));
		menu.add(new DrawerItem(TAG, "Outbox",
				count_total(context, Type.Outbox),
				R.drawable.ic_action_unsent_mail, object(Type.Outbox)));
		return menu;
	}

	private int count_total(Context context, Type key) {
		if (db == null)
			db = new MailMessage(context);
		OQuery q = db.browse().columns("id");
		// q = setSelection(context, q, key);
		return q.fetch().size();
	}

	private Fragment object(Type type) {
		MailLoader mail = new MailLoader();
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

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		if (db().isEmptyTable()) {
			scope.main().requestSync(MailProvider.AUTHORITY);
			setSwipeRefreshing(true);
		}
		String selection = "parent_id = ? or parent_id = ?";
		String[] args = new String[] { "false", "0" };
		if (mCurFilter != null) {
			selection += " and message_title like ? or body like ?";
			args = new String[] { "false", "0", "%" + mCurFilter + "%",
					"%" + mCurFilter + "%" };
		}
		return new CursorLoader(getActivity(), db().uri(), new String[] {
				"message_title", "author_id.name", "author_id.image_small",
				"total_childs", "date", "to_read", "body", "starred" },
				selection, args, "date DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
		OControls.setGone(mView, R.id.loadingProgress);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onStatusChange(Boolean refreshing) {
		if (!refreshing)
			hideRefreshingProgress();
		else
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
}
