package com.alvinhkh.buseta.utils

import androidx.room.Room
import android.content.Context
import com.alvinhkh.buseta.mtr.dao.AESBusDatabase
import timber.log.Timber
import java.io.FileOutputStream
import java.io.File


object DatabaseUtil {

    fun getAESBusDatabase(context: Context) : AESBusDatabase {
        val databaseName = "E_AES.db"
        if (!isExist(context, databaseName)) {
            copyDatabase(context, databaseName)
        }
        return Room.databaseBuilder(context, AESBusDatabase::class.java, databaseName)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
    }

    fun isExist(context: Context, databaseName: String) : Boolean {
        val dbPath = context.getDatabasePath(databaseName)
        return dbPath.exists()
    }

    fun copyDatabase(context: Context, databaseName: String) {

        val dbPath = context.getDatabasePath(databaseName)
        val cachePath = File(context.cacheDir.absolutePath + File.separator + databaseName)

        if (!cachePath.exists()) {
            return
        }

        // If the database already exists, return
        if (dbPath.exists()) {
            // return
        }

        // Make sure we have a path to the file
        dbPath.parentFile.mkdirs()

        // Try to copy database file
        try {
            val inputStream = cachePath.inputStream()
            val output = FileOutputStream(dbPath)

            val buffer = ByteArray(8192)
            var length: Int

            length = inputStream.read(buffer, 0, 8192)
            while (length > 0) {
                output.write(buffer, 0, length)
                length = inputStream.read(buffer, 0, 8192)
            }

            output.flush()
            output.close()
            inputStream.close()
        } catch (e: Throwable) {
            Timber.d(e)
        }

    }
}