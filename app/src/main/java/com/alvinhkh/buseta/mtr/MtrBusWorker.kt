package com.alvinhkh.buseta.mtr

import android.content.Context
import android.content.Intent
import android.util.SparseArray
import android.util.SparseIntArray
import androidx.core.util.set
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.mtr.model.MtrBusFare
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.utils.DatabaseUtil
import java.lang.Exception
import kotlin.collections.HashMap

class MtrBusWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.LRTFEEDER
        val outputData = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val database = DatabaseUtil.getMtrBusDatabase(applicationContext)

        val routeMap = HashMap<String, Route>()
        val fareMap = SparseArray<MtrBusFare>()
        try {
            val busRouteLines = database.mtrBusDao().allRouteLines()
            val routeLines = SparseIntArray()
            for (busRouteLine in busRouteLines) {
                routeLines.put(busRouteLine.routeId, busRouteLine.routeLineId)
            }

            val mtrBusFares = database.mtrBusDao().allFares()
            for (mtrBusFare in mtrBusFares) {
                fareMap[routeLines.get(mtrBusFare.routeId)] = mtrBusFare
            }

            val mtrBusRoutes = database.mtrBusDao().allRoutes()
            for (mtrBusRoute in mtrBusRoutes) {
                val route = Route()
                val routeLineId = routeLines.get(mtrBusRoute.routeId).toString()
                route.dataSource = C.PROVIDER.LRTFEEDER
                route.companyCode = companyCode
                route.code = routeLineId
                route.name = mtrBusRoute.routeNumber
                route.origin = mtrBusRoute.descriptionZh
                route.description = mtrBusRoute.frequencyRemarkZh
                route.sequence = "0"
                route.lastUpdate = timeNow
                routeList.add(route)
                routeMap[routeLineId] = route
                suggestionList.add(Suggestion(0, companyCode, mtrBusRoute.routeNumber, 0, Suggestion.TYPE_DEFAULT))
            }

            suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCode, timeNow)
            if (suggestionList.size > 0) {
                suggestionDatabase?.suggestionDao()?.insert(suggestionList)
            }

            val stopMap = SparseIntArray()
            val mtrBusStops = database.mtrBusDao().allStops()
            for (mtrBusStop in mtrBusStops) {
                val routeStop = RouteStop()
                routeStop.stopId = mtrBusStop.refId.toString()
                routeStop.fareFull = "0"
                routeStop.latitude = mtrBusStop.latitude
                routeStop.longitude = mtrBusStop.longitude
                routeStop.name = mtrBusStop.nameCh
                routeStop.etaGet = ""
                routeStop.imageUrl = ""

                val route = routeMap[mtrBusStop.routeLineId.toString()]?: continue
                routeStop.companyCode = route.companyCode
                routeStop.routeDestination = route.destination
                routeStop.routeSequence = route.sequence
                routeStop.routeOrigin = route.origin
                routeStop.routeNo = route.name
                routeStop.routeId = route.code
                routeStop.routeServiceType = route.serviceType

                routeStop.fareFull = fareMap[route.code?.toInt()?:0]?.cashAdult.toString()
                routeStop.fareChild = fareMap[route.code?.toInt()?:0]?.cashChild.toString()
                routeStop.fareSenior = fareMap[route.code?.toInt()?:0]?.cashSenior.toString()

                routeStop.sequence = stopMap[mtrBusStop.routeLineId].toString()
                routeStop.lastUpdate = timeNow
                stopList.add(routeStop)
                stopMap[mtrBusStop.routeLineId] = stopMap.get(mtrBusStop.routeLineId) + 1
            }
        } catch (e: Exception) {
            return Result.failure(outputData)
        }

        val count = routeDatabase?.routeDao()?.insert(routeList)?.size?:0
        if (count > 0) {
            routeDatabase?.routeDao()?.delete(companyCode, timeNow)
        }

        if (routeDatabase?.routeStopDao()?.insert(stopList)?.size?:0 > 0) {
            routeDatabase?.routeStopDao()?.delete(companyCode, timeNow)
        }

        return Result.success(outputData)
    }
}