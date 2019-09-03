package com.alvinhkh.buseta.kmb

import android.content.Context
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.kmb.model.KmbRoute
import com.alvinhkh.buseta.kmb.model.KmbRouteBound
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.HKSCSUtil
import timber.log.Timber

class KmbRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.webSearch.create(KmbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val loadStop = inputData.getBoolean(C.EXTRA.LOAD_STOP, false)
        val routeStopListTag = inputData.getString(C.EXTRA.TAG)?: "StopList_${companyCode}_${routeNo}"
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putBoolean(C.EXTRA.LOAD_STOP, loadStop)
                .build()

        try {
            val response = kmbService.routeBound(routeNo).execute()
            if (!response.isSuccessful || !response.body()?.exception.isNullOrEmpty()) {
                return Result.failure(outputData)
            }

            val routeList = arrayListOf<Route>()
            val timeNow = System.currentTimeMillis() / 1000

            val res = response.body()
            val routeBoundList = arrayListOf<Int>()
            for (bound in res?.data?: emptyList<KmbRouteBound>()) {
                if (routeBoundList.contains(bound.bound)) continue
                routeBoundList.add(bound.bound)
                val response2 = kmbService.specialRoute(bound.route, bound.bound.toString()).execute()
                if (!response2.isSuccessful) {
                    return Result.failure(outputData)
                }
                val res2 = response2.body()
                for (kmbRoute in res2?.data?.routes?: emptyList<KmbRoute>()) {
                    kmbRoute.destinationTc = HKSCSUtil.convert(kmbRoute.destinationTc)
                    kmbRoute.originTc = HKSCSUtil.convert(kmbRoute.originTc)
                    kmbRoute.descTc = HKSCSUtil.convert(kmbRoute.descTc)

                    val route = Route()
                    route.companyCode = C.PROVIDER.KMB
                    route.origin = kmbRoute.originTc
                    route.destination = kmbRoute.destinationTc
                    route.code = kmbRoute.route
                    route.name = kmbRoute.route
                    route.sequence = kmbRoute.bound
                    route.serviceType = if (kmbRoute.serviceType.isNullOrEmpty()) kmbRoute.serviceType else kmbRoute.serviceType.trim { it <= ' ' }
                    val desc = kmbRoute.descTc.trim { it <= ' ' }
                    route.description = desc
                    route.isSpecial = desc.isNotEmpty()
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
                    requests.add(OneTimeWorkRequest.Builder(KmbStopListWorker::class.java)
                            .setInputData(data).addTag(routeStopListTag).build())
                }
                WorkManager.getInstance().enqueue(requests)
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        return Result.success(outputData)
    }
}