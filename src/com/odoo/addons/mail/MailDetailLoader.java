package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.controls.OField;

import org.json.JSONArray;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnRowViewClickListener;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;
import com.openerp.R;

public class MailDetailLoader extends BaseFragment implements
		LoaderCallbacks<Cursor>, OnRowViewClickListener, OnViewBindListener {
	private Integer mMailId = null;
	private String selection = null;
	private String[] args;
	private View mView = null;
	private ListView mailList = null;
	private OCursorListAdapter mAdapter;
	private int[] background_resources = new int[] {
			R.drawable.message_listview_bg_toread_selector,
			R.drawable.message_listview_bg_tonotread_selector };

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
				R.layout.mail_detail_parent_list_item);
		mAdapter.setOnViewCreateListener(new OCursorListAdapter.OnViewCreateListener() {
			@Override
			public View onViewCreated(Context context, ViewGroup view,
					Cursor cr, int position) {
				int parent_id = cr.getInt(cr.getColumnIndex("parent_id"));
				int resource = (parent_id == 0) ? mAdapter.getResource()
						: R.layout.mail_detail_reply_list_item;
				return mAdapter.inflate(resource, view);

			}
		});
		mAdapter.setOnRowViewClickListener(R.id.imgBtn_mail_detail_starred,
				this);
		mAdapter.setOnRowViewClickListener(R.id.imgBtn_mail_detail_reply, this);
		mAdapter.setOnRowViewClickListener(R.id.imgBtn_mail_detail_rate, this);
		mAdapter.setOnViewBindListener(this);
		mAdapter.allowCacheView(true);
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
				"total_childs", "parent_id", "date", "to_read", "body",
				"starred" }, selection, args, "date DESC");

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_mail_detail, menu);
		// Fix me (check if mail is read or unread) then hide menu alternativly
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

	@Override
	public void onRowViewClick(int position, Cursor cursor, View view,
			View parent) {
		MailMessage mail = new MailMessage(getActivity());
		Cursor c = cursor;
		boolean is_fav = false;

		switch (view.getId()) {
		case R.id.imgBtn_mail_detail_rate:
			// if (inNetwork()) {
			// try {
			// int mail_id = c.getInt(c.getColumnIndex("id"));
			// OSyncHelper helper = db().getSyncHelper();
			// OArguments args = new OArguments();
			// args.add(new JSONArray().put(mail_id));
			// Boolean response = (Boolean) helper.callMethod(
			// "vote_toggle", args);
			// ImageView imgVote = (ImageView) view
			// .findViewById(R.id.imgHasVoted);
			// OField voteCounter = (OField) view
			// .findViewById(R.id.voteCounter);
			// int votes = (!voteCounter.getText().equals("")) ? Integer
			// .parseInt(voteCounter.getText()) : 0;
			// boolean has_voted = false;
			// if (response) {
			// // Vote up
			// mail.addManyToManyRecord("vote_user_ids",
			// c.getInt(c.getColumnIndex(OColumn.ROW_ID)),
			// mail.author_id());
			// // mListMessages.initListControl(mRecords);
			// has_voted = true;
			// votes++;
			// } else {
			// // Vote down
			// mail.deleteManyToManyRecord("vote_user_ids",
			// c.getInt(c.getColumnIndex(OColumn.ROW_ID)),
			// mail.author_id());
			// // mListMessages.initListControl(mRecords);
			// votes--;
			// }
			// voteCounter.setText((votes > 0) ? votes + "" : "");
			// imgVote.setColorFilter((has_voted) ? getActivity()
			// .getResources().getColor(R.color.odoo_purple)
			// : Color.parseColor("#aaaaaa"));
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// } else {
			// Toast.makeText(getActivity(), _s(R.string.no_connection),
			// Toast.LENGTH_LONG).show();
			// }
			break;
		case R.id.imgBtn_mail_detail_reply:
			OLog.log("clicked");
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
			// imm.toggleSoftInput(R.id.btnSendQuickReply,
			// InputMethodManager.SHOW_IMPLICIT);
			break;
		case R.id.imgBtn_mail_detail_starred:
			String starred = "";
			starred = c.getString(c.getColumnIndex("starred"));
			if (starred.equals("1")) {
				is_fav = true;
				OLog.log("is_fav from DB = " + is_fav);
			} else {
				is_fav = false;
				OLog.log("is_fav from DB = " + is_fav);
			}

			if (inNetwork()) {
				ImageView imgStarred = (ImageView) view;
				imgStarred.setColorFilter((!is_fav) ? Color
						.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
				// markAsTodo
				mail.markAsTodo(c, !is_fav);
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
	public void onViewBind(View view, Cursor cr) {
		// Setting background as per to_read
		int to_read = cr.getInt(cr.getColumnIndex("to_read"));
		view.setBackgroundResource(background_resources[to_read]);
		// Setting starred color
		// ImageView imgStarred = (ImageView) view
		// .findViewById(R.id.img_starred_mlist);
		//
		// String is_fav = cr.getString(cr.getColumnIndex("starred"));
		//
		// imgStarred.setColorFilter((is_fav.equals("1")) ? Color
		// .parseColor("#FF8800") : Color.parseColor("#aaaaaa"));

		// if (view.findViewById(R.id.txvTotalChilds) != null) {
		// OControls.setText(view, R.id.txvTotalChilds,
		// cr.getString(cr.getColumnIndex("total_childs")));
		// }
	}
}
