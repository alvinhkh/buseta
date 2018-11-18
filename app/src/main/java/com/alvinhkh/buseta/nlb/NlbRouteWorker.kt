package com.alvinhkh.buseta.nlb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import java.util.ArrayList

class NlbRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nlbService = NlbService.api.create(NlbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NLB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.FAILURE

        val response = nlbService.database().execute()
        if (!response.isSuccessful) {
            return Result.FAILURE
        }

        val routeList = ArrayList<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        val database = response.body()

        if (database?.routes != null) {
            for (nlbRoute in database.routes) {
                if (nlbRoute == null) continue
                if (nlbRoute.route_no == routeNo) {
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
                }
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