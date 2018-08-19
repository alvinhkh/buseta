package com.alvinhkh.buseta.kmb.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.alvinhkh.buseta.kmb.KmbService
import com.alvinhkh.buseta.kmb.model.KmbSchedule
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

class KmbScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val kmbService = KmbService.webSearchCoroutine.create(KmbService::class.java)

    lateinit var list: MutableLiveData<List<KmbSchedule>>

    init {}

    fun getAsLiveData(routeNo: String, routeBound: String): MutableLiveData<List<KmbSchedule>>{
        val result = MutableLiveData<List<KmbSchedule>>()
        result.value = null
        launch {
            try {
                val response = kmbService.schedule(routeNo, routeBound).await()
                if (response.isSuccessful) {
                    withContext(UI) {
                        result.value = response.body()?.data?.get(response.body()?.data?.keys?.first())
                    }
                } else {
                    withContext(UI) { result.value = null }
                }
            }
            catch (e: Exception) {
                withContext(UI) { result.value = null }
            }
        }
        return result
    }
}