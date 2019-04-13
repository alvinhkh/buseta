package com.alvinhkh.buseta.follow

import android.content.Context
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.datagovhk.LrtFeederWorker
import com.alvinhkh.buseta.datagovhk.MtrLineWorker
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.kmb.KmbRouteWorker
import com.alvinhkh.buseta.mtr.AESBusWorker
import com.alvinhkh.buseta.mtr.MtrResourceWorker
import com.alvinhkh.buseta.nlb.NlbWorker
import com.alvinhkh.buseta.nwst.NwstRouteWorker


// TODO: check any route in follow list updated or removed
class FollowRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val followDatabase = FollowDatabase.getInstance(context)!!

    override fun doWork(): Result {
        val outputData = Data.Builder()
                .build()

        val map = hashMapOf<String, Pair<String, String>>()

        val list = followDatabase.followDao().list()
        list.forEach { follow ->
            val id = when(follow.companyCode) {
                C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST,
                C.PROVIDER.KMB, C.PROVIDER.LWB -> follow.companyCode + follow.routeNo
                else -> follow.companyCode
            }
            map[id] = Pair(follow.companyCode, follow.routeNo)
        }
        val requests = mutableListOf<OneTimeWorkRequest>()
        map.values.forEach { item ->
            val data = Data.Builder()
                    .putBoolean(C.EXTRA.MANUAL, false)
                    .putString(C.EXTRA.COMPANY_CODE, item.first)
                    .putString(C.EXTRA.ROUTE_NO, item.second)
                    .putBoolean(C.EXTRA.LOAD_STOP, true)
                    .build()
            when (item.first) {
                C.PROVIDER.AESBUS -> {
                    requests.add(OneTimeWorkRequest.Builder(MtrResourceWorker::class.java).addTag(TAG).setInputData(data).build())
                    requests.add(OneTimeWorkRequest.Builder(AESBusWorker::class.java).addTag(TAG).setInputData(data).build())
                }
                C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> requests.add(OneTimeWorkRequest.Builder(NwstRouteWorker::class.java).addTag(TAG).setInputData(data).build())
                C.PROVIDER.KMB, C.PROVIDER.LWB -> requests.add(OneTimeWorkRequest.Builder(KmbRouteWorker::class.java).addTag(TAG).setInputData(data).build())
                C.PROVIDER.LRTFEEDER -> requests.add(OneTimeWorkRequest.Builder(LrtFeederWorker::class.java).addTag(TAG).setInputData(data).build())
                C.PROVIDER.MTR -> requests.add(OneTimeWorkRequest.Builder(MtrLineWorker::class.java).addTag(TAG).setInputData(data).build())
                C.PROVIDER.NLB -> requests.add(OneTimeWorkRequest.Builder(NlbWorker::class.java).addTag(TAG).setInputData(data).build())
            }
        }
        if (requests.size > 0) {
            WorkManager.getInstance().enqueue(requests)
        }

        return Result.success(outputData)
    }

    companion object {
        private const val TAG = "FollowRouteWorker"
    }
}