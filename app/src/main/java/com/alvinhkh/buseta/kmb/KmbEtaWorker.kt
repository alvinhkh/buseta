package com.alvinhkh.buseta.kmb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.route.dao.RouteDatabase
import org.jsoup.Jsoup

class KmbEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.etav3.create(KmbService::class.java)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(context)!!

    private val routeDatabase = RouteDatabase.getInstance(context)!!

    override fun doWork(): Result {
        val widgetId = inputData.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = inputData.getInt(C.EXTRA.NOTIFICATION_ID, -1)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:return Result.failure()
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()
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

        val routeStop = routeDatabase.routeStopDao().get(companyCode, routeNo, routeSequence, routeServiceType, stopId, stopSequence)
                ?: return Result.failure(outputData)

        val arrivalTimeList = arrayListOf<ArrivalTime>()
        val timeNow = System.currentTimeMillis()

        try {
            val response = kmbService.eta(routeStop.routeNo, routeStop.routeSequence,
                    routeStop.stopId, routeStop.sequence, routeStop.routeServiceType, "tc", "").execute()
            if (!response.isSuccessful) {
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

            val res = response.body()
            if (res == null) {
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            if (res.etas != null && res.etas!!.size > 0) {
                for (i in res.etas!!.indices) {
                    val kmbEta = res.etas!![i]
                    val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                    arrivalTime.companyCode = C.PROVIDER.KMB
                    arrivalTime.capacity = when(kmbEta.ol?.toLowerCase()) {
                        "f" -> 10
                        "e" -> 0
                        "n" -> -1
                        else -> kmbEta.ol?.toLong()?:-1
                    }
                    arrivalTime.expire = kmbEta.expire?:""
                    arrivalTime.isSchedule = !kmbEta.schedule.isNullOrEmpty() && kmbEta.schedule == "Y"
//                    arrivalTime.hasWheelchair = !kmbEta.wheelchair.isNullOrEmpty() && kmbEta.wheelchair == "Y"
//                    arrivalTime.hasWifi = !kmbEta.wifi.isNullOrEmpty() && kmbEta.wifi == "Y"
                    arrivalTime.hasWheelchair = !kmbEta.wifi.isNullOrEmpty() && kmbEta.wifi == "Y"
                    arrivalTime.text = Jsoup.parse(kmbEta.time).text().replace("　".toRegex(), " ")
                            .replace(" ?預定班次".toRegex(), "").replace(" ?時段班次".toRegex(), "")
                            .replace(" ?Scheduled".toRegex(), "")
                    arrivalTime.generatedAt = res.generated?:0
                    arrivalTime.updatedAt = timeNow
                    // arrivalTime = ArrivalTime.estimate(applicationContext, arrivalTime)

                    arrivalTime.routeNo = routeStop.routeNo?:""
                    arrivalTime.routeSeq = routeStop.routeSequence?:""
                    arrivalTime.stopId = routeStop.stopId?:""
                    arrivalTime.stopSeq = routeStop.sequence?:""
                    arrivalTime.order = i.toString()
                    arrivalTimeList.add(arrivalTime)
                }
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                return Result.success(outputData)
            }
        } catch (ignored: Throwable) {

        }

        return Result.failure(outputData)
    }
}