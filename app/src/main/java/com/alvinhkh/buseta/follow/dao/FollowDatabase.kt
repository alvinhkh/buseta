package com.alvinhkh.buseta.follow.dao

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
import android.content.Context
import com.alvinhkh.buseta.follow.model.Follow


@Database(entities = [(Follow::class)], version = 8)
abstract class FollowDatabase : RoomDatabase() {

    abstract fun followDao(): FollowDao

    companion object {
        private var instance: FollowDatabase? = null

        fun MIGRATION_1_to_8 (db: SupportSQLiteDatabase) {
            db.execSQL(("CREATE TABLE " + Follow.TABLE_NAME + "_TEMP ("
                    + Follow.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                    + ", " + Follow.COLUMN_TYPE + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_COMPANY_CODE + " TEXT NOT NULL"
                    + ", " + Follow.COLUMN_ROUTE_NO + " TEXT NOT NULL"
                    + ", " + Follow.COLUMN_ROUTE_ID + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_ROUTE_SEQ + " TEXT NOT NULL"
                    + ", " + Follow.COLUMN_ROUTE_SERVICE_TYPE + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_ROUTE_DESTINATION + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_ROUTE_ORIGIN + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_STOP_ID + " TEXT NOT NULL"
                    + ", " + Follow.COLUMN_STOP_NAME + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_STOP_SEQ + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_STOP_LATITUDE + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_STOP_LONGITUDE + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_ETA_GET + " TEXT NOT NULL DEFAULT ''"
                    + ", " + Follow.COLUMN_DISPLAY_ORDER + " INTEGER NOT NULL DEFAULT 0"
                    + ", " + Follow.COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0"
                    + ", UNIQUE ("
                    + Follow.COLUMN_TYPE + ", " + Follow.COLUMN_COMPANY_CODE
                    + ", " + Follow.COLUMN_ROUTE_NO + ", " + Follow.COLUMN_ROUTE_SEQ
                    + ", " + Follow.COLUMN_ROUTE_SERVICE_TYPE
                    + ", " + Follow.COLUMN_STOP_ID + ", " + Follow.COLUMN_STOP_SEQ
                    + ") ON CONFLICT REPLACE"
                    + ");"))
            db.execSQL("CREATE UNIQUE INDEX index_" + Follow.TABLE_NAME + "_"
                    + Follow.COLUMN_TYPE + "_" + Follow.COLUMN_COMPANY_CODE + "_"
                    + Follow.COLUMN_ROUTE_NO + "_" + Follow.COLUMN_ROUTE_SEQ + "_"
                    + Follow.COLUMN_ROUTE_SERVICE_TYPE + "_"
                    + Follow.COLUMN_STOP_ID + "_" + Follow.COLUMN_STOP_SEQ
                    + " ON " + Follow.TABLE_NAME + "_TEMP ("
                    + Follow.COLUMN_TYPE + ", " + Follow.COLUMN_COMPANY_CODE
                    + ", " + Follow.COLUMN_ROUTE_NO + ", " + Follow.COLUMN_ROUTE_SEQ
                    + ", " + Follow.COLUMN_ROUTE_SERVICE_TYPE
                    + ", " + Follow.COLUMN_STOP_ID + ", " + Follow.COLUMN_STOP_SEQ
                    + ");")
            db.execSQL("INSERT INTO " + Follow.TABLE_NAME + "_TEMP ("
                    + Follow.COLUMN_UPDATED_AT + ", " + Follow.COLUMN_ROUTE_NO
                    + ", " + Follow.COLUMN_ROUTE_SEQ + ", " + Follow.COLUMN_ROUTE_ORIGIN
                    + ", " + Follow.COLUMN_ROUTE_DESTINATION + ", " + Follow.COLUMN_STOP_SEQ
                    + ", " + Follow.COLUMN_STOP_ID + ", " + Follow.COLUMN_STOP_NAME
                    + ") SELECT "
                    + "date, no"
                    + ", bound, origin"
                    + ", destination, stop_seq"
                    + ", stop_code, stop_name"
                    + " FROM favourite")
            db.execSQL("UPDATE " + Follow.TABLE_NAME + "_TEMP SET " + Follow.COLUMN_COMPANY_CODE + " = 'KMB' WHERE (" +
                    Follow.COLUMN_COMPANY_CODE + " IS NULL OR " + Follow.COLUMN_COMPANY_CODE + " IS ''" + ")")
            db.execSQL("UPDATE " + Follow.TABLE_NAME + "_TEMP SET " + Follow.COLUMN_TYPE + " = '" + Follow.TYPE_ROUTE_STOP + "' WHERE (" +
                    Follow.COLUMN_TYPE + " IS NULL OR " + Follow.COLUMN_TYPE + " IS ''" + ")")
            db.execSQL("UPDATE " + Follow.TABLE_NAME + "_TEMP SET " + Follow.COLUMN_ROUTE_SERVICE_TYPE + " = '01' WHERE (" +
                    Follow.COLUMN_COMPANY_CODE + " IS 'KMB') AND (" +
                    Follow.COLUMN_ROUTE_SERVICE_TYPE + " IS NULL OR " + Follow.COLUMN_ROUTE_SERVICE_TYPE + " IS ''" + ")")
            db.execSQL("DROP TABLE IF EXISTS " + Follow.TABLE_NAME)
            db.execSQL("ALTER TABLE " + Follow.TABLE_NAME + "_TEMP RENAME TO " + Follow.TABLE_NAME)
        }

        private var MIGRATION_1_8 = object: Migration(1, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_8(db)
            }
        }

