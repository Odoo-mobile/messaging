package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OForm.OnViewClickListener;
import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnListRowViewClickListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.addons.mail.Mail.Type;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.BaseFragment;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.R;

public class MailDetail extends BaseFragment implements OnViewClickListener,
		OnListRowViewClickListener, BeforeListRowCreateListener {
	private View mView = null;
	private Type mType = null;
	private Integer mId = null;
	private OList mListMessages = null;
	private List<ODataRow> mRecord = null;
	Integer mMessageId = null;
	ODataRow mMessageData = null;
	List<Object> mMessageObjects = new ArrayList<Object>();
	ImageView btnStar;
	boolean isFavorite = false;
	TextView subject;
	Integer[] mStarredDrawables = new Integer[] { R.drawable.ic_action_starred,
			R.drawable.ic_action_starred };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.mail_detail_layout, container, false);
		initArgs();
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		init();
	}

	private void initArgs() {
		Bundle args = getArguments();
		mType = Mail.Type.valueOf(args.getString("key"));
		if (args.containsKey("id")) {
			mId = args.getInt("id");
		} else {
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void init() {
		mListMessages = (OList) mView.findViewById(R.id.lstMessageDetail);
		subject = (TextView) mView.findViewById(R.id.subject);
		subject.setText(getArguments().getString("subject"));
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnStar, this);
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnReply, this);

		mListMessages.setBeforeListRowCreateListener(this);
		switch (mType) {
		case Inbox:
			if (mId != null) {
				mRecord = db().select("id = ? OR parent_id = ?",
						new Object[] { mId, mId });

				mListMessages.initListControl(mRecord);
			} else {
			}
			break;
		case ToMe:
			if (mId != null) {
				mRecord = db().select(
						"res_id = ? AND to_read = ? OR parent_id = ?",
						new Object[] { 0, true, mId });
				mListMessages.initListControl(mRecord);
			}

			break;
		case ToDo:
			if (mId != null) {
				mRecord = db().select(
						"to_read = ? AND starred = ? OR parent_id = ?",
						new Object[] { true, true, mId });
				mListMessages.initListControl(mRecord);
			}

			break;
		case Archives:
			if (mId != null) {
				mRecord = db().select("id = ? AND parent_id = ?",
						new Object[] { mId, mId });
				mListMessages.initListControl(mRecord);
			} else {
				// oList.setModel(mModel);
			}
			break;
		case Outbox:

			break;

		default:
			break;
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_message_detail, menu);
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
	public void onFormViewClick(View view, ODataRow row) {

	}

	@Override
	public void onRowViewClick(ViewGroup view_group, View view, int position,
			ODataRow row) {
		if (view.getId() == R.id.imgBtnStar) {
			ImageView imgStarred = (ImageView) view;
			boolean is_fav = row.getBoolean("starred");
			imgStarred.setColorFilter((!is_fav) ? Color.parseColor("#FF8800")
					: Color.parseColor("#aaaaaa"));
			OValues values = new OValues();
			values.put("starred", !is_fav);
			db().update(values, row.getInt("id"));
			row.put("starred", !is_fav);
			mRecord.remove(position);
			mRecord.add(position, row);
		} else if (view.getId() == R.id.imgBtnReply) {
			Intent i = new Intent(getActivity(), MailComposeActivity.class);
			i.putExtra("name", "nilesh");
			startActivity(i);
		} else if (view.getId() == R.id.imgVotenb) {
			Toast.makeText(getActivity(), "Voted", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		ImageView imgstar = (ImageView) view.findViewById(R.id.imgBtnStar);
		boolean is_favorite = row.getBoolean("starred");
		imgstar.setColorFilter((is_favorite) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));

	}

}
