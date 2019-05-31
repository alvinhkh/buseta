package com.alvinhkh.buseta.route.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val searchableDataSource = arrayListOf("", C.PROVIDER.AESBUS)

    private val routeDatabase = RouteDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<Route>>

    fun getAsLiveData(companyCode: String, routeNo: String): LiveData<MutableList<Route>> {
        if (routeDatabase != null) {
            list = routeDatabase.routeDao().liveData(searchableDataSource, routeNo, arrayListOf(companyCode))
        }
        return list
    }
}