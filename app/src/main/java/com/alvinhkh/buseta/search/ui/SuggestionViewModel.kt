package com.alvinhkh.buseta.search.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion

class SuggestionViewModel(application: Application) : AndroidViewModel(application) {

    private val suggestionDatabase = SuggestionDatabase.getInstance(application)!!

    private val routeDatabase = RouteDatabase.getInstance(application)!!

    fun liveData(route: String): LiveData<MutableList<Route>>{
        return if (route.isNotEmpty()) {
            routeDatabase.routeDao().liveData(C.PROVIDER.DATAGOVHK, route)
        } else {
            routeDatabase.routeDao().liveData(C.PROVIDER.DATAGOVHK)
        }
    }

    fun getAsLiveData(route: String): LiveData<MutableList<Suggestion>>{
        return if (route.isNotEmpty()) {
            suggestionDatabase.suggestionDao().get(route)
        } else {
            suggestionDatabase.suggestionDao().suggestionLiveData()
        }
    }
}