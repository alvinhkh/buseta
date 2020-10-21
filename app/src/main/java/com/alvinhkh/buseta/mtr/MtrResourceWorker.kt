package com.alvinhkh.buseta.mtr

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.utils.ZipUtil
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

class MtrResourceWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    override fun doWork(): Result {
        val manualUpdate = inputData.getBoolean(C.EXTRA.MANUAL, false)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.LRTFEEDER
        val outputData = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .build()

        if (companyCode.isEmpty() || companyCode == C.PROVIDER.LRTFEEDER) {
            try {
                val mtrMobService = MtrService.mob.create(MtrService::class.java)
//            val response1 = mtrMobService.zipResources().execute()
//            val mtrMobileVersionCheck = response1.body()?:return Result.failure(outputData)
//            val busDatabaseFileUrl = mtrMobileVersionCheck.resources?.mtrBus?.url?:return Result.failure(outputData)
                val busDatabaseFileUrl = firebaseRemoteConfig.getString("mtr_bus_database_url")
                if (busDatabaseFileUrl.isEmpty()) {
                    return Result.failure(outputData)
                }
                val uri = Uri.parse(busDatabaseFileUrl)
                applicationContext.deleteDatabase("E_Bus.db")
                val fileName = uri.lastPathSegment ?: return Result.failure(outputData)
                val response2 = mtrMobService.downloadFile(busDatabaseFileUrl).execute()
                val body = response2.body() ?: return Result.failure(outputData)

                val zipFile = downloadFile(body, fileName)
                if (zipFile.exists()) {
                    if (zipFile.name.endsWith(".zip")) {
                        ZipUtil.decompress(zipFile)
                    }
                    zipFile.deleteOnExit()
                }
            } catch (e: Exception) {
                Timber.e(e)
                return Result.failure(outputData)
            }
        }

        return Result.success(outputData)
    }

    @Throws(Throwable::class)
    private fun downloadFile(body: ResponseBody, fileName: String): File {
        val data = ByteArray(1024 * 4)
//        val fileSize = body.contentLength()
        val bis = BufferedInputStream(body.byteStream(), 1024 * 8)
        val outputFile = File(applicationContext.cacheDir, fileName)
        val output = FileOutputStream(outputFile)
        var total: Long = 0
        val startTime = System.currentTimeMillis()
        var timeCount = 1
//        var totalFileSize = 0
        var count = bis.read(data)
        while (count != -1) {
            total += count.toLong()
//            totalFileSize = (fileSize / Math.pow(1024.0, 2.0)).toInt()
//            val current = Math.round(total / Math.pow(1024.0, 2.0)).toDouble()
//            val progress = (total * 100 / fileSize).toInt()
            val currentTime = System.currentTimeMillis() - startTime
            if (currentTime > 1000 * timeCount) {
                timeCount++
            }
            output.write(data, 0, count)
            count = bis.read(data)
        }
        output.flush()
        output.close()
        bis.close()
        return outputFile
    }
}