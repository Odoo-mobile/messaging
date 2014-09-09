package com.odoo.addons.mail;

import java.util.List;

import odoo.controls.OField;
import odoo.controls.OForm;
import odoo.controls.OTagsView.NewTokenCreateListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.base.ir.Attachments;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.ORelIds;
import com.odoo.orm.OValues;
import com.odoo.util.ODate;
import com.openerp.R;

public class ComposeMail extends Activity implements NewTokenCreateListener {
	public static final String TAG = "com.odoo.addons.mail.ComposeMail";
	private Context mContext = null;
	private Attachments mAttachment = null;
	private MailMessage mail = null;
	private OForm mForm = null;
	private Integer mMailId = null;
	private ODataRow mParentMail = null;

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
		mForm = (OForm) findViewById(R.id.mComposeMailForm);
		Bundle bundle = getIntent().getExtras();
		OField fieldPartners = (OField) mForm.findViewById(R.id.fieldPartners);
		if (bundle != null && bundle.containsKey(MailDetail.KEY_MESSAGE_ID)) {
			mMailId = bundle.getInt(MailDetail.KEY_MESSAGE_ID);
			mParentMail = mail.select(mMailId);
			fieldPartners.setObjectEditable(false);
			setTitle(getResources().getString(R.string.title_replay_mail));
			mForm.initForm(mParentMail, true);
		}
		initControls();
		fieldPartners.setOnNewTokenCreateListener(this);
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initActionbar() {
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
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
			mailcompose();
			return true;
		case R.id.menu_add_files:
			// mAttachment.requestAttachment(Attachment.Types.FILE);
			return true;
		case R.id.menu_add_images:
			// mAttachment
			// .requestAttachment(Attachment.Types.IMAGE_OR_CAPTURE_IMAGE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void mailcompose() {
		OValues values = new OValues();
		values = mForm.getFormValues();
		if (values != null) {
			if (mMailId != null) {
				int mailId = mail.sendQuickReply(values.getString("subject"),
						values.getString("body"), mMailId,
						mParentMail.getInt("total_childs"));
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
		return quickPartnerCreate(token);
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
}
