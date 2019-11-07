package com.alvinhkh.buseta.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import com.alvinhkh.buseta.C

import java.util.Calendar

object NotificationAlarm {

    private const val ALARM_ID = 10

    fun startAlarm(context: Context, seconds: Int) {
        val intervalMillis = seconds * 1000
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MILLISECOND, intervalMillis)

        val alarmIntent = Intent(C.ACTION.NOTIFICATION_UPDATE)
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(AlarmManager.RTC, calendar.timeInMillis, intervalMillis.toLong(), pendingIntent)
    }

    fun stopAlarm(context: Context) {
        val alarmIntent = Intent(C.ACTION.NOTIFICATION_UPDATE)
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

}