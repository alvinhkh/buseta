package com.alvinhkh.buseta.mtr

import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.utils.DatabaseUtil
import timber.log.Timber
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class AESBusWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.AESBUS
        val outputData = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val database = DatabaseUtil.getAESBusDatabase(applicationContext)

        val stopIds = HashMap<String, List<String>>()
        try {
            val aesBusDistricts = database.aesBusDao().allDistricts()
            val districts = HashMap<String, String>()
            for (aesBusDistrict in aesBusDistricts) {
                districts[aesBusDistrict.districtID] = aesBusDistrict.districtCn
            }

            val aesBusRoutes = database.aesBusDao().allRoutes()
            for ((busNumber, routeStr, serviceHours, _, _, _, districtID) in aesBusRoutes) {
                val route = Route()
                route.dataSource = C.PROVIDER.AESBUS
                route.companyCode = companyCode
                route.name = busNumber
                if (districtID > 0) {
                    route.origin = districts[districtID.toString()]
                }
                route.description = serviceHours
                route.sequence = "0"
                route.lastUpdate = timeNow
                routeList.add(route)

                stopIds[busNumber] = Arrays.asList(*routeStr.split("\\+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

                suggestionList.add(Suggestion(0, companyCode, busNumber, 0, Suggestion.TYPE_DEFAULT))
            }

            suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCode, timeNow)
            if (suggestionList.size > 0) {
                suggestionDatabase?.suggestionDao()?.insert(suggestionList)
                val i = Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE)
                i.putExtra(C.EXTRA.UPDATED, true)
                i.putExtra(C.EXTRA.MANUAL, manualUpdate)
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated)
                applicationContext.sendBroadcast(i)
            }
            Timber.d("%s: %s", companyCode, suggestionList.size)

            val aesBusStops = database.aesBusDao().allStops()
            val stopMap = HashMap<String, RouteStop>()
            for (aesBusStop in aesBusStops) {
                val routeStop = RouteStop()
                routeStop.stopId = aesBusStop.stopId
                routeStop.fareFull = "0"
                routeStop.latitude = aesBusStop.stopLatitude
                routeStop.longitude = aesBusStop.stopLongitude
                routeStop.name = aesBusStop.stopNameCn
                routeStop.etaGet = ""
                routeStop.imageUrl = ""
                stopMap[aesBusStop.stopId] = routeStop
            }
            for (route in routeList) {
                var i = 0
                for (stopId in stopIds[route.name]?: emptyList()) {
                    val routeStop = stopMap[stopId]?.copy()?:continue
                    routeStop.companyCode = route.companyCode
                    routeStop.routeDestination = route.destination
                    routeStop.routeSequence = route.sequence
                    routeStop.routeOrigin = route.origin
                    routeStop.routeNo = route.name
                    routeStop.routeId = route.code
                    routeStop.routeServiceType = route.serviceType

                    routeStop.sequence = i.toString()
                    routeStop.lastUpdate = timeNow
                    stopList.add(routeStop)
                    i += 1
                }
            }
        } catch (e: Exception) {
            return Result.failure(outputData)
        }

        val insertedList = routeDatabase?.routeDao()?.insert(routeList)
        if (insertedList?.size?:0 > 0) {
            routeDatabase?.routeDao()?.delete(companyCode, timeNow)
        }

        val insertedStopList = routeDatabase?.routeStopDao()?.insert(stopList)
        if (insertedStopList?.size?:0 > 0) {
            routeDatabase?.routeStopDao()?.delete(companyCode, timeNow)
        }

        return Result.success(outputData)
    }
}