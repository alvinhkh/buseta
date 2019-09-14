package com.alvinhkh.buseta.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.service.EtaService
import com.alvinhkh.buseta.service.EtaWidgetAlarm
import com.alvinhkh.buseta.ui.MainActivity

class FollowWidgetProvider : AppWidgetProvider() {

    companion object {

        fun updateWidgets(context: Context) {
            val intent = Intent(context, FollowWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetIDs = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, FollowWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            context.sendBroadcast(intent)
        }

        fun getRemoteViews(context: Context, widgetId: Int): RemoteViews {
            val widgetPreferences = WidgetPreferences(context)
            val followGroupId = widgetPreferences.getFollowGroupId(widgetId) ?: ""
            val withHeader = widgetPreferences.getWithHeader(widgetId)
            val followDatabase = FollowDatabase.getInstance(context)
            val followGroup = followDatabase?.followGroupDao()?.get(followGroupId)
            var widgetName = followGroup?.name?: context.getString(R.string.app_name)
            if (widgetName.isBlank()) {
                widgetName = context.getString(R.string.uncategorised)
            }

            // Specify the service to provide data for the collection widget.  Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            val intent = Intent(context, FollowWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_eta)
            remoteViews.setInt(R.id.header, "setBackgroundColor", if (!followGroup?.colour.isNullOrBlank())
                Color.parseColor(followGroup?.colour)
            else
                ContextCompat.getColor(context, R.color.colorPrimary))
            remoteViews.setTextViewText(R.id.header_text, widgetName)
            remoteViews.setEmptyView(R.id.list_view, R.id.empty_view)
            remoteViews.setRemoteAdapter(R.id.list_view, intent)
            // click open app intent
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(context, 0,
                    openAppIntent, 0)
            // update intent
            val refreshIntent = Intent(context, FollowWidgetProvider::class.java)
            refreshIntent.action = C.ACTION.WIDGET_UPDATE
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            refreshIntent.putExtra(C.EXTRA.GROUP_ID, followGroupId)
            val updatePendingIntent = PendingIntent.getBroadcast(context, widgetId,
                    refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val activityIntent = Intent(context, SearchActivity::class.java)
            activityIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            val pendingIntent = PendingIntent.getActivity(context, widgetId,
                    activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            remoteViews.setPendingIntentTemplate(R.id.list_view, updatePendingIntent)

            if (withHeader) {
                remoteViews.setViewVisibility(R.id.header, View.VISIBLE)
                remoteViews.setOnClickPendingIntent(R.id.header_text, openAppPendingIntent)
                remoteViews.setOnClickPendingIntent(R.id.refresh_button, updatePendingIntent)
            } else {
                remoteViews.setViewVisibility(R.id.header, View.GONE)
            }
            return remoteViews
        }

        fun updateWidgetUI(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            appWidgetManager.updateAppWidget(widgetId, getRemoteViews(context, widgetId))
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (widgetId: Int in appWidgetIds) {
            updateWidgetUI(context, appWidgetManager, widgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetPreferences = WidgetPreferences(context)
        for (appWidgetId: Int in appWidgetIds) {
            widgetPreferences.removeWidget(appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, FollowWidgetProvider::class.java)
        val widgetPreferences = WidgetPreferences(context)
        for (widgetId in appWidgetManager.getAppWidgetIds(componentName)) {
            appWidgetManager.updateAppWidget(widgetId, getRemoteViews(context, widgetId))
            EtaWidgetAlarm.startAlarm(context, 60, widgetId, widgetPreferences.getFollowGroupId(widgetId)?:"")
        }
    }

    override fun onDisabled(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, FollowWidgetProvider::class.java)
        val widgetPreferences = WidgetPreferences(context)
        for (widgetId in appWidgetManager.getAppWidgetIds(componentName)) {
            appWidgetManager.updateAppWidget(widgetId, getRemoteViews(context, widgetId))
            EtaWidgetAlarm.stopAlarm(context, widgetId, widgetPreferences.getFollowGroupId(widgetId)?:"")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action != null) {
//            val widgetIds = intent.getIntegerArrayListExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId <= AppWidgetManager.INVALID_APPWIDGET_ID) return
            val followGroupId = intent.getStringExtra(C.EXTRA.GROUP_ID)
            when (action) {
                AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                    AppWidgetManager.getInstance(context)
                            .notifyAppWidgetViewDataChanged(widgetId, R.id.list_view)
                }
                C.ACTION.WIDGET_UPDATE -> {
                    try {
                        val i = Intent(context, EtaService::class.java)
                        i.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId)
                        i.putExtra(C.EXTRA.FOLLOW, true)
                        i.putExtra(C.EXTRA.GROUP_ID, followGroupId)
                        context.startService(i)
                    } catch (ignored: IllegalStateException) {
                    }
                }
            }
        }
    }
}