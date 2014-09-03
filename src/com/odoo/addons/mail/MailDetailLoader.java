package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.R;

public class MailDetailLoader extends BaseFragment implements
		LoaderCallbacks<Cursor> {
	private Integer mMailId = null;
	private String selection = null;
	private String[] args;
	private View mView = null;
	private ListView mailList = null;
	private OCursorListAdapter mAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		scope = new AppScope(this);
		initArgs();
		return inflater.inflate(R.layout.mail_detail_layout, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init();
		mailList = (ListView) view.findViewById(R.id.lstMessageDetail);
		mAdapter = new OCursorListAdapter(getActivity(), null,
				R.layout.mail_detail_parent_list_item) {
			@Override
			public View newView(Context context, Cursor cursor,
					ViewGroup viewGroup) {
				int parent_id = cursor.getInt(cursor
						.getColumnIndex("parent_id"));
				int resource = (parent_id == 0) ? getResource()
						: R.layout.mail_detail_reply_list_item;
				return inflate(resource, viewGroup);
			}
		};
		mailList.setAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	private void initArgs() {
		Bundle args = getArguments();
		if (args.containsKey(OColumn.ROW_ID)) {
			mMailId = args.getInt(OColumn.ROW_ID);
		}
	}

	public void init() {
		if (mMailId != null) {
			ODataRow parent = db().select(mMailId);
			if (parent.getInt("id") == 0) {
				OControls.setInvisible(mView, R.id.quickReplyBox);
			}
			if (parent.getString("message_title") != null
					&& parent.getString("message_title") != "false")
				OControls.setText(mView, R.id.txvDetailSubject,
						parent.getString("message_title"));

		}
	}

	@Override
	public Object databaseHelper(Context context) {
		return new MailMessage(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

		List<String> argsList = new ArrayList<String>();
		selection = "_id = ?";
		selection += " or parent_id = ?";
		argsList.add(mMailId + "");
		argsList.add(mMailId + "");
		args = argsList.toArray(new String[argsList.size()]);
		Uri uri = ((MailMessage) db()).mailDetailUri();
		return new CursorLoader(getActivity(), uri, new String[] {
				"message_title", "author_name", "author_id.image_small",
				"parent_id", "date", "to_read", "body", "starred" }, selection,
				args, "date DESC");

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_mail_detail, menu);
		// MenuItem item = menu.findItem(R.id.menu_mail_read);
		// item.setVisible(false);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}

}
