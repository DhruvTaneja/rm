package com.dhruv.dbservices;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by dhruv on 4/1/15.
 * The Intent service class to load recruiter data
 */
public class RecruiterInitService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String recruiterUrl = intent.getStringExtra("url");
        RecruiterInitHelper recruiterInitHelper =
                new RecruiterInitHelper(recruiterUrl, getApplicationContext());
        recruiterInitHelper.execute();
        return Service.START_NOT_STICKY;
    }
}
