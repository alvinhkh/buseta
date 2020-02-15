package com.alvinhkh.buseta.datagovhk

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.datagovhk.model.TdCompanyCode
import com.alvinhkh.buseta.datagovhk.model.TdRouteBus
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import timber.log.Timber

class TdWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.static.create(DataGovHkService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000
        val companyCodeList = arrayListOf<String>()

        val codeMap = hashMapOf<String, TdCompanyCode.CompanyCode>()
        try {
            val response = dataGovHkService.tdCompanyCode().execute()
            val res = response.body()?: TdCompanyCode()

            res.list.forEach { item ->
                codeMap[item.companyCode] = item
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        try {
            val response = dataGovHkService.tdRouteBus().execute()
            val res = response.body()?: TdRouteBus()
            res.routeList.forEach { tdRoute ->
                tdRoute.companyCode.split("+").forEach { companyCode ->
                    if (arrayOf(
                                    C.PROVIDER.KMB, C.PROVIDER.LWB,
                                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST,
//                                    C.PROVIDER.LRTFEEDER,
                                    C.PROVIDER.NLB
                            ).indexOf(companyCode) >= 0) {
                        val route = Route()
                        route.dataSource = C.PROVIDER.DATAGOVHK
                        route.companyCode = when(companyCode) {
                            C.PROVIDER.LWB -> C.PROVIDER.KMB
                            else -> companyCode
                        }
                        route.origin = tdRoute.locationStartNameTc
                        route.destination = tdRoute.locationEndNameTc
                        route.name = tdRoute.routeNameTc
                        route.sequence = tdRoute.serviceMode
                        route.serviceType = tdRoute.specialType
                        route.code = tdRoute.routeId
                        route.hyperlink = tdRoute.hyperlinkTc
                        route.lastUpdate = timeNow
                        routeList.add(route)

                        suggestionList.add(Suggestion(0, route.companyCode!!, route.name!!, 0, Suggestion.TYPE_DEFAULT))
                        if (!companyCodeList.contains(companyCode)) {
                            companyCodeList.add(companyCode)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCodeList, timeNow)
        if (suggestionList.size > 0) {
            suggestionDatabase?.suggestionDao()?.insert(suggestionList)
        }

        val insertedList = routeDatabase?.routeDao()?.insert(routeList)
        if (insertedList?.size?:0 > 0) {
            routeDatabase?.routeDao()?.deleteBySource(arrayListOf(C.PROVIDER.DATAGOVHK), timeNow)
        }

//        val insertedStopList = routeDatabase?.routeStopDao()?.insert(stopList)
//        if (insertedStopList?.size?:0 > 0) {
//            routeDatabase?.routeStopDao()?.delete(companyCode, timeNow)
//        }

        return Result.success(outputData)
    }
}