package com.alvinhkh.buseta.nlb

import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.nlb.model.NlbRouteStop
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import timber.log.Timber
import java.util.TreeMap

class NlbWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nlbService = NlbService.api.create(NlbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NLB
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val response = nlbService.database().execute()
        if (!response.isSuccessful) {
            return Result.failure(outputData)
        }

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val database = response.body()

        for (nlbRoute in database?.routes?: emptyList()) {
            val route = Route()
            route.companyCode = companyCode
            val location = nlbRoute.route_name_c.split(" > ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            route.origin = location[0]
            if (location.size > 1) {
                route.destination = location[1]
            }
            route.name = nlbRoute.route_no
            route.sequence = nlbRoute.route_id
            route.code = nlbRoute.route_id
            route.lastUpdate = timeNow
            routeList.add(route)

            val map = HashMap<String, NlbRouteStop>()
            for (nlbRouteStop in database?.route_stops?: emptyList()) {
                if (nlbRouteStop.route_id != route.sequence) continue
                map[nlbRouteStop.stop_id] = nlbRouteStop
            }
            val map2 = TreeMap<Int, RouteStop>()
            for (nlbStop in database?.stops?: emptyList()) {
                if (!map.containsKey(nlbStop.stop_id)) continue
                val routeStop = RouteStop()
                routeStop.companyCode = C.PROVIDER.NLB
                routeStop.routeNo = route.name
                routeStop.routeId = map[nlbStop.stop_id]?.route_id
                routeStop.routeServiceType = route.serviceType
                routeStop.routeSequence = route.sequence
                routeStop.stopId = map[nlbStop.stop_id]?.stop_id
                routeStop.name = nlbStop.stop_name_c
                routeStop.fareFull = map[nlbStop.stop_id]?.fare
                routeStop.fareHoliday = map[nlbStop.stop_id]?.fare_holiday
                routeStop.latitude = nlbStop.latitude
                routeStop.longitude = nlbStop.longitude
                routeStop.location = nlbStop.stop_location_c
                routeStop.routeDestination = route.destination
                routeStop.routeOrigin = route.origin
                routeStop.lastUpdate = timeNow
                map2[map[nlbStop.stop_id]?.stop_sequence?.toInt()?:0] = routeStop
            }
            for ((i, stop) in map2.values.withIndex()) {
                stop.sequence = i.toString()
                stopList.add(stop)
            }

            suggestionList.add(Suggestion(0, companyCode, nlbRoute.route_no, 0, Suggestion.TYPE_DEFAULT))
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