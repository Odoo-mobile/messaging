package com.odoo.addons.mail.models;

import odoo.ODomain;

import org.json.JSONArray;

import android.content.Context;

import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OHtml;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.util.ODate;

public class MailMessage extends OModel {

	OColumn type = new OColumn("Type", OInteger.class);
	OColumn email_from = new OColumn("Email", OVarchar.class, 64)
			.setDefault("false");
	OColumn author_id = new OColumn("Author", ResPartner.class,
			RelationType.ManyToOne);
	OColumn partner_ids = new OColumn("Partners", ResPartner.class,
			RelationType.ManyToMany);
	OColumn notified_partner_ids = new OColumn("Notified Partners",
			ResPartner.class, RelationType.ManyToMany);
	OColumn attachment_ids = new OColumn("Attachments", IrAttachment.class,
			RelationType.ManyToMany);
	OColumn parent_id = new OColumn("Parent", MailMessage.class,
			RelationType.ManyToOne);
	OColumn child_ids = new OColumn("Childs", MailMessage.class,
			RelationType.OneToMany);
	OColumn model = new OColumn("Model", OVarchar.class, 64)
			.setDefault("false");
	OColumn res_id = new OColumn("Resource ID", OInteger.class).setDefault(0);
	OColumn record_name = new OColumn("Record name", OText.class)
			.setDefault("false");
	OColumn notification_ids = new OColumn("Notifications",
			MailNotification.class, RelationType.OneToMany);
	OColumn subject = new OColumn("Subject", OVarchar.class, 100)
			.setDefault("false");
	OColumn date = new OColumn("Date", ODateTime.class)
			.setParsePatter(ODate.DEFAULT_FORMAT);
	OColumn body = new OColumn("Body", OHtml.class);
	OColumn to_read = new OColumn("To Read", OBoolean.class);
	OColumn starred = new OColumn("Starred", OBoolean.class);
	OColumn vote_user_ids = new OColumn("Voters", ResUsers.class,
			RelationType.ManyToMany);

	// Functional Fields
	@Odoo.Functional(method = "getMessageTitle")
	OColumn message_title = new OColumn("Title");
	@Odoo.Functional(method = "getChildCount")
	OColumn childs_count = new OColumn("Childs");
	@Odoo.Functional(method = "getAuthorName")
	OColumn author_name = new OColumn("Author", OVarchar.class);

	public MailMessage(Context context) {
		super(context, "mail.message");
	}

	public String getMessageTitle(ODataRow row) {
		String title = "false";
		if (!row.getString("record_name").equals("false"))
			title = row.getString("record_name");
		if (title.equals("false") && !row.getString("subject").equals("false"))
			title = row.getString("subject");
		if (title.equals("false"))
			title = "comment";
		return title;
	}

	public String getChildCount(ODataRow row) {
		String total = "";
		int count = count("parent_id = ?", new Object[] { row.getInt("id") });
		if (count > 0) {
			total = count + " replies";
		}
		return total;
	}

	public String getAuthorName(ODataRow row) {
		String author_name = "";
		ODataRow author = row.getM2ORecord("author_id").browse();
		if (author != null)
			author_name = row.getM2ORecord("author_id").browse()
					.getString("name");
		else {
			author_name = row.getString("email_from");
		}
		return author_name;
	}

	@Override
	public ODomain defaultDomain() {
		Integer user_id = user().getUser_id();
		ODomain domain = new ODomain();
		domain.add("|");
		domain.add("partner_ids.user_ids", "in", new JSONArray().put(user_id));
		domain.add("|");
		domain.add("notification_ids.partner_id.user_ids", "in",
				new JSONArray().put(user_id));
		domain.add("author_id.user_ids", "in", new JSONArray().put(user_id));
		return domain;
	}

	public static class ResUsers extends OModel {

		OColumn name = new OColumn("Name", OVarchar.class, 64);

		public ResUsers(Context context) {
			super(context, "res.users");
		}

	}
}
