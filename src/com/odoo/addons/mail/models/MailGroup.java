package com.odoo.addons.mail.models;

import java.util.List;

import odoo.OArguments;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.odoo.addons.mail.providers.group.MailGroupProvider;
import com.odoo.base.mail.MailFollowers;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.OModel;
import com.odoo.orm.OSyncHelper;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBlob;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.support.provider.OContentProvider;
import com.odoo.util.JSONUtils;

public class MailGroup extends OModel {
	Context mContext = null;

	OColumn name = new OColumn("Name", OVarchar.class, 64);
	OColumn description = new OColumn("Description", OText.class);
	OColumn image_medium = new OColumn("Image_Medium", OBlob.class);
	OColumn message_follower_ids = new OColumn("Followers",
			MailFollowers.class, RelationType.ManyToMany);
	@Odoo.Functional(method = "hasFollowed", depends = { "message_follower_ids" }, store = true)
	OColumn has_followed = new OColumn("Followed", OBoolean.class)
			.setDefault(0).setLocalColumn();

	public MailGroup(Context context) {
		super(context, "mail.group");
		mContext = context;
	}

	public int hasFollowed(OValues vals) {
		List<Integer> ids = JSONUtils.toList((JSONArray) vals
				.get("message_follower_ids"));
		if (ids.indexOf(user().getPartner_id()) > -1) {
			return 1;
		}
		return 0;
	}

	public void followUnfollowGroup(int group_id, boolean follow) {
		try {
			OSyncHelper sync = getSyncHelper();
			OValues values = new OValues();
			OArguments args = new OArguments();
			args.add(new JSONArray().put(selectServerId(group_id)));
			if (follow) {
				// Action Follow
				sync.callMethod("action_follow", args, new JSONObject());
				values.put("has_followed", 1);
			} else {
				// Action unfollow
				sync.callMethod("action_unfollow", args, new JSONObject());
				values.put("has_followed", 0);
			}
			resolver().update(group_id, values);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
