package com.odoo.addons.mail.models;

import java.util.List;

import odoo.ODomain;
import android.content.Context;

import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;

public class MailNotification extends OModel {

	@Odoo.api.v7
	OColumn read = new OColumn("Read", OBoolean.class).setDefault(false);

	@Odoo.api.v8
	@Odoo.api.v9alpha
	OColumn is_read = new OColumn("Is Read", OBoolean.class).setDefault(false);
	OColumn starred = new OColumn("Starred", OBoolean.class);
	OColumn partner_id = new OColumn("Partner_id", ResPartner.class,
			RelationType.ManyToOne);
	OColumn message_id = new OColumn("Message_id", MailMessage.class,
			RelationType.ManyToOne);

	Context mContext = null;

	public MailNotification(Context context) {
		super(context, "mail.notification");
		mContext = context;
		setCreateWriteLocal(true);
	}

	public Boolean getStarred(int msgid) {
		boolean starred = false;
		List<ODataRow> row = (List<ODataRow>) select("message_id = ?",
				new String[] { msgid + "" });
		starred = row.get(0).getBoolean("starred");
		return starred;
	}

	public Boolean getIsread(int msgid) {
		boolean isread = false;
		List<ODataRow> row = (List<ODataRow>) select("message_id = ?",
				new String[] { msgid + "" });
		isread = row.get(0).getBoolean("is_read");
		return isread;
	}

	@Override
	public ODomain defaultDomain() {
		ODomain domain = new ODomain();
		domain.add("partner_id", "=", user().getPartner_id());
		return domain;
	}

	@Override
	public int update(OValues updateValues, String where, Object[] whereArgs) {
		ODataRow noti = select(updateValues.getInt(OColumn.ROW_ID));
		MailMessage mail = new MailMessage(mContext);
		OValues values = new OValues();
		values.put("starred", noti.get("starred"));
		if (getColumn("is_read") != null)
			values.put("to_read", !noti.getBoolean("is_read"));
		else
			values.put("to_read", !noti.getBoolean("read"));
		mail.update(values, (Integer) noti.getM2ORecord("message_id").getId());
		return super.update(updateValues, where, whereArgs);
	}

	@Override
	public Boolean checkForLocalLatestUpdate() {
		return false;
	}

	@Override
	public Boolean checkForLocalUpdate() {
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

	@Override
	public Boolean checkForCreateDate() {
		return false;
	}

	@Override
	public Boolean checkForWriteDate() {
		return false;
	}
}
