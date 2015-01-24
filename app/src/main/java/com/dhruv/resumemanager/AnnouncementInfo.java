package com.dhruv.resumemanager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;


public class AnnouncementInfo extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.announcement_info);

        Intent intent = getIntent();
        ArrayList<String> extras = intent.getStringArrayListExtra("displays");
        TextView titleTV = (TextView) findViewById(R.id.announcement_title);
        TextView dateTV = (TextView) findViewById(R.id.announcement_date);
        TextView timeTV = (TextView) findViewById(R.id.announcement_time);
        TextView messageTV = (TextView) findViewById(R.id.announcement_message);

        titleTV.setText(extras.get(0));
        dateTV.setText(extras.get(1));
        timeTV.setText(extras.get(2));
        messageTV.setText(extras.get(3));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(extras.get(0));
        toolbar.setTitle(extras.get(0));
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.parseColor("#1976D2"));
        }
        if(Build.VERSION.SDK_INT >= 16)
            toolbar.setBackground(new ColorDrawable(Color.parseColor("#2196F3")));
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_announcement_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
