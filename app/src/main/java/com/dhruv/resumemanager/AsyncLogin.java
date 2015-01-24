package com.dhruv.resumemanager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by dhruv on 31/12/14.
 * This class is dedicated to perform the login task.
 * The constructor takes the username, password, the context
 * and the calling activity. The preExecute creates and fires
 * a progress dialog which is dismissed in the postExecute.
 */
public class AsyncLogin extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... params) {
        try {
            return makePostRequest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    String username, password, name, urlParams;

    Activity activity;
    ProgressDialog dialog;
    AsyncLogin(String username, String password, String name, Context context, Activity activity) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.context = context;
        this.activity = activity;
        dialog = new ProgressDialog(activity);
    }

    Context context;
    @Override
    protected void onPreExecute() {
        dialog.setMessage("Logging in...");
        dialog.show();
    }

    private String makePostRequest() throws NoSuchAlgorithmException, IOException {

        //  enabling cookie for persistent login
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        String hashedPassword = new Algorithms(password).getMD5();
        String loginUrl = "http://www.dce.ac.in/placement/student_login.php";
        urlParams = "txtUsername=" + username + "&txtPassword=" + hashedPassword+ "&Submit=Login";
        URL url = new URL(loginUrl);

        try {
            InputStream is = null;
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

            //  Writing the urlParams to the output stream
            //  to the POST request
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();

            /*
            don't ever skip this statement
            won't get you any response text
            */
            is = conn.getInputStream();

            String postUrl = conn.getURL().getPath();
            Log.d("POST Url", conn.getURL().toString());

            CookieStore cookieStore = cookieManager.getCookieStore();
            List<HttpCookie> cookies = cookieStore
                    .get(new URI("http://www.dce.ac.in/placement/student_login.php"));
            String loginCookie = cookies.get(0).getValue();
            loginCookie += "PHPSESSID=" + loginCookie;

            SharedPreferences preferences = context.getSharedPreferences("resume",
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("cookie", loginCookie);
            editor.apply();

            if(postUrl.equals("/placement/student_login.php"))
                return "fail";
            if(postUrl.equals("/placement/announcements.php"))
                return "success";
        }
        catch (SocketTimeoutException e) {
            /*
            this exception would occur when the connection
            is failed because of connection timeout, i.e.,
            when the connection is taking too long
            */
            return "timeout";
        } catch (URISyntaxException e) {
            Toast.makeText(context, "Cookie Problem", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(dialog.isShowing())
            dialog.dismiss();
        if(result.equals("timeout")) {
            Toast.makeText(activity, "Login taking too long. Please try some other time.", Toast.LENGTH_LONG)
            .show();
        }
        if(result.equals("success")) {
            Toast.makeText(activity, "Login Successful", Toast.LENGTH_LONG)
                    .show();
            SharedPreferences sharedPreferences = activity
                    .getSharedPreferences("resume", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("login", true);
            editor.putString("username", username);
            editor.putString("password", password);
            editor.putString("urlParams", urlParams);
            editor.putString("name", name);
            editor.apply();
            Intent intent = new Intent(activity, CreatingDB.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            activity.finish();
            /*
            edit shared preferences
            launch activity
            this may take a few minutes
            you may come back later
            */
        }
        if(result.equals("fail")) {
            Toast.makeText(activity, "Username and password are not matching.", Toast.LENGTH_LONG)
                    .show();
        }
    }
}
