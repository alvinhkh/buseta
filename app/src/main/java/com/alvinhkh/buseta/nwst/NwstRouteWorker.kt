package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.nwst.model.NwstRoute
import com.alvinhkh.buseta.nwst.model.NwstVariant
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import timber.log.Timber

class NwstRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:""
        val loadStop = inputData.getBoolean(C.EXTRA.LOAD_STOP, false)
        val routeStopListTag = inputData.getString(C.EXTRA.TAG)?: "StopList_${companyCode}_${routeNo}"
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putBoolean(C.EXTRA.LOAD_STOP, loadStop)
                .build()

        val routeList = arrayListOf<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        try {
            val response = nwstService.routeList(routeNo, NwstService.TYPE_ALL_ROUTES,
                    NwstService.LANGUAGE_TC, NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION,
                    NwstRequestUtil.syscode2()).execute()
            if (!response.isSuccessful) {
                return Result.failure(outputData)
            }

            val res = response.body()
            val routeArray = res?.string()?.split("\\|\\*\\|".toRegex())?.toTypedArray()
            for (rt in routeArray?: emptyArray()) {
                val text = rt.replace("<br>", "").trim { it <= ' ' }
                if (text.isEmpty()) continue
                val nwstRoute = NwstRoute.fromString(text)
                if (nwstRoute != null && nwstRoute.routeNo.isNotBlank()) {
                    val response2 = nwstService.variantList(nwstRoute.rdv, NwstService.LANGUAGE_TC,
                            NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION,
                            NwstRequestUtil.syscode2()).execute()
                    if (!response2.isSuccessful) {
                        return Result.failure(outputData)
                    }
                    val res2 = response2.body()

                    val b = res2?.string()?:""
                    val datas = b.split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (data in datas) {
                        val text2 = data.trim { it <= ' ' }
                        if (text2.isEmpty()) continue
                        val variant = NwstVariant.fromString(text2)
                        val route = Route()
                        route.companyCode = nwstRoute.companyCode
                        route.description = nwstRoute.remark
                        route.origin = nwstRoute.placeFrom
                        route.destination = nwstRoute.placeTo
                        route.name = nwstRoute.routeNo
                        route.serviceType = nwstRoute.routeType
                        route.sequence = nwstRoute.bound
                        if (variant != null) {
                            route.stopsStartSequence = variant.startSequence
                            route.description = variant.remark
                            route.isSpecial = !variant.remark.isEmpty() && variant.remark != "正常路線"
                            route.code = variant.routeInfo
                        }
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
            if (routeNo.isNotEmpty()) {
                routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWST, routeNo, timeNow)
                routeDatabase?.routeStopDao()?.delete(C.PROVIDER.CTB, routeNo, timeNow)
                routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWFB, routeNo, timeNow)
            } else {
                routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWST, timeNow)
                routeDatabase?.routeStopDao()?.delete(C.PROVIDER.CTB, timeNow)
                routeDatabase?.routeStopDao()?.delete(C.PROVIDER.NWFB, timeNow)
            }
            val requests = arrayListOf<OneTimeWorkRequest>()
            routeList.forEach { route ->
                val data = Data.Builder()
                        .putString(C.EXTRA.COMPANY_CODE, route.companyCode)
                        .putString(C.EXTRA.ROUTE_ID, route.code)
                        .putString(C.EXTRA.ROUTE_NO, route.name)
                        .putString(C.EXTRA.ROUTE_SEQUENCE, route.sequence)
                        .putString(C.EXTRA.ROUTE_SERVICE_TYPE, route.serviceType)
                        .build()
                requests.add(OneTimeWorkRequest.Builder(NwstStopListWorker::class.java)
                        .setInputData(data).addTag(routeStopListTag).build())
            }
            WorkManager.getInstance().enqueue(requests)
        }

        return Result.success(outputData)
    }
}