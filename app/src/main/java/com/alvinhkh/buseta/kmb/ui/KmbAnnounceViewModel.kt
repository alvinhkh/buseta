package com.alvinhkh.buseta.kmb.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.UiThread
import com.alvinhkh.buseta.kmb.KmbService
import com.alvinhkh.buseta.kmb.model.KmbAnnounce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class KmbAnnounceViewModel(application: Application) : AndroidViewModel(application) {

    private val kmbService = KmbService.webSearchCoroutine.create(KmbService::class.java)

    @UiThread
    fun getAsLiveData(context: Context, routeNo: String, routeBound: String, routeServiceType: String): MutableLiveData<List<KmbAnnounce>>{
        val result = MutableLiveData<List<KmbAnnounce>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val response = kmbService.announce(routeNo, routeBound).await()
                if (response.isSuccessful) {
                    result.value = response.body()?.data
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