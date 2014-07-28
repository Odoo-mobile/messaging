package com.odoo.addons.mail.providers.group;

import com.odoo.support.provider.OContentProvider;

public class MailGroupProvider extends OContentProvider {

	public static String CONTENTURI = "com.odoo.addons.mail.providers.group.MailGroupProvider";
	public static String AUTHORITY = "com.odoo.addons.mail.providers.group";

	@Override
	public String authority() {
		return AUTHORITY;
	}

	@Override
	public String contentUri() {
		return CONTENTURI;
	}

}
