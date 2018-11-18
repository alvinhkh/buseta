package com.alvinhkh.buseta.nwst

import android.content.Context
import android.support.v7.preference.PreferenceManager
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.nwst.NwstService.*
import com.alvinhkh.buseta.nwst.model.NwstRoute
import com.alvinhkh.buseta.nwst.model.NwstVariant
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.RouteUtil
import timber.log.Timber
import java.util.ArrayList

class NwstRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val mode = TYPE_ALL_ROUTES

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.FAILURE

        try {
            val response = nwstService.routeList(routeNo, mode,
                    LANGUAGE_TC, NwstRequestUtil.syscode(), PLATFORM, APP_VERSION,
                    NwstRequestUtil.syscode2(), preferences.getString("nwst_tk", ""),
                    preferences.getString("nwst_syscode3", "")).execute()
            if (!response.isSuccessful) {
                return Result.FAILURE
            }

            val routeList = ArrayList<Route>()
            val timeNow = System.currentTimeMillis() / 1000

            val res = response.body()
            val routeArray = res?.string()?.split("\\|\\*\\|".toRegex())?.toTypedArray()
            for (rt in routeArray?: emptyArray()) {
                val text = rt.replace("<br>", "").trim { it <= ' ' }
                if (TextUtils.isEmpty(text)) continue
                val nwstRoute = NwstRoute.fromString(text)
                if (nwstRoute != null && nwstRoute.routeNo.isNotBlank() && nwstRoute.routeNo == routeNo) {
                    val response2 = nwstService.variantList(nwstRoute.rdv, LANGUAGE_TC,
                            NwstRequestUtil.syscode(), PLATFORM, APP_VERSION,
                            NwstRequestUtil.syscode2(), preferences.getString("nwst_tk", ""), preferences.getString("nwst_syscode3", "")).execute()
                    if (!response2.isSuccessful) {
                        return Result.FAILURE
                    }
                    val res2 = response2.body()

                    val b = res2?.string()?:""
                    val datas = b.split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (data in datas) {
                        val text2 = data.trim { it <= ' ' }
                        if (text2.isEmpty()) continue
                        val variant = NwstVariant.fromString(text2)
                        val route = RouteUtil.fromNwst(nwstRoute, variant)
                        route.lastUpdate = timeNow
                        if (route.name != null && route.name == routeNo) {
                            routeList.add(route)
                        }
                    }
                }
            }

            val insertedList = routeDatabase?.routeDao()?.insert(routeList)
            if (insertedList?.size?:0 > 0) {
                routeDatabase?.routeDao()?.delete(C.PROVIDER.NWST, routeNo, timeNow)
                routeDatabase?.routeDao()?.delete(C.PROVIDER.CTB, routeNo, timeNow)
                routeDatabase?.routeDao()?.delete(C.PROVIDER.NWFB, routeNo, timeNow)
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