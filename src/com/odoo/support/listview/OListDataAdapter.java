package com.odoo.support.listview;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;

//public class OListDataAdapter extends ArrayAdapter<String> {
//	String[] color_names;
//	Integer[] image_id;
//	Context context;
//
//	public OListDataAdapter(Activity context, Integer[] image_id, String[] text) {
//		super(context, R.layout.custom_like_layout, text);
//		// TODO Auto-generated constructor stub
//		this.color_names = text;
//		this.image_id = image_id;
//		this.context = context;
//	}setIcon
//
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		// TODO Auto-generated method stub
//		LayoutInflater inflater = (LayoutInflater) context
//				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		View single_row = inflater.inflate(R.layout.custom_like_layout, null,
//				true);
//		TextView textView = (TextView) single_row
//				.findViewById(R.id.txv_username);
//		ImageView imageView = (ImageView) single_row
//				.findViewById(R.id.img_user_image);
//		textView.setText(color_names[position]);
//		imageView.setImageResource(image_id[position]);
//		return single_row;
//	}
//}

public class OListDataAdapter extends ArrayAdapter<Object> {
	Context mContext = null;
	List<Object> mObjects = null;
	List<Object> mAllObjects = null;
	int mResourceId = 0;

	public OListDataAdapter(Context context, int resource, List<Object> objects) {
		super(context, resource, objects);
		mContext = context;
		mObjects = new ArrayList<Object>(objects);
		mAllObjects = new ArrayList<Object>(objects);
		mResourceId = resource;
	}

}
