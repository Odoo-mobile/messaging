package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.controls.OField;
import odoo.controls.OForm.OnViewClickListener;
import odoo.controls.OList;
import odoo.controls.OList.BeforeListRowCreateListener;
import odoo.controls.OList.OnListRowViewClickListener;

import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.addons.mail.Mail.MarkAsTodo;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.models.MailNotification;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.R;

public class MailDetail extends BaseFragment implements OnViewClickListener,
		OnListRowViewClickListener, BeforeListRowCreateListener,
		OnClickListener {
	public static final String TAG = "com.odoo.addons.mail.MailDetail";
	public static final String KEY_MESSAGE_REPLY_ID = "message_reply_id";
	public static final String KEY_MESSAGE_ID = "message_id";
	public static final String KEY_SUBJECT = "subject";
	public static final String KEY_BODY = "body";
	public static final Integer REQUEST_REPLY = 125;
	private View mView = null;
	private Integer mMailId = null;
	private OList mListMessages = null;
	private List<ODataRow> mRecords = new ArrayList<ODataRow>();
	private Context mContext = null;
	private MailMessage mail = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		scope = new AppScope(this);
		mContext = getActivity();
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
		mail = (MailMessage) db();
		if (args.containsKey(OColumn.ROW_ID)) {
			mMailId = args.getInt(OColumn.ROW_ID);
		}
	}

	private void init() {
		mListMessages = (OList) mView.findViewById(R.id.lstMessageDetail);
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnStar, this);
		mListMessages.setOnListRowViewClickListener(R.id.imgBtnReply, this);
		mListMessages.setOnListRowViewClickListener(R.id.imgVotenb, this);
		mListMessages.setBeforeListRowCreateListener(this);
		mView.findViewById(R.id.btnSendQuickReply).setOnClickListener(this);

		if (mMailId != null) {
			ODataRow parent = db().select(mMailId);
			if (parent.getInt("id") == 0) {
				OControls.setInvisible(mView, R.id.quickReplyBox);
			}
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
		switch (view.getId()) {
		case R.id.imgBtnStar:
			if (inNetwork()) {
				boolean starred = new MailNotification(getActivity())
						.getStarred(row.getInt(OColumn.ROW_ID));
				ImageView imgStarred = (ImageView) view;
				imgStarred.setColorFilter((!starred) ? Color
						.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
				new MarkAsTodo(getActivity(), row, !starred).execute();
			} else {
				Toast.makeText(getActivity(), "No Connection",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.imgBtnReply:
			mView.findViewById(R.id.edtQuickReplyMessage).requestFocus();
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(mView.findViewById(R.id.edtQuickReplyMessage),
					InputMethodManager.SHOW_IMPLICIT);
			break;
		case R.id.imgVotenb:
			if (inNetwork()) {
				try {
					MailMessage mail = new MailMessage(mContext);
					int mail_id = row.getInt("id");
					OSyncHelper helper = db().getSyncHelper();
					OArguments args = new OArguments();
					args.add(new JSONArray().put(mail_id));
					Boolean response = (Boolean) helper.callMethod(
							"vote_toggle", args);
					ImageView imgVote = (ImageView) view
							.findViewById(R.id.imgHasVoted);
					OField voteCounter = (OField) view
							.findViewById(R.id.voteCounter);
					int votes = (!voteCounter.getText().equals("")) ? Integer
							.parseInt(voteCounter.getText()) : 0;
					boolean has_voted = false;
					if (response) {
						// Vote up
						mail.addManyToManyRecord("vote_user_ids",
								row.getInt("local_id"), mail.author_id());
						mListMessages.initListControl(mRecords);
						has_voted = true;
						votes++;
					} else {
						// Vote down
						mail.deleteManyToManyRecord("vote_user_ids",
								row.getInt("local_id"), mail.author_id());
						mListMessages.initListControl(mRecords);
						votes--;
					}
					voteCounter.setText((votes > 0) ? votes + "" : "");
					imgVote.setColorFilter((has_voted) ? getActivity()
							.getResources().getColor(R.color.odoo_purple)
							: Color.parseColor("#aaaaaa"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Toast.makeText(getActivity(), "No Connection",
						Toast.LENGTH_LONG).show();
			}
			break;
		}
	}

	@Override
	public void beforeListRowCreate(int position, ODataRow row, View view) {
		mListMessages.showAsCard((position != 0));
		view.setBackgroundColor((position == 0) ? Color.parseColor("#e5e5e5")
				: Color.TRANSPARENT);
		ImageView imgstar = (ImageView) view.findViewById(R.id.imgBtnStar);
		ImageView imgHasVoted = (ImageView) view.findViewById(R.id.imgHasVoted);
		boolean has_voted = row.getBoolean("has_voted");
		boolean is_favorite = row.getBoolean("starred");
		imgstar.setColorFilter((is_favorite) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));
		imgHasVoted.setColorFilter((has_voted) ? getActivity().getResources()
				.getColor(R.color.odoo_purple) : Color.parseColor("#aaaaaa"));
		scope.main().refreshDrawer(Mail.TAG);
		if (mail.hasAttachment(row) == true)
			OControls.setVisible(view, R.id.msg_attachment);
		else
			OControls.setGone(view, R.id.msg_attachment);

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
			startActivityForResult(intent, REQUEST_REPLY);
			break;
		case R.id.btnSendQuickReply:
			EditText edt = (EditText) mView
					.findViewById(R.id.edtQuickReplyMessage);
			edt.setError(null);
			if (TextUtils.isEmpty(edt.getText())) {
				edt.setError("Message required");
			} else {
				MailMessage mail = new MailMessage(getActivity());
				String subject = mRecords.get(0).getString("message_title");
				String mail_body = OControls.getText(mView,
						R.id.edtQuickReplyMessage);
				Integer replyId = mail.sendQuickReply(subject, mail_body,
						mMailId);
				if (replyId != null) {
					List<ODataRow> newRecord = new ArrayList<ODataRow>();
					newRecord.add(db().select(replyId));
					mListMessages.appendRecords(1, newRecord);
					if (inNetwork()) {
						scope.main().requestSync(MailProvider.AUTHORITY);
						Toast.makeText(getActivity(), "Reply Sent",
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getActivity(), "Reply can't send",
								Toast.LENGTH_LONG).show();
					}
					OControls.setText(mView, R.id.edtQuickReplyMessage, "");
				}
			}
			break;
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_REPLY && resultCode == Activity.RESULT_OK) {
			int replyId = data.getExtras().getInt(
					MailDetail.KEY_MESSAGE_REPLY_ID);
			List<ODataRow> newRecord = new ArrayList<ODataRow>();
			newRecord.add(db().select(replyId));
			mListMessages.appendRecords(1, newRecord);
			OControls.setText(mView, R.id.edtQuickReplyMessage, "");
			if (inNetwork()) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				Toast.makeText(getActivity(), "Reply Sent", Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(getActivity(), "Reply can't send",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_mail_read:
			if (inNetwork())
				new MarkAsReadUnread(getActivity(), db().select(mMailId), true)
						.execute();
			else
				Toast.makeText(getActivity(), "No Internet Connection",
						Toast.LENGTH_SHORT).show();

			break;
		case R.id.menu_mail_unread:
			if (inNetwork())
				new MarkAsReadUnread(getActivity(), db().select(mMailId), false)
						.execute();
			else
				Toast.makeText(getActivity(), "No Connection",
						Toast.LENGTH_SHORT).show();

			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class MarkAsReadUnread extends AsyncTask<Void, Void, Boolean> {
		private ODataRow mRecord = null;
		private Context mContext = null;
		private Boolean mIsRead = null;

		public MarkAsReadUnread(Context context, ODataRow row, Boolean is_read) {
			mContext = context;
			mRecord = row;
			mIsRead = is_read;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			MailMessage mail = new MailMessage(mContext);
			return mail.markAsRead(mRecord, mIsRead);
		}

	}
}
