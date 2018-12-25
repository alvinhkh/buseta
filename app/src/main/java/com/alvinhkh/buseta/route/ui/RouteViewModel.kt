package com.alvinhkh.buseta.route.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val routeDatabase = RouteDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<Route>>

    fun getAsLiveData(companyCode: String, routeNo: String): LiveData<MutableList<Route>> {
        if (routeDatabase != null) {
            list = routeDatabase.routeDao().liveData(companyCode, routeNo)
        }
        return list
    }
}