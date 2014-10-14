package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.OArguments;
import odoo.controls.OField;
import odoo.controls.OForm.OnViewClickListener;

import org.json.JSONArray;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.addons.mail.providers.mail.MailProvider;
import com.odoo.base.ir.Attachments;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.AsyncTaskListener;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.BeforeBindUpdateData;
import com.odoo.support.listview.OCursorListAdapter.OnRowViewClickListener;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.util.OControls;
import com.odoo.util.PreferenceManager;
import com.odoo.util.drawer.DrawerItem;

public class MailDetail extends BaseFragment implements
		LoaderCallbacks<Cursor>, OnRowViewClickListener, OnViewBindListener,
		OnClickListener, BeforeBindUpdateData, OnViewClickListener {
	public static final String KEY_MESSAGE_ID = "mail_id";
	public static final String KEY_SUBJECT = "mail_subject";
	public static final String KEY_BODY = "mail_body";
	public static final int KEY_MESSAGE_REPLY_ID = 125;
	private Context mContext = null;
	private Integer mMailId = null;
	private String selection = null;
	private String[] args;
	private View mView = null;
	private ListView mailList = null;
	private OCursorListAdapter mAdapter;
	private ImageView imgBtn_send_reply, btnStartFullComposeMode;
	private Menu mMenu;
	private PreferenceManager mPref;
	private Attachments mAttachment;
	private Cursor cr = null;
	private List<Object> mBuilderitems = new ArrayList<Object>();
	private List<ODataRow> mVoters = new ArrayList<ODataRow>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		scope = new AppScope(this);
		mContext = getActivity();
		mPref = new PreferenceManager(mContext);
		mAttachment = new Attachments(mContext);
		initArgs();
		return inflater.inflate(R.layout.mail_detail_layout, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mView = view;
		init();
		imgBtn_send_reply.setOnClickListener(this);
		btnStartFullComposeMode.setOnClickListener(this);
		mailList = (ListView) view.findViewById(R.id.lstMessageDetail);
		mAdapter = new OCursorListAdapter(mContext, null,
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
		mAdapter.setBeforeBindUpdateData(this);
		mAdapter.setOnRowViewClickListener(R.id.voteCounter, this);
		mAdapter.setOnRowViewClickListener(R.id.voteCounter, this);
		mAdapter.setOnViewBindListener(this);
		mailList.setAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	private void initArgs() {
		Bundle args = getArguments();
		if (args.containsKey(OColumn.ROW_ID)) {
			mMailId = args.getInt(OColumn.ROW_ID);
			boolean autoArchive = mPref.getBoolean("mail_auto_archive", false);
			if (autoArchive) {
				archiveMail(false);
			}
		}
	}

	public void init() {
		imgBtn_send_reply = (ImageView) mView
				.findViewById(R.id.btnSendQuickReply);
		btnStartFullComposeMode = (ImageView) mView
				.findViewById(R.id.btnStartFullComposeMode);
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

	private void toggleMailToRead(int mailId, boolean to_read) {
		ContentValues values = new ContentValues();
		values.put("to_read", (to_read) ? 1 : 0);
		if (!inNetwork())
			values.put("is_dirty", 1);
		String selection = OColumn.ROW_ID + " = ? or parent_id = ?";
		String[] args = new String[] { mailId + "", mailId + "" };
		getActivity().getContentResolver().update(db().uri(), values,
				selection, args);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Boolean to_read = false;
		switch (item.getItemId()) {
		case R.id.menu_mail_read:
			to_read = false;
			mMenu.findItem(R.id.menu_mail_unread).setVisible(true);
			mMenu.findItem(R.id.menu_mail_read).setVisible(false);
			break;
		case R.id.menu_mail_unread:
			to_read = true;
			mMenu.findItem(R.id.menu_mail_unread).setVisible(false);
			mMenu.findItem(R.id.menu_mail_read).setVisible(true);
			break;
		default:
			break;
		}
		archiveMail(to_read);
		return super.onOptionsItemSelected(item);
	}

	private void archiveMail(final Boolean to_read) {
		toggleMailToRead(mMailId, to_read);
		scope.main().newBackgroundTask(new AsyncTaskListener() {

			@Override
			public Object onPerformTask() {
				if (inNetwork()) {
					MailMessage mail = new MailMessage(getActivity());
					mail.markMailReadUnread(mMailId, to_read);
				}
				return null;
			}

			@Override
			public void onFinish(Object result) {
				restartLoader();
			}
		}).execute();
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
		return new CursorLoader(mContext, uri, new String[] { "message_title",
				"author_name", "author_id", "total_childs", "parent_id",
				"date", "to_read", "body", "starred" }, selection, args,
				"date DESC");
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_mail_detail, menu);
		mMenu = menu;
		if (cr != null) {
			updateMenuVisibility();
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
		cr = cursor;
		updateMenuVisibility();
	}

	private void updateMenuVisibility() {
		boolean to_read_flag = false;
		if (cr.moveToFirst()) {
			do {
				int to_read = cr.getInt(cr.getColumnIndex("to_read"));
				if (to_read == 1) {
					to_read_flag = true;
					break;
				}
			} while (cr.moveToNext());
			int menu_id = R.id.menu_mail_unread;
			if (!to_read_flag) {
				menu_id = R.id.menu_mail_read;
			}
			if (mMenu != null)
				mMenu.findItem(menu_id).setVisible(false);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onRowViewClick(int position, Cursor cursor, final View view,
			View parent) {
		final MailMessage mail = new MailMessage(mContext);
		final Cursor c = cursor;
		switch (view.getId()) {
		case R.id.voteCounter:
			if (inNetwork()) {
				final ImageView imgVote = (ImageView) view
						.findViewById(R.id.imgBtn_mail_detail_rate);
				final TextView voteCounter = (TextView) view
						.findViewById(R.id.txv_voteCounter);

				try {
					final int mail_id = c.getInt(c.getColumnIndex("id"));
					scope.main().newBackgroundTask(new AsyncTaskListener() {
						boolean has_voted = false;

						@Override
						public Object onPerformTask() {
							int votes = (!voteCounter.getText().equals("")) ? Integer
									.parseInt(voteCounter.getText().toString())
									: 0;
							OSyncHelper helper = db().getSyncHelper();
							OArguments args = new OArguments();
							args.add(new JSONArray().put(mail_id));
							Boolean response = (Boolean) helper.callMethod(
									"vote_toggle", args);
							if (response) {
								// Vote up
								mail.addManyToManyRecord(
										"vote_user_ids",
										c.getInt(c
												.getColumnIndex(OColumn.ROW_ID)),
										mail.author_id());
								has_voted = true;
								votes++;
							} else {
								// Vote down
								mail.deleteManyToManyRecord(
										"vote_user_ids",
										c.getInt(c
												.getColumnIndex(OColumn.ROW_ID)),
										mail.author_id());
								votes--;
							}
							return votes;
						}

						@Override
						public void onFinish(Object result) {
							int votes = (Integer) result;
							voteCounter.setText((votes > 0) ? votes + "" : "");
							imgVote.setColorFilter((has_voted) ? mContext
									.getResources().getColor(
											R.color.odoo_purple) : Color
									.parseColor("#aaaaaa"));
						}
					}).execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Toast.makeText(mContext, _s(R.string.no_connection),
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.imgBtn_mail_detail_starred:
			String starred = "";
			starred = c.getString(c.getColumnIndex("starred"));
			final boolean is_fav = !starred.equals("1");
			if (inNetwork()) {
				ImageView imgStarred = (ImageView) view;
				imgStarred.setColorFilter((is_fav) ? Color
						.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
				// markAsTodo
				scope.main().newBackgroundTask(new AsyncTaskListener() {

					@Override
					public Object onPerformTask() {
						MailMessage mail = (MailMessage) db();
						mail.markAsTodo(c, is_fav);
						return null;
					}

					@Override
					public void onFinish(Object result) {
						if (getActivity() != null)
							restartLoader();
					}
				}).execute();
			} else {
				Toast.makeText(mContext, _s(R.string.no_connection),
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
	}

	private void restartLoader() {
		if (getActivity() != null)
			getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onViewBind(View view, Cursor cr, final ODataRow row) {

		// Setting starred color
		final ResPartner res = new ResPartner(mContext);

		LinearLayout lvote_Counter = (LinearLayout) view
				.findViewById(R.id.voteCounter);
		ImageView imgStarred = (ImageView) view
				.findViewById(R.id.imgBtn_mail_detail_starred);
		String is_fav = cr.getString(cr.getColumnIndex("starred"));
		imgStarred.setColorFilter((is_fav.equals("1")) ? Color
				.parseColor("#FF8800") : Color.parseColor("#aaaaaa"));
		List<Integer> voters_id = row.getM2MRecord("vote_user_ids").getRelIds();

		int voters = voters_id.size();
		if (voters > 0) {
			int author_id = ((MailMessage) db()).author_id();
			ImageView selfVoted = (ImageView) view
					.findViewById(R.id.imgBtn_mail_detail_rate);
			TextView txvVotes = (TextView) view
					.findViewById(R.id.txv_voteCounter);
			if (voters_id.indexOf(author_id) > -1) {
				selfVoted.setColorFilter(_c(R.color.odoo_purple));
				txvVotes.setTextColor(_c(R.color.odoo_purple));
			} else {
				selfVoted.setColorFilter(Color.parseColor("#aaaaaa"));
				txvVotes.setTextColor(Color.parseColor("#aaaaaa"));
			}
			txvVotes.setText(voters + "");
		}
		if (view.findViewById(R.id.txvTotalChilds) != null) {
			TextView totalChilds = (TextView) view
					.findViewById(R.id.txvTotalChilds);
			int replies = Integer.parseInt(cr.getString(cr
					.getColumnIndex("total_childs")));
			String childs = "No replies";
			if (replies > 0) {
				childs = replies + " replies";
			}
			totalChilds.setText(childs);
		}
		OField mfield = (OField) view.findViewById(R.id.msgAttachments);
		if (row.getM2MRecord("attachment_ids").getRelIds().size() > 0) {
			mfield.setOnItemClickListener(this);
			mfield.reInit();
		} else {
			mfield.setVisibility(View.GONE);
		}
		String names = ((MailMessage) db()).getPartnersName(row);
		OControls.setText(view, R.id.partner_names, names);

		lvote_Counter.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				mBuilderitems.clear();
				mVoters = row.getM2MRecord("vote_user_ids").browseEach();
				if (mVoters.size() > 0) {
					for (ODataRow row : mVoters)
						mBuilderitems.add(res.select(row.getInt("_id")));
				}
				new VoterDialog(mContext, mVoters, mBuilderitems).build()
						.show();
				return true;
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnSendQuickReply:
			if (getPref().getBoolean("confirm_send_mail", false)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				builder.setTitle(_s(R.string.dialog_send_reply_title));
				builder.setMessage(_s(R.string.dialog_send_reply_message));
				builder.setPositiveButton(
						_s(R.string.dialog_send_reply_positive_button_text),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								sendQuickMail();
							}
						});
				builder.setNegativeButton(
						_s(R.string.dialog_send_reply_negative_button_text),
						null);
				builder.show();
			} else {
				sendQuickMail();
			}
			break;
		case R.id.btnStartFullComposeMode:
			Bundle bundle = new Bundle();
			bundle.putInt(KEY_MESSAGE_ID, mMailId);
			bundle.putString(KEY_SUBJECT,
					"Re: " + OControls.getText(mView, R.id.txvDetailSubject));
			bundle.putString(KEY_BODY,
					OControls.getText(mView, R.id.edtQuickReplyMessage));
			Intent intent = new Intent(getActivity(), ComposeMail.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, KEY_MESSAGE_REPLY_ID);
			break;
		default:
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == KEY_MESSAGE_REPLY_ID
				&& resultCode == Activity.RESULT_OK) {
			if (inNetwork()) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				Toast.makeText(getActivity(), _s(R.string.message_sent),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getActivity(), _s(R.string.message_cant_sent),
						Toast.LENGTH_LONG).show();
			}
			getLoaderManager().restartLoader(0, null, this);
			OControls.setText(mView, R.id.edtQuickReplyMessage, "");
		}
	}

	private void sendQuickMail() {
		EditText edt = (EditText) mView.findViewById(R.id.edtQuickReplyMessage);
		ODataRow parent = db().select(mMailId);
		MailMessage mail = new MailMessage(mContext);
		edt.setError(null);
		if (TextUtils.isEmpty(edt.getText())) {
			edt.setError("Message required");
		} else {
			String subject = parent.getString("message_title");
			String mail_body = OControls.getText(mView,
					R.id.edtQuickReplyMessage);
			ContentValues values = new ContentValues();
			values.put("message_title", subject);
			values.put("body", mail_body);
			mail.sendQuickReply(null, subject, mail_body, mMailId,
					parent.getInt("total_childs"));
			getLoaderManager().restartLoader(0, null, this);
			if (inNetwork()) {
				scope.main().requestSync(MailProvider.AUTHORITY);
				Toast.makeText(mContext, _s(R.string.reply_sent),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(mContext, _s(R.string.reply_cant_sent),
						Toast.LENGTH_LONG).show();
			}
			OControls.setText(mView, R.id.edtQuickReplyMessage, "");
		}
	}

	@Override
	public ODataRow updateDataRow(Cursor cr) {
		ODataRow row = new ODataRow();
		ODataRow rec = db()
				.selectRelRecord(
						new String[] { "attachment_ids", "vote_user_ids",
								"partner_ids" },
						cr.getInt(cr.getColumnIndex(OColumn.ROW_ID)));
		row.addAll(rec.getAll());
		String author_image = "false";
		if (!cr.getString(cr.getColumnIndex("author_id")).equals("false")) {
			author_image = new ResPartner(getActivity()).select(
					cr.getInt(cr.getColumnIndex("author_id"))).getString(
					"image_small");
		}
		row.put("author_id_image_small", author_image);
		return row;
	}

	@Override
	public void onFormViewClick(View view, ODataRow row) {
		mAttachment.downloadAttachment(row.getInt(OColumn.ROW_ID));
	}
}
