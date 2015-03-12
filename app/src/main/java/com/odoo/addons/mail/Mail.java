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
 * Created on 12/3/15 2:17 PM
 */
package com.odoo.addons.mail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.R;
import com.odoo.base.addons.mail.MailMessage;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.utils.OResource;

import java.util.ArrayList;
import java.util.List;

public class Mail extends BaseFragment {
    public static final String TAG = Mail.class.getSimpleName();
    public static final String KEY_MAIL_TYPE = "mail_type";

    public enum Type {
        Inbox, ToMe, ToDo, Archives, Outbox
    }

    private Type mType = Type.Inbox;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.common_listview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> menu = new ArrayList<>();
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_inbox))
                .setInstance(new Mail())
                .setIcon(R.drawable.ic_action_inbox)
                .setExtra(extra(Type.Inbox)));
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_to_me))
                .setInstance(new Mail())
                .setIcon(R.drawable.ic_action_user)
                .setExtra(extra(Type.ToMe)));
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_to_do))
                .setInstance(new Mail())
                .setIcon(R.drawable.ic_action_todo)
                .setExtra(extra(Type.ToDo)));
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_archives))
                .setInstance(new Mail())
                .setIcon(R.drawable.ic_action_archive)
                .setExtra(extra(Type.Archives)));
        menu.add(new ODrawerItem(TAG).setTitle(OResource.string(context, R.string.label_outbox))
                .setInstance(new Mail())
                .setIcon(R.drawable.ic_action_outbox)
                .setExtra(extra(Type.Outbox)));
        return menu;
    }

    private Bundle extra(Type type) {
        Bundle extra = new Bundle();
        extra.putString(KEY_MAIL_TYPE, type.toString());
        return extra;
    }

    @Override
    public Class<MailMessage> database() {
        return MailMessage.class;
    }
}
