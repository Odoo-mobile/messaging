package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OList;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.addons.mail.Mail.Type;
import com.odoo.addons.mail.models.MailGroup;
import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.ODataRow;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;
import com.openerp.R;

public class Group extends BaseFragment implements OnPullListener {
	public static final String TAG = "com.odoo.addons.mail.MailGroup";

	View mView = null;
	List<ODataRow> mGroupListItems = new ArrayList<ODataRow>();
	OList mListGroup;
	MailFollowers mMailFollowerDB = null;
	private OETouchListener mTouchAttacher;
	GroupsLoader mGroupsLoader = null;
	private Boolean mSynced = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		scope = new AppScope(this);
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.mailgroup_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init();
	}

	private void init() {
		scope = new AppScope(getActivity());
		mMailFollowerDB = new MailFollowers(getActivity());
		initControls();
		mGroupsLoader = new GroupsLoader();
		mGroupsLoader.execute();

	}

	private void initControls() {
		mTouchAttacher = scope.main().getTouchAttacher();
		mListGroup = (OList) mView.findViewById(R.id.listGroups);
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
		menu.add(new DrawerItem(TAG, "Groups", count(),
				R.drawable.ic_action_social_group, object("group")));
		return menu;
	}

	private Fragment object(String value) {
		Group mail = new Group();
		Bundle bundle = new Bundle();
		bundle.putString("group", value.toString());
		mail.setArguments(bundle);
		return mail;
	}

	private int count() {
		int count = 0;
		// count = db().count();
		return count;
	}

	@Override
	public void onPullStarted(View arg0) {
		OLog.log(TAG, "MailGroup->OETouchListener->onPullStarted()");
		scope.main().requestSync(MailGroupProvider.AUTHORITY);

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
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// if (!mSyncing)
			mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
			mGroupsLoader = null;
			mListGroup.initListControl(mGroupListItems);

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
}
