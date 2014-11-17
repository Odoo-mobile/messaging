package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OField;
import odoo.controls.OForm;
import odoo.controls.OTagsView.NewTokenCreateListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.mail.models.MailMessage;
import com.odoo.base.ir.Attachments;
import com.odoo.base.ir.Attachments.Types;
import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.ORelIds;
import com.odoo.orm.OValues;
import com.odoo.util.OControls;
import com.odoo.util.ODate;
import com.odoo.util.PreferenceManager;
import com.odoo.util.Utils;

public class ComposeMail extends ActionBarActivity implements
		NewTokenCreateListener, OnClickListener, OnFocusChangeListener {
	public static final String TAG = ComposeMail.class.getSimpleName();
	private Context mContext = null;
	private Attachments mAttachment = null;
	private MailMessage mail = null;
	private OForm mForm = null;
	private Integer mMailId = null;
	private ODataRow mParentMail = null;
	private LinearLayout attachments = null;
	private OField fieldPartners = null;

	enum AttachmentType {
		IMAGE, FILE, CAPTURE_IMAGE, IMAGE_OR_CAPTURE_IMAGE, AUDIO, OTHER
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mail_compose);
		setResult(RESULT_CANCELED);
		initActionbar();
		init();
		attachments = (LinearLayout) findViewById(R.id.attachments);
		mForm = (OForm) findViewById(R.id.mComposeMailForm);
		Bundle bundle = getIntent().getExtras();
		fieldPartners = (OField) mForm.findViewById(R.id.fieldPartners);
		if (bundle != null && bundle.containsKey(MailDetail.KEY_MESSAGE_ID)) {
			mMailId = bundle.getInt(MailDetail.KEY_MESSAGE_ID);
			mParentMail = mail.select(mMailId);
			fieldPartners.setObjectEditable(false);
			setTitle(getResources().getString(R.string.title_replay_mail));
			mForm.initForm(mParentMail, true);
		}
		initControls();
		fieldPartners.setOnNewTokenCreateListener(this);
		fieldPartners.setOnTokenFocusChangeListener(this);
		if (bundle != null) {
			String subject_text = "";
			if (bundle.containsKey(MailDetail.KEY_SUBJECT)) {
				subject_text = bundle.getString(MailDetail.KEY_SUBJECT);
			}
			if (bundle.containsKey(Intent.EXTRA_SUBJECT)) {
				subject_text = bundle.getString(Intent.EXTRA_SUBJECT);
			}
			OField subject = (OField) mForm.findViewById(R.id.fieldSubject);
			subject.setText(subject_text);
		}
		if (bundle != null) {
			String body_text = "";
			if (bundle.containsKey(MailDetail.KEY_BODY)) {
				body_text = bundle.getString(MailDetail.KEY_BODY);
			}
			if (bundle.containsKey(Intent.EXTRA_TEXT)) {
				body_text = bundle.getString(Intent.EXTRA_TEXT);
			}
			OField body = (OField) mForm.findViewById(R.id.fieldBody);
			body.setText(body_text);
			body.requestFocus();
		}

		// Check for third party mails
		String action = getIntent().getAction();
		if (action != null) {
			if (action.equals(Intent.ACTION_VIEW)
					|| action.equals(Intent.ACTION_SENDTO)) {
				Uri uri = getIntent().getData();
				if (uri.getScheme().equals("mailto")) {
					String email = uri.getSchemeSpecificPart();
					fieldPartners.addTagObject(quickPartnerCreate(email));
				}
			}
		}
	}

	private void init() {
		mContext = this;
		mAttachment = new Attachments(mContext);
		mail = new MailMessage(mContext);
	}

	private void initControls() {
		mForm.setEditable(true);
	}

	/**
	 * Handling attachments
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			OValues values = mAttachment.handleResult(requestCode, data);
			if (values != null) {
				addAttachments(values.toDataRow());

			} else {
				Toast.makeText(this,
						getString(R.string.toast_unable_to_attach_file),
						Toast.LENGTH_LONG).show();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void addAttachments(ODataRow row) {
		View view = createAttachmentView(attachments, row);
		view.setTag(row);
		attachments.addView(view);
	}

	private View createAttachmentView(ViewGroup parent, ODataRow row) {
		View view = getLayoutInflater().inflate(
				R.layout.mail_compose_attachment_item, parent, false);
		view.findViewById(R.id.remove_attachment).setOnClickListener(this);
		setViewData(row, view);
		return view;
	}

	private void setViewData(ODataRow row, View view) {
		if (row.getString("file_type").contains("image")) {
			ImageView preview = (ImageView) view
					.findViewById(R.id.attachment_preview);
			preview.setImageURI(Uri.parse(row.getString("file_uri")));
		}
		OControls.setText(view, R.id.file_name, row.getString("name"));
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.remove_attachment) {
			View parent = (View) v.getParent().getParent().getParent();
			attachments.removeView(parent);
		}
	}

	private void initActionbar() {
		getActionbar().setHomeButtonEnabled(true);
		getActionbar().setDisplayHomeAsUpEnabled(true);
	}

	private ActionBar getActionbar() {
		return getSupportActionBar();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.menu_mail_compose, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_mail_compose:
			if (getPref().getBoolean("confirm_send_mail", false)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.dialog_send_mail_title));
				builder.setMessage(getString(R.string.dialog_send_mail_message));
				builder.setPositiveButton(
						getString(R.string.dialog_send_mail_positive_button_text),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mailcompose();
							}
						});
				builder.setNegativeButton(
						getString(R.string.dialog_send_mail_negative_button_text),
						null);
				builder.show();
			} else {
				mailcompose();
			}
			return true;
		case R.id.menu_add_files:
			mAttachment.newAttachment(Types.FILE);
			return true;
		case R.id.menu_add_images:
			mAttachment.newAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private PreferenceManager getPref() {
		return new PreferenceManager(this);
	}

	private void mailcompose() {
		OValues values = new OValues();
		values = mForm.getFormValues();
		if (values != null) {
			// Creating attachment lists
			IrAttachment attachmentObj = new IrAttachment(this);
			List<Integer> attachmentIds = new ArrayList<Integer>();
			for (int i = 0; i < attachments.getChildCount(); i++) {
				View attachment = attachments.getChildAt(i);
				OValues vals = ((ODataRow) attachment.getTag()).toValues();
				vals.put("res_model", false);
				vals.put("res_id", 0);
				vals.put("company_id", mail.user().getCompany_id());
				attachmentIds.add(attachmentObj.resolver().insert(vals));
			}
			if (attachmentIds.size() > 0) {
				values.put("attachment_ids", attachmentIds);
			}
			if (mMailId != null) {
				int mailId = mail.sendQuickReply(values,
						values.getString("subject"), values.getString("body"),
						mMailId, mParentMail.getInt("total_childs"));
				Intent data = new Intent();
				data.putExtra(Mail.KEY_MESSAGE_ID, mailId);
				setResult(RESULT_OK, data);
				finish();
			} else {
				ORelIds partner_ids = (ORelIds) values.get("partner_ids");
				values.put("partner_ids", partner_ids.get("KEY_Add").getIds()
						.toString());
				values.put("body", values.getString("body")
						+ getResources().getString(R.string.mail_watermark));
				values.put("author_id", mail.author_id());
				values.put("author_name", mail.user().getName());
				values.put("short_body", mail.storeShortBody(values));
				values.put("message_title", values.getString("subject"));
				values.put("date", ODate.getUTCDate(ODate.DEFAULT_FORMAT));
				values.put("to_read", 0);
				values.put("starred", 0);
				values.put("total_childs", 0);
				Integer mailId = mail.resolver().insert(values);
				Intent data = new Intent();
				data.putExtra(Mail.KEY_MESSAGE_ID, mailId);
				setResult(RESULT_OK, data);
				finish();
			}
		}
	}

	@Override
	public Object newTokenAddListener(String token) {
		if (Utils.validator().validateEmail(token)) {
			return quickPartnerCreate(token);
		}
		return null;
	}

	private ODataRow quickPartnerCreate(String email) {
		ResPartner partner = new ResPartner(this);
		List<ODataRow> records = partner.select("email = ? ",
				new Object[] { email });
		if (records.size() > 0) {
			return records.get(0);
		} else {
			OValues vals = new OValues();
			vals.put("name", email);
			vals.put("email", email);
			int id = partner.resolver().insert(vals);
			ODataRow row = new ODataRow();
			row.put(OColumn.ROW_ID, id);
			row.put("name", email);
			row.put("email", email);
			row.put("image_small", false);
			return row;
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (!hasFocus) {
			String token = fieldPartners.getToken().toString()
					.replaceAll("\\,", "").trim();
			if (token.length() > 0) {
				if (!TextUtils.isEmpty(token)) {
					Object tkn = newTokenAddListener(token);
					if (tkn != null) {
						fieldPartners.setTagText("");
						fieldPartners.addTagObject(tkn);
					}
				}
				fieldPartners.setTagText("");
			}
		}
	}

}
