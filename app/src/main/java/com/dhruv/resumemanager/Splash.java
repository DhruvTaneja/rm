package com.dhruv.resumemanager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class Splash extends Activity {

    public boolean DEV_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  if there is no storage available
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d("EXT", "No media mounted");
        }
        else {  //  create the directories
            File directory = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "Android/data/com.dhruv.resumemanager");
            boolean created = directory.mkdirs();
            Log.d("DIR", String.valueOf(created));
        }

        if(DEV_MODE) {
            StrictMode.enableDefaults();
        }

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.splash);

        //  TODO lot of gotchas here, do check these!
        SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("login", false)) {
            if (sharedPreferences.getBoolean("announcements", false)) {
                if (sharedPreferences.getBoolean("recruiters", false)) {
                    /*
                    if the recruiters were done, but
                    dbRunning is still true, it means that
                    CTCs were being loaded. In that case
                    drop the recruiters table and the
                    appropriate sharedPreferences
                    */
                    if (sharedPreferences.getBoolean("dbRunning", false)) {
                        File dbPath = new File(Environment.getExternalStorageDirectory()
                                + File.separator + "Android/data/com.dhruv.resumemanager/db");
                        SQLiteDatabase database = Splash.this.openOrCreateDatabase(dbPath.toString(), MODE_PRIVATE, null);
                        String removeRecruiters = "DROP TABLE IF EXISTS db.recruiters";
                        database.execSQL(removeRecruiters);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("recruiter", false);
                        editor.apply();
                        LaunchActivity(CreatingDB.class);
                        finish();
                        return;
                    }
                    else {
                        LaunchActivity(Main.class);
                        finish();
                        return;
                    }
                }
                else {
                    LaunchActivity(CreatingDB.class);
                    finish();
                    return;
                }
            }
            else    //  TODO should I remove the database here?
//                    cleanPreferences(sharedPreferences);
                LaunchActivity(CreatingDB.class);
        }

        //  animation thread
        Thread animationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Animation textUp = AnimationUtils.loadAnimation
                        (getApplicationContext(), R.anim.text_up);
                Animation logoAppear = AnimationUtils.loadAnimation
                        (getApplicationContext(), R.anim.logo_appear);
                TextView textRm = (TextView) findViewById(R.id.text_rm);
                ImageView logoImg = (ImageView) findViewById(R.id.splash_logo);
                textRm.setAnimation(textUp);
                logoImg.setAnimation(logoAppear);
            }
        });
        animationThread.start();

        //  the splash pausing thread
        Thread splashPause = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException e) {
                    finish();
                }
                finally {
                    launchActivity();
                    finish();
                }
            }
        });
        splashPause.start();
    }

    //  check if the login sharedPreference is set to true, or if it exists
    private boolean LoggedIn() {
        SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
        return sharedPreferences.getBoolean("login", false);
    }

    //  launch activities based on login state
    private void launchActivity() {
        Intent intent;
        if(LoggedIn()) {
            intent = new Intent(Splash.this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else {
            intent = new Intent(Splash.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        finish();
        super.onPause();
    }

    //  method to clear all the sharedPreferences to start afresh
    private void cleanPreferences(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("login")
                .remove("username")
                .remove("password")
                .remove("name")
                .remove("urlParams")
                .remove("dbUpdate")
                .remove("announcements")
                .remove("recruiters")
                .apply();
    }

    private void LaunchActivity(Class<?> cls) {
        Intent intent = new Intent(Splash.this, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}