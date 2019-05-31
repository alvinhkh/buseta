package com.alvinhkh.buseta.kmb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.annotation.UiThread
import com.alvinhkh.buseta.kmb.KmbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class KmbBBIViewModel(application: Application) : AndroidViewModel(application) {

    private val kmbService = KmbService.webCoroutine.create(KmbService::class.java)

    @UiThread
    fun getAsLiveData(routeNo: String, routeBound: String): MutableLiveData<List<KmbBBIViewAdapter.Data>>{
        val result = MutableLiveData<List<KmbBBIViewAdapter.Data>>()
        val list = arrayListOf<KmbBBIViewAdapter.Data>()
        CoroutineScope(Main).launch {
            try {
                val response1 = kmbService.bbi(routeNo, if (routeBound == "2") "B" else "F").await()
                if (response1.isSuccessful) {
                    response1.body()?.run {
                        list.add(KmbBBIViewAdapter.Data(KmbBBIViewAdapter.Data.TYPE_SECTION, "第一程 往" + (busArr[0]["dest"]?:"")))
                        records.forEach { item ->
                            list.add(KmbBBIViewAdapter.Data(KmbBBIViewAdapter.Data.TYPE_BBI, item))
                        }
                    }
                }
                val response2 = kmbService.bbi(routeNo, if (routeBound == "2") "B" else "F", "2").await()
                if (response2.isSuccessful) {
                    response2.body()?.run {
                        list.add(KmbBBIViewAdapter.Data(KmbBBIViewAdapter.Data.TYPE_SECTION, "第二程 往" + (busArr[0]["dest"]?:"")))
                        records.forEach { item ->
                            list.add(KmbBBIViewAdapter.Data(KmbBBIViewAdapter.Data.TYPE_BBI, item))
                        }
                    }
                }
            } catch (e: Exception) {
            }
            if (list.isNotEmpty()) {
                list.add(KmbBBIViewAdapter.Data(KmbBBIViewAdapter.Data.TYPE_NOTE, "乘客用八達通卡繳付第一程車資後，除個別符號之指定時間外，乘客可於150分鐘內以同一張八達通卡繳付第二程車資，享受巴士轉乘優惠。 ^ 代表“30分鐘內”; #代表“60分鐘內”; *代表“90分鐘內” 及@代表“120分鐘內”。"))
            }
            result.value = list
        }
        return result
    }
}