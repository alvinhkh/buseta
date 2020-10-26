package com.alvinhkh.buseta.mtr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.annotation.UiThread
import com.alvinhkh.buseta.mtr.MtrService
import com.alvinhkh.buseta.mtr.model.MtrLatestAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import timber.log.Timber

class MtrLatestAlertViewModel(application: Application) : AndroidViewModel(application) {

    private val mtrService = MtrService.tnews.create(MtrService::class.java)

    @UiThread
    fun getAsLiveData(): MutableLiveData<List<MtrLatestAlert>>{
        val result = MutableLiveData<List<MtrLatestAlert>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val response = mtrService.latestAlert().await()
                val latestAlert = mutableListOf<MtrLatestAlert>()
                if (response.isSuccessful) {
                    response.body()?.items?.forEach { item ->
                        if (item.type in 0..98) {
                            latestAlert.add(item)
                        }
                    }
                }
                    result.value = latestAlert
            }
            catch (e: Exception) {
                Timber.e(e)
                result.value = null
            }
        }
        return result
    }
}