package com.odoo.addons.mail.models;

import android.content.Context;

import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.base.mail.MailFollowers;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBlob;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.support.provider.OContentProvider;

public class MailGroup extends OModel {
	Context mContext = null;

	OColumn name = new OColumn("Name", OVarchar.class, 64);
	OColumn description = new OColumn("Description", OText.class);
	OColumn image_medium = new OColumn("Image_Medium", OBlob.class);
	@Odoo.Functional(method = "getJoined")
	OColumn has_joined = new OColumn("Joined", OBoolean.class);
	@Odoo.Functional(method = "getUnreadMessageCount")
	OColumn new_messages = new OColumn("New Messages", OVarchar.class);

	public MailGroup(Context context) {
		super(context, "mail.group");
		mContext = context;
	}

	public String getUnreadMessageCount(ODataRow row) {
		MailMessage mails = new MailMessage(mContext);
		int count = mails.count("res_id = ? and model = ? and to_read = ?",
				new Object[] { row.getInt("id"), getModelName(), true });
		if (count > 0)
			return count + " ";
		return "";
	}

	public Boolean getJoined(ODataRow row) {
		MailFollowers followers = new MailFollowers(mContext);
		ResPartner partners = new ResPartner(mContext);
		return (followers.select(
				"res_model = ? and res_id = ? and partner_id = ?",
				new Object[] { getModelName(), row.getInt("id"),
						partners.selectRowId(user().getPartner_id()) }).size() > 0);
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

	@Override
	public OContentProvider getContentProvider() {
		return new MailGroupProvider();
	}
}
