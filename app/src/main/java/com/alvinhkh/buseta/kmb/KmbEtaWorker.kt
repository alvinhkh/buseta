package com.alvinhkh.buseta.kmb

import android.content.Context
import android.util.Base64
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.route.dao.RouteDatabase
import org.jsoup.Jsoup
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class KmbEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.webSearch.create(KmbService::class.java)

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
//            val response = kmbService.eta(routeStop.routeNo, routeStop.routeSequence,
//                    routeStop.stopId, routeStop.sequence, routeStop.routeServiceType, "tc", "").execute()
            val now = Calendar.getInstance()
            val unixTime = now.timeInMillis.toString().dropLast(3) + "080"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.80.", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val t = sdf.format(now.time)
            val key = "--31${t}13--"
            val rawToken = routeStop.routeNo + key + routeStop.routeSequence + key + routeStop.routeServiceType + key + routeStop.stopId?.replace("-", "") + key + routeStop.sequence + key + unixTime;
            val token = "EA${Base64.encodeToString(rawToken.toByteArray(), Base64.DEFAULT)}"
            val response = kmbService.eta("1", token, t).execute()

            if (!response.isSuccessful) {
                Timber.d("%s", response)
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

            val kmbEtaRes = response.body()?.data
            if (kmbEtaRes == null) {
                Timber.d("%s", kmbEtaRes)
                if (!routeStop.routeNo.isNullOrEmpty() && !routeStop.stopId.isNullOrEmpty()
                        && !routeStop.sequence.isNullOrEmpty()) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!, routeStop.routeNo!!,
                            routeStop.routeSequence!!, routeStop.stopId!!, routeStop.sequence!!, timeNow)
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            if (kmbEtaRes.etas != null && kmbEtaRes.etas?.size?:0 > 0) {
                for (i in kmbEtaRes.etas!!.indices) {
                    val kmbEta = kmbEtaRes.etas!![i]
                    val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                    arrivalTime.companyCode = C.PROVIDER.KMB
                    arrivalTime.capacity = when(kmbEta.ol?.toLowerCase(Locale.ENGLISH)) {
                        "f" -> 10
                        "e" -> 0
                        "n" -> -1
                        else -> kmbEta.ol?.toLong()?:-1
                    }
                    arrivalTime.expire = kmbEta.expire?:""
                    if (!kmbEta.distanceM.isNullOrEmpty()) {
                        arrivalTime.distanceKM = (kmbEta.distanceM?.toDoubleOrNull()?:-1.0) / 1000.0
                        arrivalTime.isSchedule = arrivalTime.distanceKM < 0
                    } else {
                        arrivalTime.isSchedule = !kmbEta.eot.isNullOrEmpty() && kmbEta.eot == "T"
                    }
//                    arrivalTime.hasWheelchair = !kmbEta.wheelchair.isNullOrEmpty() && kmbEta.wheelchair == "Y"
//                    arrivalTime.hasWifi = !kmbEta.wifi.isNullOrEmpty() && kmbEta.wifi == "Y"
                    arrivalTime.hasWheelchair = !kmbEta.wifi.isNullOrEmpty() && kmbEta.wifi == "Y"
                    arrivalTime.text = Jsoup.parse(kmbEta.time).text().replace("　".toRegex(), " ")
                            .replace(" ?預定班次".toRegex(), "").replace(" ?時段班次".toRegex(), "")
                            .replace(" ?Scheduled".toRegex(), "")
                    arrivalTime.generatedAt = kmbEtaRes.generated?:0
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
        } catch (e: Throwable) {
            Timber.d(e)
        }

        return Result.failure(outputData)
    }
}