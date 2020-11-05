package com.alvinhkh.buseta.mtr

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.mtr.model.MtrLineName
import com.alvinhkh.buseta.mtr.model.MtrLineStation
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.google.gson.Gson
import timber.log.Timber
import java.nio.charset.Charset

class MtrLineWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val mtrService = MtrService.openData.create(MtrService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.MTR
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val codeMap = hashMapOf<String, Route>()
        try {
            val inputStream = applicationContext.assets.open("mtr_lines_name.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, Charset.forName("UTF-8"))
            val gson = Gson()
            val mtrLineNameArray = gson.fromJson(json, Array<MtrLineName>::class.java)

            mtrLineNameArray.forEach { mtrLineName ->
                val route = Route()
                route.companyCode = C.PROVIDER.MTR
                route.code = mtrLineName.lineCode
                route.name = mtrLineName.nameTc
                route.colour = mtrLineName.colour
                route.lastUpdate = timeNow

                codeMap[route.code?:""] = route
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        try {
            val response = mtrService.linesAndStations().execute()
            val res = response.body()
            val mtrStations = MtrLineStation.fromCSV(res?.string()?:"")
            mtrStations.forEach { mtrLineStation ->
                val route = codeMap[mtrLineStation.lineCode]?.copy()?: return@forEach
                val routeStop = RouteStop()
                routeStop.stopId = mtrLineStation.stationCode
                routeStop.companyCode = C.PROVIDER.MTR
//                routeStop.routeDestination = ""
                routeStop.routeSequence = mtrLineStation.direction
//                routeStop.fareFull = "0"
//                routeStop.latitude = ""
//                routeStop.longitude = ""
                routeStop.name = mtrLineStation.chineseName
//                routeStop.routeOrigin = ""
                routeStop.routeNo = route.name?:mtrLineStation.lineCode
                routeStop.routeId = mtrLineStation.lineCode
                routeStop.sequence = mtrLineStation.sequence
//                routeStop.etaGet = ""
//                routeStop.imageUrl = ""

                routeStop.lastUpdate = timeNow
                stopList.add(routeStop)

                route.sequence = mtrLineStation.direction
                route.origin = mtrLineStation.direction
                routeList.add(route)
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        val insertedList = routeDatabase?.routeDao()?.insert(routeList)
        if (insertedList?.size?:0 > 0) {
            routeDatabase?.routeDao()?.delete(companyCode, timeNow)
        }

        val insertedStopList = routeDatabase?.routeStopDao()?.insert(stopList)
        if (insertedStopList?.size?:0 > 0) {
            routeDatabase?.routeStopDao()?.delete(companyCode, timeNow)
        }

        return Result.success(outputData)
    }
}