package com.alvinhkh.buseta.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.EtaWidgetAlarm;
import com.alvinhkh.buseta.service.EtaWidgetService;
import com.alvinhkh.buseta.search.ui.SearchActivity;
import com.alvinhkh.buseta.service.ForeverStartLifecycleOwner;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.ui.MainActivity;

import java.util.List;
import java.util.concurrent.Callable;

public class EtaWidgetProvider extends AppWidgetProvider {

    private boolean isLargeLayout = true;

    public EtaWidgetProvider() {
    }

    @Override
    public void onEnabled(Context context) {
        int refreshInterval = 60;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences != null) {
            Integer i = Integer.parseInt(preferences.getString("widget_load_eta", "0"));
            if (i >= 0) {
                refreshInterval = i;
            }
        }
        EtaWidgetAlarm.start(context, refreshInterval);
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
        if (action != null) {
            Integer appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            // Timber.d("%s %s", action, appWidgetId);
            switch (action) {
                case AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED:
                case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                    AppWidgetManager.getInstance(context)
                            .notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view);
                    break;
                case C.ACTION.WIDGET_UPDATE:
                    if (ConnectivityUtil.isConnected(context)) {
                        try {
                            Intent i = new Intent(context, EtaService.class);
                            i.putExtra(C.EXTRA.WIDGET_UPDATE, appWidgetId);
                            i.putExtra(C.EXTRA.FOLLOW, true);
                            context.startService(i);
                        } catch (IllegalStateException ignored) {}
                    }
                    break;
            }
        }
        super.onReceive(context, intent);
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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            onUpdate(context, appWidgetManager, appWidgetId);
        }

    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        ArrivalTimeDatabase database = ArrivalTimeDatabase.Companion.getInstance(context);
        if (database != null) {
            LiveData<List<ArrivalTime>> resultLiveData = database.arrivalTimeDao().getLiveData();
            ready(resultLiveData, ForeverStartLifecycleOwner.INSTANCE, () -> {
                RemoteViews layout = buildLayout(context, appWidgetId, isLargeLayout);
                appWidgetManager.updateAppWidget(appWidgetId, layout);
                return null;
            });
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        updateWidgetSize(appWidgetManager.getAppWidgetOptions(appWidgetId));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view);
        appWidgetManager.updateAppWidget(appWidgetId, buildLayout(context, appWidgetId, isLargeLayout));
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateWidgetSize(Bundle bundle) {
        if (null == bundle) return;
//        int minWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
//        int maxWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
//        int maxHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        isLargeLayout = minHeight >= 80;
    }

    public static <T> void ready(LiveData<T> liveData, LifecycleOwner lifecycleOwner, Callable<T> callable) {
        T t = liveData.getValue();
        if (t != null) {
            try {
                callable.call();
            } catch (Exception ignored) {
            }
            return;
        }

        liveData.observe(lifecycleOwner, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                liveData.removeObserver(this);
                try {
                    callable.call();
                } catch (Exception ignored) {
                }
            }
        });
    }
}