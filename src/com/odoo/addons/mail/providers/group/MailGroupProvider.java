package com.odoo.addons.mail.providers.group;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.mail.models.MailGroup;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class MailGroupProvider extends OContentProvider {

	public static String AUTHORITY = "com.odoo.addons.mail.providers.group";
	public static String PATH = "mail_group";
	public static Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY, PATH);

	@Override
	public String authority() {
		return MailGroupProvider.AUTHORITY;
	}

	@Override
	public OModel model(Context context) {
		return new MailGroup(context);
	}

	@Override
	public String path() {
		return MailGroupProvider.PATH;
	}

	@Override
	public Uri uri() {
		return MailGroupProvider.CONTENT_URI;
	}

}
