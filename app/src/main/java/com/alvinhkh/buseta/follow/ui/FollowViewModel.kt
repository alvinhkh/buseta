package com.alvinhkh.buseta.follow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class FollowViewModel(application: Application) : AndroidViewModel(application) {

    private val followDatabase = FollowDatabase.getInstance(application)

    private val routeDatabase = RouteDatabase.getInstance(application)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(application)

    private val context = application.applicationContext

    fun liveData(groupId: String): LiveData<MutableList<Follow>> {
        val result = MutableLiveData<MutableList<Follow>>()
        result.value = null
        CoroutineScope(Main).launch {
            val followList = arrayListOf<Follow>()
            followDatabase?.followDao()?.list(groupId)?.forEach { follow ->
                val route = routeDatabase?.routeDao()?.get(follow.companyCode, follow.routeId, follow.routeNo, follow.routeSeq, follow.routeServiceType)
                if (!route?.colour.isNullOrBlank()) {
                    follow.routeColour = route?.colour.orEmpty()
                }
                if (follow.routeColour.isBlank()) {
                    val colour = Route.companyColour(context, follow.companyCode, follow.routeNo)
                    if (colour != null) {
                        follow.routeColour = String.format("#%08X", colour)
                    }
                }

                val arrivalTimeLiveData = arrivalTimeDatabase?.arrivalTimeDao()?.getLiveData(follow.companyCode, follow.routeNo, follow.routeSeq, follow.stopId, follow.stopSeq)
                arrivalTimeLiveData?.observeForever { etas ->
                    follow.etas = listOf()
                    etas.forEach { eta ->
                        if (eta.updatedAt > System.currentTimeMillis() - 600000) {
                            follow.etas += eta
                        }
                    }
                    result.postValue(followList)
                }

                followList.add(follow)
            }
            result.value = followList
        }
        return result
    }
}