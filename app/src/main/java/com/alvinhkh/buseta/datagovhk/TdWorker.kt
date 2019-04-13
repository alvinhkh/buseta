package com.alvinhkh.buseta.datagovhk

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.datagovhk.model.MtrLineName
import com.alvinhkh.buseta.datagovhk.model.MtrLineStation
import com.alvinhkh.buseta.datagovhk.model.TdCompanyCode
import com.alvinhkh.buseta.datagovhk.model.TdRouteBus
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.google.gson.Gson
import timber.log.Timber
import java.nio.charset.Charset

class TdWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.static.create(DataGovHkService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .build()

        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

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
                    if (arrayOf(C.PROVIDER.CTB, C.PROVIDER.KMB, C.PROVIDER.LRTFEEDER, C.PROVIDER.LWB,
                                    C.PROVIDER.NLB, C.PROVIDER.NWFB, C.PROVIDER.NWST).indexOf(companyCode) >= 0) {
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
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        val insertedList = routeDatabase?.routeDao()?.insert(routeList)
        if (insertedList?.size?:0 > 0) {
            routeDatabase?.routeDao()?.deleteBySource(C.PROVIDER.DATAGOVHK, timeNow)
        }

        return Result.success(outputData)
    }
}