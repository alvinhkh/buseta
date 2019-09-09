package com.alvinhkh.buseta.nlb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.annotation.UiThread
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.nlb.NlbService
import com.alvinhkh.buseta.nlb.model.NlbNews
import com.alvinhkh.buseta.nlb.model.NlbNewsListRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class NlbNewsViewModel(application: Application) : AndroidViewModel(application) {

    private val nlbService = NlbService.nlbApiCoroutine.create(NlbService::class.java)

    private val gmb901Service = NlbService.gmb901ApiCoroutine.create(NlbService::class.java)

    @UiThread
    fun liveData(companyCode: String): MutableLiveData<List<NlbNews>>{
        val result = MutableLiveData<List<NlbNews>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val request = NlbNewsListRequest("zh")
                val response = if (companyCode == C.PROVIDER.GMB901) {
                    gmb901Service.newsList(request).await()
                } else {
                    nlbService.newsList(request).await()
                }
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