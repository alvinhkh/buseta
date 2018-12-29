package com.alvinhkh.buseta.service

import android.arch.lifecycle.LifecycleService
import android.arch.lifecycle.Observer
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.datagovhk.MtrEtaWorker
import com.alvinhkh.buseta.kmb.KmbEtaWorker
import com.alvinhkh.buseta.mtr.AESBusEtaWorker
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.nlb.NlbEtaWorker
import com.alvinhkh.buseta.nwst.NwstEtaWorker
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.RouteStopUtil


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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return super.onStartCommand(intent, flags, startId)
        val extras = intent.extras ?: return super.onStartCommand(intent, flags, startId)

        if (!ConnectivityUtil.isConnected(this)) return super.onStartCommand(intent, flags, startId)

        val widgetId = extras.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = extras.getInt(C.EXTRA.NOTIFICATION_ID, -1)

        val routeStopList = mutableListOf<RouteStop>()
        if (extras.getBoolean(C.EXTRA.STOP_LIST)) {
            val companyCode = extras.getString(C.EXTRA.COMPANY_CODE, "")
            routeStopList.addAll(routeDatabase.routeStopDao().get(companyCode,
                    extras.getString(C.EXTRA.ROUTE_NO, ""),
                    extras.getString(C.EXTRA.ROUTE_SEQUENCE, ""),
                    extras.getString(C.EXTRA.ROUTE_SERVICE_TYPE, "")))
            if (companyCode == C.PROVIDER.AESBUS && routeStopList.size > 0) {
                val routeStop = routeStopList[0]
                routeStopList.clear()
                routeStopList.add(routeStop)
            }
        }
        val stop = extras.getParcelable<RouteStop>(C.EXTRA.STOP_OBJECT)
        if (stop != null) {
            routeStopList.add(stop)
        }
        if (extras.getBoolean(C.EXTRA.FOLLOW)) {
            val followList = followDatabase.followDao().getList()
            for (follow in followList) {
                routeStopList.add(RouteStopUtil.fromFollow(follow))
            }
        }

        WorkManager.getInstance().cancelAllWorkByTag(TAG)

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
                    C.PROVIDER.AESBUS -> {
                        workerRequest = OneTimeWorkRequest.Builder(AESBusEtaWorker::class.java)
                                .addTag(TAG).setInputData(data).build()
                    }
                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST ->
                        workerRequest = OneTimeWorkRequest.Builder(NwstEtaWorker::class.java)
                                .addTag(TAG).setInputData(data).build()
                    C.PROVIDER.KMB -> {
                        workerRequest = OneTimeWorkRequest.Builder(KmbEtaWorker::class.java)
                                .addTag(TAG).setInputData(data).build()
                    }
                    C.PROVIDER.LRTFEEDER -> {
                        val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                        arrivalTime.text = getString(R.string.provider_no_eta)
                        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                        notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId)
                    }
                    C.PROVIDER.MTR -> {
                        workerRequest = OneTimeWorkRequest.Builder(MtrEtaWorker::class.java)
                                .addTag(TAG).setInputData(data).build()
                    }
                    C.PROVIDER.NLB -> {
                        workerRequest = OneTimeWorkRequest.Builder(NlbEtaWorker::class.java)
                                .addTag(TAG).setInputData(data).build()
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
        }
        if (notificationId >= 0) {
            intent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId)
        }
        intent.putExtra(C.EXTRA.STOP_OBJECT, stop)
        sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "EtaService"
    }
}
