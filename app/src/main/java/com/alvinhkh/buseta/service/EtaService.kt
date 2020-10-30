package com.alvinhkh.buseta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.LifecycleService
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.appwidget.FollowWidgetProvider
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.datagovhk.RtNwstEtaWorker
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.mtr.MtrEtaWorker
import com.alvinhkh.buseta.kmb.KmbEtaWorker
import com.alvinhkh.buseta.mtr.AESBusEtaWorker
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.nlb.NlbEtaWorker
import com.alvinhkh.buseta.nwst.NwstEtaWorker
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.PreferenceUtil


class EtaService : LifecycleService() {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase

    private lateinit var followDatabase: FollowDatabase

    private lateinit var routeDatabase: RouteDatabase

    override fun onCreate() {
        super.onCreate()
        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(this)!!
        followDatabase = FollowDatabase.getInstance(this)!!
        routeDatabase = RouteDatabase.getInstance(this)!!
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val extras = intent.extras ?: return Service.START_NOT_STICKY

        if (!ConnectivityUtil.isConnected(this)) return Service.START_NOT_STICKY

        val widgetId = extras.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = extras.getInt(C.EXTRA.NOTIFICATION_ID, -1)

        var tag = "Eta"
        val routeStopList = mutableListOf<RouteStop>()
        if (extras.getBoolean(C.EXTRA.STOP_LIST)) {
            val routeNo = extras.getString(C.EXTRA.ROUTE_NO, "")
            val routeSequence = extras.getString(C.EXTRA.ROUTE_SEQUENCE, "")
            val companyCode = extras.getString(C.EXTRA.COMPANY_CODE, "")
            routeStopList.addAll(routeDatabase.routeStopDao().get(companyCode,
                    extras.getString(C.EXTRA.ROUTE_ID, ""),
                    routeNo,
                    routeSequence,
                    extras.getString(C.EXTRA.ROUTE_SERVICE_TYPE, "")))
            if (arrayListOf(C.PROVIDER.LRTFEEDER).contains(companyCode) && routeStopList.size > 0) {
                // require only one request to get all results
                val routeStop = routeStopList[0]
                routeStopList.clear()
                routeStopList.add(routeStop)
            }
            tag = "StopListEta_${companyCode}_${routeNo}_$routeSequence"
        }
        val stop = extras.getParcelable<RouteStop>(C.EXTRA.STOP_OBJECT)
        if (stop != null) {
            routeStopList.add(stop)
        }
        val isFollow = extras.getBoolean(C.EXTRA.FOLLOW, false)
        if (isFollow) {
            val groupId = extras.getString(C.EXTRA.GROUP_ID)?:""
            val followList = if (groupId.isEmpty()) followDatabase.followDao().list() else followDatabase.followDao().list(groupId)
            for (follow in followList) {
                routeStopList.add(follow.toRouteStop())
            }
            tag = "FollowEta_$groupId"
        }
        if (widgetId > 0) {
            tag = "${tag}_AppWidget_$widgetId"
        }
        if (widgetId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showForegroundNotification()
        }
        if (!tag.startsWith("Eta")) {
            WorkManager.getInstance().cancelAllWorkByTag(tag)
        }

        var isFinishedCount = 0
        val workerRequestList = arrayListOf<OneTimeWorkRequest>()
        for (i in routeStopList.indices) {
            val routeStop = routeStopList[i]
            if (!routeStop.companyCode.isNullOrEmpty()) {
                val data = Data.Builder()
                        .putInt(C.EXTRA.WIDGET_UPDATE, widgetId)
                        .putInt(C.EXTRA.NOTIFICATION_ID, notificationId)
                        .putString(C.EXTRA.COMPANY_CODE, routeStop.companyCode)
                        .putString(C.EXTRA.ROUTE_ID, routeStop.routeId)
                        .putString(C.EXTRA.ROUTE_NO, routeStop.routeNo)
                        .putString(C.EXTRA.ROUTE_SEQUENCE, routeStop.routeSequence)
                        .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeStop.routeServiceType)
                        .putString(C.EXTRA.STOP_ID, routeStop.stopId)
                        .putString(C.EXTRA.STOP_SEQUENCE, routeStop.sequence)
                        .build()
                notifyUpdate(routeStop, C.EXTRA.UPDATING, widgetId, notificationId)
                var workerRequest: OneTimeWorkRequest? = null
                when (routeStop.companyCode) {
                    C.PROVIDER.LRTFEEDER -> {
                        workerRequest = OneTimeWorkRequest.Builder(AESBusEtaWorker::class.java)
                                .addTag(tag).setInputData(data).build()
                    }
                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                        workerRequest = OneTimeWorkRequest.Builder(
                                if (PreferenceUtil.isUsingNwstDataGovHkApi(applicationContext)) {
                                    RtNwstEtaWorker::class.java
                                } else {
                                    NwstEtaWorker::class.java
                                }
                        ).addTag(tag).setInputData(data).build()
                    }
                    C.PROVIDER.KMB, C.PROVIDER.LWB -> {
                        workerRequest = OneTimeWorkRequest.Builder(KmbEtaWorker::class.java)
                                .addTag(tag).setInputData(data).build()
                    }
                    C.PROVIDER.MTR -> {
                        workerRequest = OneTimeWorkRequest.Builder(MtrEtaWorker::class.java)
                                .addTag(tag).setInputData(data).build()
                    }
                    C.PROVIDER.NLB, C.PROVIDER.GMB901 -> {
                        workerRequest = OneTimeWorkRequest.Builder(NlbEtaWorker::class.java)
                                .addTag(tag).setInputData(data).build()
                    }
                    else -> notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId)
                }