        private var MIGRATION_2_8 = object: Migration(2, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_8(db)
            }
        }

        private var MIGRATION_3_8 = object: Migration(3, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_8(db)
            }
        }

        private var MIGRATION_4_8 = object: Migration(4, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_8(db)
            }
        }

        private var MIGRATION_5_8 = object: Migration(5, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_8(db)
            }
        }

        private var MIGRATION_6_8 = object: Migration(6, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_8(db)
            }
        }

        private var MIGRATION_7_8 = object: Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(("CREATE TABLE " + Follow.TABLE_NAME + "_TEMP ("
                        + Follow.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                        + ", " + Follow.COLUMN_TYPE + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_COMPANY_CODE + " TEXT NOT NULL"
                        + ", " + Follow.COLUMN_ROUTE_NO + " TEXT NOT NULL"
                        + ", " + Follow.COLUMN_ROUTE_ID + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_ROUTE_SEQ + " TEXT NOT NULL"
                        + ", " + Follow.COLUMN_ROUTE_SERVICE_TYPE + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_ROUTE_DESTINATION + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_ROUTE_ORIGIN + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_STOP_ID + " TEXT NOT NULL"
                        + ", " + Follow.COLUMN_STOP_NAME + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_STOP_SEQ + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_STOP_LATITUDE + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_STOP_LONGITUDE + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_ETA_GET + " TEXT NOT NULL DEFAULT ''"
                        + ", " + Follow.COLUMN_DISPLAY_ORDER + " INTEGER NOT NULL DEFAULT 0"
                        + ", " + Follow.COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0"
                        + ", UNIQUE ("
                        + Follow.COLUMN_TYPE + ", " + Follow.COLUMN_COMPANY_CODE
                        + ", " + Follow.COLUMN_ROUTE_NO + ", " + Follow.COLUMN_ROUTE_SEQ
                        + ", " + Follow.COLUMN_ROUTE_SERVICE_TYPE
                        + ", " + Follow.COLUMN_STOP_ID + ", " + Follow.COLUMN_STOP_SEQ
                        + ") ON CONFLICT REPLACE"
                        + ");"))
                db.execSQL("CREATE UNIQUE INDEX index_" + Follow.TABLE_NAME + "_"
                        + Follow.COLUMN_TYPE + "_" + Follow.COLUMN_COMPANY_CODE + "_"
                        + Follow.COLUMN_ROUTE_NO + "_" + Follow.COLUMN_ROUTE_SEQ + "_"
                        + Follow.COLUMN_ROUTE_SERVICE_TYPE + "_"
                        + Follow.COLUMN_STOP_ID + "_" + Follow.COLUMN_STOP_SEQ
                        + " ON " + Follow.TABLE_NAME + "_TEMP ("
                        + Follow.COLUMN_TYPE + ", " + Follow.COLUMN_COMPANY_CODE
                        + ", " + Follow.COLUMN_ROUTE_NO + ", " + Follow.COLUMN_ROUTE_SEQ
                        + ", " + Follow.COLUMN_ROUTE_SERVICE_TYPE
                        + ", " + Follow.COLUMN_STOP_ID + ", " + Follow.COLUMN_STOP_SEQ
                        + ");")
                db.execSQL("INSERT INTO " + Follow.TABLE_NAME + "_TEMP ("
                        + Follow.COLUMN_COMPANY_CODE + ", " + Follow.COLUMN_UPDATED_AT
                        + ", " + Follow.COLUMN_ROUTE_NO + ", " + Follow.COLUMN_ROUTE_ID
                        + ", " + Follow.COLUMN_ROUTE_SEQ + ", " + Follow.COLUMN_ROUTE_ORIGIN
                        + ", " + Follow.COLUMN_ROUTE_DESTINATION + ", " + Follow.COLUMN_STOP_SEQ
                        + ", " + Follow.COLUMN_STOP_ID + ", " + Follow.COLUMN_STOP_NAME
                        + ", " + Follow.COLUMN_DISPLAY_ORDER
                        + ") SELECT "
                        + "company, date"
                        + ", no, route_id"
                        + ", bound, origin"
                        + ", destination, stop_seq"
                        + ", stop_code, stop_name"
                        + ", display_order"
                        + " FROM follow")
                db.execSQL("UPDATE " + Follow.TABLE_NAME + "_TEMP SET " + Follow.COLUMN_COMPANY_CODE + " = 'KMB' WHERE (" +
                        Follow.COLUMN_COMPANY_CODE + " IS NULL OR " + Follow.COLUMN_COMPANY_CODE + " IS ''" + ")")
                db.execSQL("UPDATE " + Follow.TABLE_NAME + "_TEMP SET " + Follow.COLUMN_TYPE + " = '" + Follow.TYPE_ROUTE_STOP + "' WHERE (" +
                        Follow.COLUMN_TYPE + " IS NULL OR " + Follow.COLUMN_TYPE + " IS ''" + ")")
                db.execSQL("UPDATE " + Follow.TABLE_NAME + "_TEMP SET " + Follow.COLUMN_ROUTE_SERVICE_TYPE + " = '01' WHERE (" +
                        Follow.COLUMN_COMPANY_CODE + " IS 'KMB') AND (" +
                        Follow.COLUMN_ROUTE_SERVICE_TYPE + " IS NULL OR " + Follow.COLUMN_ROUTE_SERVICE_TYPE + " IS ''" + ")")
                db.execSQL("DROP TABLE IF EXISTS " + Follow.TABLE_NAME)
                db.execSQL("ALTER TABLE " + Follow.TABLE_NAME + "_TEMP RENAME TO " + Follow.TABLE_NAME)
            }
        }

        fun getInstance(context: Context): FollowDatabase? {
            if (instance == null) {
                synchronized(FollowDatabase::class.java) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            FollowDatabase::class.java, "route.db")
                            .addMigrations(MIGRATION_1_8)
                            .addMigrations(MIGRATION_2_8)
                            .addMigrations(MIGRATION_3_8)
                            .addMigrations(MIGRATION_4_8)
                            .addMigrations(MIGRATION_5_8)
                            .addMigrations(MIGRATION_6_8)
                            .addMigrations(MIGRATION_7_8)
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return instance
        }
    }
}
