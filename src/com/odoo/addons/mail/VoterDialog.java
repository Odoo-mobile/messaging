package com.odoo.addons.mail;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.BezelImageView;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.orm.ODataRow;
import com.odoo.support.listview.OListDataAdapter;
import com.odoo.util.Base64Helper;

public class VoterDialog {
	private Context mContext;
	private List<ODataRow> mVoters = new ArrayList<ODataRow>();
	private Builder mBuilder;
	private OListDataAdapter mListAdapter = null;
	private List<Object> mBuilderitems = new ArrayList<Object>();

	public VoterDialog(Context context, List<ODataRow> voters,
			List<Object> listItems) {
		mContext = context;
		mVoters.addAll(voters);
		mBuilderitems = listItems;
	}

	public VoterDialog build() {
		mBuilder = new Builder(mContext);
		mBuilder.setTitle("Voters");
		mBuilder.setPositiveButton(mContext.getString(R.string.label_ok), null);
		View view = LayoutInflater.from(mContext).inflate(R.layout.like_layout,
				null);
		ListView lst_user_list = (ListView) view.findViewById(R.id.like_list);
		mListAdapter = new OListDataAdapter(mContext,
				R.layout.custom_like_layout, mBuilderitems) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View mView = convertView;
				if (mView == null)
					mView = LayoutInflater.from(mContext).inflate(
							R.layout.custom_like_layout, parent, false);
				ODataRow objects = (ODataRow) mBuilderitems.get(position);
				BezelImageView user_img = (BezelImageView) mView
						.findViewById(R.id.img_user_image);
				TextView username = (TextView) mView
						.findViewById(R.id.txv_username);
				String base64 = objects.getString("image_small");
				if (!base64.equals("false")) {
					user_img.setImageBitmap(Base64Helper.getBitmapImage(
							mContext, base64));
				} else
					user_img.setImageResource(R.drawable.avatar);
				username.setText(objects.getString("name"));
				return mView;
			}
		};
		lst_user_list.setAdapter(mListAdapter);
		mBuilder.setView(view);
		return this;
	}

	public void show() {
		if (mVoters.size() > 0) {
			mBuilder.create().show();
		} else {
			Toast.makeText(mContext,
					mContext.getString(R.string.label_no_voter_found),
					Toast.LENGTH_LONG).show();
		}
	}

}
