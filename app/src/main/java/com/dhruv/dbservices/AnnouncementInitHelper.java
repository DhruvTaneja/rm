package com.dhruv.dbservices;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import getdata.GetAnnouncementData;
import getdata.GetLinks;

/**
 * Created by dhruv on 2/1/15.
 * This class takes the URLs, looks for announcement
 * details and updates the database. It fetches the
 * other paginated URLs and calls itself for those URLs.
 */
public class AnnouncementInitHelper extends AsyncTask<String, Void, String> {
    private String url;
    Context context;

    AnnouncementInitHelper(String url, Context context) {
        this.url = url;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        SharedPreferences preferences = context.getSharedPreferences("resume", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("runner", "announcement");
        editor.apply();
    }

    @Override
    protected String doInBackground(String... strings) {
        if(url == null) {
            Log.d("ANNOUNCEMENT", "Url was null");
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

    private boolean checkConnection() {
        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMan.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
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

            if (result.equals(""))
                result = "lost";
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

        switch (result) {
            case "timeout":
                Toast.makeText(context, "Connection timed out. Please try some other time.", Toast.LENGTH_LONG)
                        .show();
                break;
            case "lost":
                Toast.makeText(context, "Connection lost. Please try some other time.", Toast.LENGTH_LONG)
                        .show();
                break;
            default:
                SQLiteDatabase sqLiteDatabase;
                File dbPath = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "Android/data/com.dhruv.resumemanager/db");
                sqLiteDatabase = context.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                if(result.equals("lost")) {
            /*
            Delete the database
            Check internet connection
            Restart the service if connection is available
            Broadcast failure
            */
                    String dropQuery = "DROP TABLE db.announcements";
                    sqLiteDatabase.execSQL(dropQuery);

                    if(checkConnection())
                        new AnnouncementInitHelper(url.toString(), context).execute();
                    else {
                        Log.d("type", "Connection lost");

                        //  broadcast failure
                        Intent intent = new Intent("dbUpdate");
                        intent.putExtra("dbUpdate", "notCreated");
                        context.sendBroadcast(intent);
                        return;
                    }
                }

                String next = new GetLinks(result).getNextLink();
                try {
                    String createTableQuery = "CREATE TABLE IF NOT EXISTS "
                            + "announcements" + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, "
                            + "date DATE, time VARCHAR, message VARCHAR);";

                    sqLiteDatabase.execSQL(createTableQuery);

                    List<HashMap<String, String>> announcements = new GetAnnouncementData(result).getDataList();
                    for (HashMap<String, String> announcement : announcements) {
                        String title = announcement.get("title");
                        String date = announcement.get("date");
                        String time = announcement.get("time");
                        String message = announcement.get("message");

                        String insertQuery = "INSERT INTO "
                                + "announcements" + "(title, date, time, message)" +
                                " VALUES('" + title + "', '" + date +
                                "', '" + time + "', '" + message + "');";
                        Log.d("ANN", announcement.get("title"));
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
                }
                finally {
                    if(sqLiteDatabase != null)  //  if database wasn't opened at all, else throws null exception
                        sqLiteDatabase.close();
                }
                if(next == null) {  //  there is no Next link on the page, i.e., the Last page has been reached
                    Log.d("ANNOUNCEMENTS", "DB created for this type");

                    //  add sharedPreferences to record the first entry
                    //  of the table, using cursor
                    SharedPreferences sharedPreferences = context.getSharedPreferences("resume",
                            Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    String getFirst = "SELECT * FROM announcements LIMIT 1";
                    sqLiteDatabase = context.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                            null);
                    Cursor firstCursor = sqLiteDatabase.rawQuery(getFirst, null);
                    firstCursor.moveToFirst();
                    /*
                    3 things to be added to the sharedPreferences
                    _id, title and date, to uniquely identify any announcement
                    They will be edited again when any new announcement
                    will be added to the database
                    */
                    editor.putString("1stAId", firstCursor.getString(0));
                    editor.putString("1stATitle", firstCursor.getString(1));
                    editor.putString("1stADate", firstCursor.getString(2));
                    editor.putBoolean("announcements", true);
                    editor.apply();

                    //  could not start service from a service
                    //  trying broadcast method
                    Intent intent = new Intent("dbUpdate");
                    intent.putExtra("dbUpdate", "announcement");
                    context.sendBroadcast(intent);
                }
                else
                    new AnnouncementInitHelper(next, context).execute();
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
}