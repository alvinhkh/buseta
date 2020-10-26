package com.alvinhkh.buseta.nwst.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import android.content.Context
import androidx.annotation.UiThread
import androidx.preference.PreferenceManager
import com.alvinhkh.buseta.nwst.NwstService
import com.alvinhkh.buseta.nwst.model.NwstNotice
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.util.HashMap

class NwstNoticeViewModel(application: Application) : AndroidViewModel(application) {

    private val nwstService = NwstService.apiCoroutine.create(NwstService::class.java)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)

    @UiThread
    fun getAsLiveData(context: Context, routeNo: String, routeBound: String, routeServiceType: String): MutableLiveData<List<NwstNotice>>{
        val result = MutableLiveData<List<NwstNotice>>()
        result.value = null
        CoroutineScope(Main).launch {
            val noticeList = arrayListOf<NwstNotice>()
            try {
                val version = preferences.getString("nwst_version", NwstService.APP_VERSION)?:NwstService.APP_VERSION
                val specialOptions = HashMap<String, String>()
                specialOptions[NwstService.QUERY_R] = routeNo
                specialOptions[NwstService.QUERY_LANGUAGE] = NwstService.LANGUAGE_TC
                specialOptions[NwstService.QUERY_PLATFORM] = NwstService.PLATFORM
                specialOptions[NwstService.QUERY_VERSION] = version
                specialOptions[NwstService.QUERY_SYSCODE] = NwstRequestUtil.syscode()
                val specialResponse = nwstService.specialAsync(specialOptions).await()
                if (specialResponse.isSuccessful) {
                    var text = ""
                    val nwstNotice = NwstNotice()
                    val specials = specialResponse.body()?.string()?.split("\\|\\*\\|".toRegex())?.toTypedArray()?: arrayOf()
                    for (specialText in specials.withIndex()) {
                        val data = specialText.value.replace("<br>", "").trim().split("\\|\\|".toRegex()).dropLastWhile { it.isBlank() }.toTypedArray()
                        if (data.size >= 3 && data[0] == "D") {
                            text += data[1].split(" ")[0] + "：" + data[2]
                            if (specialText.index < specials.size - 3) {
                                text += "\n"
                            }
                        }
                        if (specialText.index > 2 && data.size >= 3 && data[0] == "R") {
                            text += "\n"
                        }
                    }
                    if (text.isNotEmpty()) {
                        nwstNotice.companyCode = "NWST"
                        nwstNotice.routeNo = routeNo
                        nwstNotice.releaseDate = ""
                        nwstNotice.link = ""
                        nwstNotice.title = text
                        nwstNotice.source = "special"
                        noticeList.add(nwstNotice)
                    }
                }
            }
            catch (e: Exception) {
            }
            try {
                val noticeListOptions = HashMap<String, String>()
                noticeListOptions[NwstService.QUERY_ROUTE] = routeNo
                noticeListOptions[NwstService.QUERY_LANGUAGE] = NwstService.LANGUAGE_TC
                noticeListOptions[NwstService.QUERY_PLATFORM] = NwstService.PLATFORM
                noticeListOptions[NwstService.QUERY_VERSION] = NwstService.APP_VERSION
                noticeListOptions[NwstService.QUERY_SYSCODE] = NwstRequestUtil.syscode()
                val noticeListResponse = nwstService.noticeListAsync(noticeListOptions).await()
                if (noticeListResponse.isSuccessful) {
                    val notices = noticeListResponse.body()?.string()?.split("\\|\\*\\|".toRegex())?.toTypedArray()?: arrayOf()
                    for (noticeText in notices) {
                        val nwstNotice = NwstNotice.fromString(noticeText.replace("<br>", "").trim { it <= ' ' }) ?: continue
                        noticeList.add(nwstNotice)
                    }
                }
            }
            catch (e: Exception) {
            }
            result.value = noticeList.toList()
        }
        return result
    }
}