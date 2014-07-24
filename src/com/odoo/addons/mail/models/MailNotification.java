package com.odoo.addons.mail.models;

import android.content.Context;

import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.OModel;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;

public class MailNotification extends OModel {
	@Odoo.api.v8
	@Odoo.api.v9alpha
	OColumn is_read = new OColumn("Is Read", OBoolean.class).setDefault(false);

	@Odoo.api.v7
	OColumn read = new OColumn("Read", OBoolean.class).setDefault(false);
	OColumn is_favorite = new OColumn("Is_favorite", OBoolean.class);
	OColumn partner_id = new OColumn("Partner_id", ResPartner.class,
			RelationType.ManyToOne);
	OColumn message_id = new OColumn("Message_id", MailMessage.class,
			RelationType.ManyToOne);

	public MailNotification(Context context) {
		super(context, "mail.notification");
	}
}
