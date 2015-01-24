package com.dhruv.dbservices;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by dhruv on 2/1/15.
 * This class is used to run a service for creating
 * the database behind the scenes. The CreatingDB activity
 * will start an intent of this component.
 */
public class AnnouncementInitService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("url");
        Log.d("INIT", "Starting DB creation");
        AnnouncementInitHelper announcementInitHelper =
                new AnnouncementInitHelper(url, getApplicationContext());
        announcementInitHelper.execute();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}