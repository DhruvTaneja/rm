package com.dhruv.backgroundservices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.Time;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by dhruv on 15/1/15.
 * This class listens for the BOOT_COMPLETED
 * broadcast and starts the OnSystemStartService
 */
public class OnSystemStartReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals("com.dhruv.resumemanager.custom.intent.START_SERVICE")) {
            SharedPreferences preferences = context.getSharedPreferences("resume", Context.MODE_PRIVATE);
            int interval = preferences.getInt("interval", 20);
            Time time = new Time();
            Time timeNow = new Time();
            timeNow.setToNow();
            time.set(0, interval, timeNow.hour, timeNow.monthDay, timeNow.month, timeNow.year);
            while (timeNow.minute > time.minute) {
                timeNow.set(timeNow.toMillis(isInDst(TimeZone.getDefault(),
                        new Date(System.currentTimeMillis()))) + interval * 60 * 1000);
            }
            String announcementUrl = "http://www.dce.ac.in/placement/announcements.php";
            String recruiterUrl = "http://www.dce.ac.in/placement/recruiter_list.php";
            Intent announcementBgIntent = new Intent(context, AnnouncementBgService.class);
            announcementBgIntent.putExtra("url", announcementUrl);
            Intent recruiterBgIntent = new Intent(context, RecruiterBgService.class);
            recruiterBgIntent.putExtra("url", recruiterUrl);

            PendingIntent annPendingIntent = PendingIntent.getService(context, 0,
                    announcementBgIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent recPendingIntent = PendingIntent.getService(context, 1,
                    recruiterBgIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                    interval * 60 * 1000, recPendingIntent);
            AlarmManager alarmManager1 = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager1.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                    interval * 60 * 1000, annPendingIntent);
        }
    }

    public static boolean isInDst(TimeZone tz, Date time)
    {
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTime(time);

        return calendar.get(Calendar.DST_OFFSET) != 0;
    }
}