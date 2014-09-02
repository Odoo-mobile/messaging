package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import android.widgets.SwipeRefreshLayout.OnRefreshListener;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;
import com.openerp.R;

public class GroupsLoader extends BaseFragment implements
		LoaderCallbacks<Cursor>, OnRefreshListener, SyncStatusObserverListener {
	public static final String TAG = "com.odoo.addons.mail.GroupsLoader";
	private View mView = null;
	private Context mContext = null;
	private OCursorListAdapter mAdapter;
	private ListView groupList = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContext = getActivity();
		setHasOptionsMenu(true);
		setHasSyncStatusObserver(TAG, this, db());
		scope = new AppScope(mContext);
		return inflater.inflate(R.layout.groups, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasSwipeRefreshView(view, R.id.swipe_container, this);
		mView = view;
		groupList = (ListView) view.findViewById(R.id.listGroups);
		mAdapter = new OCursorListAdapter(getActivity(), null,
				R.layout.groups_list_item) {
			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				super.bindView(view, context, cursor);
				switch (view.getId()) {
				case R.id.btnJoinGroup:
					OLog.log("join Button cliked....");
					break;
				case R.id.btnUnJoinGroup:
					OLog.log("Un_join Button cliked....");
				default:
					break;
				}
			}
		};
		groupList.setAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailGroup(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "My Groups", true));
		menu.add(new DrawerItem(TAG, "Groups", 0,
				R.drawable.ic_action_social_group, object("group")));
		return menu;
	}

	private Fragment object(String value) {
		GroupsLoader group = new GroupsLoader();
		Bundle bundle = new Bundle();
		bundle.putString("group", value);
		group.setArguments(bundle);
		return group;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		if (db().isEmptyTable()) {
			scope.main().requestSync(MailGroupProvider.AUTHORITY);
			setSwipeRefreshing(true);
		}
		return new CursorLoader(mContext, db().uri(), db().projection(), null,
				null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}

	@Override
	public void onRefresh() {
		if (app().inNetwork()) {
			scope.main().requestSync(MailGroupProvider.AUTHORITY);
		} else {
			hideRefreshingProgress();
			Toast.makeText(getActivity(), _s(R.string.no_connection),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onStatusChange(Boolean refreshing) {
		if (!refreshing)
			hideRefreshingProgress();
		else
			setSwipeRefreshing(true);
	}

}
