package com.alvinhkh.buseta.datagovhk

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RtNwstEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.transport.create(DataGovHkService::class.java)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(context)!!

    private val routeDatabase = RouteDatabase.getInstance(context)!!

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

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

        if (routeNo.isEmpty() || stopId.isEmpty()) {
            return Result.failure(outputData)
        }

        val routeStopList = routeDatabase.routeStopDao().getByRouteNoStopId(companyCode, routeNo, stopId)
        if (routeStopList.size < 1) {
            arrivalTimeDatabase.arrivalTimeDao().clear(companyCode, routeNo, routeSequence, stopId, stopSequence, System.currentTimeMillis())
            return Result.failure(outputData)
        }
        val routeStop = routeStopList[0]

        val arrivalTimeList = arrayListOf<ArrivalTime>()
        val timeNow = System.currentTimeMillis()

        val response1 = dataGovHkService.nwstETA(companyCode, stopId, routeNo).execute()
        if (!response1.isSuccessful) {
            message(context.getString(R.string.message_fail_to_request), routeStop, timeNow)
            return Result.failure(outputData)
        }

        try {
            val body = response1.body()
            if (body == null) {
                message(context.getString(R.string.message_fail_to_request), routeStop, timeNow)
                return Result.failure(outputData)
            }

            body.data?.forEach { eta ->
                val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                arrivalTime.companyCode = C.PROVIDER.NWST
                if (eta.companyCode == C.PROVIDER.CTB || eta.companyCode == C.PROVIDER.NWFB) {
                    arrivalTime.companyCode = eta.companyCode
                }
                try {
                    arrivalTime.text = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(isoDateFormat.parse(eta.eta))
                    arrivalTime.isoTime = eta.eta
                } catch (ee: ParseException) {
                    arrivalTime.text = eta.eta
                }
                arrivalTime.note = eta.remarkTc
//                arrivalTime.isSchedule = false
                arrivalTime.generatedAt = isoDateFormat.parse(eta.dataTimestamp).time
                arrivalTime.companyCode = routeStop.companyCode?:""
                arrivalTime.routeNo = routeStop.routeNo?:""
                arrivalTime.routeSeq = routeStop.routeSequence?:""
                arrivalTime.stopId = routeStop.stopId?:""
                arrivalTime.stopSeq = routeStop.sequence?:""
                arrivalTime.order = (eta.etaSequence - 1).toString()

                arrivalTime.updatedAt = timeNow
                arrivalTimeList.add(arrivalTime)
            }
            arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
            return Result.success(outputData)
        } catch (e: Throwable) {
            Timber.d(e)
        }

        message(context.getString(R.string.message_no_data), routeStop, timeNow)
        return Result.failure(outputData)
    }

    private fun message(text: String, routeStop: RouteStop, timeNow: Long) {
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
        arrivalTime.text = text
        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
    }
}