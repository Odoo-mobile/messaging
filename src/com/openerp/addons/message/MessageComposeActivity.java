/**
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http://www.openerp.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * 
 */
package com.openerp.addons.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import openerp.OEArguments;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.R;
import com.openerp.addons.message.providers.message.MessageProvider;
import com.openerp.addons.note.NoteDB;
import com.openerp.addons.note.NoteDetail;
import com.openerp.auth.OpenERPAccountManager;
import com.openerp.base.ir.Attachment;
import com.openerp.base.res.ResPartnerDB;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEM2MIds;
import com.openerp.orm.OEM2MIds.Operation;
import com.openerp.orm.OEValues;
import com.openerp.support.OEUser;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.support.listview.OEListAdapter.RowFilterTextListener;
import com.openerp.util.Base64Helper;
import com.openerp.util.HTMLHelper;
import com.openerp.util.OEDate;
import com.openerp.util.controls.ExpandableHeightGridView;
import com.openerp.util.tags.MultiTagsTextView.TokenListener;
import com.openerp.util.tags.TagsView;
import com.openerp.util.tags.TagsView.CustomTagViewListener;

public class MessageComposeActivity extends Activity implements TokenListener {

	public static final String TAG = "com.openerp.addons.message.MessageComposeActivity";
	public static final Integer PICKFILE_RESULT_CODE = 1;
	Context mContext = null;

	Boolean isReply = false;
	Boolean isQuickCompose = false;
	Integer mParentMessageId = 0;
	OEDataRow mMessageRow = null;

	OEDataRow mNoteObj = null;

	HashMap<String, OEDataRow> mSelectedPartners = new HashMap<String, OEDataRow>();

	/**
	 * DBs
	 */
	ResPartnerDB mPartnerDB = null;
	MessageDB mMessageDB = null;
	PartnerLoader mPartnerLoader = null;

	/**
	 * Attachment
	 */
	Attachment mAttachment = null;
	List<Object> mAttachments = new ArrayList<Object>();
	ExpandableHeightGridView mAttachmentGridView = null;
	OEListAdapter mAttachmentAdapter = null;

	enum AttachmentType {
		IMAGE, FILE
	}

