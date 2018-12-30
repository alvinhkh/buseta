package com.alvinhkh.buseta.service

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.Api
import android.graphics.Bitmap
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


class ImageDownloadWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    override fun doWork(): Result {
        val url = inputData.getString("url")?:return Result.failure()

        val outputData = Data.Builder()
                .putString("url", url)
                .build()

        if (url.isEmpty()) {
            return Result.failure(outputData)
        }

        try {
            val response = Api.raw.create(Api::class.java).get(url).execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val bitmap = BitmapFactory.decodeStream(body.byteStream())
                    val file = File(context.cacheDir, "images-" + System.currentTimeMillis())
                    val outStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                    outStream.close()
                    Timber.d("cache file: %s", file)
                    return Result.success(Data.Builder()
                            .putString("url", url)
                            .putString("filepath", file.absolutePath)
                            .build())
                }
            }
        } catch (e: Throwable) {
            Timber.d(e)
        }

        return Result.failure(outputData)
    }
}