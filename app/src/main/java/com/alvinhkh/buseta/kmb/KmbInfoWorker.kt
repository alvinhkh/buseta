package com.alvinhkh.buseta.kmb

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import timber.log.Timber

class KmbInfoWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val kmbService = KmbService.webSearch.create(KmbService::class.java)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.KMB
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:""
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()

        try {
            val response = kmbService.stops(routeNo, routeSequence, routeServiceType).execute()
            if (!response.isSuccessful) {
                return Result.failure()
            }

            val res = response.body()?: return Result.failure()

            val outputData = Data.Builder()
                    .putString(C.EXTRA.COMPANY_CODE, companyCode)
                    .putString(C.EXTRA.ROUTE_NO, routeNo)
                    .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                    .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                    .putString(C.EXTRA.HTML, res.data?.additionalInfo?.tc)
                    .build()
            return Result.success(outputData)

        } catch (e: Exception) {
            Timber.d(e)
            return Result.failure()
        }
    }
}