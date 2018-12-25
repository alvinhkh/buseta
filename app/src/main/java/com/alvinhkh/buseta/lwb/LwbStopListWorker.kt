package com.alvinhkh.buseta.lwb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.lwb.model.LwbRouteStop
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import timber.log.Timber
import java.util.ArrayList

class LwbStopListWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val lwbService = LwbService.retrofit.create(LwbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()

        try {
            val response = lwbService.routeMap(routeNo, routeSequence, routeServiceType).execute()
            if (!response.isSuccessful) {
                return Result.failure()
            }

            val stopList = ArrayList<RouteStop>()
            val timeNow = System.currentTimeMillis() / 1000

            val lwbRouteStopList = response.body()
            var i = 0
            val route = routeDatabase?.routeDao()?.get(companyCode, routeNo, routeSequence, routeServiceType, "")?: Route()
            for (lwbRouteStop in lwbRouteStopList?: emptyList<LwbRouteStop>()) {
                val isLastStop = i >= (lwbRouteStopList?.size?:0) - 1
                val routeStop = RouteStop()
                routeStop.companyCode = C.PROVIDER.KMB
                routeStop.routeNo = route.name
                routeStop.routeId = route.code
                routeStop.routeServiceType = route.serviceType
                routeStop.routeSequence = route.sequence
                routeStop.stopId = lwbRouteStop.subarea
                routeStop.sequence = if (isLastStop) "999" else i.toString()
                routeStop.name = lwbRouteStop.name_tc
                routeStop.fareFull = lwbRouteStop.air_cond_fare
                routeStop.location = lwbRouteStop.address_tc
                routeStop.latitude = lwbRouteStop.lat
                routeStop.longitude = lwbRouteStop.lng
                routeStop.routeDestination = route.destination
                routeStop.routeOrigin = route.origin
                if (!routeStop.stopId.isNullOrEmpty()) {
                    routeStop.imageUrl = "http://www.kmb.hk/chi/img.php?file=" + routeStop.stopId
                }
                routeStop.etaGet = String.format("/?action=geteta&lang=tc&route=%s&bound=%s&stop=%s&stop_seq=%s&serviceType=%s",
                        routeStop.routeNo, routeStop.routeSequence, routeStop.stopId, if (isLastStop) 999 else routeStop.sequence, route.serviceType)

                routeStop.lastUpdate = timeNow
                stopList.add(routeStop)
                i += 1
            }

            val insertedList = routeDatabase?.routeStopDao()?.insert(stopList)
            if (insertedList?.size?:0 > 0) {
                routeDatabase?.routeStopDao()?.delete(companyCode, routeNo, routeSequence, routeServiceType, timeNow)
            }

            routeDatabase?.routeDao()?.deleteCoordinates(companyCode, routeNo, routeSequence, routeServiceType, "")
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure()
        }

        val output = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                .build()
        return Result.success(output)
    }
}