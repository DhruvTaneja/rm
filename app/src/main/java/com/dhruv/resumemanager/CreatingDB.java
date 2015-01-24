package com.dhruv.resumemanager;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Window;

import com.dhruv.dbservices.AnnouncementInitService;
import com.dhruv.dbservices.RecruiterInitService;


public class CreatingDB extends Activity {
    Intent announcementIntent;

    BroadcastReceiver broadcastReceiver, broadcastReceiverOnPause;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.creating_db);

        SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);

        if(!sharedPreferences.getBoolean("announcements", false)) {
            if(!sharedPreferences.getBoolean("dbRunning", false)) {
                if(!sharedPreferences.getString("runner", "").equals("announcement")) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("dbRunning", true);
                    editor.apply();
                    announcementIntent = new Intent(this, AnnouncementInitService.class);
                    announcementIntent.putExtra("url",
                            "http://www.dce.ac.in/placement/announcements.php?pageNum_rsAnnouncements=1&totalRows_rsAnnouncements=934");
                    startService(announcementIntent);
                }
            }
        }
        else if(!sharedPreferences.getBoolean("recruiters", false)) {
            if(!sharedPreferences.getString("runner", "").equals("recruiter")) {
                Intent recruiterIntent = new Intent(this, RecruiterInitService.class);
                recruiterIntent.putExtra("url",
                        "http://www.dce.ac.in/placement/recruiter_list.php?pageNum_rsRecruiter=2&totalRows_rsRecruiter=168");
                startService(recruiterIntent);
            }
        }
    }

    @Override
    protected void onResume() {
        /*
        the broadcastReceiverOnPause is never registered
        when the activity shows up for the first time
        but will be registered when it goes into the
        background. To avoid error at the first time,
        try/catch block is used.
        */
        try {
            unregisterReceiver(broadcastReceiverOnPause);
        }
        catch (IllegalArgumentException e) {
            Log.d("BroadcastReceiver", "BroadcastReceiverOnPause not yet registered");
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /*
                db creation failed
                clean preferences
                fire notification and go to splash
                */
                if(intent.getStringExtra("dbUpdate").equals("")) {
                    SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
                    cleanPreferences(sharedPreferences);
                    fireNotification("Resume Manager",
                            "Cannot set database. No internet connection available." +
                                    "Check your connection and try again.",
                            "Database Interrupted", null);  //  null means no notification action
                    Intent toSplash = new Intent(CreatingDB.this, Splash.class);
                    startActivity(toSplash);
                    finish();
                }

                //  announcements done, begin recruiters
                else if(intent.getStringExtra("dbUpdate").equals("announcement")) {
                    Intent recruiterIntent = new Intent(CreatingDB.this, RecruiterInitService.class);
                    recruiterIntent.putExtra("url",
                            "http://www.dce.ac.in/placement/recruiter_list.php?pageNum_rsRecruiter=2&totalRows_rsRecruiter=168");
                    stopService(announcementIntent);
                    startService(recruiterIntent);
                }
                /*
                recruiters also done
                fire notification
                finish activity and start main
                */
                else if(intent.getStringExtra("dbUpdate").equals("created")) {
                    fireNotification("Resume", "Database successfully created.",
                            "Database created", null);
                    Intent toMain = new Intent(CreatingDB.this, Main.class);
                    startActivity(toMain);
                    finish();
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("dbUpdate"));
        super.onResume();
    }

    @Override
    protected void onPause() {
        /*
        no need for try/catch block here
        this broadcastReceiver is always registered
        before the following code is executed
        */
        unregisterReceiver(broadcastReceiver);

        broadcastReceiverOnPause = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                /*
                db creation failed
                clean preferences
                fire notification and go to splash
                */
                if(intent.getStringExtra("dbUpdate").equals("")) {
                    SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
                    cleanPreferences(sharedPreferences);
                    fireNotification("Resume Manager",
                            "Cannot set database. No internet connection available." +
                                    "Check your connection and try again.",
                            "Database Interrupted");
                    finish();
                }

                //  announcements done, begin recruiters
                if(intent.getStringExtra("dbUpdate").equals("announcement")) {
                    Intent recruiterIntent = new Intent(CreatingDB.this, RecruiterInitService.class);
                    recruiterIntent.putExtra("url",
                            "http://www.dce.ac.in/placement/recruiter_list.php?pageNum_rsRecruiter=2&totalRows_rsRecruiter=168");
                    stopService(announcementIntent);
                    startService(recruiterIntent);
                }
                /*
                recruiters also done
                fire notification
                finish activity and start main
                */
                if(intent.getStringExtra("dbUpdate").equals("created")) {
                    fireNotification("Resume", "Database successfully created. Tap to continue",
                            "Database created");
                    finish();
                }

            }
        };

        registerReceiver(broadcastReceiverOnPause, new IntentFilter("dbUpdate"));
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //  again to handle already unregistered receivers
        try {
            unregisterReceiver(broadcastReceiver);
        }
        catch (IllegalArgumentException e) {
            Log.d("BroadcastReceiver", "BroadcastReceiver not registered anymore");
        }
        try {
            unregisterReceiver(broadcastReceiverOnPause);
        }
        catch (IllegalArgumentException e) {
            Log.d("BroadcastReceiver", "BroadcastReceiverOnPause not registered anymore");
        }
        super.onDestroy();
    }

    private void fireNotification(String title, String message, String ticker) {

        //  new intent with LoginActivity component
        Intent loginIntent = new Intent(getApplicationContext(), Splash.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //  The action for the notification
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                loginIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //  notificationSound is a reference to the default notification sound of the device
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                R.drawable.logo))   //  setLargeIcon won't take integer parameter
                        .setWhen(System.currentTimeMillis())    //  to show the timestamp
                        .setTicker(ticker)  //  the text that comes in the status bar before swiped down
                        .setSmallIcon(R.drawable.logo)  //  the one that comes with the ticker
                        .setContentTitle(title) //  title of the notification
                        .setAutoCancel(true)    //  to dismiss on click
                        .setSound(notificationSound)    //  the notification sound
                        .setContentIntent(pendingIntent)    //  the action of onclick
                        .setContentText(message);   //  the message body
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        /*
        notificationId is used to refer to the notification
        once it's fired. It can be later updated,
        removed, etc
        */
        int notificationId = 0;
        notificationManager.notify(notificationId, builder.build());
    }

    private void fireNotification(String title, String message, String ticker, PendingIntent pendingIntent) {

        //  new intent with LoginActivity component
        Intent loginIntent = new Intent(getApplicationContext(), Splash.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //  notificationSound is a reference to the default notification sound of the device
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                R.drawable.logo))   //  setLargeIcon won't take integer parameter
                        .setWhen(System.currentTimeMillis())    //  to show the timestamp
                        .setTicker(ticker)  //  the text that comes in the status bar before swiped down
                        .setSmallIcon(R.drawable.logo)  //  the one that comes with the ticker
                        .setContentTitle(title) //  title of the notification
                        .setAutoCancel(true)    //  to dismiss on click
                        .setSound(notificationSound)    //  the notification sound
                        .setContentText(message);   //  the message body
        if(pendingIntent != null) {
            //  The action for the notification
            pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    loginIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(pendingIntent);    //  the action of onclick
        }
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        /*
        notificationId is used to refer to the notification
        once it's fired. It can be later updated,
        removed, etc
        */
        int notificationId = 0;
        notificationManager.notify(notificationId, builder.build());
    }

    //  method to clear all the sharedPreferences to start afresh
    private void cleanPreferences(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("login")
                .remove("username")
                .remove("password")
                .remove("name")
                .remove("urlParams")
                .remove("dbRunning")
                .apply();
    }
}