package com.alvinhkh.buseta.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.RemoteViews;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.EtaWidgetAlarm;
import com.alvinhkh.buseta.service.EtaWidgetService;
import com.alvinhkh.buseta.ui.search.SearchActivity;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.alvinhkh.buseta.view.MainActivity;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class EtaWidgetProvider extends AppWidgetProvider {

    private boolean isLargeLayout = true;

    public EtaWidgetProvider() {
    }

    @Override
    public void onEnabled(Context context) {
        EtaWidgetAlarm.start(context, 1);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, EtaWidgetProvider.class);
        for (int appWidgetId : mgr.getAppWidgetIds(cn)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                updateWidgetSize(mgr.getAppWidgetOptions(appWidgetId));
            }
            RemoteViews layout = buildLayout(context, appWidgetId, isLargeLayout);
            mgr.updateAppWidget(appWidgetId, layout);
        }
    }

    @Override
    public void onDisabled(Context context) {
        EtaWidgetAlarm.stop(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Integer appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        // Timber.d("%s %s", action, appWidgetId);
        if (action != null) {
            switch (action) {
                // case AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED:
                case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                case C.ACTION.WIDGET_UPDATE:
                    onRefresh(context, appWidgetId);
                    break;
            }
        }
        super.onReceive(context, intent);
    }

    private void onRefresh(@NonNull Context context, int widgetId) {
        Timber.d("refresh widget: %s", widgetId);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.list_view);
        if (ConnectivityUtil.isConnected(context)) {
            List<FollowStop> followStops = FollowStopUtil.toList(context);
            ArrayList<RouteStop> routeStops = new ArrayList<>();
            for (FollowStop stop: followStops) {
                routeStops.add(RouteStopUtil.fromFollowStop(stop));
            }
            try {
                Intent intent = new Intent(context, EtaService.class);
                intent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId);
                intent.putParcelableArrayListExtra(C.EXTRA.STOP_LIST, routeStops);
                context.startService(intent);
            } catch (IllegalStateException ignored) {}
        }
    }

    private RemoteViews buildLayout(final Context context, final int appWidgetId, boolean largeLayout) {
        // Specify the service to provide data for the collection widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        Intent intent = new Intent(context, EtaWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
        remoteViews.setRemoteAdapter(R.id.list_view, intent);
        remoteViews.setEmptyView(R.id.list_view, R.id.empty_view);
        // click open app intent
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0,
                openAppIntent, 0);
        // update intent
        Intent refreshIntent = new Intent(context, EtaWidgetProvider.class);
        refreshIntent.setAction(C.ACTION.WIDGET_UPDATE);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0,
                refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent activityIntent = new Intent(context, SearchActivity.class);
        activityIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // Timber.d("%s", Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        // activityIntent.setData();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.list_view, updatePendingIntent);

        if (largeLayout) {
            remoteViews.setViewVisibility(R.id.header, View.VISIBLE);
            remoteViews.setOnClickPendingIntent(R.id.header_text, openAppPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.refresh_button, updatePendingIntent);
        } else {
            remoteViews.setViewVisibility(R.id.header, View.GONE);
        }
        return remoteViews;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        updateWidgetSize(newOptions);
        RemoteViews layout = buildLayout(context, appWidgetId, isLargeLayout);
        appWidgetManager.updateAppWidget(appWidgetId, layout);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateWidgetSize(Bundle bundle) {
        if (null == bundle) return;
        int minWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        isLargeLayout = minHeight >= 80;
    }
}