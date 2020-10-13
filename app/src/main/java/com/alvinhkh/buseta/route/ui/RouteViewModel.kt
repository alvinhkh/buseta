package com.alvinhkh.buseta.route.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.PreferenceUtil

class RouteViewModel(application: Application) : AndroidViewModel(application) {

    private val searchableDataSource = arrayListOf("", C.PROVIDER.LRTFEEDER, C.PROVIDER.NLB, C.PROVIDER.GMB901)

    private val routeDatabase = RouteDatabase.getInstance(application)

    private val isUsingNwstDataGovHkApi = PreferenceUtil.isUsingNwstDataGovHkApi(application)

    lateinit var list: LiveData<MutableList<Route>>

    fun getAsLiveData(companyCode: String, routeNo: String): LiveData<MutableList<Route>> {
        if (routeDatabase != null) {
            when (companyCode) {
                C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                    if (isUsingNwstDataGovHkApi) {
                        searchableDataSource.remove("")
                        searchableDataSource.add(C.PROVIDER.DATAGOVHK_NWST)
                    }
                }
            }
            list = routeDatabase.routeDao().liveData(searchableDataSource, routeNo, arrayListOf(companyCode))
        }
        return list
    }
}