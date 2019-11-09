package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.nwst.NwstService.Companion.APP_VERSION
import com.alvinhkh.buseta.nwst.NwstService.Companion.LANGUAGE_TC
import com.alvinhkh.buseta.nwst.NwstService.Companion.PLATFORM
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

class NwstStopTimetableWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:return Result.failure()
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        var outputData= Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_ID, routeId)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .build()

        try {
            if (routeId.isNotEmpty()) {
                val tk = preferences.getString("nwst_tk", "")?: ""

                val temp = routeId.substring(1).split("\\*{3}".toRegex()).
                        dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temp.size >= 4) {
                    val rdv = temp[0] + "||" + temp[1] + "||" + temp[2] + "||" + temp[3]
                    val syscode = NwstRequestUtil.syscode()
                    val syscode2 = NwstRequestUtil.syscode2()

                    val timetableResponse = nwstService.timetable(rdv, routeSequence,
                            LANGUAGE_TC, syscode, PLATFORM, APP_VERSION, syscode2, tk).execute()
                    val timetableHtml = timetableResponse.body()?.string()?:""
                    val timetableFile = File(context.cacheDir, "nwst-timeable-" + routeNo + "-" + System.currentTimeMillis() + ".html")
                    val timetableStream = FileOutputStream(timetableFile)
                    PrintStream(timetableStream).use { out -> out.print(timetableHtml) }
                    timetableStream.close()
                    Timber.d("timetable file: %s", timetableFile)

                    val remarkResponse = nwstService.remark(rdv,
                            LANGUAGE_TC, syscode, PLATFORM, APP_VERSION, syscode2, tk).execute()
                    val remarkHtml = remarkResponse.body()?.string()?:""
                    val remarkFile = File(context.cacheDir, "nwst-remark-" + routeNo + "-" + System.currentTimeMillis() + ".html")
                    val remarkStream = FileOutputStream(remarkFile)
                    PrintStream(remarkStream).use { out -> out.print(remarkHtml) }
                    remarkStream.close()
                    Timber.d("remark file: %s", remarkFile)

                    outputData = Data.Builder()
                            .putString(C.EXTRA.COMPANY_CODE, companyCode)
                            .putString(C.EXTRA.ROUTE_ID, routeId)
                            .putString(C.EXTRA.ROUTE_NO, routeNo)
                            .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                            .putString(C.EXTRA.ROUTE_TIMETABLE_FILE, timetableFile.absolutePath)
                            .putString(C.EXTRA.ROUTE_REMARK_FILE, remarkFile.absolutePath)
                            .build()
                    return Result.success(outputData)
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
        }

        return Result.failure(outputData)
    }
}