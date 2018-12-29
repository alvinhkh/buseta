package com.alvinhkh.buseta.datagovhk

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.datagovhk.model.MtrLineStation
import com.alvinhkh.buseta.mtr.MtrService
import com.alvinhkh.buseta.mtr.model.MtrSchedule
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.HashUtil
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MtrEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.resource.create(DataGovHkService::class.java)

    private val mtrService = MtrService.api.create(MtrService::class.java)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(context)!!

    private val routeDatabase = RouteDatabase.getInstance(context)!!

    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    override fun doWork(): Result {
        val widgetId = inputData.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = inputData.getInt(C.EXTRA.NOTIFICATION_ID, -1)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.MTR
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:return Result.failure()
        val stopId = inputData.getString(C.EXTRA.STOP_ID)?:return Result.failure()
        val stopSequence = inputData.getString(C.EXTRA.STOP_SEQUENCE)?:return Result.failure()

        val outputData = Data.Builder()
                .putInt(C.EXTRA.WIDGET_UPDATE, widgetId)
                .putInt(C.EXTRA.NOTIFICATION_ID, notificationId)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_ID, routeId)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                .putString(C.EXTRA.STOP_ID, stopId)
                .putString(C.EXTRA.STOP_SEQUENCE, stopSequence)
                .build()

        if (routeId.isEmpty() || stopId.isEmpty()) {
            return Result.failure(outputData)
        }

        val routeStopList = routeDatabase.routeStopDao().get(companyCode, routeId, stopId)
        if (routeStopList.size < 1) {
            return Result.failure(outputData)
        }
        val routeStop = routeStopList[0]

        val arrivalTimeList = arrayListOf<ArrivalTime>()
        val timeNow = System.currentTimeMillis()
        
        val response1 = dataGovHkService.mtrLinesAndStations().execute()
        if (!response1.isSuccessful) {
            if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                    && !routeStop.sequence.isNullOrEmpty()) {
                arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                        routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
            }
            val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
            arrivalTime.text = context.getString(R.string.message_fail_to_request)
            arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
            return Result.failure(outputData)
        }

        try {
            val body = response1.body()
            if (body == null) {
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = context.getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }
            val codeMap = HashMap<String, String>()
            val stations = MtrLineStation.fromCSV(body.string(), routeStop.routeId!!)
            for ((_, _, stationCode, _, chineseName) in stations) {
                if (!codeMap.containsKey(stationCode?:"")) {
                    codeMap[stationCode?:""] = chineseName?:""
                }
            }

            val lang = "en"
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            val secret = firebaseRemoteConfig.getString("mtr_schedule_secret")
            val key = HashUtil.sha1(routeStop.routeId + "|" + routeStop.stopId + "|" + lang + "|" + today + "|" + secret)
            if (key.isEmpty() || routeStop.routeId.isNullOrEmpty() || routeStop.stopId.isNullOrEmpty()) {
                return Result.failure(outputData)
            }

            val response = mtrService.getSchedule(key!!, routeStop.routeId!!, routeStop.stopId!!, lang).execute()
            val res = response.body() ?: return Result.failure(outputData)
            Timber.d("%s", res)
            if (res.status == 0) {
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = context.getString(R.string.provider_no_eta)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.success(outputData)
            } else if (res.data != null && res.data!!.isNotEmpty()) {
                var hasData = false
                for ((_, value) in res.data!!) {
                    val (currentTime, _, up, down) = value
                    var i = 0
                    if (up != null && up.isNotEmpty()) {
                        hasData = true
                        for (schedule in up) {
                            val arrivalTime = MtrSchedule.toArrivalTime(applicationContext, "UT", schedule, currentTime, codeMap)
                            arrivalTime.routeNo = routeStop.routeNo?:""
                            arrivalTime.routeSeq = routeStop.routeSequence?:""
                            arrivalTime.stopId = routeStop.stopId?:""
                            arrivalTime.stopSeq = routeStop.sequence?:""
                            arrivalTime.order = i.toString()

                            arrivalTime.updatedAt = timeNow
                            arrivalTimeList.add(arrivalTime)
                            i++
                        }
                    }
                    if (down != null && down.isNotEmpty()) {
                        hasData = true
                        for (schedule in down) {
                            val arrivalTime = MtrSchedule.toArrivalTime(applicationContext, "DT", schedule, currentTime, codeMap)
                            arrivalTime.routeNo = routeStop.routeNo?:""
                            arrivalTime.routeSeq = routeStop.routeSequence?:""
                            arrivalTime.stopId = routeStop.stopId?:""
                            arrivalTime.stopSeq = routeStop.sequence?:""
                            arrivalTime.order = i.toString()

                            arrivalTime.updatedAt = timeNow
                            arrivalTimeList.add(arrivalTime)
                            i++
                        }
                    }
                    arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
                }
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                if (hasData) {
                    return Result.success(outputData)
                }
            }
        } catch (e: IOException) {
            Timber.d(e)
        }

        if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                && !routeStop.sequence.isNullOrEmpty()) {
            arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                    routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
        }
        val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
        arrivalTime.routeNo = routeStop.routeNo?:""
        arrivalTime.routeSeq = routeStop.routeSequence?:""
        arrivalTime.stopId = routeStop.stopId?:""
        arrivalTime.stopSeq = routeStop.sequence?:""
        arrivalTime.order = "0"
        arrivalTime.text = context.getString(R.string.message_no_data)
        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
        return Result.failure(outputData)
    }
}