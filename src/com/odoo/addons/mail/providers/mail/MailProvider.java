package com.odoo.addons.mail.providers.mail;

import com.odoo.support.provider.OContentProvider;

public class MailProvider extends OContentProvider {

	public static String CONTENTURI = "com.odoo.addons.mail.providers.mail.MailProvider";
	public static String AUTHORITY = "com.odoo.addons.mail.providers.mail";

	@Override
	public String authority() {
		return AUTHORITY;
	}

	@Override
	public String contentUri() {
		return CONTENTURI;
	}

}
