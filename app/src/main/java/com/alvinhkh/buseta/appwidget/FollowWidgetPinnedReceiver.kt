package com.alvinhkh.buseta.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

class FollowWidgetPinnedReceiver : BroadcastReceiver() {
    companion object {
        const val WIDGET_WITH_HEADER = "WIDGET_WITH_HEADER"
        const val WIDGET_FOLLOW_GROUP_ID = "WIDGET_FOLLOW_GROUP_ID"
        const val WIDGET_ROWS = "WIDGET_ROWS"
        const val BROADCAST_ID = 100000

        fun getPendingIntent(context: Context, widgetId: Int): PendingIntent {
            val widgetPreferences = WidgetPreferences(context)
            val withHeader = widgetPreferences.getWithHeader(widgetId)
            val followGroupId = widgetPreferences.getFollowGroupId(widgetId)
            val noOfRows = widgetPreferences.getItemRowNum(widgetId)
            val callbackIntent = Intent(context, FollowWidgetPinnedReceiver::class.java)
            val bundle = Bundle()
            bundle.putBoolean(WIDGET_WITH_HEADER, withHeader)
            bundle.putString(WIDGET_FOLLOW_GROUP_ID, followGroupId)
            bundle.putInt(WIDGET_ROWS, noOfRows)
            callbackIntent.putExtras(bundle)
            return PendingIntent.getBroadcast(
                    context, BROADCAST_ID + widgetId, callbackIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (widgetId == -1) {
            return
        }

        val withHeader = intent.getBooleanExtra(WIDGET_WITH_HEADER, true)
        val followGroupId = intent.getStringExtra(WIDGET_FOLLOW_GROUP_ID)?:""
        val noOfRows = intent.getIntExtra(WIDGET_ROWS, 2)

        val widgetPreferences = WidgetPreferences(context)
        widgetPreferences.setWidgetValues(widgetId, withHeader, followGroupId, noOfRows)
        FollowWidgetProvider.updateWidgets(context)
    }
}