                if (workerRequest != null) {
                    workerRequestList.add(workerRequest)
                    WorkManager.getInstance().getWorkInfoByIdLiveData(workerRequest.id)
                            .observe(this, { workInfo ->
                                if (workInfo.state.isFinished) isFinishedCount += 1
                                if (workInfo?.state == WorkInfo.State.FAILED) {
                                    notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId)
                                }
                                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                                    notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId)
                                }
                                if (isFinishedCount >= workerRequestList.size) {
                                    stopSelf()
                                }
                            })
                }
            } else {
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId)
            }
        }

        if (workerRequestList.size > 0) {
            WorkManager.getInstance().enqueue(workerRequestList)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun notifyUpdate(stop: RouteStop, status: String,
                             widgetId: Int, notificationId: Int) {
        val intent = Intent(C.ACTION.ETA_UPDATE)
        intent.putExtra(status, true)
        if (widgetId > 0) {
            intent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId)
            val widgetIntent = Intent(applicationContext, FollowWidgetProvider::class.java)
            widgetIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            sendBroadcast(widgetIntent)
        }
        if (notificationId >= 0) {
            intent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId)
        }
        intent.putExtra(C.EXTRA.STOP_OBJECT, stop)
        sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (notificationManager?.getNotificationChannel(C.NOTIFICATION.CHANNEL_FOREGROUND) == null) {
            val foregroundChannel = NotificationChannel(C.NOTIFICATION.CHANNEL_FOREGROUND,
                    getString(R.string.channel_name_foreground, getString(R.string.app_name)), NotificationManager.IMPORTANCE_NONE)
            foregroundChannel.description = getString(R.string.channel_description_foreground)
            foregroundChannel.enableLights(false)
            foregroundChannel.enableVibration(false)
            foregroundChannel.importance = NotificationManager.IMPORTANCE_NONE
            notificationManager?.createNotificationChannel(foregroundChannel)
        }
        val notificationId = 1001
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, C.NOTIFICATION.CHANNEL_FOREGROUND)
        } else {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", packageName, null)
        }
        val contentIntent = PendingIntent.getActivity(this,
                notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, C.NOTIFICATION.CHANNEL_FOREGROUND)
        builder.setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_outline_directions_bus_24dp)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setShowWhen(false)
                .setContentTitle(getString(R.string.channel_name_update))
                .setContentText(getString(R.string.channel_description_foreground))
                .setContentIntent(contentIntent)
        startForeground(notificationId, builder.build())
    }
}
