package com.dhruv.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.dhruv.resumemanager.R;
import com.dhruv.resumemanager.RecruiterInfo;

import java.io.File;
import java.util.ArrayList;

/**
 * This is the fragment class of the
 * recruiters. It extends ListFragment.
 */
public class RecruitersFragment extends ListFragment {
    SQLiteDatabase sqLiteDatabase;
    Cursor selectStar;
    CustomAdapter customAdapter;
    Activity activity;
    BroadcastReceiver receiver;

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
                if(newText.length() == 0)
                    customAdapter.getFilter().filter("");
                return false;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        activity = getActivity();
        setHasOptionsMenu(true);
        getListView().setTextFilterEnabled(true);

        //  open the database
        File dbPath = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Android/data/com.dhruv.resumemanager/db");
        sqLiteDatabase = getActivity().openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE, null);

        //  query the database and store the result in a cursor
        String findAllQuery = "SELECT * FROM recruiters ORDER BY '_id';";
        selectStar = sqLiteDatabase.rawQuery(findAllQuery, null);
        selectStar.moveToFirst();
        sqLiteDatabase.close();

        String[] from = {
                "name",
                "category",
                "ctc",
                "branches",
                "BE",
                "ME",
                "intern",
                "MBA",
                "visitDate",
                "url"
        };

        int[] toViews = {
                R.id.recruiter_name,
                R.id.recruiter_category,
                R.id.recruiter_ctc,
                R.id.recruiter_branches,
                R.id.recruiter_be,
                R.id.recruiter_me,
                R.id.recruiter_intern,
                R.id.recruiter_mba,
                R.id.recruiter_date,
                R.id.recruiter_url
        };

        customAdapter = new CustomAdapter(getActivity(),
                R.layout.recruiter_list_view, selectStar, from, toViews, 0);
        setListAdapter(customAdapter);
        customAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                final Cursor filteredCursor;

                Log.d("LEN", String.valueOf(constraint.length()));
                filteredCursor = getFilteredCursor(constraint.toString());
                filteredCursor.moveToFirst();
                getActivity().runOnUiThread(new Runnable() {
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
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView nameTView = (TextView) view.findViewById(R.id.recruiter_name);
                TextView categoryTView = (TextView) view.findViewById(R.id.recruiter_category);
                TextView dateTView = (TextView) view.findViewById(R.id.recruiter_date);
                TextView ctcTView = (TextView) view.findViewById(R.id.recruiter_ctc);
                TextView branchTView = (TextView) view.findViewById(R.id.recruiter_branches);
                TextView beTView = (TextView) view.findViewById(R.id.recruiter_be);
                TextView meTView = (TextView) view.findViewById(R.id.recruiter_me);
                TextView internTView = (TextView) view.findViewById(R.id.recruiter_intern);
                TextView mbaTView = (TextView) view.findViewById(R.id.recruiter_mba);
                TextView urlTView = (TextView) view.findViewById(R.id.recruiter_url);

                ArrayList<String> extraList = new ArrayList<>();
                extraList.add(nameTView.getText().toString());
                extraList.add(categoryTView.getText().toString());
                extraList.add(dateTView.getText().toString());
                extraList.add(ctcTView.getText().toString());
                extraList.add(branchTView.getText().toString());
                extraList.add(beTView.getText().toString());
                extraList.add(meTView.getText().toString());
                extraList.add(internTView.getText().toString());
                extraList.add(mbaTView.getText().toString());
                extraList.add(urlTView.getText().toString());

                Intent recruiterInfo = new Intent(getActivity(), RecruiterInfo.class);
                recruiterInfo.putStringArrayListExtra("displays", extraList);
                recruiterInfo.setFlags
                        (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(recruiterInfo);
            }
        });
    }

    @Override
    public void onResume() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getBooleanExtra("more", true)) {
                    //  cancel notification
                    NotificationManager notificationManager = (NotificationManager) activity
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(2);

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
    public void onDestroyView() {
        activity.unregisterReceiver(receiver);
        super.onDestroyView();
    }

    private class CustomAdapter extends SimpleCursorAdapter implements Filterable{

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
        public int getCount() {
            return cursor.getCount();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.d("GETVIEW", "position: " + position + " " + String.valueOf(convertView));
            ViewHolder viewHolder;
            LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(convertView == null) {
                convertView = layoutInflater.inflate(R.layout.recruiter_list_view, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.category = (TextView) convertView.findViewById(R.id.recruiter_category);
                viewHolder.name = (TextView) convertView.findViewById(R.id.recruiter_name);
                viewHolder.ctc = (TextView) convertView.findViewById(R.id.recruiter_ctc);
                viewHolder.visitDate = (TextView) convertView.findViewById(R.id.recruiter_date);
                viewHolder.branches = (TextView) convertView.findViewById(R.id.recruiter_branches);
                viewHolder.be = (TextView) convertView.findViewById(R.id.recruiter_be);
                viewHolder.me = (TextView) convertView.findViewById(R.id.recruiter_me);
                viewHolder.intern = (TextView) convertView.findViewById(R.id.recruiter_intern);
                viewHolder.mba = (TextView) convertView.findViewById(R.id.recruiter_mba);

                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();
            cursor.moveToPosition(position);
            viewHolder.category.setText(cursor.getString(cursor.getColumnIndex("category")));
            viewHolder.name.setText(cursor.getString(cursor.getColumnIndex("name")));
            viewHolder.ctc.setText(cursor.getString(cursor.getColumnIndex("ctc")));
            viewHolder.visitDate.setText(cursor.getString(cursor.getColumnIndex("visitDate")));
            viewHolder.branches.setText(cursor.getString(cursor.getColumnIndex("branches")));
            viewHolder.me.setText(cursor.getString(cursor.getColumnIndex("ME")));
            viewHolder.intern.setText(cursor.getString(cursor.getColumnIndex("intern")));
            viewHolder.mba.setText(cursor.getString(cursor.getColumnIndex("MBA")));
            viewHolder.be.setText(cursor.getString(cursor.getColumnIndex("BE")));
            Log.d("GETNAME", viewHolder.name.getText().toString());

            GradientDrawable gradientDrawable = (GradientDrawable) convertView.
                    findViewById(R.id.recruiter_category).getBackground();
            String category = cursor.getString(cursor.getColumnIndex("category"));
            switch (category) {
                case "S" :
                    gradientDrawable.setColor(getResources().getColor(R.color.s));
                    break;
                case "A+" :
                    gradientDrawable.setColor(getResources().getColor(R.color.a_plus));
                    break;
                case "A" :
                    gradientDrawable.setColor(getResources().getColor(R.color.a));
                    break;
                case "B" :
                    gradientDrawable.setColor(getResources().getColor(R.color.b));
                    break;
            }
            /*
            THIS SHIT IS AWESOME
            if(viewHolder.be.getText().equals("No"))
            viewHolder.be.setPaintFlags(viewHolder.be.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            if(viewHolder.me.getText().equals("No"))
            viewHolder.me.setPaintFlags(viewHolder.me.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            if(viewHolder.intern.getText().equals("No"))
            viewHolder.intern.setPaintFlags(viewHolder.intern.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            if(viewHolder.mba.getText().equals("No"))
            viewHolder.mba.setPaintFlags(viewHolder.mba.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            */
            return convertView;
        }
    }

    public static class ViewHolder {
        public TextView category, name, ctc, visitDate, branches, be, me, intern, mba;
    }

    private Cursor getFilteredCursor(String filter) {
        String findQuery;
        if(filter.length() == 0)
            findQuery = "SELECT * FROM recruiters";
        else {
            filter = "%" + filter + "%";
            findQuery = "SELECT * FROM recruiters WHERE " +
                    "name LIKE '" + filter + "' OR branches LIKE '" + filter +
                    "' OR ctc LIKE '" + filter + "';";
        }
        File dbPath = new File(Environment.getExternalStorageDirectory()
                + File.separator + "Android/data/com.dhruv.resumemanager/db");
        sqLiteDatabase = getActivity().openOrCreateDatabase(dbPath.toString(), Context.MODE_PRIVATE, null);
        return sqLiteDatabase.rawQuery(findQuery, null);
    }
}