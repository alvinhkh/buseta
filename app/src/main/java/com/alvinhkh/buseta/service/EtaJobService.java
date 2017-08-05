package com.alvinhkh.buseta.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.NotificationUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

public class EtaJobService extends JobService {

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence name = getString(R.string.channel_name_eta);
            String description = getString(R.string.channel_description_eta);
            NotificationChannel channel = new NotificationChannel(C.NOTIFICATION.CHANNEL_ETA, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            channel.enableLights(false);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
        disposables.add(RxBroadcastReceiver.create(this, new IntentFilter(C.ACTION.ETA_UPDATE))
                .share()
                .subscribeWith(etaObserver()));
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        Bundle bundle = job.getExtras();
        if (bundle == null) return false;
        BusRouteStop busRouteStop = new Gson().fromJson(bundle.getString(C.EXTRA.STOP_OBJECT_STRING), BusRouteStop.class);
        Integer notificationId = bundle.getInt(C.EXTRA.NOTIFICATION_ID, -1);
        Integer rowId = bundle.getInt(C.EXTRA.ROW, -1);
        Integer widgetId = bundle.getInt(C.EXTRA.WIDGET_UPDATE, -1);
        if (busRouteStop != null) {
            Timber.d(busRouteStop.toString());
            Intent intent = new Intent(getApplicationContext(), EtaService.class);
            intent.putExtra(C.EXTRA.STOP_OBJECT, busRouteStop);
            intent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId);
            intent.putExtra(C.EXTRA.ROW, rowId);
            intent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId);
            startService(intent);
        }
        if (widgetId >= 0) {
            if (ConnectivityUtil.isConnected(this)) {
                List<FollowStop> followStops = FollowStopUtil.toList(this);
                ArrayList<BusRouteStop> busRouteStops = new ArrayList<>();
                for (FollowStop stop: followStops) {
                    busRouteStops.add(BusRouteStopUtil.fromFollowStop(stop));
                }
                try {
                    Intent intent = new Intent(this, EtaService.class);
                    intent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId);
                    intent.putParcelableArrayListExtra(C.EXTRA.STOP_LIST, busRouteStops);
                    startService(intent);
                } catch (IllegalStateException ignored) {}
            }
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Integer notificationId = bundle.getInt(C.EXTRA.NOTIFICATION_ID);
                if (notificationId > 0) {
                    BusRouteStop routeStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                    if (routeStop == null) return;
                    Timber.d("notificationId: %s UPDATE", notificationId);
                    NotificationCompat.Builder builder = NotificationUtil.showArrivalTime(getApplicationContext(), routeStop);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(notificationId, builder.build());
                }
                Integer widgetId = bundle.getInt(C.EXTRA.WIDGET_UPDATE, -1);
                if (widgetId >= 0) {
                    Timber.d("WIDGET: %s", widgetId);
                    if (bundle.getBoolean(C.EXTRA.COMPLETE)) {
                        AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
                        mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.list_view);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }
}