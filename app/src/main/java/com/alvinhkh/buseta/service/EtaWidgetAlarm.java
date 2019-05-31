package com.alvinhkh.buseta.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.provider.EtaWidgetProvider;

import java.util.Calendar;

public class EtaWidgetAlarm {

    private static final Integer ALARM_ID = 0;

    public static void start(@NonNull Context context, int seconds) {
        if (seconds <= 0) return;
        Integer intervalMillis = seconds * 1000;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, intervalMillis);

        Intent alarmIntent = new Intent(context, EtaWidgetProvider.class);
        alarmIntent.setAction(C.EXTRA.WIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // RTC does not wake the device up
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), intervalMillis, pendingIntent);
        }
    }

    public static void stop(@NonNull Context context) {
        Intent alarmIntent = new Intent(context, EtaWidgetProvider.class);
        alarmIntent.setAction(C.EXTRA.WIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
