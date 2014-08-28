package com.odoo.addons.mail.providers.mail;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.mail.models.MailMessage;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class MailProvider extends OContentProvider {

	public static String AUTHORITY = "com.odoo.addons.mail.providers.mail";
	public static String PATH = "mail_message";
	public static Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY, PATH);

	@Override
	public String authority() {
		return MailProvider.AUTHORITY;
	}

	@Override
	public OModel model(Context context) {
		return new MailMessage(context);
	}

	@Override
	public String path() {
		return MailProvider.PATH;
	}

	@Override
	public Uri uri() {
		return MailProvider.CONTENT_URI;
	}

}
