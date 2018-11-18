package com.alvinhkh.buseta.kmb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.kmb.model.KmbRoute
import com.alvinhkh.buseta.kmb.model.KmbRouteBound
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.HKSCSUtil
import com.alvinhkh.buseta.utils.RouteUtil
import timber.log.Timber
import java.util.ArrayList

class KmbRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.webSearch.create(KmbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.FAILURE

        try {
            val response = kmbService.routeBound(routeNo).execute()
            if (!response.isSuccessful) {
                return Result.FAILURE
            }

            val routeList = ArrayList<Route>()
            val timeNow = System.currentTimeMillis() / 1000

            val res = response.body()
            val routeBoundList = ArrayList<Int>()
            for (bound in res?.data?: emptyList<KmbRouteBound>()) {
                if (routeBoundList.contains(bound.bound)) continue
                routeBoundList.add(bound.bound)
                val response2 = kmbService.specialRoute(bound.route, bound.bound.toString()).execute()
                if (!response2.isSuccessful) {
                    return Result.FAILURE
                }
                val res2 = response2.body()
                for (kmbRoute in res2?.data?.routes?: emptyList<KmbRoute>()) {
                    kmbRoute.destinationTc = HKSCSUtil.convert(kmbRoute.destinationTc)
                    kmbRoute.originTc = HKSCSUtil.convert(kmbRoute.originTc)
                    kmbRoute.descTc = HKSCSUtil.convert(kmbRoute.descTc)
                    val route = RouteUtil.fromKmb(kmbRoute)
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
        } catch (e: Exception) {
            Timber.d(e)
            return Result.FAILURE
        }

        return Result.SUCCESS
    }
}