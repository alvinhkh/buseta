package com.alvinhkh.buseta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings

import androidx.annotation.RequiresApi
import androidx.collection.SparseArrayCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.utils.NotificationUtil

import timber.log.Timber


class EtaNotificationService : Service() {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase
    private lateinit var broadcastReceiver: BroadcastReceiver
    private val routeStops = SparseArrayCompat<RouteStop>()

    override fun onCreate() {
        super.onCreate()

        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(this)?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(C.NOTIFICATION.CHANNEL_ETA) == null) {
                val etaChannel = NotificationChannel(C.NOTIFICATION.CHANNEL_ETA,
                        getString(R.string.channel_name_eta), NotificationManager.IMPORTANCE_DEFAULT)
                etaChannel.description = getString(R.string.channel_description_eta)
                etaChannel.enableLights(false)
                etaChannel.enableVibration(false)
                etaChannel.importance = NotificationManager.IMPORTANCE_DEFAULT
                notificationManager.createNotificationChannel(etaChannel)
            }
            if ( notificationManager.getNotificationChannel(C.NOTIFICATION.CHANNEL_FOREGROUND) == null) {
                val foregroundChannel = NotificationChannel(C.NOTIFICATION.CHANNEL_FOREGROUND,
                        getString(R.string.channel_name_foreground, getString(R.string.app_name)), NotificationManager.IMPORTANCE_NONE)
                foregroundChannel.description = getString(R.string.channel_description_foreground)
                foregroundChannel.enableLights(false)
                foregroundChannel.enableVibration(false)
                foregroundChannel.importance = NotificationManager.IMPORTANCE_NONE
                notificationManager.createNotificationChannel(foregroundChannel)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showForegroundNotification()
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when(intent.action) {
                    C.ACTION.ETA_UPDATE -> {
                        val bundle = intent.extras ?: return
                        val routeStop = bundle.getParcelable<RouteStop>(C.EXTRA.STOP_OBJECT)
                        val notificationId = bundle.getInt(C.EXTRA.NOTIFICATION_ID)
                        if (routeStop == null) return
                        if (notificationId > 0 && routeStops.get(notificationId) != null) {
                            Timber.d("notification: %s UPDATE", notificationId)
                            val arrivalTimeList: List<ArrivalTime> = ArrivalTime.getList(arrivalTimeDatabase, routeStop)
                            val builder = NotificationUtil.showArrivalTime(applicationContext, routeStop, arrivalTimeList)
                            val notificationManager = NotificationManagerCompat.from(applicationContext)
                            notificationManager.notify(notificationId, builder.build())
                        }
                    }
                    C.ACTION.NOTIFICATION_UPDATE -> {
                        for (i in 0 until routeStops.size()) {
                            Timber.d("request update notification: %s", routeStops.keyAt(i))
                            val startIntent = Intent(applicationContext, EtaService::class.java)
                            startIntent.putExtra(C.EXTRA.STOP_OBJECT, routeStops.valueAt(i))
                            startIntent.putExtra(C.EXTRA.NOTIFICATION_ID, routeStops.keyAt(i))
                            startService(startIntent)
                        }
                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter(C.ACTION.NOTIFICATION_UPDATE))
        registerReceiver(broadcastReceiver, IntentFilter(C.ACTION.ETA_UPDATE))

        val pendingIntent = PendingIntent.getBroadcast(this, 0,
                Intent(C.ACTION.NOTIFICATION_UPDATE), PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            Timber.d("Alarm is already active")
        } else {
            var interval = 30
            val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            if (preferences != null) {
                val i = preferences.getString("load_eta", "0")?.toInt()?:0
                if (i > 0) {
                    interval = i
                }
            }
            NotificationAlarm.startAlarm(this, interval)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val extras = intent.extras
        if (extras == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notificationManager = NotificationManagerCompat.from(this)
        val action = intent.action
        var notificationId = extras.getInt(C.EXTRA.NOTIFICATION_ID)
        val widgetId = extras.getInt(C.EXTRA.WIDGET_UPDATE)
        if (action == C.ACTION.CANCEL) {
            notificationManager.cancel(notificationId)
            routeStops.remove(notificationId)
            if (routeStops.size() < 1) {
                stopSelf()
            }
            return START_NOT_STICKY
        }

        val routeStop = extras.getParcelable<RouteStop>(C.EXTRA.STOP_OBJECT)
        if (routeStop != null) {
            notificationId = NotificationUtil.getNotificationId(routeStop)?: 1000
            val arrivalTimeList: List<ArrivalTime> = ArrivalTime.getList(arrivalTimeDatabase, routeStop)
            val builder = NotificationUtil.showArrivalTime(this, routeStop, arrivalTimeList)
            notificationManager.notify(notificationId, builder.build())
            routeStops.put(notificationId, routeStop)
            val startIntent = Intent(applicationContext, EtaService::class.java)
            startIntent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
            startIntent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId)
            startIntent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId)
            startService(startIntent)
            return START_STICKY
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        NotificationAlarm.stopAlarm(this)
        routeStops.clear()
        stopForeground(true)
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showForegroundNotification() {
        val notificationId = 1000
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, C.NOTIFICATION.CHANNEL_FOREGROUND)
        } else {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", packageName, null)
        }
        val contentIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, C.NOTIFICATION.CHANNEL_FOREGROUND)
        builder.setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_outline_directions_bus_24dp)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setShowWhen(false)
                .setContentTitle(getString(R.string.channel_name_foreground, getString(R.string.app_name)))
                .setContentText(getString(R.string.channel_description_foreground))
                .setContentIntent(contentIntent)
        startForeground(notificationId, builder.build())
    }
}
