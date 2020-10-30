package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.nwst.model.NwstRoute
import com.alvinhkh.buseta.nwst.model.NwstVariant
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.HashUtil
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
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val routeList = arrayListOf<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        try {
            val sysCode5 = preferences.getString("nwst_syscode5", "")?:""
            val appId = preferences.getString("nwst_appId", "")?:""
            val version = preferences.getString("nwst_version", NwstService.APP_VERSION)?:NwstService.APP_VERSION
            val version2 = preferences.getString("nwst_version2", NwstService.APP_VERSION2)?:NwstService.APP_VERSION2
            var tk = preferences.getString("nwst_tk", "")?:""
            val r0 = nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "", "Y", NwstService.DEVICETYPE,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                    NwstRequestUtil.syscode2()).execute()
            if (r0.body() != "Already Registered") {
                tk = HashUtil.randomHexString(64)
                nwstService.pushToken(tk, tk, NwstService.LANGUAGE_TC, "", "R", NwstService.DEVICETYPE,
                        NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                        NwstRequestUtil.syscode2()).execute()
                nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "", "Y", NwstService.DEVICETYPE,
                        NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                        NwstRequestUtil.syscode2()).execute()
                nwstService.adv(NwstService.LANGUAGE_TC, "640",
                        NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                        NwstRequestUtil.syscode2(), tk).execute()
                val editor = preferences.edit()
                editor.putString("nwst_tk", tk)
                editor.apply()
            }

            val response = nwstService.routeList(routeNo, NwstService.LANGUAGE_TC,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, version, tk).execute()
            if (!response.isSuccessful) {
                Timber.d("%s", response.message())
                return Result.failure(outputData)
            }
            val res = response.body()
            if (res == null || res.isNullOrEmpty() || res.length < 10) {
                return Result.failure(outputData)
            }

            val routeArray = res.split("\\|\\*\\|".toRegex()).toTypedArray()
            for (rt in routeArray) {
                val text = rt.replace("<br>", "").trim { it <= ' ' }
                if (text.isEmpty()) continue
                val nwstRoute = NwstRoute.fromString(text)
                if (nwstRoute != null && nwstRoute.routeNo.isNotBlank() && nwstRoute.routeNo == routeNo) {
                    val response2 = nwstService.variantList(nwstRoute.rdv,
                            NwstService.LANGUAGE_TC, nwstRoute.routeNo+"-"+nwstRoute.locationCode+"-1", nwstRoute.bound,
                            sysCode5, "Y", NwstService.PLATFORM, version, version2, appId).execute()
                    if (!response2.isSuccessful) {
                        Timber.d("%s", response2.message())
                        return Result.failure(outputData)
                    }
                    val res2 = response2.body()
                    if (res2 == null || res2.length < 10) {
                        return Result.failure(outputData)
                    }
                    val datas = res2.split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (data in datas) {
                        val text2 = data.trim { it <= ' ' }
                        if (text2.length < 10) continue
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
                            route.description = "(${variant.rank}) ${variant.remark}"
                            route.isSpecial = variant.remark.isNotEmpty() && !variant.remark.contains("正常路線")
                            route.isActive = variant.ball.isNotEmpty() && variant.ball == "R"
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
                routeDatabase?.routeDao()?.deleteBySource("", companyCode, routeNo, timeNow)
            } else {
                routeDatabase?.routeDao()?.deleteBySource("", companyCode, timeNow)
            }
        }

        if (loadStop) {
            if (routeNo.isNotEmpty()) {
                routeDatabase?.routeStopDao()?.deleteBySource("", companyCode, routeNo, timeNow)
            } else {
                routeDatabase?.routeStopDao()?.deleteBySource("", companyCode, timeNow)
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
            if (requests.size > 0) {
                WorkManager.getInstance().enqueue(requests)
            }
        }

        return Result.success(outputData)
    }
}