package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.utils.HashUtil
import timber.log.Timber

class NwstTokenWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        var outputData= Data.Builder()
                .putString(NwstService.QUERY_TK, "")
                .build()

        try {
            val version = preferences.getString("nwst_version", NwstService.APP_VERSION)?:NwstService.APP_VERSION
            val version2 = preferences.getString("nwst_version2", NwstService.APP_VERSION2)?:NwstService.APP_VERSION2
            val tk = HashUtil.randomHexString(64)
            nwstService.pushToken(tk, tk, NwstService.LANGUAGE_TC, "", "R", NwstService.DEVICETYPE,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                    NwstRequestUtil.syscode2()).execute()
            nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "", "Y", NwstService.DEVICETYPE,
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                    NwstRequestUtil.syscode2()).execute()
            nwstService.adv(NwstService.LANGUAGE_TC, "640",
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, version, version2,
                    NwstRequestUtil.syscode2(), tk).execute()
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