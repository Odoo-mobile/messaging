package com.odoo.addons.mail.models;

import java.util.ArrayList;
import java.util.List;

import odoo.ODomain;

import org.json.JSONArray;

import android.content.Context;
import android.text.TextUtils;

import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OHtml;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.util.ODate;

public class MailMessage extends OModel {
	private Context mContext = null;
	private MailNotification notification = null;

	OColumn type = new OColumn("Type", OInteger.class).setDefault("email");
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
			RelationType.ManyToOne).setDefault(0);
	OColumn child_ids = new OColumn("Childs", MailMessage.class,
			RelationType.OneToMany).setRelatedColumn("parent_id")
			.setLocalColumn();
	OColumn model = new OColumn("Model", OVarchar.class, 64)
			.setDefault("false");
	OColumn res_id = new OColumn("Resource ID", OInteger.class).setDefault(0);
	OColumn record_name = new OColumn("Record name", OText.class)
			.setDefault("false");
	OColumn notification_ids = new OColumn("Notifications",
			MailNotification.class, RelationType.OneToMany)
			.setRelatedColumn("message_id");
	OColumn subject = new OColumn("Subject", OVarchar.class, 100)
			.setDefault("false");
	OColumn date = new OColumn("Date", ODateTime.class).setParsePatter(
			ODate.DEFAULT_FORMAT).setDefault(
			ODate.getUTCDate(ODate.DEFAULT_FORMAT));
	OColumn body = new OColumn("Body", OHtml.class).setDefault("");
	OColumn vote_user_ids = new OColumn("Voters", ResUsers.class,
			RelationType.ManyToMany);
	OColumn to_read = new OColumn("To Read", OBoolean.class).setDefault(true);
	OColumn starred = new OColumn("Starred", OBoolean.class).setDefault(false);

	// Functional Fields
	@Odoo.Functional(method = "getMessageTitle")
	OColumn message_title = new OColumn("Title");
	@Odoo.Functional(method = "getChildCount")
	OColumn childs_count = new OColumn("Childs");
	@Odoo.Functional(method = "getAuthorName")
	OColumn author_name = new OColumn("Author", OVarchar.class);
	@Odoo.Functional(method = "hasVoted")
	OColumn has_voted = new OColumn("Has voted", OVarchar.class);
	@Odoo.Functional(method = "getPartnersName")
	OColumn partners_name = new OColumn("Partners", OVarchar.class);

	public MailMessage(Context context) {
		super(context, "mail.message");
		mContext = context;
		write_date.setDefault(false);
		create_date.setDefault(false);
	}

	public Integer author_id() {
		return new ResPartner(mContext).selectRowId(user().getPartner_id());
	}

	@Override
	public Boolean checkForWriteDate() {
		return true;
	}

	@Override
	public Boolean checkForLocalUpdate() {
		return false;
	}

	@Override
	public Boolean checkForLocalLatestUpdate() {
		return false;
	}

	@Override
	public Boolean canCreateOnServer() {
		return false;
	}

	@Override
	public Boolean canDeleteFromLocal() {
		return false;
	}

	@Override
	public Boolean canDeleteFromServer() {
		return false;
	}

	@Override
	public Boolean canUpdateToServer() {
		return false;
	}

	public void markAsRead(Boolean to_read, Integer row_id) {
		notification = new MailNotification(mContext);
		ODataRow parent = select(row_id);
		_markAsRead(to_read, row_id);
		for (ODataRow child : parent.getO2MRecord("child_ids").browseEach()) {
			_markAsRead(to_read, child.getInt(OColumn.ROW_ID));
		}
	}

	private void _markAsRead(Boolean to_read, Integer row_id) {
		// update MailMessage
		OValues values = new OValues();
		values.put("to_read", to_read);
		update(values, row_id);

		// updating mail.notification
		OValues values_noti = new OValues();
		values_noti.put("is_read", !to_read);
		notification.update(values_noti, "message_id = ?",
				new Object[] { row_id });
	}

	public String getPartnersName(ODataRow row) {
		String partners = "to ";
		List<String> partners_name = new ArrayList<String>();
		for (ODataRow p : row.getM2MRecord("partner_ids").browseEach()) {
			partners_name.add(p.getString("name"));
		}
		return partners + TextUtils.join(", ", partners_name);
	}

	public Boolean hasVoted(ODataRow row) {
		for (ODataRow r : row.getM2MRecord("vote_user_ids").browseEach()) {
			if (r.getInt("id") == user().getUser_id()) {
				return true;
			}
		}
		return false;
	}

	public String getMessageTitle(ODataRow row) {
		String title = "false";
		if (!row.getString("record_name").equals("false"))
			title = row.getString("record_name");
		if (!title.equals("false") && !row.getString("subject").equals("false"))
			title += ": " + row.getString("subject");
		if (title.equals("false") && !row.getString("subject").equals("false"))
			title = row.getString("subject");
		if (title.equals("false"))
			title = "comment";
		return title;
	}

	public String getChildCount(ODataRow row) {
		List<ODataRow> childs = row.getO2MRecord("child_ids").browseEach();
		return (childs.size() > 0) ? childs.size() + " replies" : " ";
	}

	public String getAuthorName(ODataRow row) {
		String author_name = null;
		ODataRow author = row.getM2ORecord("author_id").browse();
		if (author != null) {
			author_name = author.getString("name");
		} else {
			author_name = row.getString("email_from");
		}
		return author_name;
	}

	@Override
	public ODomain defaultDomain() {
		Integer user_id = user().getUser_id();
		List<Integer> parent_ids = new ArrayList<Integer>();
		for (ODataRow row : select("parent_id = ?", new Object[] { "0" })) {
			parent_ids.add(row.getInt("id"));
		}
		ODomain domain = new ODomain();
		if (parent_ids.size() > 0) {
			domain.add("|");
			domain.add("id", "child_of", parent_ids);
		}
		domain.add("|");
		domain.add("partner_ids.user_ids", "in", new JSONArray().put(user_id));
		domain.add("|");
		domain.add("notification_ids.partner_id.user_ids", "in",
				new JSONArray().put(user_id));
		domain.add("author_id.user_ids", "in", new JSONArray().put(user_id));
		if (!isEmptyTable()) {
			domain.add("|");
			domain.add("date", ">", getSyncHelper().getLastSyncDate(this));
		}
		return domain;
	}

	public static class ResUsers extends OModel {

		OColumn name = new OColumn("Name", OVarchar.class, 64);

		public ResUsers(Context context) {
			super(context, "res.users");
		}

	}
}
