package com.alvinhkh.buseta.kmb

import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import timber.log.Timber

class KmbEtaRouteWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.etadatafeed.create(KmbService::class.java)

    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        val response = kmbService.etaRoutes().execute()
        if (!response.isSuccessful) {
            return Result.failure(outputData)
        }

        val suggestionList = arrayListOf<Suggestion>()
        val timeNow = System.currentTimeMillis() / 1000

        val list = response.body()

        if (list?.isNotEmpty() == true) {
            val routes = list[0].r_no
            val routeArray = routes.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in routeArray.indices) {
                suggestionList.add(Suggestion(0, companyCode, routeArray[i], 0, Suggestion.TYPE_DEFAULT))
            }
        }

        suggestionDatabase?.suggestionDao()?.delete(Suggestion.TYPE_DEFAULT, companyCode, timeNow)
        if (suggestionList.size > 0) {
            suggestionDatabase?.suggestionDao()?.insert(suggestionList)
        }
        Timber.d("%s: %s", companyCode, suggestionList.size)

        return Result.success(outputData)
    }
}