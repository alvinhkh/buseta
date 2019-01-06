package com.alvinhkh.buseta.kmb.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.UiThread
import com.alvinhkh.buseta.kmb.KmbService
import com.alvinhkh.buseta.kmb.model.KmbSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class KmbScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val kmbService = KmbService.webSearchCoroutine.create(KmbService::class.java)

    init {}

    @UiThread
    fun getAsLiveData(routeNo: String, routeBound: String): MutableLiveData<List<KmbSchedule>>{
        val result = MutableLiveData<List<KmbSchedule>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val response = kmbService.schedule(routeNo, routeBound).await()
                if (response.isSuccessful) {
                    result.value = response.body()?.data?.flatMap { it.value }
                } else {
                    result.value = null
                }
            }
            catch (e: Exception) {
                result.value = null
            }
        }
        return result
    }
}