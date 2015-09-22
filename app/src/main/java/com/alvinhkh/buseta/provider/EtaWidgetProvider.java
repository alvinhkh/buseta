package com.alvinhkh.buseta.provider;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

public class EtaWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "EtaWidgetProvider";

    RemoteViews rv;
    int intervalMinutes = 1;

    private boolean mIsLargeLayout = true;

    public EtaWidgetProvider() {
    }

    public static Intent getRefreshBroadcastIntent(Context context) {
        Intent intent = new Intent(Constants.MESSAGE.ETA_UPDATED);
        intent.setComponent(new ComponentName(context, EtaWidgetProvider.class));
        intent.putExtra(Constants.MESSAGE.ETA_UPDATED, true);
        intent.putExtra(Constants.MESSAGE.WIDGET_UPDATE, true);
        return intent;
    }

    @Override
    public void onEnabled(Context context) {
        // Log.d(TAG, "onEnabled");
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
        // final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (action.equals(Constants.MESSAGE.WIDGET_TRIGGER_UPDATE)) {
            // Log.d(TAG, "WIDGET_TRIGGER_UPDATE");
            onRefresh(context);
        } else if (action.equals(Constants.MESSAGE.ETA_UPDATED)) {
            if (bundle.getBoolean(Constants.MESSAGE.WIDGET_UPDATE)) {
                // Log.d(TAG, "WIDGET_UPDATE");
                final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                final ComponentName cn = new ComponentName(context, EtaWidgetProvider.class);
                mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.listView);
            }
        }
        super.onReceive(context, intent);
    }

    private void onRefresh(final Context context) {
        // Log.d(TAG, "onRefresh");
        final ConnectivityManager conMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        // Check internet connection
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                final Cursor c = context.getContentResolver().query(
                        FollowProvider.CONTENT_URI_FOLLOW, null, null, null,
                        FollowTable.COLUMN_DATE + " DESC");
                while (null != c && c.moveToNext()) {
                    RouteBound routeBound = new RouteBound();
                    routeBound.route_no = getColumnString(c, FollowTable.COLUMN_ROUTE);
                    routeBound.route_bound = getColumnString(c, FollowTable.COLUMN_BOUND);
                    routeBound.origin_tc = getColumnString(c, FollowTable.COLUMN_ORIGIN);
                    routeBound.destination_tc = getColumnString(c, FollowTable.COLUMN_DESTINATION);
                    RouteStop routeStop = new RouteStop();
                    routeStop.route_bound = routeBound;
                    routeStop.stop_seq = getColumnString(c, FollowTable.COLUMN_STOP_SEQ);
                    routeStop.name_tc = getColumnString(c, FollowTable.COLUMN_STOP_NAME);
                    routeStop.code = getColumnString(c, FollowTable.COLUMN_STOP_CODE);
                    routeStop.follow = true;
                    Intent intent = new Intent(context, CheckEtaService.class);
                    intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                    intent.putExtra(Constants.MESSAGE.WIDGET_UPDATE, true);
                    context.startService(intent);
                }
                if (null != c)
                    c.close();
            } catch (SQLiteCantOpenDatabaseException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
    private String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    private RemoteViews buildLayout(final Context context, final int appWidgetId, boolean largeLayout) {
        // Log.d(TAG, "buildLayout");
        // Specify the service to provide data for the collection widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        final Intent intent = new Intent(context, EtaWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
        if (null != rv) {
            rv.setRemoteAdapter(R.id.listView, intent);
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
        }
        return rv;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Log.d(TAG, "onUpdate");
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
        RemoteViews layout = buildLayout(context, appWidgetId, mIsLargeLayout);
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