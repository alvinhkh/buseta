package com.alvinhkh.buseta.route.dao

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Room
import android.content.Context
import com.alvinhkh.buseta.model.Route


@Database(entities = [(Route::class)], version = 1)
abstract class RouteDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    companion object {
        private var instance: RouteDatabase? = null

        fun getInstance(context: Context): RouteDatabase? {
            if (instance == null) {
                synchronized(RouteDatabase::class.java) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            RouteDatabase::class.java, "routes.db")
                            .fallbackToDestructiveMigration()
                            .build()
                }
            }
            return instance
        }
    }
}
