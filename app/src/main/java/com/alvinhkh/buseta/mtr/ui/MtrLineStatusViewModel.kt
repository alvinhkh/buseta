package com.alvinhkh.buseta.mtr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.annotation.UiThread
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.mtr.MtrService
import com.alvinhkh.buseta.mtr.model.MtrLineStatus
import com.alvinhkh.buseta.route.dao.RouteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import timber.log.Timber

class MtrLineStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val mtrService = MtrService.tnews.create(MtrService::class.java)

    private val routeDatabase = RouteDatabase.getInstance(application.applicationContext)

    @UiThread
    fun getAsLiveData(): MutableLiveData<List<MtrLineStatus>>{
        val result = MutableLiveData<List<MtrLineStatus>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val response = mtrService.lineStatus().await()
                if (response.isSuccessful) {
                    val statusList = mutableListOf<MtrLineStatus>()
                    response.body()?.lines?.forEach { status ->
                        val route = routeDatabase?.routeDao()?.getByCode(C.PROVIDER.MTR, status.lineCode)
                        status.lineName = route?.name?:status.lineCode
                        status.lineColour = route?.colour?:""
                        if (route != null) {
                            statusList.add(status)
                        }
                    }
                    result.value = statusList
                } else {
                    result.value = null
                }
            }
            catch (e: Exception) {
                Timber.e(e)
                result.value = null
            }
        }
        return result
    }
}