package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.nwst.NwstService.*
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

class NwstStopTimetableWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

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
            if (!routeId.isEmpty()) {
                val temp = routeId.substring(1).split("\\*{3}".toRegex()).
                        dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temp.size >= 4) {
                    val rdv = temp[0] + "||" + temp[1] + "||" + temp[2] + "||" + temp[3]
                    val syscode = NwstRequestUtil.syscode()
                    val syscode2 = NwstRequestUtil.syscode2()
                    val response = nwstService.timetable(rdv, routeSequence,
                            LANGUAGE_TC, syscode, PLATFORM, APP_VERSION, syscode2).execute()
                    val res = response.body()
                    val timetableHtml = res?.string()?:""

                    val file = File(context.cacheDir, "nwst-timeable-" + routeNo + "-" + System.currentTimeMillis() + ".html")
                    val outStream = FileOutputStream(file)
                    PrintStream(outStream).use { out -> out.print(timetableHtml) }
                    outStream.close()
                    Timber.d("cache file: %s", file)

                    outputData = Data.Builder()
                            .putString(C.EXTRA.COMPANY_CODE, companyCode)
                            .putString(C.EXTRA.ROUTE_ID, routeId)
                            .putString(C.EXTRA.ROUTE_NO, routeNo)
                            .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                            .putString(C.EXTRA.ROUTE_TIMETABLE_FILE, file.absolutePath)
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