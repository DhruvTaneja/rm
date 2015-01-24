package com.dhruv.backgroundservices;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dhruv.resumemanager.Main;
import com.dhruv.resumemanager.R;
import com.dhruv.resumemanager.Splash;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by dhruv on 20/1/15.
 * This class grabs the CTCs of the
 * recruiters fetched from the background service.
 */
public class CTCBgService extends AsyncTask<String, Void, String> {
    ArrayList<String> urls;
    int index;
    Context context;
    File dbPath = new File(Environment.getExternalStorageDirectory()
            + File.separator + "Android/data/com.dhruv.resumemanager/db");

    public CTCBgService(ArrayList<String> urls, int index, Context context) {
        this.urls = urls;
        this.index = index;
        this.context = context;
    }



    @Override
    protected String doInBackground(String... params) {
        try {
            Log.d("INDEX", String.valueOf(index));
            handleUrlResponse(index);
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleUrlResponse(int urlIndex) throws MalformedURLException {
        String result = "", s;
        if(index == urls.size()) {    //  all the urls have been visited
            result = "done";
        }
        else{
            s = urls.get(urlIndex);
            URL url = new URL(s);
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                //  headers of the POST request
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept-encoding", "gzip, deflate");

                /*
                don't ever skip this statement
                won't get you any response text
                */
                InputStream is = conn.getInputStream();

                result = readStream(is);
                if (result.length() == 0)
                    result = "lost";
            }
            catch (SocketTimeoutException e) {
            /*
            this exception would occur when the connection
            is failed because of connection timeout, i.e.,
            when the connection is taking too long
            */
                result = "timeout";
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        switch (result) {
            case "timeout":
                fireNotification(
                        "Connection timed out. Please try some other time.",
                        "Connection timed out. Please try some other time.");
                broadcastFailure();
                break;
            case "lost":
                fireNotification(
                        "Connection lost. Please try some other time.",
                        "Connection lost. Please try some other time.");
                broadcastFailure();
                break;
            case "done":
                Intent recruiterService = new Intent(context, RecruiterBgService.class);
                context.stopService(recruiterService);
                SharedPreferences sharedPreferences = context.getSharedPreferences("resume",
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                //  notify for i new recruiters
                fireNotification(urlIndex);

                //  edit sharedPreferences to change the first recruiters entry reference
                String getFirst = "SELECT * FROM recruiters LIMIT 1";
                SQLiteDatabase sqLiteDatabase = context
                        .openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                Cursor firstCursor = sqLiteDatabase.rawQuery(getFirst, null);
                firstCursor.moveToFirst();
                editor.putString("1stRId", firstCursor.getString(0));
                editor.putString("1stRUrl", firstCursor.getString(1));
                editor.apply();
                sqLiteDatabase.close();

                //  broadcast to active activity, if any
                Intent broadcastNewData = new Intent("newData");
                broadcastNewData.putExtra("more", true);
                context.sendBroadcast(broadcastNewData);
                break;
            default:
                String[] lines = result.split("\n");
                String ctc = "";
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].contains("contentTextTitle2\">CTC")) {
                        int startIndex = lines[i + 1].indexOf("left\">");
                        int endIndex = lines[i + 1].indexOf("</td>");
                        ctc = lines[i + 1].substring(startIndex + 6, endIndex);
                        break;
                    }
                }

                sqLiteDatabase = context.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                String insertCTC = "UPDATE recruiters SET ctc = '" + ctc + "' WHERE" +
                        " url = '" + urls.get(index) + "';";
                sqLiteDatabase.execSQL(insertCTC);
                sqLiteDatabase.close();
                new CTCBgService(urls, index + 1, context).execute();
                break;
        }
    }

    //  the method used to read the response text in HttpUrlConnection implementation
    public String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private void broadcastFailure() {
        fireNotification(
                "Cannot create database. Internet connection might be temporarily unavailable. " +
                        "Check your connection and try again.",
                "Database Interrupted");
        Log.d("RECRUITER", "Connection lost");

        //  broadcasting failure
        Intent intent = new Intent("dbUpdate");
        intent.putExtra("dbUpdate", "notCreated");
        context.sendBroadcast(intent);
    }

    private void fireNotification(String message, String ticker) {

        //  new intent with LoginActivity component
        Intent loginIntent = new Intent(context, Splash.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //  The action for the notification
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                loginIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //  notificationSound is a reference to the default notification sound of the device
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.logo))   //  setLargeIcon won't take integer parameter
                        .setWhen(System.currentTimeMillis())    //  to show the timestamp
                        .setTicker(ticker)  //  the text that comes in the status bar before swiped down
                        .setSmallIcon(R.drawable.logo)  //  the one that comes with the ticker
                        .setContentTitle("Resume Manager") //  title of the notification
                        .setAutoCancel(true)    //  to dismiss on click
                        .setSound(notificationSound)    //  the notification sound
                        .setContentIntent(pendingIntent)    //  the action of onclick
                        .setContentText(message);   //  the message body
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        /*
        notificationId is used to refer to the notification
        once it's fired. It can be later updated,
        removed, etc
        */
        int notificationId = 1001;
        notificationManager.notify(notificationId, builder.build());
    }

    private void fireNotification(int i) {
        //  new intent with Main component
        Intent mainIntent = new Intent(context, Main.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("position", 1);   //  what to open when the activity is started

        //  The action for the notification
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //  notificationSound is a reference to the default notification sound of the device
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentText(String.valueOf(i) + " unread recruiters")
                .setLargeIcon(BitmapFactory.decodeResource
                        (context.getResources(), R.drawable.logo))
                .setContentTitle("Resume Manager")
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setSound(notificationSound)
                .setNumber(i)
                .setLights(Color.WHITE, 400, 4000)
                .setWhen(System.currentTimeMillis());
        NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(2, builder.build());
    }
}