package com.skillconnect.app;

import android.app.Application;

import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.notifications.NotificationHelper;

/** Application class: runs once when the app process starts. */
public class SkillConnectApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        RepositoryProvider.init(this);
    }
}
