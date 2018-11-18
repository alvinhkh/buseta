package com.alvinhkh.buseta.lwb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import java.util.ArrayList

class LwbRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val lwbService = LwbService.retrofit.create(LwbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.FAILURE

        val response = lwbService.routeBound(routeNo, Math.random()).execute()
        if (!response.isSuccessful) {
            return Result.FAILURE
        }

        val routeList = ArrayList<Route>()
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

        val output = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .build()

        outputData = output

        return Result.SUCCESS
    }
}