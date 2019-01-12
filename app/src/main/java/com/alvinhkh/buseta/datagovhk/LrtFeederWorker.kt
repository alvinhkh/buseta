package com.alvinhkh.buseta.datagovhk

import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.model.MtrBusFare
import com.alvinhkh.buseta.datagovhk.model.MtrBusRoute
import com.alvinhkh.buseta.datagovhk.model.MtrBusStop
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import org.jsoup.parser.Parser
import timber.log.Timber

class LrtFeederWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.resource.create(DataGovHkService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.LRTFEEDER
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val mtrBusRouteMap = HashMap<String, Route>()
        val mtrBusFareMap = HashMap<String, MtrBusFare>()

        try {
            val response = dataGovHkService.mtrBusRoutes().execute()
            val res = response.body()

            val mtrBusRouteList = MtrBusRoute.fromCSV(res?.string()?:"")
            for (mtrBusRoute in mtrBusRouteList) {
                if (mtrBusRoute.routeId.isNullOrEmpty()) continue
                val route = Route()
                route.companyCode = C.PROVIDER.LRTFEEDER
                route.code = mtrBusRoute.routeId
                route.name = mtrBusRoute.routeId
                if (!mtrBusRoute.routeNameChi.isNullOrEmpty()) {
                    mtrBusRoute.routeNameChi = Parser.unescapeEntities(mtrBusRoute.routeNameChi!!, false)
                    val routeName = mtrBusRoute.routeNameChi!!.split("至".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (routeName.size == 2) {
                        route.destination = routeName[0]
                        route.origin = routeName[1]
                    } else {
                        route.origin = mtrBusRoute.routeNameChi
                    }
                }
                route.sequence = "0"
                route.lastUpdate = timeNow
                routeList.add(route)
                mtrBusRouteMap[mtrBusRoute.routeId!!] = route

                suggestionList.add(Suggestion(0, companyCode, mtrBusRoute.routeId!!, 0, Suggestion.TYPE_DEFAULT))
            }
        } catch (e: Throwable) {
            return Result.failure(outputData)
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
        
        try {
            val response = dataGovHkService.mtrBusFares().execute()
            val res = response.body()
            val mtrBusFareList = MtrBusFare.fromCSV(res?.string()?:"")
            for (mtrBusFare in mtrBusFareList) {
                if (mtrBusFare.routeId.isNullOrEmpty()) continue
                mtrBusFareMap[mtrBusFare.routeId!!] = mtrBusFare
            }
        } catch (e: Throwable) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        try {
            val response = dataGovHkService.mtrBusStops().execute()
            val res = response.body()
            val mtrBusStopList = MtrBusStop.fromCSV(res?.string()?:"")

            val stops = arrayListOf<RouteStop>()
            for (mtrBusStop in mtrBusStopList) {
                if (mtrBusStop.routeId.isNullOrEmpty()) continue
                val mtrBusFare = mtrBusFareMap[mtrBusStop.routeId!!]
                val route = mtrBusRouteMap[mtrBusStop.routeId!!]

                val routeStop = RouteStop()
                routeStop.stopId = mtrBusStop.stationSequenceNo.toString()
                routeStop.companyCode = route?.companyCode?:C.PROVIDER.LRTFEEDER
                routeStop.routeDestination = route?.destination?:""
                routeStop.routeSequence = route?.sequence?:"0"
                if (mtrBusFare != null) {
                    routeStop.fareFull = mtrBusFare.fareSingleAdult.toString()
                    routeStop.fareChild = mtrBusFare.fareSingleChild.toString()
                    routeStop.fareSenior = mtrBusFare.fareSingleElderly.toString()
                }
                if (!mtrBusStop.stationNameChi.isNullOrEmpty()) {
                    routeStop.name = Parser.unescapeEntities(mtrBusStop.stationNameChi!!, false)
                }
                routeStop.routeOrigin = route?.origin?:""
                routeStop.routeNo = route?.name?:mtrBusStop.routeId
                routeStop.routeId = route?.code?:mtrBusStop.routeId
                routeStop.routeServiceType = route?.serviceType?:""
                routeStop.sequence = mtrBusStop.stationSequenceNo.toString()
                routeStop.etaGet = ""
                routeStop.imageUrl = ""
                routeStop.lastUpdate = timeNow
                stops.add(routeStop)
            }
            for ((i, stop) in stops.withIndex()) {
                stop.sequence = i.toString()
                stopList.add(stop)
            }

            val insertedStopList = routeDatabase?.routeStopDao()?.insert(stopList)
            if (insertedStopList?.size?:0 > 0) {
                routeDatabase?.routeStopDao()?.delete(companyCode, timeNow)
            }
        } catch (e: Throwable) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        return Result.success(outputData)
    }
}