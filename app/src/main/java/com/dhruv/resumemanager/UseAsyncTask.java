package com.dhruv.resumemanager;

import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
/**
 * Created by dhruv on 31/12/14.
 * This class is solely created for
 * fetching the response text of the urls.
 * While using this class for getting
 * response text, keep in mind to use the
 * .get() method after the .execute() method
 * If used on main UI thread, the execute() method
 * will freeze it and may throw errors.
 */
public class UseAsyncTask extends AsyncTask<String, Void, String> {

    private String url;

    public UseAsyncTask(String url) {
        this.url = url;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            return getAsyncResults(url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        //  do nothing
    }

    public String getAsyncResults(String pageUrl) throws IOException {
        final URL url;
        try {
            url = new URL(pageUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(100000);
        conn.setReadTimeout(150000);
        conn.setDoOutput(true);

        InputStream is = conn.getInputStream();
        String result = readStream(is);

        if(result.equals(""))
            return "No response";
        return result;
    }

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
