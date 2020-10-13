package com.alvinhkh.buseta.route.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.Route

class RouteListViewModel(application: Application) : AndroidViewModel(application) {

    private val searchableDataSource = arrayListOf(C.PROVIDER.DATAGOVHK, C.PROVIDER.LRTFEEDER, C.PROVIDER.GMB901)

    private val routeDatabase = RouteDatabase.getInstance(application)!!

    fun liveData(route: String, companyCodes: List<String>): LiveData<MutableList<Route>>{
        return if (companyCodes.isNotEmpty()) {
            if (route.isNotEmpty()) {
                routeDatabase.routeDao().liveDataLike(searchableDataSource, route, companyCodes)
            } else {
                routeDatabase.routeDao().liveData(searchableDataSource, companyCodes)
            }
        } else {
            if (route.isNotEmpty()) {
                routeDatabase.routeDao().liveDataLike(searchableDataSource, route)
            } else {
                routeDatabase.routeDao().liveData(searchableDataSource)
            }
        }
    }
}