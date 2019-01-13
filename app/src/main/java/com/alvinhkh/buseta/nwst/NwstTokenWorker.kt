package com.alvinhkh.buseta.nwst

import android.content.Context
import android.support.v7.preference.PreferenceManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.utils.HashUtil
import timber.log.Timber

class NwstTokenWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        var outputData= Data.Builder()
                .putString(NwstService.QUERY_TK, "")
                .build()

        try {
            val tk = HashUtil.randomHexString(64)
            nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "Y", NwstService.DEVICETYPE,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                    NwstRequestUtil.syscode2()).execute()
            nwstService.pushToken(tk, tk, NwstService.LANGUAGE_TC, "R", NwstService.DEVICETYPE,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                    NwstRequestUtil.syscode2()).execute()
            nwstService.adv(NwstService.LANGUAGE_TC, NwstService.DEVICETYPE,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                    NwstRequestUtil.syscode2()).execute()
            val editor = preferences.edit()
            editor.putString("nwst_tk", tk)
            editor.apply()
            outputData= Data.Builder()
                    .putString(NwstService.QUERY_TK, tk)
                    .build()
            return Result.success(outputData)
        } catch (e: Exception) {
            Timber.d(e)
        }

        return Result.failure(outputData)
    }
}