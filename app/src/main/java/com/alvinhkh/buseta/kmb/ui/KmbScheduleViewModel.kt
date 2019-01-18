package com.alvinhkh.buseta.kmb.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.UiThread
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.kmb.KmbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class KmbScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val kmbService = KmbService.webSearchCoroutine.create(KmbService::class.java)

    @UiThread
    fun getAsLiveData(context: Context, routeNo: String, routeBound: String, routeServiceType: String): MutableLiveData<List<KmbScheduleViewAdapter.Data>>{
        val result = MutableLiveData<List<KmbScheduleViewAdapter.Data>>()
        result.value = null
        CoroutineScope(Main).launch {
            val list = arrayListOf<KmbScheduleViewAdapter.Data>()
            try {
                val response = kmbService.schedule(routeNo, routeBound).await()
                if (response.isSuccessful) {
                    var lastDayType = ""
                    var lastServiceType = ""
                    response.body()?.data?.flatMap { it.value }?.forEach { item ->
                        if (routeServiceType.isEmpty() || item.serviceType?.trim() == routeServiceType) {
                            if (lastServiceType != item.serviceTypeTc && !item.serviceTypeTc.isNullOrEmpty()) {
                                list.add(KmbScheduleViewAdapter.Data(KmbScheduleViewAdapter.Data.TYPE_SECTION, item.serviceTypeTc?:""))
                                lastServiceType = item.serviceTypeTc?:""
                            }
                            if (lastDayType != item.dayType) {
                                var dayTypeText = ""
                                when ((item.dayType?:"").trim().toUpperCase()) {
                                    "W" -> dayTypeText = context.getString(R.string.monday_to_friday)
                                    "MF" -> dayTypeText = context.getString(R.string.monday_to_friday)
                                    "S" -> dayTypeText = context.getString(R.string.saturday)
                                    "H" -> dayTypeText = context.getString(R.string.sunday_and_holiday)
                                    "MS" -> dayTypeText = context.getString(R.string.monday_to_saturday)
                                    "D" -> dayTypeText = context.getString(R.string.everyday)
                                    "X" -> dayTypeText = " "
                                }
                                list.add(KmbScheduleViewAdapter.Data(KmbScheduleViewAdapter.Data.TYPE_SECTION, dayTypeText))
                                lastDayType = item.dayType?:""
                            }
                            if ((routeBound.replace("0", "") == "1" && (item.boundText1?:"").isNotEmpty())
                                    || routeBound.replace("0", "") == "2" && (item.boundText2?:"").isNotEmpty()) {
                                list.add(KmbScheduleViewAdapter.Data(KmbScheduleViewAdapter.Data.TYPE_SCHEDULE, item))
                            }
                        }
                    }
                    result.value = list
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