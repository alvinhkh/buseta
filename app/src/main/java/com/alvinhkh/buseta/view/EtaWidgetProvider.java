package com.alvinhkh.buseta.view;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.FavouriteProvider;
import com.alvinhkh.buseta.service.EtaWidgetService;

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
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.listView);
    }
}

public class EtaWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "EtaWidgetProvider";

    public static String CLICK_ACTION = "com.alvinhkh.buseta.widget.CLICK";

    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;
    private static EtaDataProviderObserver sDataObserver;

    RemoteViews rv;

    private boolean mIsLargeLayout = true;

    public EtaWidgetProvider() {
        // Start the worker thread
        sWorkerThread = new HandlerThread("EtaWidgetProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    @Override
    public void onEnabled(Context context) {
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
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();
        /*if (action.equals(REFRESH_ACTION)) {
            // BroadcastReceivers have a limited amount of time to do work, so for this sample, we
            // are triggering an update of the data on another thread.  In practice, this update
            // can be triggered from a background service, or perhaps as a result of user actions
            // inside the main application.
            final Context context = ctx;
            sWorkerQueue.removeMessages(0);
            sWorkerQueue.post(new Runnable() {
                @Override
                public void run() {
                    final ContentResolver r = context.getContentResolver();
                    final Cursor c = r.query(WeatherDataProvider.CONTENT_URI, null, null, null,
                            null);
                    final int count = c.getCount();

                    // We disable the data changed observer temporarily since each of the updates
                    // will trigger an onChange() in our data observer.
                    r.unregisterContentObserver(sDataObserver);
                    for (int i = 0; i < count; ++i) {
                        final Uri uri = ContentUris.withAppendedId(WeatherDataProvider.CONTENT_URI, i);
                        final ContentValues values = new ContentValues();
                        values.put(WeatherDataProvider.Columns.TEMPERATURE,
                                new Random().nextInt(sMaxDegrees));
                        r.update(uri, values, null, null);
                    }
                    r.registerContentObserver(WeatherDataProvider.CONTENT_URI, true, sDataObserver);

                    final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    final ComponentName cn = new ComponentName(context, WeatherWidgetProvider.class);
                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.weather_list);

                }
            });

            final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        } else */if (action.equals(CLICK_ACTION)) {
            // Show a toast
            final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Toast.makeText(ctx, "Click", Toast.LENGTH_SHORT).show();
        }

        super.onReceive(ctx, intent);
    }

    private RemoteViews buildLayout(final Context context, final int appWidgetId, boolean largeLayout) {
        if (largeLayout) {
            Log.d(TAG, "buildLayout");
            // Specify the service to provide data for the collection widget.  Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            final Intent intent = new Intent(context, EtaWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            rv = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
            rv.setRemoteAdapter(R.id.listView, intent);
            final RemoteViews rv1 = rv;
            sWorkerQueue.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // rv1.setScrollPosition(R.id.listView, 5);
                    final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    mgr.partiallyUpdateAppWidget(appWidgetId, rv);
                }

            }, 1000);

            // Set the empty view to be displayed if the collection is empty.  It must be a sibling
            // view of the collection view.
            // rv.setEmptyView(R.id.listView, R.id.emptyView);

            // Bind a click listener template for the contents of the  list.  Note that we
            // need to update the intent's data if we set an extra, since the extras will be
            // ignored otherwise.
            final Intent onClickIntent = new Intent(context, EtaWidgetProvider.class);
            onClickIntent.setAction(EtaWidgetProvider.CLICK_ACTION);
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                    onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.actionButton, onClickPendingIntent);
            rv.setPendingIntentTemplate(R.id.listView, onClickPendingIntent);
        } else {
            rv = new RemoteViews(context.getPackageName(), R.layout.widget_eta);
            // ...
        }
        return rv;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews layout = buildLayout(context, appWidgetIds[i], mIsLargeLayout);
            appWidgetManager.updateAppWidget(appWidgetIds[i], layout);
        }
        Toast.makeText(context, "widget updated", Toast.LENGTH_SHORT).show();
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @TargetApi(16)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {

        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        RemoteViews layout;
        if (minHeight < 100) {
            mIsLargeLayout = false;
        } else {
            mIsLargeLayout = true;
        }
        layout = buildLayout(context, appWidgetId, mIsLargeLayout);
        appWidgetManager.updateAppWidget(appWidgetId, layout);
    }

}