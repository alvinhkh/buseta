package com.alvinhkh.buseta.nwst.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import android.content.Context
import androidx.annotation.UiThread
import com.alvinhkh.buseta.kmb.model.KmbAnnounce
import com.alvinhkh.buseta.nwst.NwstService
import com.alvinhkh.buseta.nwst.NwstService.*
import com.alvinhkh.buseta.nwst.model.NwstNotice
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.util.HashMap

class NwstNoticeViewModel(application: Application) : AndroidViewModel(application) {

    private val nwstService = NwstService.apiCoroutine.create(NwstService::class.java)

    @UiThread
    fun getAsLiveData(context: Context, routeNo: String, routeBound: String, routeServiceType: String): MutableLiveData<List<NwstNotice>>{
        val result = MutableLiveData<List<NwstNotice>>()
        result.value = null
        CoroutineScope(Main).launch {
            try {
                val options = HashMap<String, String>()
                options[QUERY_ROUTE] = routeNo
                options[QUERY_LANGUAGE] = LANGUAGE_TC
                options[QUERY_PLATFORM] = PLATFORM
                options[QUERY_VERSION] = APP_VERSION
                options[QUERY_SYSCODE] = NwstRequestUtil.syscode()
                val response = nwstService.noticeList(options).await()
                if (response.isSuccessful) {
                    val noticeList = arrayListOf<NwstNotice>()
                    val notices = response.body()?.string()?.split("\\|\\*\\|".toRegex())?.toTypedArray()?: arrayOf()
                    for (noticeText in notices) {
                        val nwstNotice = NwstNotice.fromString(noticeText.replace("<br>", "").trim { it <= ' ' }) ?: continue
                        noticeList.add(nwstNotice)
                    }
                    result.value = noticeList.toList()
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