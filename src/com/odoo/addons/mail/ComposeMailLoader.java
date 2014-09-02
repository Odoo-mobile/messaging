package com.odoo.addons.mail;

import com.openerp.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class ComposeMailLoader extends Activity {

	enum AttachmentType {
		IMAGE, FILE, CAPTURE_IMAGE, IMAGE_OR_CAPTURE_IMAGE, AUDIO, OTHER
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mail_compose);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.menu_mail_compose, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_mail_compose:
			// mailcompose();
			finish();
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

	public void mailcompose() {

	}
}
