package com.odoo.addons.mail;

import android.app.Activity;
import android.os.Bundle;

public class ComposeMailLoader extends Activity {

	enum AttachmentType {
		IMAGE, FILE, CAPTURE_IMAGE, IMAGE_OR_CAPTURE_IMAGE, AUDIO, OTHER
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}
