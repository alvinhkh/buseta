package com.alvinhkh.buseta.datagovhk.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.annotation.UiThread
import com.alvinhkh.buseta.datagovhk.DataGovHkService
import com.alvinhkh.buseta.datagovhk.model.TdTrafficNewsV1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import timber.log.Timber


class TdTrafficNewsViewModel(application: Application) : AndroidViewModel(application) {

    private val tdService = DataGovHkService.tdCoroutine.create(DataGovHkService::class.java)

    @UiThread
    fun getAsLiveData(): MutableLiveData<List<TdTrafficNewsV1.Message>>{
        val result = MutableLiveData<List<TdTrafficNewsV1.Message>>()
        result.value = null
        CoroutineScope(Main).launch {
            val messageList = arrayListOf<TdTrafficNewsV1.Message>()
            try {
                val response = tdService.trafficnewsTcAsync().await()
                if (response.isSuccessful) {
                    val trafficNews = response.body()
                    for (message in trafficNews?.messages?: emptyList()) {
                        messageList.add(message)
                    }
                }
            }
            catch (e: Exception) {
                Timber.d(e)
            }
            result.value = messageList.toList()
        }
        return result
    }
}