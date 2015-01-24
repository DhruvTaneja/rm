package com.dhruv.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.TextView;

import com.dhruv.resumemanager.AnnouncementInfo;
import com.dhruv.resumemanager.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by dhruv on 4/1/15.
 * This is the fragment class of the
 * announcements. It extends ListFragment.
 */
public class AnnouncementsFragment extends ListFragment {

    SQLiteDatabase sqLiteDatabase;
    CustomAdapter customAdapter;
    File dbPath = new File(Environment.getExternalStorageDirectory()
            + File.separator + "Android/data/com.dhruv.resumemanager/db");
    Activity activity;
    BroadcastReceiver receiver;


    @Override
    public void onResume() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getBooleanExtra("more", true)) {
                    //  cancel notification
                    NotificationManager notificationManager = (NotificationManager) activity
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(1);

                    customAdapter.getFilter().filter("");
                }
            }
        };

        activity.registerReceiver(receiver, new IntentFilter("newData"));
        super.onResume();
    }

    @Override
    public void onDestroy() {
        try {
            activity.unregisterReceiver(receiver);
        }
        catch (IllegalArgumentException e) {
            Log.d("Receiver", "Broadcast not registered yet");
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final SearchView searchView = (SearchView) menu.findItem(R.id.searchable).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                customAdapter.getFilter().filter(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0)
                    customAdapter.getFilter().filter("");
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery("", false);
                customAdapter.getFilter().filter("");
            }
        });

        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    searchView.setQuery("", false);
                    customAdapter.getFilter().filter("");
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        activity = getActivity();
        setHasOptionsMenu(true);
        //  open the database
        Log.d("NULL", String.valueOf((activity == null)));
        sqLiteDatabase = activity.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE, null);

        //  query the database and store the result in a cursor
        String findAllQuery = "SELECT * FROM announcements ORDER BY '_id';";
        Cursor selectStar = sqLiteDatabase.rawQuery(findAllQuery, null);
        selectStar.moveToFirst();

        /*
        ************ IMPORTANT ************
        **** The data table should have "_id" column to make use of SimpleCursorAdapter ****

        The SimpleCursorAdapter takes 6 parameters
        1.  The activity where it resides
        2.  A custom layout for list view
        3.  A "from" string array telling what columns the data is coming from
        4.  An "to" integer array having the resource IDs of the views where
            the data is to be shown. The order of items in to should align
             with the order of from.
        5.  Flags
        */
        String[] from = {
                "title",
                "date",
                "time",
                "message"
        };

        int[] toViews = {
                R.id.announcement_title,
                R.id.announcement_date,
                R.id.announcement_time,
                R.id.announcement_message
        };

        customAdapter = new CustomAdapter(activity,
                R.layout.announcement_list_view, selectStar, from, toViews, TRIM_MEMORY_BACKGROUND);
        setListAdapter(customAdapter);
        customAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                final Cursor filteredCursor;

                Log.d("LEN", String.valueOf(constraint.length()));
                filteredCursor = getFilteredCursor(constraint.toString());
                filteredCursor.moveToFirst();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (filteredCursor.getCount() > 0)
                            customAdapter.notifyDataSetChanged();
                        else
                            customAdapter.notifyDataSetInvalidated();
                    }
                });
                filteredCursor.moveToFirst();
                customAdapter.cursor = filteredCursor;
                return filteredCursor;
            }
        });

        ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent announcementInfo = new Intent(activity, AnnouncementInfo.class);
                TextView titleTV = (TextView) view.findViewById(R.id.announcement_title);
                TextView dateTV = (TextView) view.findViewById(R.id.announcement_date);
                TextView timeTV = (TextView) view.findViewById(R.id.announcement_time);
                TextView messageTV = (TextView) view.findViewById(R.id.announcement_message);

                ArrayList<String> extras = new ArrayList<>();
                extras.add(titleTV.getText().toString());
                extras.add(dateTV.getText().toString());
                extras.add(timeTV.getText().toString());
                extras.add(messageTV.getText().toString());

                announcementInfo.putStringArrayListExtra("displays", extras);
                announcementInfo.setFlags
                        (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(announcementInfo);
            }
        });

        super.onActivityCreated(savedInstanceState);
    }

    private class CustomAdapter extends SimpleCursorAdapter {

        Context context;
        int layout, flags;
        Cursor cursor;
        String[] from;
        int[] to;
        LayoutInflater inflater;

        public CustomAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            this.context = context;
            this.layout = layout;
            this.flags = flags;
            this.cursor = c;
            this.from = from;
            this.to = to;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) activity
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.announcement_list_view, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.title = (TextView) convertView.findViewById(R.id.announcement_title);
                viewHolder.date = (TextView) convertView.findViewById(R.id.announcement_date);
                viewHolder.time = (TextView) convertView.findViewById(R.id.announcement_time);
                viewHolder.message = (TextView) convertView.findViewById(R.id.announcement_message);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();
            try {
                cursor.moveToPosition(position);    //  remove it and you will see duplicate data
                viewHolder.title.setText(cursor.getString(cursor.getColumnIndex("title")));
                viewHolder.date.setText(cursor.getString(cursor.getColumnIndex("date")));
                viewHolder.time.setText(cursor.getString(cursor.getColumnIndex("time")));
                viewHolder.message.setText(cursor.getString(cursor.getColumnIndex("message")));
            }
            catch (CursorIndexOutOfBoundsException exc) {
                Log.d("CURSOR", String.valueOf(cursor.getCount()));
            }
            return convertView;
        }
    }

    public static class ViewHolder {
        public TextView title, date, time, message;
    }

    private Cursor getFilteredCursor(String filter) {
        String findQuery;
        if(filter.length() == 0)
            findQuery = "SELECT * FROM announcements";
        else {
            filter = "%" + filter + "%";
            findQuery = "SELECT * FROM announcements WHERE " +
                    "title LIKE '" + filter + "' OR message LIKE '" + filter + "';";
        }
        File dbPath = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Android/data/com.dhruv.resumemanager/db");
        sqLiteDatabase = activity.openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE, null);
        return sqLiteDatabase.rawQuery(findQuery, null);
    }
}