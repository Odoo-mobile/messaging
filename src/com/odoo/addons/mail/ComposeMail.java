package com.odoo.addons.mail;

import odoo.controls.OForm;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.base.ir.Attachment;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.util.ODate;
import com.openerp.R;

public class ComposeMail extends Activity {
	public static final String TAG = "com.odoo.addons.mail.ComposeMail";
	Context mContext = null;
	Attachment mAttachment = null;
	MailMessage mail = null;
	private OForm mForm = null;

	enum AttachmentType {
		IMAGE, FILE
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mail_compose);
		initActionbar();
		init();
	}

	private void init() {
		initControls();
		mContext = this;
		mAttachment = new Attachment(mContext);
		mail = new MailMessage(mContext);
	}

	private void initControls() {
		mForm = (OForm) findViewById(R.id.mComposeMailForm);
		mForm.setEditable(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			ODataRow attachment = mAttachment.handleResult(requestCode, data);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void initActionbar() {
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
			Toast.makeText(this, "Compose Mail", Toast.LENGTH_SHORT).show();
			finish();
			return true;
		case R.id.menu_add_files:
			Toast.makeText(this, "Attach Images", Toast.LENGTH_SHORT).show();
			mAttachment.requestAttachment(Attachment.Types.FILE);
			return true;
		case R.id.menu_add_images:
			Toast.makeText(this, "Attach Files", Toast.LENGTH_SHORT).show();
			mAttachment
					.requestAttachment(Attachment.Types.IMAGE_OR_CAPTURE_IMAGE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void mailcompose() {
		OValues values = new OValues();
		values = mForm.getFormValues();
		values.put("author_id", mail.author_id());
		values.put("date", ODate.getUTCDate(ODate.DEFAULT_FORMAT));
		mail.create(values);
	}
}
