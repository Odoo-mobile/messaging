/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
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
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 * Created on 12/3/15 2:39 PM
 */
package com.odoo.addons.groups;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.groups.models.MailGroup;
import com.odoo.addons.mail.Mail;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OResource;

import java.util.ArrayList;
import java.util.List;


public class Groups extends BaseFragment implements OCursorListAdapter.OnViewBindListener,
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener, ISyncStatusObserverListener {
    public static final String TAG = Groups.class.getSimpleName();
    public static final String KEY_GROUP_ID = "key_group_id";
    private ListView listView;
    private OCursorListAdapter listAdapter;
    private View mView;
    private Boolean syncRequested = false;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.common_listview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        mView.findViewById(R.id.fabButton).setVisibility(View.GONE);
        setHasSyncStatusObserver(TAG, this, db());
        init();
    }

    private void init() {
        listView = (ListView) mView.findViewById(R.id.listview);
        listAdapter = new OCursorListAdapter(getActivity(), null, R.layout.groups_item_view);
        listAdapter.setOnViewBindListener(this);
        listView.setAdapter(listAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        OControls.setText(view, R.id.group_name, row.getString("name"));
        OControls.setText(view, R.id.group_description, row.getString("description"));
        if (!row.getString("image").equals("false")) {
            Bitmap bmp = BitmapUtils.getBitmapImage(getActivity(), row.getString("image"));
            OControls.setImage(view, R.id.group_icon, bmp);
        } else {
            OControls.setImage(view, R.id.group_icon, R.drawable.default_group);
        }
        Boolean has_followed = row.getBoolean("has_followed");
        if (has_followed) {
            OControls.setVisible(view, R.id.btnUnfollow);
            OControls.setGone(view, R.id.btnJoin);
        } else {
            OControls.setVisible(view, R.id.btnJoin);
            OControls.setGone(view, R.id.btnUnfollow);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), db().uri(), null, null, null, "has_followed desc, name");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        listAdapter.changeCursor(data);
        if (data.getCount() > 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setVisible(mView, R.id.swipe_container);
                    OControls.setGone(mView, R.id.customer_no_items);
                    setHasSwipeRefreshView(mView, R.id.swipe_container, Groups.this);
                }
            }, 500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    OControls.setGone(mView, R.id.loadingProgress);
                    OControls.setGone(mView, R.id.swipe_container);
                    OControls.setVisible(mView, R.id.customer_no_items);
                    setHasSwipeRefreshView(mView, R.id.customer_no_items, Groups.this);
                    OControls.setImage(mView, R.id.icon, R.drawable.ic_action_social_group);
                    OControls.setText(mView, R.id.title, _s(R.string.label_no_groups_found));
                    OControls.setText(mView, R.id.subTitle, "");
                }
            }, 500);
            if (db().isEmptyTable() && !syncRequested) {
                syncRequested = true;
                onRefresh();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        listAdapter.changeCursor(null);
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        MailGroup mailGroup = new MailGroup(context, null);
        List<ODrawerItem> menu = new ArrayList<>();
        menu.add(new ODrawerItem(TAG)
                .setTitle(OResource.string(context, R.string.label_my_groups))
                .setGroupTitle());
        menu.add(new ODrawerItem(TAG)
                .setTitle(OResource.string(context, R.string.label_join_group))
                .setInstance(new Groups())
                .setIcon(R.drawable.ic_action_social_group));
        for (ODataRow row : mailGroup.select(new String[]{"name"}, "has_followed = ?",
                new String[]{"true"}, "has_followed desc, name")) {
            menu.add(new ODrawerItem(TAG)
                    .setTitle(row.getString("name"))
                    .setInstance(new Mail())
                    .setExtra(extra(row.getInt(OColumn.ROW_ID)))
                    .setIcon(R.drawable.ic_action_group_icon));
        }
        return menu;
    }

    private Bundle extra(int id) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_GROUP_ID, id);
        return bundle;
    }

    @Override
    public Class<MailGroup> database() {
        return MailGroup.class;
    }

    @Override
    public void onRefresh() {
        if (inNetwork()) {
            setSwipeRefreshing(true);
            parent().sync().requestSync(MailGroup.AUTHORITY);
        } else {
            Toast.makeText(getActivity(), R.string.toast_network_required, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        if (!refreshing) {
            setSwipeRefreshing(refreshing);
            getLoaderManager().restartLoader(0, null, this);
        }
    }
}
