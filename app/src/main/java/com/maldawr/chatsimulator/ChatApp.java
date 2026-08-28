package com.maldawr.chatsimulator;

import android.app.Application;

public class ChatApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Store.ensureSeeded(this);
        NotificationHelper.ensureChannels(this);
        AutomationManager.ensureScheduled(this);
    }
}
