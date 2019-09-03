package com.alvinhkh.buseta.datagovhk

import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.model.*
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import timber.log.Timber

class RtNwstWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.transport.create(DataGovHkService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:""
        val loadStop = inputData.getBoolean(C.EXTRA.LOAD_STOP, false)
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putBoolean(C.EXTRA.LOAD_STOP, loadStop)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val tempRouteList = arrayListOf<Route>()
        try {
            val responseList = arrayListOf<NwstRoute>()
            if (arrayListOf(NwstCompany.CTB, NwstCompany.NWFB).contains(companyCode)) {
                if (routeNo.isEmpty()) {
                    val response = dataGovHkService.nwstRouteList(companyCode).execute()
                    responseList.addAll(response.body()?.data?: emptyList())
                } else {
                    val response = dataGovHkService.nwstRoute(companyCode, routeNo).execute()
                    val data = response.body()?.data
                    if (data != null) {
                        responseList.add(data)
                    }
                }
            } else {
                if (routeNo.isEmpty()) {
                    val response1 = dataGovHkService.nwstRouteList(NwstCompany.CTB).execute()
                    val response2 = dataGovHkService.nwstRouteList(NwstCompany.NWFB).execute()
                    responseList.addAll(response1.body()?.data?: emptyList())
                    responseList.addAll(response2.body()?.data?: emptyList())
                } else {
                    val response1 = dataGovHkService.nwstRoute(NwstCompany.CTB, routeNo).execute()
                    val response2 = dataGovHkService.nwstRoute(NwstCompany.NWFB, routeNo).execute()
                    val data1 = response1.body()?.data
                    if (data1 != null) {
                        responseList.add(data1)
                    }
                    val data2 = response2.body()?.data
                    if (data2 != null) {
                        responseList.add(data2)
                    }
                }
            }
            responseList.forEach { nwstRoute ->
                val route = Route()
                route.dataSource = C.PROVIDER.DATAGOVHK_NWST
                route.companyCode = nwstRoute.companyCode
                route.origin = nwstRoute.originTc
                route.destination = nwstRoute.destinationTc
                route.name = nwstRoute.routeName
                route.sequence = "O"
//                route.serviceType = tdRoute.specialType
                route.code = nwstRoute.routeName
//                route.hyperlink = tdRoute.hyperlinkTc
                route.lastUpdate = timeNow
//                route.lastUpdate = nwstRoute.dataTimestamp
                tempRouteList.add(route)
                suggestionList.add(Suggestion(0, nwstRoute.companyCode, nwstRoute.routeName, 0, Suggestion.TYPE_DEFAULT))
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        try {
            val stopMap = hashMapOf<String, NwstStop>()
            for (route in tempRouteList) {
                val inboundResponse = dataGovHkService.nwstRouteStop(route.companyCode?:"", route.name?:"", "inbound").execute()
                val inboundData = inboundResponse.body()?.data
                routeList.add(route)
                val inboundRoute = route.copy()
                inboundRoute.sequence = "I"
                val origin = inboundRoute.origin
                inboundRoute.origin = inboundRoute.destination
                inboundRoute.destination = origin
                if (inboundData?.size?:0 > 0) {
                    routeList.add(inboundRoute)
                }
                if (loadStop) {
                    val outboundResponse = dataGovHkService.nwstRouteStop(route.companyCode?:"", route.name?:"", "outbound").execute()
                    val outboundData = outboundResponse.body()?.data
                    outboundData?.forEach { nwstRouteStop ->
                        val routeStop = RouteStop()
                        var nwstStop: NwstStop? = null
                        if (stopMap.containsKey(nwstRouteStop.stopId)) {
                            nwstStop = stopMap[nwstRouteStop.stopId]
                        }
                        if (nwstStop == null) {
                            val stopResponse = dataGovHkService.nwstStop(nwstRouteStop.stopId).execute()
                            nwstStop = stopResponse.body()?.data
                            if (nwstStop != null) {
                                stopMap[nwstRouteStop.stopId] = nwstStop!!
                            }
                        }
                        if (nwstStop != null) {
                            routeStop.companyCode = nwstRouteStop.companyCode
                            routeStop.description = ""
                            routeStop.latitude = nwstStop.latitude.toString()
                            routeStop.longitude = nwstStop.longitude.toString()
                            routeStop.name = nwstStop.nameTc
                            if (routeStop.name.isNullOrEmpty()) {
                                routeStop.name = nwstStop.nameEn
                            }
                            routeStop.routeDestination = route.destination
                            routeStop.routeId = route.code
                            routeStop.routeNo = route.name
                            routeStop.routeOrigin = route.origin
                            routeStop.routeSequence = route.sequence
                            routeStop.routeServiceType = route.serviceType
                            routeStop.sequence = nwstRouteStop.sequence.toString()
                            routeStop.stopId = nwstRouteStop.stopId
                            routeStop.lastUpdate = timeNow
//                        routeStop.lastUpdate = nwstRouteStop.dataTimestamp
                            stopList.add(routeStop)
                        }
                    }
                    inboundData?.forEach { nwstRouteStop ->
                        val routeStop = RouteStop()
                        var nwstStop: NwstStop? = null
                        if (stopMap.containsKey(nwstRouteStop.stopId)) {
                            nwstStop = stopMap[nwstRouteStop.stopId]
                        }
                        if (nwstStop == null) {
                            val stopResponse = dataGovHkService.nwstStop(nwstRouteStop.stopId).execute()
                            nwstStop = stopResponse.body()?.data
                            if (nwstStop != null) {
                                stopMap[nwstRouteStop.stopId] = nwstStop!!
                            }
                        }
                        if (nwstStop != null) {
                            routeStop.companyCode = nwstRouteStop.companyCode
                            routeStop.description = ""
                            routeStop.latitude = nwstStop?.latitude.toString()
                            routeStop.longitude = nwstStop?.longitude.toString()
                            routeStop.name = nwstStop?.nameTc
                            routeStop.routeDestination = inboundRoute.destination
                            routeStop.routeId = inboundRoute.code
                            routeStop.routeNo = inboundRoute.name
                            routeStop.routeOrigin = inboundRoute.origin
                            routeStop.routeSequence = inboundRoute.sequence
                            routeStop.routeServiceType = inboundRoute.serviceType
                            routeStop.sequence = nwstRouteStop.sequence.toString()
                            routeStop.stopId = nwstRouteStop.stopId
                            routeStop.lastUpdate = timeNow
//                        routeStop.lastUpdate = nwstRouteStop.dataTimestamp
                            stopList.add(routeStop)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        if (routeNo.isEmpty()) {
            suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCode, timeNow)
            if (suggestionList.size > 0) {
                suggestionDatabase?.suggestionDao()?.insert(suggestionList)
            }
            Timber.d("%s: %s", companyCode, suggestionList.size)
        }

        val insertedList = routeDatabase?.routeDao()?.insert(routeList)
        val c1 = insertedList?.size?:0
        if (c1 > 0) {
            if (routeNo.isNotEmpty()) {
                routeDatabase?.routeDao()?.delete(C.PROVIDER.NWST, routeNo, timeNow)
                routeDatabase?.routeDao()?.delete(C.PROVIDER.CTB, routeNo, timeNow)
                routeDatabase?.routeDao()?.delete(C.PROVIDER.NWFB, routeNo, timeNow)
            } else {
                routeDatabase?.routeDao()?.delete(C.PROVIDER.NWST, timeNow)
                routeDatabase?.routeDao()?.delete(C.PROVIDER.CTB, timeNow)
                routeDatabase?.routeDao()?.delete(C.PROVIDER.NWFB, timeNow)
            }
        }

        if (loadStop) {
            val insertedStopList = routeDatabase?.routeStopDao()?.insert(stopList)
            val c2 = insertedStopList?.size ?: 0
            if (c2 > 0) {
                if (routeNo.isNotEmpty()) {
                    routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWST, routeNo, timeNow)
                    routeDatabase?.routeStopDao()?.delete(C.PROVIDER.CTB, routeNo, timeNow)
                    routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWFB, routeNo, timeNow)
                } else {
                    routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWST, timeNow)
                    routeDatabase?.routeStopDao()?.delete(C.PROVIDER.CTB, timeNow)
                    routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWFB, timeNow)
                }
            }
        }

        return Result.success(outputData)
    }
}