package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.Odoo;
import odoo.controls.OList.OnRowClickListener;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widgets.SwipeRefreshLayout.OnRefreshListener;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.AppScope;
import com.odoo.support.OUser;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnRowViewClickListener;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;
import com.openerp.R;

public class Groups extends BaseFragment implements OnRowClickListener,
		OnRefreshListener, LoaderCallbacks<Cursor>, OnRowViewClickListener,
		SyncStatusObserverListener, OnItemClickListener { // BeforeListRowCreateListener,
	public static final String TAG = "com.odoo.addons.mail.MailGroup";
	public static final String KEY = "group_id";
	private View mView = null;
	private List<ODataRow> mGroupListItems = new ArrayList<ODataRow>();
	private ListView mListGroup;
	private GroupsLoader mGroupsLoader = null;
	private Boolean mSynced = false;
	private Integer mLastPosition = -1;
	private OCursorListAdapter mAdapter;
	private Odoo odoo = null;
	Context mContext = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContext = getActivity();
		scope = new AppScope(mContext);
		setHasOptionsMenu(true);
		setHasSyncStatusObserver(TAG, this, db());
		return inflater.inflate(R.layout.groups, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		setHasSwipeRefreshView(view, R.id.swipe_container, this);
		mAdapter = new OCursorListAdapter(mContext, null,
				R.layout.groups_list_item);
		initControls();
		mListGroup.setAdapter(mAdapter);
		mListGroup.setOnItemClickListener(this);
		mAdapter.setOnRowViewClickListener(R.id.btnJoinGroup, this);
		mAdapter.setOnRowViewClickListener(R.id.btnUnJoinGroup, this);
		getLoaderManager().initLoader(0, null, this);
		// init();
	}

	// private void init() {
	// initControls();
	// if (mLastPosition == -1) {
	// mGroupsLoader = new GroupsLoader();
	// mGroupsLoader.execute();
	// } else
	// showData();
	// }

	// private void showData() {
	// mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
	// mGroupsLoader = null;
	// mListGroup.initListControl(mGroupListItems);
	// }

	private void initControls() {
		mListGroup = (ListView) mView.findViewById(R.id.listGroups);
		// mListGroup.setBeforeListRowCreateListener(this);
		// mListGroup.setOnRowClickListener(this);
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
		Groups group = new Groups();
		Bundle bundle = new Bundle();
		bundle.putString("group", value);
		group.setArguments(bundle);
		return group;
	}

	// class GroupsLoader extends AsyncTask<Void, Void, Void> {
	// // Boolean mSyncing = false;
	//
	// public GroupsLoader() {
	// mView.findViewById(R.id.loadingProgress)
	// .setVisibility(View.VISIBLE);
	// if (db().isEmptyTable() && !mSynced) {
	// scope.main().requestSync(MailGroupProvider.AUTHORITY);
	// // mSyncing = true;
	// }
	// }
	//
	// @Override
	// protected Void doInBackground(Void... params) {
	// mGroupListItems.clear();
	// mGroupListItems.addAll(db().select());
	// return null;
	// }
	//
	// @Override
	// protected void onPostExecute(Void result) {
	// // if (!mSyncing)
	// showData();
	//
	// }
	// }

	// @Override
	// public void onResume() {
	// super.onResume();
	// getActivity().registerReceiver(syncFinishReceiver,
	// new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
	// }
	//
	// @Override
	// public void onPause() {
	// super.onPause();
	// getActivity().unregisterReceiver(syncFinishReceiver);
	// }

	// private SyncFinishReceiver syncFinishReceiver = new SyncFinishReceiver()
	// {
	//
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// scope.main().refreshDrawer(TAG);
	// mGroupsLoader = new GroupsLoader();
	// mGroupsLoader.execute();
	// hideRefreshingProgress();
	// }
	//
	// };

	// @Override
	// public void beforeListRowCreate(int position, ODataRow row, View view) {
	// Boolean joined = row.getBoolean("has_joined");
	// OControls.toggleViewVisibility(view, R.id.btnJoinGroup, !joined);
	// OControls.toggleViewVisibility(view, R.id.btnUnJoinGroup, joined);
	//
	// }

	@Override
	public void onRowItemClick(int position, View view, ODataRow row) {
		if (row.getBoolean("has_joined")) {
			mLastPosition = position;
			Mail mail = new Mail();
			Bundle bundle = new Bundle();
			bundle.putInt(KEY, row.getInt("id"));
			bundle.putString(Mail.KEY, Mail.Type.Group.toString());
			mail.setArguments(bundle);
			startFragment(mail, true);
		} else {
			Toast.makeText(getActivity(), _s(R.string.first_join_group),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onRefresh() {
		if (inNetwork()) {
			scope.main().requestSync(MailGroupProvider.AUTHORITY);
		} else {
			hideRefreshingProgress();
			Toast.makeText(getActivity(), _s(R.string.no_connection),
					Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		if (db().isEmptyTable()) {
			scope.main().requestSync(MailGroupProvider.AUTHORITY);
			setSwipeRefreshing(true);
		}
		Uri uri = ((MailGroup) db()).groupUri();
		return new CursorLoader(getActivity(), uri, db().projection(), null,
				null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onRowViewClick(int position, Cursor cursor, View view,
			View parent) {
		switch (view.getId()) {
		case R.id.btnJoinGroup:
			OLog.log(" Join Clicked......");
			break;
		case R.id.btnUnJoinGroup:
			OLog.log(" Un Join Clicked......");
			break;
		default:
			break;
		}
	}

	public class JoinUnfollowGroup extends AsyncTask<Void, Void, Boolean> {
		int mGroupId = 0;
		boolean mJoin = false;
		String mToast = "";
		JSONObject result = new JSONObject();
		MailFollowers followers = new MailFollowers(getActivity());

		public JoinUnfollowGroup(int group_id, boolean join) {
			mGroupId = group_id;
			mJoin = join;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			try {

				int partner_id = OUser.current(getActivity()).getPartner_id();
				OSyncHelper oe = db().getSyncHelper();
				if (oe == null) {
					mToast = "No Connection";
					return false;
				}
				JSONArray arguments = new JSONArray();
				arguments.put(new JSONArray().put(mGroupId));
				arguments.put(odoo.updateContext(new JSONObject()));
				if (mJoin) {
					odoo.call_kw("mail.group", "action_follow", arguments);
					// odoo.call_kw("action_follow", arguments, null);
					mToast = "Group joined";
					oe.syncWithServer();
				} else {
					odoo.call_kw("mail.group", "action_unfollow", arguments);
					mToast = "Unfollowed from group";
					followers.delete(
							"res_id = ? AND partner_id = ? AND res_model = ? ",
							new String[] { mGroupId + "", partner_id + "",
									db().getModelName() });

				}
				return true;

			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

	}

	@Override
	public void onStatusChange(Boolean refreshing) {
		if (!refreshing) {
			hideRefreshingProgress();
		} else
			setSwipeRefreshing(true);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long arg3) {
		OLog.log(position + " row clicked");
	}
}
