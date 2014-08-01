package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnRowClickListener;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.orm.ODataRow;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Groups extends BaseFragment implements OnPullListener,
		BeforeListRowCreateListener, OnRowClickListener {
	public static final String TAG = "com.odoo.addons.mail.MailGroup";
	public static final String KEY = "group_id";

	private View mView = null;
	private List<ODataRow> mGroupListItems = new ArrayList<ODataRow>();
	private OList mListGroup;
	private OETouchListener mTouchAttacher;
	private GroupsLoader mGroupsLoader = null;
	private Boolean mSynced = false;
	private Integer mLastPosition = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		scope = new AppScope(this);
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.groups, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init();
	}

	private void init() {
		initControls();
		if (mLastPosition == -1) {
			mGroupsLoader = new GroupsLoader();
			mGroupsLoader.execute();
		} else
			showData();
	}

	private void showData() {
		mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
		mGroupsLoader = null;
		mListGroup.initListControl(mGroupListItems);
	}

	private void initControls() {
		mTouchAttacher = scope.main().getTouchAttacher();
		mListGroup = (OList) mView.findViewById(R.id.listGroups);
		mListGroup.setBeforeListRowCreateListener(this);
		mListGroup.setOnRowClickListener(this);
		mTouchAttacher.setPullableView(mListGroup, this);
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
		Groups mail = new Groups();
		Bundle bundle = new Bundle();
		bundle.putString("group", value);
		mail.setArguments(bundle);
		return mail;
	}

	@Override
	public void onPullStarted(View arg0) {
		if (inNetwork())
			scope.main().requestSync(MailGroupProvider.AUTHORITY);
		else
			mTouchAttacher.setPullComplete();

	}

	class GroupsLoader extends AsyncTask<Void, Void, Void> {
		// Boolean mSyncing = false;

		public GroupsLoader() {
			mView.findViewById(R.id.loadingProgress)
					.setVisibility(View.VISIBLE);
			if (db().isEmptyTable() && !mSynced) {
				scope.main().requestSync(MailGroupProvider.AUTHORITY);
				// mSyncing = true;
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			mGroupListItems.clear();
			mGroupListItems.addAll(db().select());
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// if (!mSyncing)
			showData();

		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().registerReceiver(syncFinishReceiver,
				new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(syncFinishReceiver);
	}

	private SyncFinishReceiver syncFinishReceiver = new SyncFinishReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mTouchAttacher.setPullComplete();
			mGroupsLoader = new GroupsLoader();
			mGroupsLoader.execute();
			scope.main().refreshDrawer(TAG);
		}

	};

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		Boolean joined = row.getBoolean("has_joined");
		OControls.toggleViewVisibility(view, R.id.btnJoinGroup, !joined);
		OControls.toggleViewVisibility(view, R.id.btnUnJoinGroup, joined);

	}

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
			Toast.makeText(getActivity(), "You must first join group.",
					Toast.LENGTH_LONG).show();
		}
	}
}
