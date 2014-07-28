package com.odoo.addons.mail.models;

import android.content.Context;

import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OBlob;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

public class MailGroup extends OModel {
	OColumn name = new OColumn("Name", OVarchar.class, 64);
	OColumn description = new OColumn("Description", OText.class);
	OColumn image_medium = new OColumn("Image_Medium", OBlob.class);

	public MailGroup(Context context) {
		super(context, "mail.group");
	}

}
