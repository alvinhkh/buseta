package com.alvinhkh.buseta.datagovhk

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.datagovhk.model.NwstEta
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
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
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

        val routeStopList = routeDatabase.routeStopDao().getRtNwstStop(companyCode, routeNo, stopId, routeSequence, stopSequence)
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
            if (body == null || body.data?.size == 0) {
                message(context.getString(R.string.message_no_data), routeStop, timeNow)
                return Result.failure(outputData)
            }

            var pos = 0
            val ignoreEtaUpdate = checkIgnoreEtaUpdate(routeStop, body.data)
            body.data?.forEachIndexed { index, eta ->
                // Because of unreliable eta data, try to ignore update with eta.eta if
                // (1) eta belongs to either of two extreme bus stops, and
                // (2) eta belongs to the route in opposite direction
                if (ignoreEtaUpdate[index]) {
                    return@forEachIndexed
                }

                val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                arrivalTime.companyCode = C.PROVIDER.NWST
                if (eta.companyCode == C.PROVIDER.CTB || eta.companyCode == C.PROVIDER.NWFB) {
                    arrivalTime.companyCode = eta.companyCode
                }
                try {
                    val etaDate = isoDateFormat.parse(eta.eta)
                    if (etaDate != null) {
                        arrivalTime.text = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(etaDate)
                    }
                    arrivalTime.isoTime = eta.eta
                } catch (ee: ParseException) {
                    arrivalTime.text = eta.eta
                }
                if (eta.remarkTc.startsWith("九巴")) {
                    if (arrivalTime.text.isNotEmpty()) {
                        arrivalTime.text += " "
                    }
                    arrivalTime.text += eta.remarkTc
                } else {
                    arrivalTime.note = eta.remarkTc
                }
                if (checkRouteDirectionByDestination(routeStop, eta)) {
                    if (arrivalTime.note.isNotEmpty()) {
                        arrivalTime.note += " "
                    }
                    arrivalTime.note += eta.destinationTc
                }
//                arrivalTime.isSchedule = false
                arrivalTime.generatedAt = (isoDateFormat.parse(eta.dataTimestamp)?:Date()).time
                arrivalTime.companyCode = routeStop.companyCode?:""
                arrivalTime.routeNo = routeStop.routeNo?:""
                arrivalTime.routeSeq = routeStop.routeSequence?:""
                arrivalTime.stopId = routeStop.stopId?:""
                arrivalTime.stopSeq = routeStop.sequence?:""
                arrivalTime.order = pos.toString()

                arrivalTime.updatedAt = timeNow
                arrivalTimeList.add(pos, arrivalTime)
                pos++

             }
            // To avoid blank item_route_stop if arrivalTimeList is empty.
            // This is not rare especially in overnight citybus, which usually has loose schedule.
            if (arrivalTimeList.size > 0) {
                if (arrivalTimeList.size < 3) {
                    // In this case, first clean up the database so as to remove any possible wrong outdated eta
                    message("", routeStop, timeNow)
                }
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
                return Result.success(outputData)
            }
        } catch (e: Throwable) {
            Timber.d(e)
        }

        message(context.getString(R.string.message_no_data), routeStop, timeNow)
        return Result.failure(outputData)
    }

    // Try to detect if eta information is irrelevant
    private fun checkIgnoreEtaUpdate(routeStop: RouteStop, etaData: List<NwstEta>?): BooleanArray {
        val size = etaData?.size?:0

        // First check if routeStop belongs to the first or last bus stop,
        // which are marked FIRST_STOP or LAST_STOP respectively.
        val description = routeStop.description?:""
        if (description.isEmpty()) {
            return BooleanArray(size) // return all-false array to continue eta update
        }

        // Check route direction. As there is an exceptional case (no. N170),
        // where routeStop.routeDestination does not match with eta.destinationTc,
        // consider both routeStop.routeDestination and routeStop.routeOrigin to detect correct direction
        val checkRouteDestination = BooleanArray(size)
        val checkRouteOrigin = BooleanArray(size)
        etaData?.forEachIndexed { index, eta ->
            checkRouteDestination[index] = checkRouteDirectionByDestination(routeStop, eta)
            checkRouteOrigin[index] = checkRouteDirectionByOrigin(routeStop, eta)
        }

        // Assumption: At two extreme bus stops, it is more likely that eta has 3 sets of eta data
        // containing a mixture of inbound's and outbound's eta.eta
        // Not interested in all-true or all-false results, which can be observed in exceptional cases
        if (!checkRouteOrigin.all{it} && !checkRouteOrigin.all{!it}) {
            return checkRouteOrigin
        } else if (!checkRouteDestination.all{it} && !checkRouteDestination.all{!it}) {
            return checkRouteDestination
        }

        if ("FIRST_STOP".equals(description)) {
            return checkRouteOrigin
        } else {
            return checkRouteDestination
        }
    }

    private fun checkRouteDirectionByDestination(routeStop: RouteStop, eta: NwstEta): Boolean {
        val routeDestination = (routeStop.routeDestination?:"").replace(" ", "")
        val destinationTc = eta.destinationTc.replace(" ", "")
        if (routeDestination.isEmpty() || destinationTc.isEmpty()) {
            return false
        }
        return routeDestination != destinationTc
    }

    private fun checkRouteDirectionByOrigin(routeStop: RouteStop, eta: NwstEta): Boolean {
        val routeOrigin = (routeStop.routeOrigin?:"").replace(" ", "")
        val destinationTc = eta.destinationTc.replace(" ", "")
        if (routeOrigin.isEmpty() || destinationTc.isEmpty()) {
            return false
        }
        return routeOrigin == destinationTc
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