package com.alvinhkh.buseta.mtr

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.DatabaseUtil
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap

class AESBusRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.AESBUS
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.FAILURE

        val routeList = ArrayList<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        try {
            val database = DatabaseUtil.getAESBusDatabase(applicationContext)
            val aesBusDistricts = database.aesBusDao().allDistricts()
            val districts = HashMap<String, String>()
            for (aesBusDistrict in aesBusDistricts) {
                districts[aesBusDistrict.districtID] = aesBusDistrict.districtCn
            }
            val aesBusRoutes = database.aesBusDao().allRoutes()

            for ((busNumber, _, serviceHours, _, _, _, districtID) in aesBusRoutes) {
                val route = Route()
                route.companyCode = companyCode
                route.name = busNumber
                if (districtID > 0) {
                    route.origin = districts[districtID.toString()]
                }
                route.description = serviceHours
                route.sequence = "0"
                route.lastUpdate = timeNow
                routeList.add(route)
            }

        } catch (e: Exception) {
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