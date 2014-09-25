package com.odoo.addons.mail.widgets;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class MailRemoteViewService extends RemoteViewsService {
	public static final String TAG = "com.odoo.addons.mail.widgets.MailRemoteViewService";

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		Log.d(TAG, "MessageRemoteViewService->onGetViewFactory()");
		MailRemoteViewFactory rvFactory = new MailRemoteViewFactory(
				this.getApplicationContext(), intent);
		return rvFactory;

	}

}
