package com.odoo.addons.mail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.odoo.base.ir.Attachment;
import com.odoo.orm.ODataRow;
import com.openerp.R;

public class ComposeMail extends Activity {
	public static final String TAG = "com.odoo.addons.mail.ComposeMail";
	Context mContext = null;
	Attachment mAttachment = null;

	enum AttachmentType {
		IMAGE, FILE
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mailcompose);
		initActionbar();
		init();
	}

	private void init() {
		initControls();
		mContext = this;
		mAttachment = new Attachment(mContext);
	}

	private void initControls() {

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
			break;
		case R.id.menu_mail_compose:
			Toast.makeText(this, "Compose Mail", Toast.LENGTH_SHORT).show();
			if (findViewById(R.id.name).equals("null"))
				Toast.makeText(this, "plz select the at least one Recipient",
						Toast.LENGTH_SHORT).show();
		case R.id.menu_add_files:
			Toast.makeText(this, "Attach Images", Toast.LENGTH_SHORT).show();
			mAttachment.requestAttachment(Attachment.Types.FILE);
			break;
		case R.id.menu_add_images:
			Toast.makeText(this, "Attach Files", Toast.LENGTH_SHORT).show();
			mAttachment
					.requestAttachment(Attachment.Types.IMAGE_OR_CAPTURE_IMAGE);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
