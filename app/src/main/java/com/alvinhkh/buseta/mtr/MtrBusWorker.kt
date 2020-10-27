package com.alvinhkh.buseta.mtr

import android.content.Context
import android.util.SparseArray
import android.util.SparseIntArray
import androidx.core.util.set
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.mtr.dao.MtrBusDatabase
import com.alvinhkh.buseta.mtr.model.MtrBusFare
import com.alvinhkh.buseta.mtr.model.MtrBusRoute
import com.alvinhkh.buseta.mtr.model.MtrBusStop
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber
import java.io.File
import java.lang.Exception

class MtrBusWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val routeDatabase = RouteDatabase.getInstance(context)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.LRTFEEDER
        val outputData = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val suggestionList = arrayListOf<Suggestion>()
        val routeList = arrayListOf<Route>()
        val stopList = arrayListOf<RouteStop>()
        val timeNow = System.currentTimeMillis() / 1000

        val busDatabaseFileName = firebaseRemoteConfig.getString("mtr_bus_database_file")
        val lastBusDatabaseFileName = preferences.getString("mtr_bus_database_file", "")?: ""
        if (busDatabaseFileName.isEmpty()) {
            return Result.failure(outputData)
        }
        if (!manualUpdate && lastBusDatabaseFileName == busDatabaseFileName) {
            Timber.d("MTR Bus database: %s", busDatabaseFileName)
            return Result.success(outputData)
        }

        applicationContext.deleteDatabase("E_Bus.db")
        val database = Room.databaseBuilder(applicationContext, MtrBusDatabase::class.java, "E_Bus.db")
                .allowMainThreadQueries()
                .createFromFile(File("${applicationContext.cacheDir}/$busDatabaseFileName"))
                .build()

        val mtrBusFareArray = SparseArray<MtrBusFare>()
        try {
            val mtrBusRouteLines = database.mtrBusDao().allRouteLines()
            val mtrBusRouteArray = SparseArray<MtrBusRoute>()
            val mtrBusRoutes = database.mtrBusDao().allRoutes()
            for (mtrBusRoute in mtrBusRoutes) {
                mtrBusRouteArray.put(mtrBusRoute.routeId?:0, mtrBusRoute)
            }

            val mtrBusRouteLineArray = SparseIntArray()
            val mtrBusRouteIdCount = SparseIntArray()
            for (mtrBusRouteLine in mtrBusRouteLines) {
                val mtrBusRoute = mtrBusRouteArray.get(mtrBusRouteLine.routeId?:0)?: continue
                val descriptionZh = mtrBusRoute.descriptionZh?.split("←→")
                val sequence = mtrBusRouteIdCount[mtrBusRouteLine.routeId?:0]
                val route = Route()
                route.dataSource = C.PROVIDER.LRTFEEDER
                route.companyCode = companyCode
                route.code = mtrBusRouteLine.routeLineId.toString()
                route.name = mtrBusRoute.routeNumber
                if (descriptionZh?.size?:0 == 2) {
                    if (sequence > 0) {
                        route.origin = descriptionZh?.get(1)?.trim()
                        route.destination = descriptionZh?.get(0)?.trim()
                    } else {
                        route.origin = descriptionZh?.get(0)?.trim()
                        route.destination = descriptionZh?.get(1)?.trim()
                    }
                } else {
                    route.origin =  mtrBusRoute.descriptionZh?: mtrBusRouteLine.fromStop
                }
                route.description = mtrBusRoute.frequencyRemarkZh
                route.sequence = sequence.toString()
                route.lastUpdate = timeNow
                routeList.add(route)
                mtrBusRouteIdCount[mtrBusRouteLine.routeId?:0] += 1
                mtrBusRouteLineArray[mtrBusRouteLine.routeLineId?:0] = mtrBusRouteLine.routeId?:0
                suggestionList.add(Suggestion(0, companyCode, mtrBusRoute.routeNumber?:"", 0, Suggestion.TYPE_DEFAULT))
            }

            val mtrBusFares = database.mtrBusDao().allFares()
            for (mtrBusFare in mtrBusFares) {
                mtrBusFareArray[mtrBusFare.routeId?:0] = mtrBusFare
            }

//            val mtrBusFrequences = database.mtrBusDao().allFrequences()
//            for (mtrBusFrequence in mtrBusFrequences) {
//                Timber.d("%s", mtrBusFrequence)
//            }

            val mtrBusStops = database.mtrBusDao().allStops()
            val mtrBusStopsArray = SparseArray<ArrayList<MtrBusStop>>()
            for (mtrBusStop in mtrBusStops) {
                if (mtrBusStopsArray[mtrBusStop.routeLineId?:0] == null) {
                    mtrBusStopsArray[mtrBusStop.routeLineId?:0] = arrayListOf()
                }
                mtrBusStopsArray[mtrBusStop.routeLineId?:0].add(mtrBusStop)
            }

            for (route in routeList) {
                val routeLineId = route.code?.toInt()?: continue
                val routeId = mtrBusRouteLineArray.get(routeLineId)
                val mtrBusStopList = mtrBusStopsArray[routeLineId].toList()
                if (mtrBusStopList.isEmpty()) continue
                for (mtrBusStop in mtrBusStopList) {
                    val routeStop = RouteStop()
                    routeStop.stopId = mtrBusStop.refId.toString()
                    routeStop.latitude = mtrBusStop.latitude
                    routeStop.longitude = mtrBusStop.longitude
                    routeStop.name = mtrBusStop.nameCh
                    routeStop.etaGet = ""
                    routeStop.imageUrl = ""

                    routeStop.companyCode = route.companyCode
                    routeStop.routeDestination = route.destination
                    routeStop.routeSequence = route.sequence
                    routeStop.routeOrigin = route.origin
                    routeStop.routeNo = route.name
                    routeStop.routeId = route.code
                    routeStop.routeServiceType = route.serviceType

                    routeStop.fareFull = mtrBusFareArray[routeId]?.cashAdult?.toString()
                    routeStop.fareChild = mtrBusFareArray[routeId]?.cashChild
                    routeStop.fareSenior = mtrBusFareArray[routeId]?.cashSenior

                    routeStop.sequence = mtrBusStop.sortOrder.toString()
                    routeStop.lastUpdate = timeNow
                    stopList.add(routeStop)
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure(outputData)
        }

        suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCode, timeNow)
        if (suggestionList.size > 0) {
            suggestionDatabase?.suggestionDao()?.insert(suggestionList)
        }

        routeDatabase?.routeDao()?.deleteBySource(arrayListOf(C.PROVIDER.LRTFEEDER), timeNow)
        routeDatabase?.routeDao()?.insert(routeList)

        if (routeDatabase?.routeStopDao()?.insert(stopList)?.size?:0 > 0) {
            routeDatabase?.routeStopDao()?.delete(companyCode, timeNow)
        }

        routeDatabase?.routeDao()?.delete(C.PROVIDER.AESBUS, timeNow)
        routeDatabase?.routeStopDao()?.delete(C.PROVIDER.AESBUS, timeNow)

        val editor = preferences.edit()
        editor.putString("mtr_bus_database_file", busDatabaseFileName)
        editor.apply()

        return Result.success(outputData)
    }
}