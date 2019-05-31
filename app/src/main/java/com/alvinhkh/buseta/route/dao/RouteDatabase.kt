package com.alvinhkh.buseta.route.dao

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import android.content.Context
import androidx.preference.PreferenceManager
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop


@Database(entities = [(Route::class), (RouteStop::class)], version = 3)
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
                            .addMigrations(object: Migration(0, 3) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                                    val editor = sharedPreferences.edit()
                                    editor.putLong("last_update_suggestions", 0)
                                    editor.apply()
                                    throw IllegalStateException()
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return instance
        }
    }
}
