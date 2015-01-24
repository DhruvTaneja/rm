package com.dhruv.resumemanager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
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


public class RecruiterInfo extends ActionBarActivity {

    ArrayList<String> extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recruiter_info);

        Intent intent = getIntent();
        extras = intent.getStringArrayListExtra("displays");
        setTextViews();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(extras.get(0));
        toolbar.setTitle(extras.get(0));
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
        getMenuInflater().inflate(R.menu.menu_recruiter_info, menu);
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
    
    private void setTextViews() {
        TextView nameTView = (TextView) findViewById(R.id.recruiter_name);
        TextView categoryTView = (TextView) findViewById(R.id.recruiter_category);
        TextView dateTView = (TextView) findViewById(R.id.recruiter_date);
        TextView ctcTView = (TextView) findViewById(R.id.recruiter_ctc);
        TextView branchTView = (TextView) findViewById(R.id.recruiter_branches);
        TextView beTView = (TextView) findViewById(R.id.recruiter_be);
        TextView meTView = (TextView) findViewById(R.id.recruiter_me);
        TextView internTView = (TextView) findViewById(R.id.recruiter_intern);
        TextView mbaTView = (TextView) findViewById(R.id.recruiter_mba);
//        TextView urlTView = (TextView) findViewById(R.id.recruiter_url);

        nameTView.setText(extras.get(0));
        categoryTView.setText("Category: " + extras.get(1));
        dateTView.setText("Visit Date: " + extras.get(2));
        ctcTView.setText("CTC: " + extras.get(3));
        branchTView.setText(extras.get(4));
        beTView.setText("BE");
        if(extras.get(5).equals("No"))
            beTView.setPaintFlags(beTView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        meTView.setText("ME");
        if(extras.get(6).equals("No"))
            meTView.setPaintFlags(beTView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        internTView.setText("Intern");
        if(extras.get(7).equals("No"))
            internTView.setPaintFlags(internTView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        mbaTView.setText("MBA");
        if(extras.get(8).equals("No"))
            mbaTView.setPaintFlags(beTView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }
}
