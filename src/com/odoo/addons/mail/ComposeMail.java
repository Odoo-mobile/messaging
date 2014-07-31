package com.odoo.addons.mail;

import odoo.controls.OForm;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.openerp.R;

public class ComposeMail extends Activity {

	private OForm mForm = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mail_compose);
		initActionbar();
		init();
	}

	private void init() {
		mForm = (OForm) findViewById(R.id.mComposeMailForm);
		mForm.setEditable(true);
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
		case R.id.menu_add_files:
			Toast.makeText(this, "Attach Files", Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_add_images:
			Toast.makeText(this, "Attach Images", Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
