package com.alvinhkh.buseta.nlb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.annotation.UiThread
import com.alvinhkh.buseta.nlb.NlbService
import com.alvinhkh.buseta.nlb.model.NlbNews
import com.alvinhkh.buseta.nlb.model.NlbNewsListRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class NlbNewsViewModel(application: Application) : AndroidViewModel(application) {

    private val nlbService = NlbService.apiCoroutine.create(NlbService::class.java)

    @UiThread
    fun getAsLiveData(): MutableLiveData<List<NlbNews>>{
        val result = MutableLiveData<List<NlbNews>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val response = nlbService.newsList(NlbNewsListRequest("zh")).await()
                if (response.isSuccessful) {
                    result.value = response.body()?.newses
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