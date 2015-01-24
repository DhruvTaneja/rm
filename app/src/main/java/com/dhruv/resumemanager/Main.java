package com.dhruv.resumemanager;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.dhruv.fragments.AnnouncementsFragment;
import com.dhruv.fragments.RecruitersFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import adapter.NavDrawerListAdapter;

public class Main extends ActionBarActivity {

    //  string action to register the broadcast that starts the background services
    public static final String START_SERVICE = "com.dhruv.resumemanager.custom.intent.START_SERVICE";

    ActionBarDrawerToggle drawerToggle;
    DrawerLayout drawer;
    ListView drawerList;
    TypedArray navDrawerIcons;
    String[] navDrawerTitles;
    Toolbar toolbar;
    TextView nameTV;
    ArrayList<NavDrawerItem> navDrawerItems;
    SearchView searchView;

    /*
    current position stores the value
    of the item selected in the drawer
    it is set to -1 so that it can be compared
    to position = 0 when the app is created
    */
    private int currentPosition = -1;

    private void displayView(int position) {
        Fragment fragment = null;
        /*
        if newly selected item in drawer
        is not the same as the previous one
        */
        if(currentPosition != position) {
            switch (position) {
                case 0:
                    fragment = new AnnouncementsFragment();
                    break;
                case 1:
                    fragment = new RecruitersFragment();
                    break;
                case 2:
                    Intent settingsActivity = new Intent(Main.this, Settings.class);
                    startActivity(settingsActivity);
                    drawerList.setItemChecked(currentPosition, true);
                    return;
                case 3:
                    new AlertDialog.Builder(Main.this)
                            .setTitle("Logout?")
                            .setMessage("Are you sure you want to logout?")
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    drawer.closeDrawer(Gravity.START);
                                    SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
                                    cleanPreferences(sharedPreferences);
                                    boolean deleted = new File(Environment.getExternalStorageDirectory()
                                            + File.separator + "Android/data/com.dhruv.resumemanager/db").delete();
                                    Log.d("DELETION", String.valueOf(deleted));
                                    Intent logoutIntent = new Intent(Main.this, Splash.class);
                                    startActivity(logoutIntent);
                                    finish();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    drawerList.setItemChecked(currentPosition, true);
                    return;
            }
        }

        //  a simple implementation of showing selected fragment
        FragmentManager manager = getFragmentManager();
        if(fragment != null) {
            manager.beginTransaction()
                    .replace(R.id.main_frame, fragment, String.valueOf(position)).commit();
            currentPosition = position;
        }
        /*
        fragment will be null if the position
        will be same as the currentPosition
        Just move the list to top in this case
        */
        else {
            ListFragment selectedFragment = (ListFragment) manager.
                    findFragmentByTag(String.valueOf(position));
            selectedFragment.getListView().setSelectionAfterHeaderView();
            manager.beginTransaction().show(selectedFragment).commit();
        }
    }

    //  a class that implements OnItemClickListener
    public class NavDrawerClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            displayView(position);
            drawer.closeDrawer(Gravity.START);
            NavDrawerItem checkedItem = (NavDrawerItem) drawerList.getAdapter().
                    getItem(drawerList.getCheckedItemPosition());
            getSupportActionBar().setTitle(checkedItem.getTitle());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //  setting views and resources
        setViewsAndResources();
        //  styling the toolbar
        styleToolbar();

        //  setting the click handler of the drawer list items
        drawerList.setOnItemClickListener(new NavDrawerClickListener());

        NavDrawerListAdapter adapter = new NavDrawerListAdapter(getApplicationContext(),
                navDrawerItems);
        drawerList.setAdapter(adapter);
        drawerList.setItemChecked(0, true);

        drawerToggle = new ActionBarDrawerToggle(Main.this, drawer, toolbar,
                R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerClosed(View drawerView) {
                drawerToggle.syncState();
                NavDrawerItem checkedItem = (NavDrawerItem) drawerList.getAdapter().
                        getItem(drawerList.getCheckedItemPosition());
                getSupportActionBar().setTitle(checkedItem.getTitle());
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                searchView.setVisibility(View.GONE);
                searchView.setEnabled(false);
                drawerToggle.syncState();
                getSupportActionBar().setTitle("Resume Manager");

                invalidateOptionsMenu();
            }
        };

        drawer.setDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(true);

        int pos = getIntent().getIntExtra("position", 0);
        displayView(pos); //  display the selected fragment onCreate, by default - announcement

        Intent bgServiceIntent = new Intent(START_SERVICE);
        sendBroadcast(bgServiceIntent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isOpen = drawer.isDrawerOpen(Gravity.START);
        menu.findItem(R.id.searchable).setVisible(!isOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(Gravity.START))
            drawer.closeDrawer(Gravity.START);
        else
            super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        searchView = (SearchView) menu.findItem(R.id.searchable).getActionView();
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            List<SearchableInfo> searchables = searchManager.getSearchablesInGlobalSearch();

            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            for (SearchableInfo inf : searchables) {
                if (inf.getSuggestAuthority() != null
                        && inf.getSuggestAuthority().startsWith("applications")) {
                    info = inf;
                }
            }
            searchView.setSearchableInfo(info);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Log.d("id", String.valueOf(id));

        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }

    private void cleanPreferences(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("login")
                .remove("username")
                .remove("password")
                .remove("name")
                .remove("urlParams")
                .remove("dbUpdate")
                .remove("database")
                .remove("db")
                .remove("announcements")
                .remove("recruiters")
                .remove("1stAId")
                .remove("1stATitle")
                .remove("1stADate")
                .remove("1stRId")
                .remove("1stRUrl")
                .apply();
    }

    private void setViewsAndResources() {
        navDrawerTitles = getResources()
                .getStringArray(R.array.drawer_list);  //  the array of strings to be used in DrawerList
        navDrawerIcons = getResources().obtainTypedArray(R.array.drawer_icons);  //  the array of icons
        drawer = (DrawerLayout) findViewById(R.id.main_drawer);   //  v4.widget.DrawerLayout
        drawerList = (ListView) findViewById(R.id.left_drawer); //  the drawer
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        nameTV = (TextView) findViewById(R.id.name_drawer);

        //  ste the 3 items for the drawer
        navDrawerItems = new ArrayList<>();
        navDrawerItems.add(new NavDrawerItem(navDrawerTitles[0], navDrawerIcons.getResourceId(0, -1)));
        navDrawerItems.add(new NavDrawerItem(navDrawerTitles[1], navDrawerIcons.getResourceId(1, -1)));
        navDrawerItems.add(new NavDrawerItem(navDrawerTitles[2], navDrawerIcons.getResourceId(2, -1)));
        navDrawerItems.add(new NavDrawerItem(navDrawerTitles[3], navDrawerIcons.getResourceId(3, -1)));

        navDrawerIcons.recycle();   //  reuses the array, no need for creating again and again

        //  sharedPreferences are used to set the name of the student
        SharedPreferences sharedPreferences = getSharedPreferences("resume", MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "NoName");
        nameTV.setText(name);
    }

    private void styleToolbar() {
        setSupportActionBar(toolbar);

        if(Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.parseColor("#1976D2"));
        }
        if(Build.VERSION.SDK_INT >= 16)
            toolbar.setBackground(new ColorDrawable(Color.parseColor("#2196F3")));
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
}