package com.dhruv.backgroundservices;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dhruv.resumemanager.Main;
import com.dhruv.resumemanager.R;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import getdata.GetAnnouncementData;
import getdata.GetLinks;

/**
 * Created by dhruv on 15/1/15.
 * This class takes the responsibility to
 * start looking for new data as soon as
 * the system starts.
 */
public class AnnouncementBgService extends Service {

    String urlParams;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences preferences = getSharedPreferences("resume", MODE_PRIVATE);
        String announcementUrl = intent.getStringExtra("url");
        if(preferences.getBoolean("database", false)) { //  sharedPref for if DB has been created
            //  get other sharedPrefs for POST request
            Log.d("START", "Service started");
            urlParams = preferences.getString("urlParams", null);
            NewAnnouncements newAnnouncements = new NewAnnouncements(announcementUrl, null);
            newAnnouncements.execute();

            return Service.START_REDELIVER_INTENT;
        }
        return Service.START_NOT_STICKY;    //  don't make it sticky if DB not created
    }

    private class NewAnnouncements extends AsyncTask<String, Void, String> {

        String nextUrl;
        List<HashMap<String, String>> newData;

        public NewAnnouncements(String nextUrl, List<HashMap<String, String>> newData) {
            this.nextUrl = nextUrl;
            this.newData = newData;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                return handleAsyncResults(nextUrl);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        //  the regular HttpUrlConnection implementation to get response text
        public String handleAsyncResults(String pageUrl) throws IOException {
            /*
            connecting to student_login to get cookies
            do I really need it??
            */
            URL url;
            try {
                String mainUrl = "http://www.dce.ac.in/placement/student_login.php";
                url = new URL(mainUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }

            try {
                CookieManager cookieManager = new CookieManager();
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                CookieHandler.setDefault(cookieManager);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(100000);
                conn.setReadTimeout(150000);
                conn.setDoOutput(true);

                //  headers of the POST request
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept-encoding", "gzip, deflate");

                //  Writing the urlParams to the output stream
                //  to the POST request
                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(urlParams);
                wr.flush();
                wr.close();

                InputStream is = conn.getInputStream();
                String result = readStream(is);

                if (result.equals(""))
                    return "lost";
            }
            catch (SocketException e) {
            /*
            possible causes :-
            1.  the host refused connection
            2.  the socket connection was lost
            3.  the internet connection was lost
            */
                return "lost";
            }
            catch (SocketTimeoutException e) {
            /*
            possible causes :-
            1.  the host did not respond in time
            2.  network failure, like DNS resolution
             */
                return "lost";
            }

            try {
                url = new URL(pageUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }

            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(100000);
                conn.setReadTimeout(150000);
                conn.setDoOutput(true);

                InputStream is = conn.getInputStream();
                Log.d("PATH", conn.getURL().getPath());
                String result = readStream(is);

                if (result.equals(""))
                    return "lost";
                return result;
            }
            catch (SocketException e) {
            /*
            possible causes :-
            1.  the host refused connection
            2.  the socket connection was lost
            3.  the internet connection was lost
            */
                return "lost";
            }
            catch (SocketTimeoutException e) {
            /*
            possible causes :-
            1.  the host did not respond in time
            2.  network failure, like DNS resolution
             */
                return "lost";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(newData == null)
                newData = new ArrayList<>();
            SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
            int firstId = Integer.parseInt(sharedPreferences.getString("1stAId", null));
            String firstTitle = sharedPreferences.getString("1stATitle", null);
            String firstDate = sharedPreferences.getString("1stADate", null);
            List<HashMap<String, String>> candidateData =
                    new GetAnnouncementData(result).getDataList();
            for(HashMap<String, String> candidate : candidateData)
                newData.add(candidate);
            int i = 0;
            while (!(newData.get(i).get("title").equals(firstTitle) &&
                    newData.get(i).get("date").equals(firstDate))) {
                i++;
                if(i == newData.size())
                    break;
            }
            if(i == newData.size()) { //  there wasn't any candidate data that matched the first
                String nextUrl = new GetLinks(result).getNextLink();
                if(nextUrl != null)
                    new NewAnnouncements(nextUrl, newData).execute();
            }
            /*
            there was a candidate that matched with the first
            insert all the previous ones
            */
            else if(i != 0){
                SQLiteDatabase sqLiteDatabase;
                File dbPath = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "Android/data/com.dhruv.resumemanager/db");
                sqLiteDatabase = openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                for(int j = i - 1; j > -1; j--) {
                    String title = newData.get(j).get("title");
                    String date = newData.get(j).get("date");
                    String time = newData.get(j).get("time");
                    String message = newData.get(j).get("message");
                    String insertQuery = "INSERT INTO "
                            + "announcements" + "(_id, title, date, time, message)" +
                            " VALUES('" + String.valueOf(firstId - (i - j))
                            + "', '" + title + "', '" + date +
                            "', '" + time + "', '" + message + "');";
                    sqLiteDatabase.execSQL(insertQuery);
                }
                if(sqLiteDatabase != null)
                    sqLiteDatabase.close();

                //  notify for i new announcements
                fireNotification(i);

                //  edit sharedPreferences to change the first announcement entry reference
                SharedPreferences.Editor editor = sharedPreferences.edit();
                String getFirst = "SELECT * FROM announcements LIMIT 1";
                sqLiteDatabase = openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE,
                        null);
                Cursor firstCursor = sqLiteDatabase.rawQuery(getFirst, null);
                firstCursor.moveToFirst();
                editor.putString("1stAId", firstCursor.getString(0));
                editor.putString("1stATitle", firstCursor.getString(1));
                editor.putString("1stADate", firstCursor.getString(2));
                editor.apply();

                //  broadcast to active activity, if any
                Intent broadcastNewData = new Intent("newData");
                broadcastNewData.putExtra("more", true);
                sendBroadcast(broadcastNewData);
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

    @Override
    public void onDestroy() {
        Intent announcementBgService = new Intent(getBaseContext(), AnnouncementBgService.class);
        announcementBgService.putExtra("url",
                "http://www.dce.ac.in/placement/announcements.php");
        startService(announcementBgService);
        super.onDestroy();
    }

    private void fireNotification(int i) {
        //  new intent with LoginActivity component
        Intent loginIntent = new Intent(AnnouncementBgService.this, Main.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("position", 0);   //  what to open when the activity is started

        //  The action for the notification
        PendingIntent pendingIntent = PendingIntent.getActivity(AnnouncementBgService.this, 0,
                loginIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //  notificationSound is a reference to the default notification sound of the device
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(AnnouncementBgService.this)
                .setContentText(String.valueOf(i) + " unread announcements")
                .setLargeIcon(BitmapFactory.decodeResource
                        (AnnouncementBgService.this.getResources(), R.drawable.logo))
                .setContentTitle("Resume Manager")
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setSound(notificationSound)
                .setNumber(i)
                .setLights(Color.WHITE, 400, 4000)
                .setWhen(System.currentTimeMillis());
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }
}