package com.odoo.base.mail.provider;

import android.content.Context;
import android.net.Uri;

import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class MailFollowerProvider extends OContentProvider {

	public static String AUTHORITY = "com.odoo.base.mail.provider.mailfollower";
	public static String PATH = "mail_follower";
	public static Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY, PATH);

	@Override
	public OModel model(Context context) {
		return new MailFollowers(context);
	}

	@Override
	public String authority() {
		return MailFollowerProvider.AUTHORITY;
	}

	@Override
	public String path() {
		return MailFollowerProvider.PATH;
	}

	@Override
	public Uri uri() {
		return MailFollowerProvider.CONTENT_URI;
	}

}
