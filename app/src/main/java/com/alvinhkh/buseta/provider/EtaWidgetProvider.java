package com.alvinhkh.buseta.provider;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.service.EtaWidgetAlarm;
import com.alvinhkh.buseta.service.EtaWidgetService;
import com.alvinhkh.buseta.view.MainActivity;

/**
 * Our data observer just notifies an update for all weather widgets when it detects a change.
 */
class EtaDataProviderObserver extends ContentObserver {
    private AppWidgetManager mAppWidgetManager;
    private ComponentName mComponentName;

    EtaDataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
        super(h);
        mAppWidgetManager = mgr;
        mComponentName = cn;
    }

    @Override
    public void onChange(boolean selfChange) {
        // The data has changed, so notify the widget that the collection view needs to be updated.
        // In response, the factory's onDataSetChanged() will be called which will requery the
        // cursor for the new data.
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.textView_title);
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.textView_text);
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.listView);
    }
}

public class EtaWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "EtaWidgetProvider";

    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;
    private static EtaDataProviderObserver sDataObserver;

    RemoteViews rv;
    int intervalMinutes = 1;

    private boolean mIsLargeLayout = true;

    public EtaWidgetProvider() {
        // Start the worker thread
        sWorkerThread = new HandlerThread("EtaWidgetProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    @Override
    public void onEnabled(Context context) {
        // Log.d(TAG, "onEnabled");
        // Register for external updates to the data to trigger an update of the widget.  When using
        // content providers, the data is often updated via a background service, or in response to
        // user interaction in the main app.  To ensure that the widget always reflects the current
        // state of the data, we must listen for changes and update ourselves accordingly.
        final ContentResolver r = context.getContentResolver();
        if (sDataObserver == null) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, EtaWidgetProvider.class);
            sDataObserver = new EtaDataProviderObserver(mgr, cn, sWorkerQueue);
            r.registerContentObserver(FavouriteProvider.CONTENT_URI, true, sDataObserver);
        }
        // start alarm
        EtaWidgetAlarm alarm = new EtaWidgetAlarm(context.getApplicationContext());
        alarm.startAlarm(intervalMinutes);
    }

    @Override
    public void onDisabled(Context context) {
        // Log.d(TAG, "onDisabled");
        EtaWidgetAlarm appWidgetAlarm = new EtaWidgetAlarm(context.getApplicationContext());
        appWidgetAlarm.stopAlarm();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final Bundle bundle = intent.getExtras();
        final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (action.equals(Constants.MESSAGE.WIDGET_TRIGGER_UPDATE)) {
            sWorkerQueue.removeMessages(0);
            onRefresh(context);
        } else if (action.equals(Constants.MESSAGE.ETA_UPDATED)) {
            if (bundle.getBoolean(Constants.MESSAGE.WIDGET_UPDATE)) {
                rv = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
                final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                final ComponentName cn = new ComponentName(context, EtaWidgetProvider.class);
                mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.listView);
                mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.textView_text);
            }
        }
        super.onReceive(context, intent);
    }

    private void onRefresh(Context ctx) {
        // Log.d(TAG, "onRefresh");
        final Context context = ctx;
        sWorkerQueue.post(new Runnable() {
            @Override
            public void run() {
                final ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                // Check internet connection
                if (activeNetwork != null && activeNetwork.isConnected()) {
                    final ContentResolver r = context.getContentResolver();
                    final Cursor c = r.query(
                            FavouriteProvider.CONTENT_URI_FAV, null, null, null,
                            FavouriteTable.COLUMN_DATE + " DESC");
                    while (null != c && c.moveToNext()) {
                        RouteBound routeBound = new RouteBound();
                        routeBound.route_no = getColumnString(c, FavouriteTable.COLUMN_ROUTE);
                        routeBound.route_bound = getColumnString(c, FavouriteTable.COLUMN_BOUND);
                        routeBound.origin_tc = getColumnString(c, FavouriteTable.COLUMN_ORIGIN);
                        routeBound.destination_tc = getColumnString(c, FavouriteTable.COLUMN_DESTINATION);
                        RouteStop routeStop = new RouteStop();
                        routeStop.route_bound = routeBound;
                        routeStop.stop_seq = getColumnString(c, FavouriteTable.COLUMN_STOP_SEQ);
                        routeStop.name_tc = getColumnString(c, FavouriteTable.COLUMN_STOP_NAME);
                        routeStop.code = getColumnString(c, FavouriteTable.COLUMN_STOP_CODE);
                        routeStop.favourite = true;
                        Intent intent = new Intent(context, CheckEtaService.class);
                        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                        intent.putExtra(Constants.MESSAGE.WIDGET_UPDATE, true);
                        context.startService(intent);
                    }
                    if (null != c)
                        c.close();
                } else {
                    rv = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
                    rv.setViewVisibility(R.id.textView_text, View.VISIBLE);
                    rv.setTextViewText(R.id.textView_text,
                            context.getString(R.string.message_no_internet_connection));
                    final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    final ComponentName cn = new ComponentName(context, EtaWidgetProvider.class);
                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.textView_text);
                }
            }

            private String getColumnString(Cursor cursor, String column) {
                int index = cursor.getColumnIndex(column);
                return cursor.isNull(index) ? "" : cursor.getString(index);
            }
        });
    }

    private RemoteViews buildLayout(final Context context, final int appWidgetId, boolean largeLayout) {
        // Specify the service to provide data for the collection widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        final Intent intent = new Intent(context, EtaWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
        rv.setRemoteAdapter(R.id.listView, intent);
        sWorkerQueue.postDelayed(new Runnable() {
            @Override
            public void run() {
                final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                mgr.partiallyUpdateAppWidget(appWidgetId, rv);
            }
        }, 1000);
        rv.setEmptyView(R.id.listView, R.id.emptyView);
        // click open app intent
        final Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, 0);
        // update intent
        final Intent updateIntent = new Intent(context, EtaWidgetProvider.class);
        updateIntent.setAction(Constants.MESSAGE.WIDGET_TRIGGER_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // updateIntent.setData(Uri.parse(updateIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, 0,
                updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.listView, updatePendingIntent);
        if (largeLayout) {
            rv.setViewVisibility(R.id.headerLayout, View.VISIBLE);
            rv.setOnClickPendingIntent(R.id.headerText, openAppPendingIntent);
            rv.setOnClickPendingIntent(R.id.refreshButton, updatePendingIntent);
        } else {
            rv.setViewVisibility(R.id.headerLayout, View.GONE);
        }
        return rv;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Log.d(TAG, "onUpdate");
        onRefresh(context);
        boolean alarmUp = (PendingIntent.getBroadcast(context, 0,
                new Intent(Constants.MESSAGE.WIDGET_TRIGGER_UPDATE),
                PendingIntent.FLAG_NO_CREATE) != null);
        if (alarmUp) {
            Log.d(TAG, "Alarm is already active");
        } else {
            EtaWidgetAlarm alarm = new EtaWidgetAlarm(context.getApplicationContext());
            alarm.startAlarm(intervalMinutes);
        }
        // Update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            if (i == 0 && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                updateWidgetSize(appWidgetManager.getAppWidgetOptions(appWidgetIds[i]));
            }
            RemoteViews layout = buildLayout(context, appWidgetIds[i], mIsLargeLayout);
            appWidgetManager.updateAppWidget(appWidgetIds[i], layout);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        updateWidgetSize(newOptions);
        RemoteViews layout;
        layout = buildLayout(context, appWidgetId, mIsLargeLayout);
        appWidgetManager.updateAppWidget(appWidgetId, layout);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateWidgetSize(Bundle bundle) {
        if (null == bundle) return;
        int minWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        if (minHeight < 80) {
            mIsLargeLayout = false;
        } else {
            mIsLargeLayout = true;
        }
    }

}