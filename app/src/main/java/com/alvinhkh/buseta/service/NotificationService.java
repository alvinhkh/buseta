package com.alvinhkh.buseta.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.collection.SparseArrayCompat;
import androidx.preference.PreferenceManager;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.route.model.RouteStop;
import com.alvinhkh.buseta.utils.NotificationUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;


public class NotificationService extends Service {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private static ArrivalTimeDatabase arrivalTimeDatabase = null;

    private SparseArrayCompat<RouteStop> routeStops = new SparseArrayCompat<>();

    private NotificationAlarm alarm;

    public NotificationService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        arrivalTimeDatabase = ArrivalTimeDatabase.Companion.getInstance(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel etaChannel = new NotificationChannel(C.NOTIFICATION.CHANNEL_ETA,
                        getString(R.string.channel_name_eta), NotificationManager.IMPORTANCE_DEFAULT);
                etaChannel.setDescription(getString(R.string.channel_description_eta));
                etaChannel.enableLights(false);
                etaChannel.enableVibration(false);
                etaChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(etaChannel);
                NotificationChannel foregroundChannel = new NotificationChannel(C.NOTIFICATION.CHANNEL_FOREGROUND,
                        getString(R.string.channel_name_foreground, getString(R.string.app_name)), NotificationManager.IMPORTANCE_NONE);
                foregroundChannel.setDescription(getString(R.string.channel_description_foreground));
                foregroundChannel.enableLights(false);
                foregroundChannel.enableVibration(false);
                foregroundChannel.setImportance(NotificationManager.IMPORTANCE_NONE);
                notificationManager.createNotificationChannel(foregroundChannel);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showForegroundNotification();
        }
        disposables.add(RxBroadcastReceiver.create(this, new IntentFilter(C.ACTION.ETA_UPDATE))
                .share()
                .subscribeWith(etaObserver()));
        disposables.add(RxBroadcastReceiver.create(this, new IntentFilter(C.ACTION.NOTIFICATION_UPDATE))
                .share()
                .subscribeWith(updateObserver()));
        // Alarm Service
        boolean alarmUp = PendingIntent.getBroadcast(this, 0, new Intent(C.ACTION.NOTIFICATION_UPDATE),
                PendingIntent.FLAG_NO_CREATE) != null;
        if (alarmUp) {
            Timber.d("Alarm is already active");
        } else {
            Integer interval = 30;
            alarm = new NotificationAlarm(this);
            if (getApplicationContext() != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (preferences != null) {
                    Integer i = Integer.parseInt(preferences.getString("load_eta", "0"));
                    if (i > 0) {
                        interval = i;
                    }
                }
            }
            alarm.startAlarm(interval);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        String action = intent.getAction();
        int notificationId = extras.getInt(C.EXTRA.NOTIFICATION_ID);
        int widgetId = extras.getInt(C.EXTRA.WIDGET_UPDATE);
        if (action != null && action.equals(C.ACTION.CANCEL)) {
            notificationManager.cancel(notificationId);
            routeStops.delete(notificationId);
            if (routeStops.size() < 1) {
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        RouteStop routeStop = extras.getParcelable(C.EXTRA.STOP_OBJECT);
        if (routeStop != null) {
            notificationId = NotificationUtil.getNotificationId(routeStop);
            List<ArrivalTime> arrivalTimeList = new ArrayList<>();
            if (arrivalTimeDatabase != null) {
                arrivalTimeList = ArrivalTime.getList(arrivalTimeDatabase, routeStop);
            }
            NotificationCompat.Builder builder = NotificationUtil.showArrivalTime(this, routeStop, arrivalTimeList);
            notificationManager.notify(notificationId, builder.build());
            routeStops.put(notificationId, routeStop);
            Intent startIntent = new Intent(getApplicationContext(), EtaService.class);
            startIntent.putExtra(C.EXTRA.STOP_OBJECT, routeStop);
            startIntent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId);
            startIntent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId);
            startService(startIntent);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        alarm = new NotificationAlarm(getApplicationContext());
        alarm.stopAlarm();
        routeStops.clear();
        stopForeground(true);
        super.onDestroy();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void showForegroundNotification() {
        int notificationId = 1000;
        Intent i = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            i.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            i.putExtra(Settings.EXTRA_CHANNEL_ID, C.NOTIFICATION.CHANNEL_FOREGROUND);
        } else {
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getPackageName(), null));
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, notificationId, i, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, C.NOTIFICATION.CHANNEL_FOREGROUND);
        builder.setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_outline_directions_bus_24dp)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setShowWhen(false)
                .setContentTitle(getString(R.string.channel_name_foreground, getString(R.string.app_name)))
                .setContentText(getString(R.string.channel_description_foreground))
                .setContentIntent(contentIntent);
        startForeground(notificationId, builder.build());
    }

    private DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                RouteStop routeStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                Integer notificationId = bundle.getInt(C.EXTRA.NOTIFICATION_ID);
                if (routeStop == null) return;
                if (notificationId > 0 && routeStops.get(notificationId) != null) {
                    Timber.d("notification: %s UPDATE", notificationId);
                    List<ArrivalTime> arrivalTimeList = new ArrayList<>();
                    if (arrivalTimeDatabase != null) {
                        arrivalTimeList = ArrivalTime.getList(arrivalTimeDatabase, routeStop);
                    }
                    NotificationCompat.Builder builder = NotificationUtil.showArrivalTime(getApplicationContext(), routeStop, arrivalTimeList);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(notificationId, builder.build());
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

    private DisposableObserver<Intent> updateObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                for (int i = 0; i < routeStops.size(); i++) {
                    Timber.d("request update notification: %s", routeStops.keyAt(i));
                    Intent startIntent = new Intent(getApplicationContext(), EtaService.class);
                    startIntent.putExtra(C.EXTRA.STOP_OBJECT, routeStops.valueAt(i));
                    startIntent.putExtra(C.EXTRA.NOTIFICATION_ID, routeStops.keyAt(i));
                    startService(startIntent);
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
