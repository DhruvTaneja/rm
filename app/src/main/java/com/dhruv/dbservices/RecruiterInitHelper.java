package com.dhruv.dbservices;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.dhruv.resumemanager.CTCInitHelper;
import com.dhruv.resumemanager.R;
import com.dhruv.resumemanager.Splash;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import getdata.GetLinks;
import getdata.GetRecruiterData;

/**
 * Created by dhruv on 4/1/15.
 * This class takes the URLs, looks for recruiter
 * details and updates the database. It fetches the
 * other paginated URLs and calls itself for those URLs.
 */
public class RecruiterInitHelper extends AsyncTask<String, Void, String>{

    private Context context;
    private String url;

    public RecruiterInitHelper(String url, Context context) {
        this.context = context;
        this.url = url;
    }

    @Override
    protected void onPreExecute() {
        SharedPreferences preferences = context.getSharedPreferences("resume", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("runner", "recruiter");
        editor.apply();
    }

    @Override
    protected String doInBackground(String... params) {
        if(url == null) {
            Log.d("RECRUITER", "Url was null");
            return null;
        }
        try {
            getAsyncResults(url);
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void broadcastFailure() {
        if (checkConnection())
            new RecruiterInitHelper(url, context).execute();
        else {
            fireNotification("Resume Manager",
                    "Cannot create database. Internet connection might be temporarily unavailable. " +
                            "Check your connection and try again.",
                    "Database Interrupted");
            Log.d("RECRUITER", "Connection lost");

            //  broadcasting failure
            Intent intent = new Intent("dbUpdate");
            intent.putExtra("dbUpdate", "notCreated");
            context.sendBroadcast(intent);
        }
    }

    private boolean checkConnection() {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMan.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
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

    //  the regular HttpUrlConnection implementation to get response text
    public void getAsyncResults(String pageUrl) throws IOException {
        String result;
        final URL url;
        try {
            url = new URL(pageUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(100000);
            conn.setReadTimeout(150000);
            conn.setDoOutput(true);

            InputStream is = conn.getInputStream();
            result = readStream(is);
        }
        catch (SocketException e) {
            /*
            possible causes :-
            1.  the host refused connection
            2.  the socket connection was lost
            3.  the internet connection was lost
            */
            result = "lost";
        }
        catch (SocketTimeoutException e) {
            /*
            possible causes :-
            1.  the host did not respond in time
            2.  network failure, like DNS resolution
             */
            result = "timeout";
        }

        SQLiteDatabase sqLiteDatabase;
        File dbPath = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Android/data/com.dhruv.resumemanager/db");
        sqLiteDatabase = context.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                null);

        String dropQuery = "DROP TABLE IF EXISTS db.recruiters";

        if(result.equals("timeout"))
            Toast.makeText(context, "Timeout. Check connection", Toast.LENGTH_LONG).show();

        if(!result.equalsIgnoreCase("LOST") && result.length() != 0) {
            String next = new GetLinks(result).getNextLink();
            if (next != null)
                Log.d("RECRUITER", next);
            try {
                String createTableQuery = "CREATE TABLE IF NOT EXISTS "
                        + "recruiters" + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, url VARCHAR, "
                        + "category VARCHAR, accepting VARCHAR, name VARCHAR, " +
                        "branches VARCHAR, BE VARCHAR, ME VARCHAR, " +
                        "intern VARCHAR, MBA VARCHAR, visitDate VARCHAR, ctc VARCHAR, fav VARCHAR);";

                sqLiteDatabase.execSQL(createTableQuery);

                List<HashMap<String, String>> recruiters = new GetRecruiterData(result).getDataList();
                for (HashMap<String, String> recruiter : recruiters) {
                    String recruiterUrl = recruiter.get("url");
                    String category = recruiter.get("category");
                    String accepting = recruiter.get("accepting");
                    String name = recruiter.get("name");
                    String branches = recruiter.get("branches");
                    String BE = recruiter.get("BE");
                    String ME = recruiter.get("ME");
                    String intern = recruiter.get("Intern");
                    String MBA = recruiter.get("MBA");
                    String visitDate = recruiter.get("visitDate");

                    Log.d("REC", recruiter.toString());
                    String insertQuery = "INSERT INTO recruiters (url, category, accepting, name "
                            + ", branches, BE, ME, intern, MBA, visitDate) VALUES('"
                            + recruiterUrl + "', '" +
                            category + "', '" + accepting + "', '" + name + "', '" + branches + "', '" +
                            BE + "', '" + ME + "', '" + intern + "', '" + MBA + "', '" + visitDate + "');";
                    sqLiteDatabase.execSQL(insertQuery);
                }
            }
        /*
        if not able to connect to the database
        maybe due to non-availability of privileges
        */
            catch (SQLiteCantOpenDatabaseException e) {
                Log.d("can't open", e.toString());
                Toast.makeText(context, "Cannot connect to the database", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                if (sqLiteDatabase != null)  //  if database wasn't opened at all, else throws null exception
                    sqLiteDatabase.close();
            }

            if (next == null) {  //  there is no Next link on the page, i.e., the Last page has been reached
                Log.d("RECRUITERS", "DB created for this type");

                String getFirst = "SELECT * FROM recruiters LIMIT 1";
                sqLiteDatabase = context.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                Cursor firstCursor = sqLiteDatabase.rawQuery(getFirst, null);
                firstCursor.moveToFirst();

                SharedPreferences sharedPreferences = context.getSharedPreferences("resume",
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("recruiters", true);
                editor.putBoolean("database", true);

                /*
                2 things to be added to the sharedPreferences
                _id and url, to uniquely identify any recruiter
                They will be edited again when any new recruiter
                will be added to the database
                */
                editor.putString("1stRId", firstCursor.getString(0));
                editor.putString("1stRUrl", firstCursor.getString(1));
                editor.remove("runner");
                editor.apply();

                sqLiteDatabase = context.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                Cursor cursor = sqLiteDatabase.rawQuery("SELECT url FROM recruiters;", null);
                cursor.moveToFirst();
                ArrayList<String> urls = new ArrayList<>();
                do {
                    urls.add(cursor.getString(0));
                } while (cursor.moveToNext());

                CTCInitHelper ctcInitHelper = new CTCInitHelper(urls, 0, context);
                ctcInitHelper.execute();
            }
            else
                new RecruiterInitHelper(next, context).execute();
        }
        else {
            /*
            Delete the recruiter table
            Check internet connection
            Restart service if connection available
            else fire a failure notification
            and broadcast failure
            */
            sqLiteDatabase.execSQL(dropQuery);
            broadcastFailure();
        }
    }

    private void fireNotification(String title, String message, String ticker) {

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
                        .setContentTitle(title) //  title of the notification
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
}
