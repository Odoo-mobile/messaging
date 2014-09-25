package com.openerp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;

public class NewAppAvailable extends Activity implements OnClickListener {
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_app);
		findViewById(R.id.remind_later).setOnClickListener(this);
		findViewById(R.id.get_now).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.get_now:
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("market://details?id=com.odoo"));
			startActivity(intent);
			break;
		case R.id.remind_later:
			finish();
			break;
		}
	}
}