	/**
	 * Controls & Adapters
	 */
	TagsView mPartnerTagsView = null;
	OEListAdapter mPartnerTagsAdapter = null;
	List<Object> mTagsPartners = new ArrayList<Object>();
	EditText edtSubject = null, edtBody = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message_compose);
		mContext = this;

		if (OEUser.current(mContext) == null) {
			// No Account
			Toast.makeText(mContext, "No Account Found", Toast.LENGTH_LONG)
					.show();
			finish();
		} else {
			initDBs();
			initActionbar();
			handleIntent();
			initControls();
			checkForContact();
		}
	}

	private void initControls() {
		if (!isReply) {
			mPartnerLoader = new PartnerLoader();
			mPartnerLoader.execute();
		}

		mPartnerTagsView = (TagsView) findViewById(R.id.receipients_view);
		mPartnerTagsView.setCustomTagView(new CustomTagViewListener() {

			@Override
			public View getViewForTags(LayoutInflater layoutInflater,
					Object object, ViewGroup tagsViewGroup) {
				OEDataRow row = (OEDataRow) object;
				View mView = layoutInflater.inflate(
						R.layout.fragment_message_receipient_tag_layout, null);
				TextView txvSubject = (TextView) mView
						.findViewById(R.id.txvTagSubject);
				txvSubject.setText(row.getString("name"));
				ImageView imgPic = (ImageView) mView
						.findViewById(R.id.imgTagImage);
				if (!row.getString("image_small").equals("false")) {
					imgPic.setImageBitmap(Base64Helper.getBitmapImage(
							getApplicationContext(),
							row.getString("image_small")));
				}
				return mView;
			}
		});
		mPartnerTagsAdapter = new OEListAdapter(this,
				R.layout.tags_view_partner_item_layout, mTagsPartners) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View mView = convertView;
				if (mView == null) {
					mView = getLayoutInflater().inflate(getResource(), parent,
							false);
				}
				OEDataRow row = (OEDataRow) mTagsPartners.get(position);
				TextView txvSubject = (TextView) mView
						.findViewById(R.id.txvSubject);
				TextView txvSubSubject = (TextView) mView
						.findViewById(R.id.txvSubSubject);
				ImageView imgPic = (ImageView) mView
						.findViewById(R.id.imgReceipientPic);
				txvSubject.setText(row.getString("name"));
				if (!row.getString("email").equals("false")) {
					txvSubSubject.setText(row.getString("email"));
				} else {
					txvSubSubject.setText("No email");
				}
				if (!row.getString("image_small").equals("false")) {
					imgPic.setImageBitmap(Base64Helper.getBitmapImage(mContext,
							row.getString("image_small")));
				}
				return mView;
			}
		};
		mPartnerTagsAdapter
				.setRowFilterTextListener(new RowFilterTextListener() {

					@Override
					public String filterCompareWith(Object object) {
						OEDataRow row = (OEDataRow) object;
						return row.getString("name") + " "
								+ row.getString("email");
					}
				});
		mPartnerTagsView.setAdapter(mPartnerTagsAdapter);
		mPartnerTagsView.setPrefix("To: ");
		mPartnerTagsView.allowDuplicates(false);
		mPartnerTagsView.setTokenListener(this);

		// Attachment View
		mAttachmentGridView = (ExpandableHeightGridView) findViewById(R.id.lstAttachments);
		mAttachmentGridView.setExpanded(true);
		mAttachmentAdapter = new OEListAdapter(this,
				R.layout.activity_message_compose_attachment_file_view_item,
				mAttachments) {
			@Override
			public View getView(final int position, View convertView,
					ViewGroup parent) {
				OEDataRow row = (OEDataRow) mAttachments.get(position);
				View mView = convertView;
				if (mView == null)
					mView = getLayoutInflater().inflate(getResource(), parent,
							false);
				TextView txvFileName = (TextView) mView
						.findViewById(R.id.txvFileName);
				txvFileName.setText(row.getString("name"));

				ImageView imgAttachmentImg = (ImageView) mView
						.findViewById(R.id.imgAttachmentFile);
				if (!row.getString("file_type").contains("image")) {
					imgAttachmentImg
							.setImageResource(R.drawable.file_attachment);
				} else {
					imgAttachmentImg.setImageURI(Uri.parse(row
							.getString("file_uri")));
				}
				mView.findViewById(R.id.imgBtnRemoveAttachment)
						.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								mAttachments.remove(position);
								mAttachmentAdapter
										.notifiyDataChange(mAttachments);
							}
						});
				return mView;
			}
		};
		mAttachmentGridView.setAdapter(mAttachmentAdapter);

		// Edittext
		edtSubject = (EditText) findViewById(R.id.edtMessageSubject);
		edtBody = (EditText) findViewById(R.id.edtMessageBody);
	}

	private void initDBs() {
		mPartnerDB = new ResPartnerDB(mContext);
		mMessageDB = new MessageDB(mContext);
		mAttachment = new Attachment(mContext);
		mAttachments.clear();
	}

	private void checkForContact() {
		Intent intent = getIntent();
		handleIntentFilter(intent);
		if (intent.getData() != null) {
			Cursor cursor = getContentResolver().query(intent.getData(), null,
					null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int partner_id = cursor.getInt(cursor.getColumnIndex("data2"));
				OEDataRow row = mPartnerDB.select(partner_id);
				mSelectedPartners.put("key_" + row.getString("id"), row);
				mPartnerTagsView.addObject(row);
				isQuickCompose = true;
			}
		}

		if (isReply) {
			mMessageRow = mMessageDB.select(mParentMessageId);
			List<OEDataRow> partners = mMessageRow.getM2MRecord("partner_ids")
					.browseEach();
			if (partners != null) {
				for (OEDataRow partner : partners) {
					mSelectedPartners.put("key_" + partner.getString("id"),
							partner);
					mPartnerTagsView.addObject(partner);
				}
			}
			edtSubject.setText("Re: " + mMessageRow.getString("subject"));
			edtBody.requestFocus();
		}
	}

	private void handleIntent() {
		Intent intent = getIntent();
		String title = "Compose";
		if (intent.hasExtra("send_reply")) {
			isReply = true;
			mParentMessageId = intent.getExtras().getInt("message_id");
			title = "Reply";
		}
		setTitle(title);
	}

	private void initActionbar() {
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_message_compose_activty, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_message_compose_add_attachment_images:
			mAttachment
					.requestAttachment(Attachment.Types.IMAGE_OR_CAPTURE_IMAGE);
			return true;
		case R.id.menu_message_compose_add_attachment_files:
			mAttachment.requestAttachment(Attachment.Types.FILE);
			return true;
		case R.id.menu_message_compose_send:
			edtSubject.setError(null);
			edtBody.setError(null);
			if (mSelectedPartners.size() == 0)
				Toast.makeText(mContext, "Select atleast one receiptent",
						Toast.LENGTH_LONG).show();
			else if (TextUtils.isEmpty(edtSubject.getText())) {
				edtSubject.setError("Provide Message Subject !");
			} else if (TextUtils.isEmpty(edtBody.getText())) {
				edtBody.setError("Provide Message Body !");
			} else {
				Toast.makeText(this, "Sending message...", Toast.LENGTH_LONG)
						.show();
				SendMessage sendMessage = new SendMessage();
				sendMessage.execute();
				if (isQuickCompose)
					finish();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	class SendMessage extends AsyncTask<Void, Void, Void> {
		OEHelper mOE = null;
		boolean isConnection = true;
		String mToast = "";
		int newMessageId = 0;

		public SendMessage() {
			mOE = mMessageDB.getOEInstance();
			if (mOE == null)
				isConnection = false;
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (isConnection) {
				Object record_name = false, res_model = false;
				int res_id = 0;
				List<Integer> attachmentIds = new ArrayList<Integer>();
				if (mNoteObj == null) {
					mAttachment.updateAttachments("mail.compose.message", 0,
							mAttachments, false);
					List<Long> lAttachmentIds = mAttachment.newAttachmentIds();
					for (long id : lAttachmentIds)
						attachmentIds.add(Integer.parseInt(id + ""));
				} else {
					for (Object obj : mAttachments) {
						OEDataRow attachment = (OEDataRow) obj;
						attachmentIds.add(attachment.getInt("id"));
					}
					record_name = edtSubject.getText().toString();
					res_model = "note.note";
					res_id = mNoteObj.getInt("id");
				}
				try {
					OEDataRow user = new ResPartnerDB(mContext).select(OEUser
							.current(mContext).getPartner_id());
					OEArguments args = new OEArguments();

					// Partners
					JSONArray partners = new JSONArray();
					List<Integer> partner_ids_list = new ArrayList<Integer>();
					for (String key : mSelectedPartners.keySet()) {
						partners.put(mSelectedPartners.get(key).getInt("id"));
						partner_ids_list.add(mSelectedPartners.get(key).getInt(
								"id"));
					}
					JSONArray partner_ids = new JSONArray();
					if (partners.length() > 0) {
						partner_ids.put(6);
						partner_ids.put(false);
						partner_ids.put(partners);
					}

					// attachment ids
					JSONArray attachment_ids = new JSONArray();
					if (attachmentIds.size() > 0) {
						attachment_ids.put(6);
						attachment_ids.put(false);
						attachment_ids.put(new JSONArray(attachmentIds
								.toString()));
					}
					if (!isReply) {
						mToast = "Message sent.";
						JSONObject arguments = new JSONObject();
						arguments.put("composition_mode", "comment");
						arguments.put("model", res_model);
						arguments.put("parent_id", false);
						String email_from = user.getString("name") + " <"
								+ user.getString("email") + ">";
						arguments.put("email_from", email_from);
						arguments.put("subject", edtSubject.getText()
								.toString());
						arguments.put("body", edtBody.getText().toString());
						arguments.put("post", true);
						arguments.put("notify", false);
						arguments.put("same_thread", true);
						arguments.put("use_active_domain", false);
						arguments.put("reply_to", false);
						arguments.put("res_id", res_id);
						arguments.put("record_name", record_name);

						if (partner_ids.length() > 0)
							arguments.put("partner_ids", new JSONArray("["
									+ partner_ids.toString() + "]"));
						else
							arguments.put("partner_ids", new JSONArray());

						if (attachment_ids.length() > 0)
							arguments.put("attachment_ids", new JSONArray("["
									+ attachment_ids.toString() + "]"));
						else
							arguments.put("attachment_ids", new JSONArray());
						arguments.put("template_id", false);

						JSONObject kwargs = new JSONObject();
						kwargs.put("context",
								mOE.openERP().updateContext(new JSONObject()));

						args.add(arguments);
						String model = "mail.compose.message";

						// Creating compose message
						int id = (Integer) mOE.call_kw(model, "create", args,
								null, kwargs);

						// Resetting kwargs
						args = new OEArguments();
						args.add(new JSONArray().put(id));
						args.add(mOE.openERP().updateContext(new JSONObject()));

						// Sending mail
						mOE.call_kw(model, "send_mail", args, null, null);
						syncMessage();
					} else {
						mToast = "Message reply sent.";
						String model = "mail.thread";
						String method = "message_post";
						args = new OEArguments();
						args.add(false);

						JSONObject context = new JSONObject();
						res_id = mMessageRow.getInt("res_id");
						res_model = mMessageRow.getString("model");
						context.put("default_model",
								(res_model.equals("false") ? false : res_model));
						context.put("default_res_id", (res_id == 0) ? false
								: res_id);
						context.put("default_parent_id", mParentMessageId);
						context.put("mail_post_autofollow", true);
						context.put("mail_post_autofollow_partner_ids",
								new JSONArray());

						JSONObject kwargs = new JSONObject();
						kwargs.put("context", context);
						kwargs.put("subject", edtSubject.getText().toString());
						kwargs.put("body", edtBody.getText().toString());
						kwargs.put("parent_id", mParentMessageId);
						kwargs.put("attachment_ids", attachmentIds);
						if (partner_ids.length() > 0)
							kwargs.put("partner_ids", new JSONArray("["
									+ partner_ids.toString() + "]"));
						else
							kwargs.put("partner_ids", new JSONArray());
						newMessageId = (Integer) mOE.call_kw(model, method,
								args, null, kwargs);

						// Creating local entry
						OEValues values = new OEValues();

						OEM2MIds partnerIds = new OEM2MIds(Operation.ADD,
								partner_ids_list);
						values.put("id", newMessageId);
						values.put("partner_ids", partnerIds);
						values.put("subject", edtSubject.getText().toString());
						values.put("type", "comment");
						values.put("body", edtBody.getText().toString());
						values.put("email_from", false);
						values.put("parent_id", mParentMessageId);
						values.put("record_name", false);
						values.put("to_read", false);
						values.put("author_id", user.getInt("id"));
						values.put("model", res_model);
						values.put("res_id", res_id);
						values.put("date", OEDate.getDate());
						values.put("has_voted", false);
						values.put("vote_nb", 0);
						values.put("starred", false);
						OEM2MIds attachment_Ids = new OEM2MIds(Operation.ADD,
								attachmentIds);
						values.put("attachment_ids", attachment_Ids);
						newMessageId = (int) mMessageDB.create(values);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (!isConnection) {
				Toast.makeText(mContext, "No Connection", Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(mContext, mToast, Toast.LENGTH_LONG).show();
				Intent intent = new Intent();
				intent.putExtra("new_message_id", newMessageId);
				setResult(RESULT_OK, intent);
				finish();
			}
		}

	}

	private void syncMessage() {
		Bundle bundle = new Bundle();
		Account account = OpenERPAccountManager.getAccount(
				getApplicationContext(), OEUser
						.current(getApplicationContext()).getAndroidName());
		Bundle settingsBundle = new Bundle();
		settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
		if (bundle != null) {
			settingsBundle.putAll(bundle);
		}
		ContentResolver.requestSync(account, MessageProvider.AUTHORITY,
				settingsBundle);
	}

	class PartnerLoader extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			mTagsPartners.clear();
			// Loading records from server
			OEHelper oe = mPartnerDB.getOEInstance();
			if (oe != null) {
				mTagsPartners.addAll(oe.search_read());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mPartnerTagsAdapter.notifiyDataChange(mTagsPartners);
		}

	}

	@Override
	public void onTokenAdded(Object token, View view) {
		OEDataRow row = (OEDataRow) token;
		mSelectedPartners.put("key_" + row.getString("id"), row);
	}

	@Override
	public void onTokenSelected(Object token, View view) {

	}

	@Override
	public void onTokenRemoved(Object token) {
		OEDataRow row = (OEDataRow) token;
		if (!isReply)
			mSelectedPartners.remove("key_" + row.getString("id"));
		else
			mPartnerTagsView.addObject(token);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			OEDataRow attachment = mAttachment.handleResult(requestCode, data);
			mAttachments.add(attachment);
			mAttachmentAdapter.notifiyDataChange(mAttachments);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Handle message intent filter for attachments
	 * 
	 * @param intent
	 */
	private void handleIntentFilter(Intent intent) {
		String action = intent.getAction();
		String type = intent.getType();

		// Single attachment
		if (Intent.ACTION_SEND.equals(action) && type != null) {
			OEDataRow single = mAttachment.handleResult(intent);
			single.put("file_type", type);
			mAttachments.add(single);
			mAttachmentAdapter.notifiyDataChange(mAttachments);
		}

		// Multiple Attachments
		if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
			List<OEDataRow> multiple = mAttachment.handleMultipleResult(intent);
			mAttachments.addAll(multiple);
			mAttachmentAdapter.notifiyDataChange(mAttachments);
		}
		// note.note send as mail
		if (intent.getAction() != null
				&& intent.getAction().equals(
						NoteDetail.ACTION_FORWARD_NOTE_AS_MAIL)) {
			edtSubject = (EditText) findViewById(R.id.edtMessageSubject);
			edtBody = (EditText) findViewById(R.id.edtMessageBody);

			NoteDB note = new NoteDB(mContext);
			int note_id = intent.getExtras().getInt("note_id");
			List<OEDataRow> attachment = mAttachment.select(
					note.getModelName(), note_id);
			mNoteObj = note.select(note_id);
			edtSubject.setText("I've shared note with you.");
			edtBody.setText(HTMLHelper.stringToHtml(mNoteObj.getString("name")));
			if (attachment.size() > 0) {
				mAttachments.addAll(attachment);
				mAttachmentAdapter.notifiyDataChange(mAttachments);
			}
		}

		if (intent.hasExtra(Intent.EXTRA_TEXT)) {
			edtBody.setText(intent.getExtras().getString(Intent.EXTRA_TEXT));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mPartnerLoader != null)
			mPartnerLoader.cancel(true);
	}
}
