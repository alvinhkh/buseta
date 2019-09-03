package com.alvinhkh.buseta.lwb

import android.content.Context
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase

class LwbRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val lwbService = LwbService.retrofit.create(LwbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val loadStop = inputData.getBoolean(C.EXTRA.LOAD_STOP, false)
        val routeStopListTag = inputData.getString(C.EXTRA.TAG)?: "RouteStopList"
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .build()

        val response = lwbService.routeBound(routeNo, Math.random()).execute()
        if (!response.isSuccessful) {
            return Result.failure(outputData)
        }

        val routeList = arrayListOf<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        val res = response.body()
        if (res?.bus_arr != null) {
            var i = 1
            for (bound in res.bus_arr) {
                if (bound == null) continue
                val route = Route()
                route.companyCode = C.PROVIDER.KMB
                route.origin = bound.origin_tc
                route.destination = bound.destination_tc
                route.name = routeNo
                route.sequence = i++.toString()
                route.serviceType = "01"
                route.lastUpdate = timeNow
                routeList.add(route)
            }
        }

        val insertedList = routeDatabase?.routeDao()?.insert(routeList)
        if (insertedList?.size?:0 > 0) {
            routeDatabase?.routeDao()?.delete(companyCode, routeNo, timeNow)
        }

        if (loadStop) {
            val requests = arrayListOf<OneTimeWorkRequest>()
            routeList.forEach { route ->
                val data = Data.Builder()
                        .putString(C.EXTRA.COMPANY_CODE, route.companyCode)
                        .putString(C.EXTRA.ROUTE_ID, route.code)
                        .putString(C.EXTRA.ROUTE_NO, route.name)
                        .putString(C.EXTRA.ROUTE_SEQUENCE, route.sequence)
                        .putString(C.EXTRA.ROUTE_SERVICE_TYPE, route.serviceType)
                        .build()
                requests.add(OneTimeWorkRequest.Builder(LwbStopListWorker::class.java)
                        .setInputData(data).addTag(routeStopListTag).build())
            }
            WorkManager.getInstance().enqueue(requests)
        }

        return Result.success(outputData)
    }
}