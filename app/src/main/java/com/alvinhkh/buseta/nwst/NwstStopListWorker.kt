package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.nwst.model.NwstLatLong
import com.alvinhkh.buseta.nwst.model.NwstStop
import com.alvinhkh.buseta.nwst.model.NwstVariant
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.LatLong
import com.alvinhkh.buseta.route.model.RouteStop
import timber.log.Timber

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
        val outputData = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_ID, routeId)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                .build()
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val qInfo = paramInfo(routeId)
        if (routeId.isEmpty() || qInfo.isNullOrEmpty()) {
            return Result.failure(outputData)
        }
        try {
            val sysCode5 = preferences.getString("nwst_syscode5", "")?:""
            val appId = preferences.getString("nwst_appId", "")?:""
            val version = preferences.getString("nwst_version", NwstService.APP_VERSION)?:NwstService.APP_VERSION
            val version2 = preferences.getString("nwst_version2", NwstService.APP_VERSION2)?:NwstService.APP_VERSION2
            val response = nwstService.ppStopList(qInfo, NwstService.LANGUAGE_TC,
                    sysCode5, "Y", NwstService.PLATFORM, version, version2, appId).execute()
            if (!response.isSuccessful) {
                Timber.d("%s", response.message())
                return Result.failure(outputData)
            }

            val stopList = arrayListOf<RouteStop>()
            val timeNow = System.currentTimeMillis() / 1000

            val res = response.body()
            if (res.isNullOrEmpty() || res.length < 10) {
                return Result.failure(outputData)
            }

            val route = routeDatabase?.routeDao()?.get(companyCode, routeId, routeNo, routeSequence, routeServiceType)?: return Result.failure(outputData)
            val stopsStartSequence = route.stopsStartSequence
            if (stopsStartSequence == null || stopsStartSequence <= 0) {
                return Result.failure(outputData)
            }

            val routeStrArray = res.split("<br>".toRegex()).toTypedArray()
            var i = stopsStartSequence
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
                    if (nwstStop.poleId.isNotEmpty()) {
                        routeStop.imageUrl = "https://mobile.nwstbus.com.hk/api6/getstopphoto.php?filename=w" + nwstStop.poleId + "001.jpg&syscode=" + NwstRequestUtil.syscode()
                    }

                    routeStop.sequence = i.toString()
                    routeStop.lastUpdate = timeNow
                    stopList.add(routeStop)
                    i += 1
                }
            }

            val insertedList = routeDatabase.routeStopDao().insert(stopList)
            if (insertedList.isNotEmpty()) {
                routeDatabase.routeStopDao().delete(companyCode, routeId, routeNo, routeSequence, routeServiceType, timeNow)
            }

        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        try {
            var hasCoordinates = false
            val mapCoordinates: MutableList<LatLong> = arrayListOf()

            val variant = NwstVariant.parseInfo(routeId)
            val syscode = NwstRequestUtil.syscode()
            val syscode2 = NwstRequestUtil.syscode2()
            val response = nwstService.lineMulti2(variant?.rdv?:"", NwstService.LANGUAGE_TC,
                    syscode, NwstService.PLATFORM, NwstService.APP_VERSION, syscode2).execute()
            if (response.isSuccessful) {
                hasCoordinates = true
                val data = response.body()?.split("\\|\\*\\|".toRegex())?.
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
                routeDatabase.routeDao().updateCoordinates(companyCode, routeId, routeNo, routeSequence, routeServiceType, mapCoordinates)
            } else {
                routeDatabase.routeDao().deleteCoordinates(companyCode, routeId, routeNo, routeSequence, routeServiceType)
            }
        } catch (e: Exception) {
            Timber.d(e)
        }

        return Result.success(outputData)
    }


    private fun paramInfo(infoKey: String?): String? {
        if (infoKey.isNullOrEmpty()) {
            return null
        }
        val variant = NwstVariant.parseInfo(infoKey)?: return null
        return "1|*|${variant.companyCode}||${variant.rdv}||${variant.startSequence}||${variant.endSequence}"
    }
}