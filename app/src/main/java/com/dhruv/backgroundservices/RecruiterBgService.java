package com.dhruv.backgroundservices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

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

import getdata.GetLinks;
import getdata.GetRecruiterData;

/**
 * Created by dhruv on 19/1/15.
 * This class takes the responsibility to
 * start looking for new data as soon as
 * the system starts.
 */
public class RecruiterBgService extends Service {

    String urlParams;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences preferences = getSharedPreferences("resume", MODE_PRIVATE);
        String recruiterUrl = intent.getStringExtra("url");
        if(preferences.getBoolean("database", false)) { //  sharedPref for if DB has been created
            //  get other sharedPrefs for POST request
            urlParams = preferences.getString("urlParams", null);
            Log.d("START", "Service started");
            NewRecruiters newRecruiters = new NewRecruiters(recruiterUrl, null);
            newRecruiters.execute();

            return Service.START_REDELIVER_INTENT;
        }
        return Service.START_NOT_STICKY;    //  don't make it sticky if DB not created
    }

    private class NewRecruiters extends AsyncTask<String, Void, String> {

        String nextUrl;
        List<HashMap<String, String>> newData;

        public NewRecruiters(String nextUrl, List<HashMap<String, String>> newData) {
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
            int firstId = Integer.parseInt(sharedPreferences.getString("1stRId", null));
            String firstUrl = sharedPreferences.getString("1stRUrl", null);
            List<HashMap<String, String>> candidateData =
                    new GetRecruiterData(result).getDataList();
            for(HashMap<String, String> candidate : candidateData)
                newData.add(candidate);
            int i = 0;
            while (!(newData.get(i).get("url").equals(firstUrl))) {
                i++;
                if(i == newData.size())
                    break;
            }
            if(i == newData.size()) { //  there wasn't any candidate data that matched the first
                String nextUrl = new GetLinks(result).getNextLink();
                if(nextUrl != null)
                    new NewRecruiters(nextUrl, newData).execute();
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
                    String recruiterUrl = newData.get(j).get("url");
                    String category = newData.get(j).get("category");
                    String accepting = newData.get(j).get("accepting");
                    String name = newData.get(j).get("name");
                    String branches = newData.get(j).get("branches");
                    String BE = newData.get(j).get("BE");
                    String ME = newData.get(j).get("ME");
                    String intern = newData.get(j).get("Intern");
                    String MBA = newData.get(j).get("MBA");
                    String visitDate = newData.get(j).get("visitDate");
                    String insertQuery = "INSERT INTO recruiters (_id, url, category, accepting, name "
                            + ", branches, BE, ME, intern, MBA, visitDate) VALUES('" +
                            String.valueOf(firstId - (i - j)) + "', '" + recruiterUrl + "', '" +
                            category + "', '" + accepting + "', '" + name + "', '" + branches + "', '" +
                            BE + "', '" + ME + "', '" + intern + "', '" + MBA + "', '" + visitDate + "');";
                    sqLiteDatabase.execSQL(insertQuery);
                }

                if(sqLiteDatabase != null)
                    sqLiteDatabase.close();

                ArrayList<String> urls = new ArrayList<>();
                for(int j = 0; j < i; j++)
                    urls.add(newData.get(j).get("url"));

                CTCBgService ctcBgService = new CTCBgService(urls, 0, getApplicationContext());
                ctcBgService.execute();
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
        Intent RecruiterBgService = new Intent(getBaseContext(), RecruiterBgService.class);
        RecruiterBgService.putExtra("url",
                "http://www.dce.ac.in/placement/recruiter_list.php");
        startService(RecruiterBgService);
        super.onDestroy();
    }
}