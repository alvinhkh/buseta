package com.alvinhkh.buseta.nwst

import android.content.Context
import android.support.v7.preference.PreferenceManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.nwst.model.NwstEta
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.HashUtil
import org.jsoup.Jsoup
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

class NwstEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(context)!!

    private val routeDatabase = RouteDatabase.getInstance(context)!!

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        val widgetId = inputData.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = inputData.getInt(C.EXTRA.NOTIFICATION_ID, -1)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
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

        val routeStop = routeDatabase.routeStopDao().get(companyCode, routeNo, routeSequence, routeServiceType, stopId, stopSequence)
                ?: return Result.failure(outputData)
        
        try {
//            val randomHex64 = HashUtil.randomHexString(64)
//            val tk = randomHex64 // preferences.getString("nwst_tk", randomHex64)
//            val syscode3 = preferences.getString("nwst_syscode3", "")
//            nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "Y", NwstService.DEVICETYPE,
//                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
//                    NwstRequestUtil.syscode2()).execute()
//            nwstService.pushToken(tk, tk, NwstService.LANGUAGE_TC, "R", NwstService.DEVICETYPE,
//                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
//                    NwstRequestUtil.syscode2()).execute()
//            nwstService.adv(NwstService.LANGUAGE_TC, NwstService.DEVICETYPE,
//                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
//                    NwstRequestUtil.syscode2()).execute()
//            val editor = preferences.edit()
//            editor.putString("nwst_tk", tk)
//            editor.apply()

            val response = nwstService.eta((routeStop.stopId?:"0").toInt().toString(),
                    routeStop.routeNo, "Y", "60", NwstService.LANGUAGE_TC, routeStop.routeSequence,
                    routeStop.sequence, routeStop.routeId, "Y", "Y",
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2, NwstRequestUtil.syscode2()).execute()
            if (!response.isSuccessful) {
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = context.getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            val arrivalTimeList = arrayListOf<ArrivalTime>()
            val timeNow = System.currentTimeMillis()

            val res = response.body()
            if (res == null || res.contentLength() < 1) {
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            val text = res.string()
            val serverTime = text.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' }
            val data = text.trim { it <= ' ' }.replaceFirst("^[^|]*\\|##\\|".toRegex(), "").split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in data.indices) {
                val nwstEta = NwstEta.fromString(data[i]) ?: continue
                nwstEta.serverTime = serverTime.replace("[^0-9:]".toRegex(), "")

                val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                arrivalTime.companyCode = C.PROVIDER.NWST
                if (nwstEta.companyCode == C.PROVIDER.CTB || nwstEta.companyCode == C.PROVIDER.NWFB) {
                    arrivalTime.companyCode = nwstEta.companyCode
                }
                arrivalTime.text = text(nwstEta.title)
                val subtitle = text(nwstEta.subtitle)
                if (!subtitle.isEmpty()) {
                    if (subtitle.contains("距離") || subtitle.contains("距离") || subtitle.contains("Distance")) {
                        arrivalTime.distanceKM = parseDistance(subtitle).toDouble()
                    }
                    if (arrivalTime.distanceKM < 0) {
                        arrivalTime.text = arrivalTime.text + " " + subtitle
                    }
                }
                arrivalTime.note = nwstEta.boundText.trim { it <= ' ' }
                arrivalTime.isoTime = nwstEta.etaIsoTime
                arrivalTime.isSchedule = !nwstEta.subtitle.isEmpty() && (nwstEta.subtitle.contains("預定班次") || nwstEta.subtitle.contains("预定班次") || nwstEta.subtitle.contains("Scheduled"))
                val data1 = nwstEta.serverTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (data1.size == 3) {
                    try {
                        val c = Calendar.getInstance()
                        c.set(Calendar.HOUR, data1[0].toInt())
                        c.set(Calendar.MINUTE, data1[1].toInt())
                        c.set(Calendar.SECOND, data1[2].toInt())
                        arrivalTime.generatedAt = c.time.time
                    } catch (ignored: NumberFormatException) {
                    }

                }
                // arrivalTime = ArrivalTime.estimate(context, arrivalTime)
                
                arrivalTime.companyCode = routeStop.companyCode?:""
                arrivalTime.routeNo = routeStop.routeNo?:""
                arrivalTime.routeSeq = routeStop.routeSequence?:""
                arrivalTime.stopId = routeStop.stopId?:""
                arrivalTime.stopSeq = routeStop.sequence?:""
                arrivalTime.order = i.toString()

                arrivalTime.updatedAt = timeNow
                arrivalTimeList.add(arrivalTime)
            }
            arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        return Result.failure(outputData)
    }

    fun text(text: String): String {
        return Jsoup.parse(text).text().replace("　".toRegex(), " ")
                .replace(" ?預定班次".toRegex(), "")
                .replace(" ?预定班次".toRegex(), "")
                .replace(" ?Scheduled".toRegex(), "")
                .replace("預計未來([0-9]+)分鐘沒有抵站班次或服務時間已過".toRegex(), "$1分鐘+/已過服務時間")
                .replace("预计未来([0-9]+)分钟没有抵站班次或服务时间已过".toRegex(), "$1分钟+/已过服务时间")
                .replace("No departure estimated in the next ([0-9]+) min or outside service hours".toRegex(), "$1 mins+/outside service hours")
                .replace("。$".toRegex(), "").replace("\\.$".toRegex(), "")
                .replace("往: ".toRegex(), "往")
                .replace(" ?新巴".toRegex(), "")
                .replace(" ?城巴".toRegex(), "")
    }

    private fun parseDistance(text: String): Float {
        if (text.isEmpty()) return -1.0f
        val matcher = Pattern.compile("[距離|距离|Distance]: (\\d*\\.?\\d*)").matcher(text)
        if (matcher.find()) {
            var distanceKM = matcher.group(1).toFloat()
            if (distanceKM < 0.0f) {
                distanceKM = -1.0f
            } else if (distanceKM > 10000.0f) {
                // filter out extreme value
                distanceKM = -1.0f
            }
            return distanceKM
        }
        return -1.0f
    }
}