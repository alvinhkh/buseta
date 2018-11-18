package com.alvinhkh.buseta.mtr

import android.content.Context
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.datagovhk.DataGovHkService
import com.alvinhkh.buseta.datagovhk.model.MtrBusRoute
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.DatabaseUtil
import com.alvinhkh.buseta.utils.RouteUtil
import java.io.IOException
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class LrtFeederRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val dataGovHkService = DataGovHkService.resource.create(DataGovHkService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.LRTFEEDER
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.FAILURE

        val routeList = ArrayList<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        try {
            val response = dataGovHkService.mtrBusRoutes().execute()
            val res = response.body()

            val routes = MtrBusRoute.fromCSV(res?.string()?:"")
            for (mtrBusRoute in routes) {
                if (mtrBusRoute.routeId.isNullOrEmpty()) continue
                val route = RouteUtil.fromMtrBus(mtrBusRoute)
                route.lastUpdate = timeNow
                routeList.add(route)
            }
        } catch (e: IOException) {
            return Result.FAILURE
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