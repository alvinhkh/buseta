package com.alvinhkh.buseta.kmb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.kmb.model.KmbRouteStop
import com.alvinhkh.buseta.kmb.model.network.KmbStopsRes
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.LatLong
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.utils.HKSCSUtil
import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateTransformFactory
import org.osgeo.proj4j.ProjCoordinate
import timber.log.Timber
import java.util.ArrayList

class KmbStopListWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.webSearch.create(KmbService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:""
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()

        try {
            val response = kmbService.stops(routeNo, routeSequence, routeServiceType).execute()
            if (!response.isSuccessful) {
                return Result.failure()
            }

            val stopList = ArrayList<RouteStop>()
            val timeNow = System.currentTimeMillis() / 1000

            val res = response.body()
            var i = 0
            val route = routeDatabase?.routeDao()?.get(companyCode, routeId, routeNo, routeSequence, routeServiceType)?: Route()
            for (kmbRouteStop in res?.data?.routeStops?: emptyList<KmbRouteStop>()) {
                val isLastStop = i >= (res?.data?.routeStops?.size?:0) - 1
                val routeStop = RouteStop()
                routeStop.companyCode = C.PROVIDER.KMB
                routeStop.routeNo = kmbRouteStop.route
                routeStop.routeId = route.code
                routeStop.routeServiceType = route.serviceType
                routeStop.routeSequence = kmbRouteStop.bound
                routeStop.stopId = kmbRouteStop.bsiCode
                routeStop.sequence = if (isLastStop) "999" else i.toString()
                routeStop.name = HKSCSUtil.convert(kmbRouteStop.nameTc)
                routeStop.fareFull = kmbRouteStop.airFare
                routeStop.location = HKSCSUtil.convert(kmbRouteStop.locationTc)
                val latlong = fromHK80toWGS84(kmbRouteStop.X.toDouble(), kmbRouteStop.Y.toDouble())
                routeStop.latitude = latlong?.first.toString()
                routeStop.longitude = latlong?.second.toString()
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
                routeDatabase?.routeStopDao()?.delete(companyCode, routeId, routeNo, routeSequence, routeServiceType, timeNow)
            }

            var hasCoordinates = false
            val mapCoordinates: MutableList<LatLong> = arrayListOf()
            val lineGeometryStr = res?.data?.route?.lineGeometry
            if (!lineGeometryStr.isNullOrEmpty()) {
                try {
                    hasCoordinates = true
                    val gson = Gson()
                    val lineGeometry = gson.fromJson(lineGeometryStr, KmbStopsRes.Data.Route.LineGeometry::class.java)
                    for (index in lineGeometry.paths.indices) {
                        val path = lineGeometry.paths[index]
                        for (j in path.indices) {
                            val p = path[j]
                            val loc = fromHK80toWGS84(p[0], p[1])
                            if (loc != null) {
                                mapCoordinates.add(LatLong(loc.first, loc.second))
                            }
                        }
                    }
                } catch (e: JsonParseException) {
                    hasCoordinates = false
                }
            }
            if (hasCoordinates) {
                routeDatabase?.routeDao()?.updateCoordinates(companyCode, routeId, routeNo, routeSequence, routeServiceType, mapCoordinates)
            } else {
                routeDatabase?.routeDao()?.deleteCoordinates(companyCode, routeId, routeNo, routeSequence, routeServiceType)
            }
        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure()
        }

        val outputData = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                .build()
        return Result.success(outputData)
    }

    private fun fromHK80toWGS84(lat: Double, lng: Double): Pair<Double, Double>? {
        try {
            // reference: blog.tiger-workshop.com/hk1980-grid-to-wgs84/
            val ctFactory = CoordinateTransformFactory()
            val csFactory = CRSFactory()
            val HK80 = csFactory.createFromParameters("EPSG:2326", "+proj=tmerc +lat_0=22.31213333333334 +lon_0=114.1785555555556 +k=1 +x_0=836694.05 +y_0=819069.8 +ellps=intl +towgs84=-162.619,-276.959,-161.764,0.067753,-2.24365,-1.15883,-1.09425 +units=m +no_defs")
            val WGS84 = csFactory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs")
            val trans = ctFactory.createTransform(HK80, WGS84)
            val p = ProjCoordinate()
            val p2 = ProjCoordinate()
            p.x = lat
            p.y = lng
            trans.transform(p, p2)
            return Pair(p2.y, p2.x)
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }
        return null
    }
}