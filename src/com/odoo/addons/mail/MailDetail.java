package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.controls.OForm.OnViewClickListener;
import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnListRowViewClickListener;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.models.MailNotification;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.R;

public class MailDetail extends BaseFragment implements OnViewClickListener,
		OnListRowViewClickListener, BeforeListRowCreateListener,
		OnClickListener {
	public static final String TAG = "com.odoo.addons.mail.MailDetail";
	public static final String KEY_MESSAGE_ID = "message_id";
	public static final String KEY_SUBJECT = "subject";
	public static final String KEY_BODY = "body";
	private View mView = null;
	private Integer mMailId = null;
	private OList mListMessages = null;
	private List<ODataRow> mRecords = new ArrayList<ODataRow>();
	Integer mMessageId = null;
	ODataRow mMessageData = null;
	List<Object> mMessageObjects = new ArrayList<Object>();
	Integer[] mStarredDrawables = new Integer[] { R.drawable.ic_action_starred,
			R.drawable.ic_action_starred };

	ReadUnreadOperation mReadUnread = null;
	StarredOperation mStarredOperation = null;
	MailMessage mail = null;
	MailNotification noti = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		scope = new AppScope(this);
		mail = new MailMessage(getActivity());
		initArgs();

		return inflater.inflate(R.layout.mail_detail_layout, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mView = view;
		init();

	}

	private void initArgs() {
		Bundle args = getArguments();
		if (args.containsKey(OColumn.ROW_ID)) {
			mMailId = args.getInt(OColumn.ROW_ID);
		}
		noti = new MailNotification(getActivity());
	}

	public class StarredOperation extends AsyncTask<Void, Void, Boolean> {
		boolean mStarred = false;
		boolean isConnection = true;
		OSyncHelper mos = null;
		int mPos = 0, row_id = 0, sid = 0;

		public StarredOperation(int position, Boolean starred) {
			mPos = position;
			mos = db().getSyncHelper();
			mStarred = starred;
			if (mos == null)
				isConnection = false;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (!isConnection) {
				return false;
			}
			JSONArray mIds = new JSONArray();
			ODataRow row = (ODataRow) mRecords.get(mPos);
			mIds.put(row.getInt("id"));
			row_id = row.getInt(OColumn.ROW_ID);
			OArguments args = new OArguments();
			args.add(mIds);
			args.add(mStarred);
			args.add(true);
			OValues values = new OValues();
			String value = (mStarred) ? "true" : "false";
			values.put("starred", value);
			boolean response = (Boolean) mos.callMethod("set_message_starred",
					args, null);
			db().update(values, row_id);
			return response;
		}

		@Override
		protected void onPostExecute(Boolean result) {

		}

	}

	private void init() {
		mListMessages = (OList) mView.findViewById(R.id.lstMessageDetail);
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnStar, this);
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnReply, this);
		mListMessages.setBeforeListRowCreateListener(this);
		if (mMailId != null) {
			ODataRow parent = db().select(mMailId);
			OControls.setText(mView, R.id.txvDetailSubject,
					parent.getString("message_title"));
			mRecords.add(0, parent);
			mRecords.addAll(parent.getO2MRecord("child_ids")
					.setOrder("date DESC").browseEach());
			mListMessages.initListControl(mRecords);
		}
		mView.findViewById(R.id.btnStartFullComposeMode).setOnClickListener(
				this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_mail_detail, menu);
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
			if (inNetwork()) {
				ImageView imgStarred = (ImageView) view;
				boolean is_fav = row.getBoolean("starred");
				imgStarred.setColorFilter((is_fav) ? Color
						.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
				mStarredOperation = new StarredOperation(position,
						(is_fav) ? false : true);
				mStarredOperation.execute();
			} else {
				Toast.makeText(getActivity(), "Not in Network",
						Toast.LENGTH_SHORT).show();
			}
		} else if (view.getId() == R.id.imgBtnReply) {
			mView.findViewById(R.id.edtQuickReplyMessage).requestFocus();
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(mView.findViewById(R.id.edtQuickReplyMessage),
					InputMethodManager.SHOW_IMPLICIT);
		} else if (view.getId() == R.id.imgVotenb) {
			Toast.makeText(getActivity(), "Voted", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		mListMessages.showAsCard((position != 0));
		ImageView imgstar = (ImageView) view.findViewById(R.id.imgBtnStar);
		ImageView imgHasVoted = (ImageView) view.findViewById(R.id.imgHasVoted);
		boolean has_voted = row.getBoolean("has_voted");
		boolean is_favorite = row.getBoolean("starred");
		imgstar.setColorFilter((is_favorite) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));
		imgHasVoted.setColorFilter((has_voted) ? getActivity().getResources()
				.getColor(R.color.odoo_purple) : Color.parseColor("#aaaaaa"));
		scope.main().refreshDrawer(Mail.TAG);

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnStartFullComposeMode:
			Bundle bundle = new Bundle();
			bundle.putInt(KEY_MESSAGE_ID, mMailId);
			bundle.putString(KEY_SUBJECT,
					"Re: " + OControls.getText(mView, R.id.txvDetailSubject));
			bundle.putString(KEY_BODY,
					OControls.getText(mView, R.id.edtQuickReplyMessage));
			Intent intent = new Intent(getActivity(), ComposeMail.class);
			intent.putExtras(bundle);
			startActivity(intent);
			break;

		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// MailMessage mail = (MailMessage) db();

		switch (item.getItemId()) {
		case R.id.menu_mail_read:
			boolean is_read = noti.getIsread(mMailId);
			if (inNetwork()) {
				mReadUnread = new ReadUnreadOperation((is_read) ? false : true);
				mReadUnread.execute();
			} else {
				Toast.makeText(getActivity(), "No Internet Connection",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.menu_mail_unread:
			is_read = noti.getIsread(mMailId);
			if (inNetwork()) {
				mReadUnread = new ReadUnreadOperation((is_read) ? false : true);
				mReadUnread.execute();

			} else {
				Toast.makeText(getActivity(), "No Internet Connection",
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public class ReadUnreadOperation extends AsyncTask<Void, Void, Boolean> {
		boolean mIsRead = false;
		boolean isConnection = true;
		OSyncHelper mor = null;

		public ReadUnreadOperation(boolean isread) {
			mor = db().getSyncHelper();
			mIsRead = isread;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (!isConnection)
				return false;
			try {
				// update is_read server
				OArguments args = new OArguments();
				JSONArray mIds = new JSONArray();
				JSONObject newContext = new JSONObject();
				Object default_model = false;
				Object default_res_id = false;
				for (ODataRow row : db().select(
						OColumn.ROW_ID + " = ? or parent_id = ?",
						new String[] { mMailId + "", mMailId + "" })) {
					mIds.put(row.getInt("id"));
					if (row.getInt(OColumn.ROW_ID) == mMailId) {
						default_model = row.getString("model");
						default_res_id = row.getInt("res_id");
					}
				}
				newContext.put("default_parent_id", mMailId);
				newContext.put("default_model", default_model);
				newContext.put("default_res_id", default_res_id);
				args.add(mIds);
				args.add(mIsRead);
				args.add(true);
				args.add(newContext);
				Integer updated = (Integer) mor.callMethod("set_message_read",
						args, null);
				// update local table
				if (updated > 0) {
					OValues values = new OValues();
					if (noti.getColumn("is_read") != null)
						values.put("is_read", mIsRead);
					else
						values.put("read", mIsRead);
					// Updating notification model
					noti.update(values, "message_id = ?",
							new Object[] { mMailId });
					values = new OValues();
					values.put("to_read", !mIsRead);
					// updating mail message model
					db().update(values,
							OColumn.ROW_ID + " = ? or parent_id = ?",
							new Object[] { mMailId, mMailId });
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
		}
	}
}
