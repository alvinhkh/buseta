package com.alvinhkh.buseta.nlb

import android.content.Context
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.nlb.model.NlbEtaRequest
import com.alvinhkh.buseta.route.dao.RouteDatabase
import org.jsoup.Jsoup

class NlbEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nlbService = NlbService.api.create(NlbService::class.java)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(context)!!

    private val routeDatabase = RouteDatabase.getInstance(context)!!

    override fun doWork(): Result {
        val widgetId = inputData.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = inputData.getInt(C.EXTRA.NOTIFICATION_ID, -1)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NLB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()
        val stopId = inputData.getString(C.EXTRA.STOP_ID)?:return Result.failure()
        val stopSequence = inputData.getString(C.EXTRA.STOP_SEQUENCE)?:return Result.failure()

        val outputData = Data.Builder()
                .putInt(C.EXTRA.WIDGET_UPDATE, widgetId)
                .putInt(C.EXTRA.NOTIFICATION_ID, notificationId)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                .putString(C.EXTRA.STOP_ID, stopId)
                .putString(C.EXTRA.STOP_SEQUENCE, stopSequence)
                .build()

        val routeStopList = routeDatabase.routeStopDao().get(companyCode, routeNo, routeSequence, routeServiceType, stopId, stopSequence)
        if (routeStopList.size < 1) {
            return Result.failure(outputData)
        }
        val routeStop = routeStopList[0]

        val arrivalTimeList = arrayListOf<ArrivalTime>()
        val timeNow = System.currentTimeMillis()

        try {
            val response = nlbService.eta(NlbEtaRequest(routeStop.routeSequence, routeStop.stopId, "zh")).execute()
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
            if (res?.estimatedArrivalTime == null || res.estimatedArrivalTime.html.isNullOrEmpty()) {
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            val doc = Jsoup.parse(res.estimatedArrivalTime.html)
            val divs = doc.body().getElementsByTag("div")
            if (divs != null && divs.size > 0) {
                var s = divs.size
                if (s > 1) {
                    s -= 1
                }
                for (i in 0 until s) {
                    val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                    arrivalTime.companyCode = C.PROVIDER.NLB
                    val text = divs[i].text()
                    arrivalTime.text = Jsoup.parse(text).text().replace("　".toRegex(), " ")
                            .replace(" ?預計時間".toRegex(), "")
                            .replace(" ?Estimated time".toRegex(), "")
                            .replace(" ?預定班次".toRegex(), "")
                            .replace(" ?Scheduled".toRegex(), "")
                            .replace("此路線於未來([0-9]+)分鐘沒有班次途經本站".toRegex(), "$1分鐘+")
                            .replace("This route has no departure via this stop in next ([0-9]+) mins".toRegex(), "$1 mins+")
                            .replace("此路線的巴士預計抵站時間查詢服務將於稍後推出".toRegex(), "此路線未有預計抵站時間服務")
                    arrivalTime.isSchedule = text.isNotEmpty() && (text.contains("預定班次") || text.contains("Scheduled"))
                    arrivalTime.hasWheelchair = divs[i].getElementsByAttributeValueContaining("alt", "Wheelchair").size > 0 || divs[i].getElementsByAttributeValueContaining("alt", "輪椅").size > 0

                    arrivalTime.order = i.toString()
                    arrivalTime.routeNo = routeStop.routeNo?:""
                    arrivalTime.routeSeq = routeStop.routeSequence?:""
                    arrivalTime.stopId = routeStop.stopId?:""
                    arrivalTime.stopSeq = routeStop.sequence?:""

                    arrivalTimeList.add(arrivalTime)
                }
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
                return Result.success(outputData)
            }
        } catch (ignored: Throwable) {

        }

        return Result.failure(outputData)
    }
}