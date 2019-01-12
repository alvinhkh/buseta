package com.alvinhkh.buseta.route.dao

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.TypeConverters
import android.content.Context
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop


@Database(entities = [(Route::class), (RouteStop::class)], version = 2)
@TypeConverters(LatLngListConverter::class)
abstract class RouteDatabase : RoomDatabase() {

    abstract fun routeDao(): RouteDao

    abstract fun routeStopDao(): RouteStopDao

    companion object {
        private var instance: RouteDatabase? = null

        fun getInstance(context: Context): RouteDatabase? {
            if (instance == null) {
                synchronized(RouteDatabase::class.java) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            RouteDatabase::class.java, "routes.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return instance
        }
    }
}
