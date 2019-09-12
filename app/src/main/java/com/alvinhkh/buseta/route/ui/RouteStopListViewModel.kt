package com.alvinhkh.buseta.route.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RouteStopListViewModel(application: Application) : AndroidViewModel(application) {

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(application)

    private val followDatabase = FollowDatabase.getInstance(application)

    private val routeDatabase = RouteDatabase.getInstance(application)

    fun liveData(companyCode: String, routeId: String, routeNo: String,
                 routeSequence: String, routeServiceType: String): LiveData<MutableList<RouteStop>> {
        val result = MutableLiveData<MutableList<RouteStop>>()
        result.value = null
        CoroutineScope(Dispatchers.Main).launch {
            routeDatabase?.routeStopDao()?.liveData(companyCode, routeId, routeNo, routeSequence, routeServiceType)?.observeForever { routeStopList ->
                val list = arrayListOf<RouteStop>()
                routeStopList.forEach { routeStop ->
                    arrivalTimeDatabase?.arrivalTimeDao()
                            ?.getLiveData(routeStop.companyCode?:"", routeStop.routeNo?:"",
                                    routeStop.routeSequence?:"", routeStop.stopId?:"", routeStop.sequence?:"")
                            ?.observeForever { etas ->
                                routeStop.etas = listOf()
                                etas.forEach { eta ->
                                    if (eta.updatedAt > System.currentTimeMillis() - 600000) {
                                        routeStop.etas += eta
                                    }
                                }
                                if (result.value != null) {
                                    result.postValue(list)
                                }
                            }
                    followDatabase?.followDao()?.liveCount(
                            routeStop.companyCode?:"", routeStop.routeNo?:"",
                            routeStop.routeSequence?:"", routeStop.routeServiceType?:"",
                            routeStop.stopId?:"", routeStop.sequence?:"")
                            ?.observeForever { count ->
                                routeStop.isFollow = count > 0
                                if (result.value != null) {
                                    result.postValue(list)
                                }
                            }
                    list.add(routeStop)
                }
                if (result.value != null) {
                    result.postValue(list)
                } else {
                    result.value = list
                }
            }
        }
        return result
    }
}