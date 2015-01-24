package com.dhruv.resumemanager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class Settings extends ActionBarActivity {

    Toolbar toolbar;
    ListView settingsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        settingsList = (ListView) findViewById(R.id.settings_list);
        styleToolbar();

        String[] settings = getResources().getStringArray(R.array.settings);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, settings);
        settingsList.setAdapter(adapter);
        settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0) {
                    Intent changeNameIntent = new Intent(Settings.this, ChangeName.class);
                    startActivity(changeNameIntent);
                }
            }
        });

    }

    private void styleToolbar() {
        setSupportActionBar(toolbar);

        if(Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.parseColor("#1976D2"));
        }
        if(Build.VERSION.SDK_INT >= 16)
            toolbar.setBackground(new ColorDrawable(Color.parseColor("#2196F3")));
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));
    }
}
