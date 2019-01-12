package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.nwst.NwstService.*
import com.alvinhkh.buseta.nwst.model.NwstLatLong
import com.alvinhkh.buseta.nwst.model.NwstStop
import com.alvinhkh.buseta.nwst.model.NwstVariant
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.LatLong
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import timber.log.Timber
import java.util.ArrayList

class NwstStopListWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:return Result.failure()
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:""
        var outputData: Data

//        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
//        val tk = preferences.getString("nwst_tk", "")
//        val syscode3 = preferences.getString("nwst_syscode3", "")
        val qInfo = paramInfo(companyCode, routeSequence, routeId)
        if (qInfo.isNullOrEmpty()) {
            return Result.failure()
        }
        try {
            val syscode = NwstRequestUtil.syscode()
            val syscode2 = NwstRequestUtil.syscode2()
            val response = nwstService.ppStopList(qInfo, LANGUAGE_TC,
                    syscode, PLATFORM, APP_VERSION, syscode2).execute()
            if (!response.isSuccessful) {
                return Result.failure()
            }

            val stopList = ArrayList<RouteStop>()
            val timeNow = System.currentTimeMillis() / 1000

            val res = response.body()
            val route = routeDatabase?.routeDao()?.get(companyCode, routeId, routeNo, routeSequence, routeServiceType)?: Route()
            if (route.stopsStartSequence == null) {
                return Result.failure()
            }

            val routeStrArray = res?.string()?.split("<br>".toRegex())?.toTypedArray()?: emptyArray()
            var i = route.stopsStartSequence!!
            for (routeStr in routeStrArray) {
                val text = routeStr.replace("<br>", "").trim { it <= ' ' }
                if (text.isEmpty()) continue
                val nwstStop = NwstStop.fromString(text)
                if (nwstStop != null) {
                    val routeStop = RouteStop()
                    routeStop.stopId = nwstStop.stopId
                    routeStop.companyCode = route.companyCode
                    routeStop.routeDestination = route.destination
                    routeStop.routeSequence = route.sequence
                    routeStop.fareFull = nwstStop.adultFare.toString()
                    routeStop.fareChild = nwstStop.childFare.toString()
                    routeStop.fareSenior = nwstStop.seniorFare.toString()
                    routeStop.latitude = nwstStop.latitude.toString()
                    routeStop.longitude = nwstStop.longitude.toString()
                    routeStop.name = if (nwstStop.stopName.isEmpty()) nwstStop.stopName else
                        nwstStop.stopName.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                    routeStop.routeOrigin = route.origin
                    routeStop.routeNo = route.name
                    routeStop.routeId = route.code
                    routeStop.routeServiceType = route.serviceType
                    routeStop.sequence = nwstStop.sequence.toString()
                    routeStop.etaGet = nwstStop.isEta.toString()
                    routeStop.description = route.description
                    if (!nwstStop.poleId.isEmpty()) {
                        routeStop.imageUrl = "http://mobile.nwstbus.com.hk/api6/getstopphoto.php?filename=w" + nwstStop.poleId + "001.jpg&syscode=" + NwstRequestUtil.syscode()
                    }

                    routeStop.sequence = i.toString()
                    routeStop.lastUpdate = timeNow
                    stopList.add(routeStop)
                    i += 1
                }
            }

            val insertedList = routeDatabase?.routeStopDao()?.insert(stopList)
            if (insertedList?.size?:0 > 0) {
                routeDatabase?.routeStopDao()?.delete(companyCode, routeId, routeNo, routeSequence, routeServiceType, timeNow)
            }

            outputData = Data.Builder()
                    .putString(C.EXTRA.COMPANY_CODE, companyCode)
                    .putString(C.EXTRA.ROUTE_ID, routeId)
                    .putString(C.EXTRA.ROUTE_NO, routeNo)
                    .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                    .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                    .build()
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure()
        }

        try {
            var hasCoordinates = false
            val mapCoordinates: MutableList<LatLong> = arrayListOf()

            val variant = NwstVariant.parseInfo(routeId)
            val syscode = NwstRequestUtil.syscode()
            val syscode2 = NwstRequestUtil.syscode2()
            val response = nwstService.lineMulti2(variant?.rdv, LANGUAGE_TC,
                    syscode, PLATFORM, APP_VERSION, syscode2).execute()
            if (response.isSuccessful) {
                hasCoordinates = true
                val res = response.body()
                val data = res?.string()?.split("\\|\\*\\|".toRegex())?.
                        dropLastWhile { it.isEmpty() }?.toTypedArray()?: emptyArray()
                for (text in data) {
                    if (text.isEmpty()) continue
                    val nwstLatLong = NwstLatLong.fromString(text)
                    if (nwstLatLong != null && nwstLatLong.path.isNotEmpty()) {
                        for ((first, second) in nwstLatLong.path) {
                            mapCoordinates.add(LatLong(first, second))
                        }
                    }
                }
            }

            if (hasCoordinates) {
                routeDatabase?.routeDao()?.updateCoordinates(companyCode, routeId, routeNo, routeSequence, routeServiceType, mapCoordinates)
            } else {
                routeDatabase?.routeDao()?.deleteCoordinates(companyCode, routeId, routeNo, routeSequence, routeServiceType)
            }
        } catch (e: Exception) {
            Timber.d(e)
        }

        try {
            if (!routeId.isEmpty()) {
                val temp = routeId.substring(1).split("\\*{3}".toRegex()).
                        dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temp.size >= 4) {
                    val rdv = temp[0] + "||" + temp[1] + "||" + temp[2] + "||" + temp[3]
                    val syscode = NwstRequestUtil.syscode()
                    val syscode2 = NwstRequestUtil.syscode2()
                    val response = nwstService.timetable(rdv, routeSequence,
                            LANGUAGE_TC, syscode, PLATFORM, APP_VERSION, syscode2).execute()
                    val res = response.body()
                    val timetableHtml = res?.string()?:""

                    outputData = Data.Builder()
                            .putString(C.EXTRA.COMPANY_CODE, companyCode)
                            .putString(C.EXTRA.ROUTE_ID, routeId)
                            .putString(C.EXTRA.ROUTE_NO, routeNo)
                            .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                            .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                            .putString(C.EXTRA.ROUTE_TIMETABLE_HTML, timetableHtml)
                            .build()
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
        }

        return Result.success(outputData)
    }


    private fun paramInfo(companyCode: String?, sequence: String?, infoKey: String?): String? {
        if (infoKey.isNullOrEmpty()) {
            return null
        }
        val (_, _, rdv, _, _, _, _, _, _, startSequence, endSequence) = NwstVariant.parseInfo(infoKey)
                ?: return null
        return "1|*|$companyCode||$rdv||$startSequence||$endSequence||$sequence"
    }
}