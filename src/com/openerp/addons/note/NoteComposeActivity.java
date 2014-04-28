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
package com.openerp.addons.note;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import openerp.OEArguments;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.R;
import com.openerp.addons.note.NoteDB.NoteStages;
import com.openerp.addons.note.NoteDB.NoteTags;
import com.openerp.base.ir.Attachment;
import com.openerp.base.ir.Attachment.Types;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEM2MIds;
import com.openerp.orm.OEM2MIds.Operation;
import com.openerp.orm.OEValues;
import com.openerp.support.OEUser;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.util.HTMLHelper;
import com.openerp.util.controls.ExpandableHeightGridView;
import com.openerp.util.tags.MultiTagsTextView.TokenListener;
import com.openerp.util.tags.TagsView;
import com.openerp.util.tags.TagsView.CustomTagViewListener;
import com.openerp.util.tags.TagsView.NewTokenCreateListener;

public class NoteComposeActivity extends Activity implements
		OnNavigationListener, NewTokenCreateListener, TokenListener {

	public static final String TAG = "com.openerp.addons.note.NoteComposeActivity";

	public static final int REQUEST_SPEECH_TO_TEXT = 333;
	Context mContext = null;

	/**
	 * Note db, OpenERP Instance
	 */
	NoteDB mDb = null;
	OEHelper mOpenERP = null;
	/**
	 * Database Objects
	 */
	NoteDB mNoteDB = null;
	NoteTags mTagsDb = null;
	NoteStages mNoteStageDB = null;
	Integer mStageId = -1;
	List<Object> mNoteTags = new ArrayList<Object>();
	OEListAdapter mNoteStageAdapter = null;
	OEListAdapter mNoteTagsAdapter = null;
	OEDataRow mNoteRow = null;
	Integer mNoteId = null;
	Boolean mEditMode = false;
	HashMap<String, Integer> mSelectedTagsIds = new HashMap<String, Integer>();
	String mPadURL = "";
	/**
	 * Actionbar
	 */
	ActionBar mActionbar;
	List<Object> mActionbarSpinnerItems = new ArrayList<Object>();
	HashMap<String, Integer> mActionbarSpinnerItemsPositions = new HashMap<String, Integer>();

	/**
	 * Note pad status
	 */
	boolean mPadInstalled = false;

	boolean isDirty = false;

	WebView mWebViewPad = null;
	EditText edtNoteTitle = null;
	EditText edtNoteDescription = null;
	TagsView mNoteTagsView = null;
	ExpandableHeightGridView mNoteAttachmentGrid = null;
	List<Object> mNoteAttachmentList = new ArrayList<Object>();
	OEListAdapter mNoteListAdapterAttach = null;
	Attachment mAttachment = null;
	PackageManager mPackageManager = null;
	String oldName = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_note_compose);
		mContext = this;
		mDb = new NoteDB(mContext);
		mTagsDb = mDb.new NoteTags(mContext);
		mOpenERP = mDb.getOEInstance();
		mAttachment = new Attachment(this);
		init();
	}

	private void init() {
		mNoteDB = new NoteDB(mContext);
		mNoteStageDB = mNoteDB.new NoteStages(mContext);
		initActionBar();
		initNoteTags();
		checkForPad();
		initNote();
		handleIntent();
	}

	private void handleIntent() {
		Log.d(TAG, "NoteComposeActivity->handleIntent()");
		Intent intent = getIntent();
		if (intent.hasExtra("request_code")) {
			Attachment.Types type = (Types) intent.getExtras().get(
					"request_code");
			mAttachment.requestAttachment(type);
		}
		if (intent.hasExtra(Intent.EXTRA_TEXT)) {
			edtNoteDescription.setText(intent.getExtras().getString(
					Intent.EXTRA_TEXT));
		}
	}

	public void checkForPad() {
		Log.d(TAG, "NoteComposeActivity->checkForPad()");
		if (mOpenERP != null) {
			mPadInstalled = mOpenERP.moduleExists("note_pad");
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initNote() {
		Intent intent = getIntent();
		mNoteAttachmentGrid = (ExpandableHeightGridView) findViewById(R.id.noteAttachmentGrid);
		mNoteAttachmentGrid.setExpanded(true);
		edtNoteTitle = (EditText) findViewById(R.id.edtNoteTitleInput);
		edtNoteDescription = (EditText) findViewById(R.id.edtNoteComposeDescription);
		mWebViewPad = (WebView) findViewById(R.id.webNoteComposeWebViewPad);
		if (intent.hasExtra("note_id")) {
			mEditMode = true;
			mNoteId = intent.getIntExtra("note_id", 0);
			mNoteRow = mDb.select(mNoteId);
			OEDataRow stage = mNoteRow.getM2ORecord("stage_id").browse();
			if (stage != null) {
				mStageId = stage.getInt("id");
			}
			List<OEDataRow> attachments = mAttachment.select("note.note",
					mNoteId);
			mNoteAttachmentList.addAll(attachments);
		}
		if (intent.hasExtra("stage_id")) {
			mStageId = intent.getIntExtra("stage_id", 0);
		}
		if (mActionbarSpinnerItemsPositions.containsKey("key_" + mStageId))
			mActionbar
					.setSelectedNavigationItem(mActionbarSpinnerItemsPositions
							.get("key_" + mStageId));
		if (intent.hasExtra("note_title")) {
			edtNoteTitle.setText(intent.getStringExtra("note_title"));
			oldName = intent.getStringExtra("note_title");
		}

		if (mPadInstalled) {
			edtNoteDescription.setVisibility(View.GONE);
			mWebViewPad.setVisibility(View.VISIBLE);
			if (mEditMode) {
				mPadURL = mNoteRow.getString("note_pad_url");
				if (mPadURL.equals("false")) {
					mPadURL = getPadURL(mNoteRow.getInt("id"));
				}
			} else {
				mPadURL = getPadURL(null);
			}
			mWebViewPad.getSettings().setJavaScriptEnabled(true);
			mWebViewPad.getSettings().setJavaScriptCanOpenWindowsAutomatically(
					true);
			mWebViewPad.loadUrl(mPadURL + "?showChat=false&userName="
					+ OEUser.current(mContext).getUsername());
		} else {
			edtNoteDescription.setVisibility(View.VISIBLE);
			mWebViewPad.setVisibility(View.GONE);
			if (mEditMode) {
				edtNoteDescription
						.setMovementMethod(new ScrollingMovementMethod());
				edtNoteDescription.setText(HTMLHelper.stringToHtml(mNoteRow
						.getString("memo")));
			}
		}
		if (mEditMode) {
			edtNoteTitle.setText(mNoteRow.getString("name"));
			oldName = mNoteRow.getString("name");
			List<OEDataRow> tags = mNoteRow.getM2MRecord("tag_ids")
					.browseEach();
			if (tags != null) {
				for (OEDataRow row : tags) {
					mNoteTagsView.addObject(row);
				}
			}
		}
		mNoteListAdapterAttach = new OEListAdapter(mContext,
				R.layout.fragment_note_grid_custom_attachment,
				mNoteAttachmentList) {
			@Override
			public View getView(final int position, View convertView,
					ViewGroup parent) {
				View mView = convertView;
				if (mView == null) {
					mView = ((Activity) mContext).getLayoutInflater().inflate(
							getResource(), parent, false);
				}
				final OEDataRow attachment = (OEDataRow) mNoteAttachmentList
						.get(position);
				ImageView imgNoteAttach = (ImageView) mView
						.findViewById(R.id.imgNoteAttach);
				TextView txvNoteattachmentName = (TextView) mView
						.findViewById(R.id.txvNoteattachmentName);
				final ImageView imgNoteAttachClose = (ImageView) mView
						.findViewById(R.id.imgNoteAttachClose);
				if (attachment.getString("file_type").contains("image")
						&& !attachment.getString("file_uri").equals("false")) {
					imgNoteAttach.setImageURI(Uri.parse(attachment
							.getString("file_uri")));
				} else {
					imgNoteAttach.setImageResource(R.drawable.attachment);
				}
				imgNoteAttach.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (attachment.get("id") != null)
							mAttachment.downloadFile(attachment.getInt("id"));
					}
				});
				imgNoteAttachClose
						.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								if (attachment.get("id") != null) {
									mAttachment.removeAttachment(attachment
											.getInt("id"));
								}
								mNoteAttachmentList.remove(position);
								mNoteListAdapterAttach
										.notifiyDataChange(mNoteAttachmentList);
							}
						});
				txvNoteattachmentName.setText(attachment.getString("name"));
				return mView;
			}
		};
		mNoteAttachmentGrid.setAdapter(mNoteListAdapterAttach);
	}

	private String getPadURL(Integer note_id) {
		if (mOpenERP != null) {
			JSONObject newContext = new JSONObject();
			try {
				boolean flag = false;
				if (note_id != null) {
					List<Object> ids = new ArrayList<Object>();
					ids.add(note_id);
					if (mOpenERP.syncWithServer(false, null, ids)) {
						mNoteRow = mDb.select(note_id);
						if (!mNoteRow.getString("note_pad_url").equals("false")) {
							mPadURL = mNoteRow.getString("note_pad_url");
							flag = true;
						}
					}
				}
				if (!flag) {
					newContext.put("model", "note.note");
					newContext.put("field_name", "note_pad_url");
					JSONObject kwargs = new JSONObject();
					if (note_id != null)
						newContext.put("object_id", note_id);
					kwargs.accumulate("context", newContext);
					OEArguments arguments = new OEArguments();
					JSONObject result = (JSONObject) mOpenERP.call_kw(
							"pad_generate_url", arguments, kwargs);
					mPadURL = result.getString("url");
				}

			} catch (Exception e) {
			}
		}
		return mPadURL;
	}

	private void initNoteTags() {
		mNoteTagsView = (TagsView) findViewById(R.id.edtComposeNoteTags);

		mNoteTagsView.setCustomTagView(new CustomTagViewListener() {

			@Override
			public View getViewForTags(LayoutInflater layoutInflater,
					Object object, ViewGroup tagsViewGroup) {
				View mView = layoutInflater.inflate(
						R.layout.custom_note_tags_adapter_view_item,
						tagsViewGroup, false);
				OEDataRow row = (OEDataRow) object;
				TextView txvName = (TextView) mView
						.findViewById(R.id.txvCustomNoteTagsAdapterViewItem);
				txvName.setText(row.getString("name"));
				return mView;
			}
		});
		mNoteTags.addAll(mTagsDb.select());

		mNoteTagsAdapter = new OEListAdapter(mContext,
				R.layout.custom_note_tags_adapter_view_item, mNoteTags) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View mView = convertView;
				if (mView == null) {
					mView = getLayoutInflater().inflate(getResource(), parent,
							false);
				}
				OEDataRow row = (OEDataRow) mNoteTags.get(position);
				TextView txvName = (TextView) mView
						.findViewById(R.id.txvCustomNoteTagsAdapterViewItem);
				txvName.setText(row.getString("name"));
				return mView;
			}
		};
		mNoteTagsView.setAdapter(mNoteTagsAdapter);
		mNoteTagsView.setNewTokenCreateListener(this);
		mNoteTagsView.setTokenListener(this);
	}

	private void initActionBar() {
		mActionbar = getActionBar();
		mActionbar.setHomeButtonEnabled(true);
		mActionbar.setDisplayHomeAsUpEnabled(true);
		mActionbar.setDisplayShowTitleEnabled(false);
		mActionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		setupActionbarSpinner();
	}

	private void setupActionbarSpinner() {
		mActionbarSpinnerItems.add(new SpinnerNavItem(0, "Stages"));
		int i = 1;
		for (OEDataRow stage : mNoteStageDB.select()) {
			mActionbarSpinnerItems.add(new SpinnerNavItem(stage.getInt("id"),
					stage.getString("name")));
			mActionbarSpinnerItemsPositions.put("key_" + stage.getInt("id"), i);
			i++;
		}
		mActionbarSpinnerItems.add(new SpinnerNavItem(-1, "Add New"));
		mNoteStageAdapter = new OEListAdapter(mContext,
				R.layout.spinner_custom_layout, mActionbarSpinnerItems) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View mView = convertView;
				if (mView == null) {
					mView = getLayoutInflater().inflate(getResource(), parent,
							false);
				}
				TextView txvTitle = (TextView) mView
						.findViewById(R.id.txvCustomSpinnerItemText);
				txvTitle.setTextColor(Color.parseColor("#ffffff"));
				SpinnerNavItem item = (SpinnerNavItem) mActionbarSpinnerItems
						.get(position);
				txvTitle.setText(item.get_title());
				return mView;
			}

			@Override
			public View getDropDownView(int position, View convertView,
					ViewGroup parent) {
				View mView = convertView;
				if (mView == null) {
					mView = getLayoutInflater().inflate(getResource(), parent,
							false);
				}
				TextView txvTitle = (TextView) mView
						.findViewById(R.id.txvCustomSpinnerItemText);
				txvTitle.setTextColor(Color.parseColor("#ffffff"));
				SpinnerNavItem item = (SpinnerNavItem) mActionbarSpinnerItems
						.get(position);
				txvTitle.setText(item.get_title());
				return mView;
			}
		};
		mActionbar.setListNavigationCallbacks(mNoteStageAdapter, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.menu_fragment_note_new_edit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (edtNoteTitle.getText().length() != 0
					|| edtNoteDescription.getText().length() != 0) {
				saveNote(mNoteId);
			} else {
				finish();
			}
			return true;
		case R.id.menu_note_audio:
			mAttachment.requestAttachment(Types.AUDIO);
			return true;
		case R.id.menu_note_image:
			mAttachment.requestAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
			return true;
		case R.id.menu_note_file:
			mAttachment.requestAttachment(Types.FILE);
			return true;
		case R.id.menu_note_speech_to_text:
			requestSpeechToText();
			return true;
		case R.id.menu_note_cancel:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void requestSpeechToText() {
		mPackageManager = mContext.getPackageManager();
		List<ResolveInfo> activities = mPackageManager.queryIntentActivities(
				new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() == 0) {
			Toast.makeText(mContext, "No audio recorder present.",
					Toast.LENGTH_LONG).show();
		} else {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
					RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "speak now...");
			startActivityForResult(intent, REQUEST_SPEECH_TO_TEXT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			if (requestCode != REQUEST_SPEECH_TO_TEXT) {
				OEDataRow newAttachment = mAttachment.handleResult(requestCode,
						data);
				if (newAttachment.getString("content").equals("false")) {
					mNoteAttachmentList.add(newAttachment);
					mNoteListAdapterAttach
							.notifiyDataChange(mNoteAttachmentList);
				}
			} else {
				String noteText = (edtNoteDescription.getText().length() > 0) ? edtNoteDescription
						.getText().toString() + "\n"
						: "";
				ArrayList<String> matches = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				edtNoteDescription.setText(noteText + matches.get(0));
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (edtNoteTitle.getText().length() != 0
				|| edtNoteDescription.getText().length() != 0) {
			saveNote(mNoteId);
		} else {
			super.onBackPressed();
		}
	}

	public void saveNote(Integer mNoteId) {
		if (mOpenERP == null) {
			Toast.makeText(mContext, "No Connection", Toast.LENGTH_LONG).show();
		} else {
			OEValues values = new OEValues();
			String name = edtNoteTitle.getText().toString();
			String memo = "";
			if (mStageId == -1) {
				Toast.makeText(this, "Please select stage", Toast.LENGTH_LONG)
						.show();
				return;
			}
			if (mPadInstalled) {
				try {
					JSONArray args = new JSONArray();
					args.put(mPadURL);
					JSONObject content = mOpenERP.openERP().call_kw(
							"pad.common", "pad_get_content", args);
					memo = content.getString("result");
					values.put("note_pad_url", mPadURL);
					memo = name + " <br> " + memo;
				} catch (Exception e) {
				}
			} else {
				memo = edtNoteDescription.getText().toString();
				if (oldName != name)
					memo = name + "<br/>" + memo.replace(oldName, "");
			}
			name = noteName(name + "\n" + memo);
			List<Integer> tag_ids = new ArrayList<Integer>();
			for (String key : mSelectedTagsIds.keySet())
				tag_ids.add(mSelectedTagsIds.get(key));
			OEM2MIds m2mIds = new OEM2MIds(Operation.ADD, tag_ids);
			values.put("name", name);
			values.put("memo", memo);
			values.put("open", true);
			values.put("date_done", false);
			values.put("stage_id", mStageId);
			values.put("tag_ids", m2mIds);
			values.put("current_partner_id", OEUser.current(mContext)
					.getPartner_id());
			String mToast = "Note Created";
			int id = (mNoteId == null) ? 0 : mNoteId;
			boolean is_new = true;
			if (mNoteId != null) {
				// Updating
				mToast = "Note Updated";
				mOpenERP.update(values, mNoteId);
				is_new = false;
			} else {
				// Creating
				id = mOpenERP.create(values);
			}
			mAttachment.updateAttachments("note.note", id, mNoteAttachmentList);
			Toast.makeText(mContext, mToast, Toast.LENGTH_LONG).show();
			Intent data = new Intent();
			data.putExtra("result", id);
			data.putExtra("is_new", is_new);
			setResult(RESULT_OK, data);
		}
		finish();
	}

	private String noteName(String memo) {
		String name = "";
		String[] parts = memo.split("\\n");
		if (parts.length == 1) {
			parts = memo.split("\\</br>");
			if (parts.length == 1)
				parts = memo.split("\\<br>");
		}
		name = parts[0];
		return name;
	}

	class SpinnerNavItem {
		int _id;
		String _title;

		public SpinnerNavItem(int _id, String _title) {
			this._id = _id;
			this._title = _title;
		}

		public int get_id() {
			return _id;
		}

		public void set_id(int _id) {
			this._id = _id;
		}

		public String get_title() {
			return _title;
		}

		public void set_title(String _title) {
			this._title = _title;
		}

	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		SpinnerNavItem item = (SpinnerNavItem) mActionbarSpinnerItems
				.get(itemPosition);
		if (item.get_id() == 0) {
			return false;
		}
		if (item.get_id() == -1) {
			createNoteStage();
		}
		mStageId = item.get_id();
		return true;
	}

	public void createNoteStage() {

		AlertDialog.Builder builder = new Builder(this);
		final EditText stage = new EditText(this);
		builder.setTitle("Stage Name").setMessage("Enter new Stage")
				.setView(stage);

		builder.setPositiveButton("Create", new OnClickListener() {
			public void onClick(DialogInterface di, int i) {
				String mToast = "No Connection ";
				if ((stage.getText().toString()).equalsIgnoreCase("Add New")
						|| (stage.getText().toString())
								.equalsIgnoreCase("AddNew")) {
					mToast = "You can't take " + stage.getText().toString();
				} else {
					OEHelper oe = mNoteStageDB.getOEInstance();
					if (oe != null) {
						String stageName = stage.getText().toString();
						OEValues values = new OEValues();
						values.put("name", stageName);
						Integer newId = oe.create(values);
						if (newId != null) {
							mActionbarSpinnerItems.add(
									mActionbarSpinnerItems.size() - 1,
									new SpinnerNavItem(newId, stageName));
							mActionbarSpinnerItemsPositions.put("key_" + newId,
									mActionbarSpinnerItems.size() - 2);
							mActionbar
									.setSelectedNavigationItem(mActionbarSpinnerItemsPositions
											.get("key_" + newId));
							mStageId = newId;
							mNoteStageAdapter
									.notifiyDataChange(mActionbarSpinnerItems);
							mToast = "Stage created";
						}
					}
				}
				Toast.makeText(mContext, mToast, Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancel", null);
		builder.create().show();
	}

	@Override
	public Object newTokenAddListener(String token) {
		OEHelper oe = mTagsDb.getOEInstance();
		if (oe != null) {
			OEValues values = new OEValues();
			values.put("name", token);
			int id = oe.create(values);
			OEDataRow row = mTagsDb.select(id);
			mNoteTags.add(row);
			mNoteTagsAdapter.notifiyDataChange(mNoteTags);
		}
		return null;
	}

	@Override
	public void onTokenAdded(Object token, View view) {
		OEDataRow item = (OEDataRow) token;
		mSelectedTagsIds.put("key_" + item.getInt("id"), item.getInt("id"));
	}

	@Override
	public void onTokenSelected(Object token, View view) {

	}

	@Override
	public void onTokenRemoved(Object token) {
		OEDataRow item = (OEDataRow) token;
		mSelectedTagsIds.remove("key_" + item.getInt("id"));
	}

}
