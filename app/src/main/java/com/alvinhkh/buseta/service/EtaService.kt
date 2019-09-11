package com.alvinhkh.buseta.service

import android.app.Service
import android.appwidget.AppWidgetManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.appwidget.FollowWidgetProvider
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.datagovhk.RtNwstEtaWorker
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.datagovhk.MtrEtaWorker
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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val extras = intent.extras ?: return Service.START_NOT_STICKY

        if (!ConnectivityUtil.isConnected(this)) return super.onStartCommand(intent, flags, startId)

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
            if (arrayListOf(C.PROVIDER.AESBUS, C.PROVIDER.LRTFEEDER).contains(companyCode) && routeStopList.size > 0) {
                // require only one request to get all results
                val routeStop = routeStopList[0]
                routeStopList.clear()
                routeStopList.add(routeStop)
            }
            tag = "StopListEta_${companyCode}_${routeNo}_$routeSequence"
            WorkManager.getInstance().cancelAllWorkByTag(tag)
        }
        val stop = extras.getParcelable<RouteStop>(C.EXTRA.STOP_OBJECT)
        if (stop != null) {
            routeStopList.add(stop)
        }
        val isFollow = extras.getBoolean(C.EXTRA.FOLLOW)
        if (isFollow) {
            val groupId = extras.getString(C.EXTRA.GROUP_ID)?:""
            val followList = if (groupId.isEmpty()) followDatabase.followDao().list() else followDatabase.followDao().list(groupId)
            for (follow in followList) {
                routeStopList.add(follow.toRouteStop())
            }
            tag = "FollowEta_$groupId"
            WorkManager.getInstance().cancelAllWorkByTag(tag)
        }

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
                    C.PROVIDER.AESBUS, C.PROVIDER.LRTFEEDER -> {
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
                            .observe(this, Observer { workInfo ->
                                if (workInfo?.state == WorkInfo.State.FAILED) {
                                    notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId)
                                }
                                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                                    notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId)
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
        if (widgetId >= 0) {
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
}
