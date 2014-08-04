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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
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
		mView = view;
		init();

	}

	private void initArgs() {
		Bundle args = getArguments();
		if (args.containsKey(OColumn.ROW_ID)) {
			mMailId = args.getInt(OColumn.ROW_ID);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MailMessage mail = (MailMessage) db();
		switch (item.getItemId()) {
		case R.id.menu_mail_read:
			mail.markAsRead(true, mMailId);
			scope.main().refreshDrawer(Mail.TAG);
			Toast.makeText(getActivity(), "Mark as Read", Toast.LENGTH_SHORT)
					.show();
			break;
		case R.id.menu_mail_unread:
			mail.markAsRead(false, mMailId);
			Toast.makeText(getActivity(), "Mark as Un Read", Toast.LENGTH_SHORT)
					.show();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
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
			ImageView imgStarred = (ImageView) view;
			boolean is_fav = row.getBoolean("starred");
			imgStarred.setColorFilter((!is_fav) ? Color.parseColor("#FF8800")
					: Color.parseColor("#aaaaaa"));
			OValues values = new OValues();
			values.put("starred", !is_fav);
			db().update(values, row.getInt(OColumn.ROW_ID));
			row.put("starred", !is_fav);
			scope.main().refreshDrawer(Mail.TAG);
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
		if (position == 0) {
			view.setBackgroundColor(Color.parseColor("#ebebeb"));
		} else {
			view.setBackgroundColor(Color.WHITE);
		}
		ImageView imgstar = (ImageView) view.findViewById(R.id.imgBtnStar);
		boolean is_favorite = row.getBoolean("starred");
		imgstar.setColorFilter((is_favorite) ? Color.parseColor("#FF8800")
				: Color.parseColor("#aaaaaa"));
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

}
