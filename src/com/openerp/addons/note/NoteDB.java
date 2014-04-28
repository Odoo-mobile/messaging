/**
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http://www.openerp.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * 
 */
package com.openerp.addons.note;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.openerp.base.res.ResPartnerDB;
import com.openerp.orm.OEColumn;
import com.openerp.orm.OEDatabase;
import com.openerp.orm.OEFields;
import com.openerp.orm.OEValues;

public class NoteDB extends OEDatabase {
	Context mContext = null;

	public NoteDB(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public String getModelName() {
		return "note.note";
	}

	@Override
	public List<OEColumn> getModelColumns() {
		List<OEColumn> cols = new ArrayList<OEColumn>();
		cols.add(new OEColumn("name", "Name", OEFields.varchar(64)));
		cols.add(new OEColumn("memo", "Memo", OEFields.varchar(64)));
		cols.add(new OEColumn("open", "Open", OEFields.varchar(64)));
		cols.add(new OEColumn("date_done", "Date_Done", OEFields.varchar(64)));
		cols.add(new OEColumn("stage_id", "NoteStages", OEFields
				.manyToOne(new NoteStages(mContext))));
		cols.add(new OEColumn("tag_ids", "NoteTags", OEFields
				.manyToMany(new NoteTags(mContext))));
		cols.add(new OEColumn("current_partner_id", "Res_Partner", OEFields
				.manyToOne(new ResPartnerDB(mContext))));
		cols.add(new OEColumn("note_pad_url", "URL", OEFields.text()));
		cols.add(new OEColumn("message_follower_ids", "Followers", OEFields
				.manyToMany(new ResPartnerDB(mContext))));
		return cols;
	}

	public class NoteStages extends OEDatabase {

		String[] mStageColors = new String[] { "#f56447", "#ffba24", "#eded24",
				"#b5c4c4", "#b5c4c4", "#76dbba", "#9fc22f", "#7edfff",
				"#EAC14D", "#cbcca2", "#01B169", "#80A8CC", "#CBD0D3",
				"#ffe825", "#EFF4FF", "#CDC81E", "#00C0E4", "#edb8ff",
				"#ffafb7", "#E98B39" };

		public NoteStages(Context context) {
			super(context);
		}

		@Override
		public String getModelName() {
			return "note.stage";
		}

		@Override
		public List<OEColumn> getModelColumns() {
			List<OEColumn> cols = new ArrayList<OEColumn>();
			cols.add(new OEColumn("name", "Name", OEFields.text()));
			cols.add(new OEColumn("sequence", "Sequence", OEFields.integer()));
			cols.add(new OEColumn("stage_color", "Stage Color", OEFields
					.varchar(10), false));
			
			return cols;
		}

		@Override
		public long create(OEValues values) {
			int total = count();
			int i = (total > mStageColors.length) ? 0 : total;
			String stage_color = mStageColors[i];
			values.put("stage_color", stage_color);
			return super.create(values);
		}

	}

	public class NoteTags extends OEDatabase {

		public NoteTags(Context context) {
			super(context);
		}

		@Override
		public String getModelName() {
			return "note.tag";
		}

		@Override
		public List<OEColumn> getModelColumns() {
			List<OEColumn> cols = new ArrayList<OEColumn>();
			cols.add(new OEColumn("name", "Name", OEFields.text()));
			return cols;
		}
	}

}
