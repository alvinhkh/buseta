package com.alvinhkh.buseta.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.appwidget.FollowWidgetProvider

import java.util.Calendar

object EtaWidgetAlarm {

    private const val ALARM_ID = 0

    fun startAlarm(context: Context, seconds: Int, widgetId: Int, groupId: String) {
        if (seconds <= 0) return
        val intervalMillis = seconds * 1000
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MILLISECOND, intervalMillis)

        val alarmIntent = Intent(context, FollowWidgetProvider::class.java)
        alarmIntent.action = C.ACTION.WIDGET_UPDATE
        alarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        alarmIntent.putExtra(C.EXTRA.GROUP_ID, groupId)
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // RTC does not wake the device up
        alarmManager.setRepeating(AlarmManager.RTC, calendar.timeInMillis, intervalMillis.toLong(), pendingIntent)
    }

    fun stopAlarm(context: Context, widgetId: Int, groupId: String) {
        val alarmIntent = Intent(context, FollowWidgetProvider::class.java)
        alarmIntent.action = C.ACTION.WIDGET_UPDATE
        alarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        alarmIntent.putExtra(C.EXTRA.GROUP_ID, groupId)
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
