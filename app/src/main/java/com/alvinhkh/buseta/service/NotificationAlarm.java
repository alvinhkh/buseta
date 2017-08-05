package com.alvinhkh.buseta.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.alvinhkh.buseta.C;

import java.util.Calendar;

public class NotificationAlarm {

    private final int ALARM_ID = 10;

    private Context context;

    public NotificationAlarm(Context context) {
        this.context = context;
    }

    public void startAlarm(int seconds) {
        int interval_millis = seconds * 1000;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, interval_millis);

        Intent alarmIntent = new Intent(C.ACTION.NOTIFICATION_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // RTC does not wake the device up
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), interval_millis, pendingIntent);
    }

    public void stopAlarm() {
        Intent alarmIntent = new Intent(C.ACTION.NOTIFICATION_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

}