package com.alvinhkh.buseta.nwst

import android.content.Context
import android.content.Intent
import android.support.v7.preference.PreferenceManager
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.nwst.NwstService.*
import com.alvinhkh.buseta.nwst.model.NwstRoute
import com.alvinhkh.buseta.nwst.model.NwstVariant
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.utils.HashUtil
import timber.log.Timber

class NwstRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:""
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val timeNow = System.currentTimeMillis() / 1000

        try {
//            val randomHex64 = HashUtil.randomHexString(64)
//            val tk = randomHex64
//            val syscode3 = preferences.getString("nwst_syscode3", "")
//            nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "Y", NwstService.DEVICETYPE,
//                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
//                    NwstRequestUtil.syscode2()).execute()
//            nwstService.pushToken(tk, tk, NwstService.LANGUAGE_TC, "R", NwstService.DEVICETYPE,
//                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
//                    NwstRequestUtil.syscode2()).execute()
//            nwstService.adv(NwstService.LANGUAGE_TC, NwstService.DEVICETYPE,
//                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
//                    NwstRequestUtil.syscode2()).execute()
//            val editor = preferences.edit()
//            editor.putString("nwst_tk", tk)
//            editor.apply()

            val response = nwstService.routeList(routeNo, TYPE_ALL_ROUTES,
                    LANGUAGE_TC, NwstRequestUtil.syscode(), PLATFORM, APP_VERSION,
                    NwstRequestUtil.syscode2()).execute()
            if (!response.isSuccessful) {
                return Result.failure(outputData)
            }

            val res = response.body()
            val routeArray = res?.string()?.split("\\|\\*\\|".toRegex())?.toTypedArray()
            for (rt in routeArray?: emptyArray()) {
                val text = rt.replace("<br>", "").trim { it <= ' ' }
                if (TextUtils.isEmpty(text)) continue
                val nwstRoute = NwstRoute.fromString(text)
                if (nwstRoute != null && nwstRoute.routeNo.isNotBlank()) {
                    val response2 = nwstService.variantList(nwstRoute.rdv, LANGUAGE_TC,
                            NwstRequestUtil.syscode(), PLATFORM, APP_VERSION,
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
                        route.rdv = nwstRoute.rdv
                        route.sequence = nwstRoute.bound
                        if (variant != null) {
                            route.stopsStartSequence = variant.startSequence
                            route.infoKey = variant.routeInfo
                            route.description = variant.remark
                            route.isSpecial = !TextUtils.isEmpty(variant.remark) && variant.remark != "正常路線"
                        }

                        route.lastUpdate = timeNow
                        routeList.add(route)

                        suggestionList.add(Suggestion(0, nwstRoute.companyCode, nwstRoute.routeNo, 0, Suggestion.TYPE_DEFAULT))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        if (routeNo.isEmpty()) {
            suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCode, timeNow)
            if (suggestionList.size > 0) {
                suggestionDatabase?.suggestionDao()?.insert(suggestionList)
                val i = Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE)
                i.putExtra(C.EXTRA.UPDATED, true)
                i.putExtra(C.EXTRA.MANUAL, manualUpdate)
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated)
                applicationContext.sendBroadcast(i)
            }
            Timber.d("%s: %s", companyCode, suggestionList.size)
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

        return Result.success(outputData)
    }
}