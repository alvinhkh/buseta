package com.alvinhkh.buseta.follow.dao

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
import android.content.Context
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.follow.model.FollowGroup


@Database(entities = [(Follow::class), (FollowGroup::class)], version = 9)
abstract class FollowDatabase : RoomDatabase() {

    abstract fun followDao(): FollowDao

    abstract fun followGroupDao(): FollowGroupDao

    companion object {
        private var instance: FollowDatabase? = null

        fun MIGRATION_1_to_8 (db: SupportSQLiteDatabase) {
            db.execSQL(("CREATE TABLE " + "follow" + "_TEMP ("
                    + "_id" + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                    + ", " + "type" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "company" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "no" + " TEXT NOT NULL"
                    + ", " + "route_id" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "bound" + " TEXT NOT NULL"
                    + ", " + "route_service_type" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "destination" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "origin" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "stop_code" + " TEXT NOT NULL"
                    + ", " + "stop_name" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "stop_seq" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "stop_latitude" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "stop_longitude" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "eta_get" + " TEXT NOT NULL DEFAULT ''"
                    + ", " + "display_order" + " INTEGER NOT NULL DEFAULT 0"
                    + ", " + "date" + " INTEGER NOT NULL DEFAULT 0"
                    + ", UNIQUE ("
                    + "type" + ", " + "company"
                    + ", " + "no" + ", " + "bound"
                    + ", " + "route_service_type"
                    + ", " + "stop_code" + ", " + "stop_seq"
                    + ") ON CONFLICT REPLACE"
                    + ");"))
            db.execSQL("CREATE UNIQUE INDEX index_" + "follow" + "_"
                    + "type" + "_" + "company" + "_"
                    + "no" + "_" + "bound" + "_"
                    + "route_service_type" + "_"
                    + "stop_code" + "_" + "stop_seq"
                    + " ON " + "follow" + "_TEMP ("
                    + "type" + ", " + "company"
                    + ", " + "no" + ", " + "bound"
                    + ", " + "route_service_type"
                    + ", " + "stop_code" + ", " + "stop_seq"
                    + ");")
            db.execSQL("INSERT INTO " + "follow" + "_TEMP ("
                    + "date" + ", " + "no"
                    + ", " + "bound" + ", " + "origin"
                    + ", " + "destination" + ", " + "stop_seq"
                    + ", " + "stop_code" + ", " + "stop_name"
                    + ") SELECT "
                    + "date, no"
                    + ", bound, origin"
                    + ", destination, stop_seq"
                    + ", stop_code, stop_name"
                    + " FROM favourite")
            db.execSQL("UPDATE " + "follow" + "_TEMP SET " + "company" + " = 'KMB' WHERE (" +
                    "company" + " IS NULL OR " + "company" + " IS ''" + ")")
            db.execSQL("UPDATE " + "follow" + "_TEMP SET " + "type" + " = '" + "route_stop" + "' WHERE (" +
                    "type" + " IS NULL OR " + "type" + " IS ''" + ")")
            db.execSQL("UPDATE " + "follow" + "_TEMP SET " + "route_service_type" + " = '01' WHERE (" +
                    "company" + " IS 'KMB') AND (" +
                    "route_service_type" + " IS NULL OR " + "route_service_type" + " IS ''" + ")")
            db.execSQL("DROP TABLE IF EXISTS " + "follow")
            db.execSQL("ALTER TABLE " + "follow" + "_TEMP RENAME TO " + "follow")
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
                db.execSQL(("CREATE TABLE " + "follow" + "_TEMP ("
                        + "_id" + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                        + ", " + "type" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "company" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "no" + " TEXT NOT NULL"
                        + ", " + "route_id" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "bound" + " TEXT NOT NULL"
                        + ", " + "route_service_type" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "destination" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "origin" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "stop_code" + " TEXT NOT NULL"
                        + ", " + "stop_name" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "stop_seq" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "stop_latitude" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "stop_longitude" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "eta_get" + " TEXT NOT NULL DEFAULT ''"
                        + ", " + "display_order" + " INTEGER NOT NULL DEFAULT 0"
                        + ", " + "date" + " INTEGER NOT NULL DEFAULT 0"
                        + ", UNIQUE ("
                        + "type" + ", " + "company"
                        + ", " + "no" + ", " + "bound"
                        + ", " + "route_service_type"
                        + ", " + "stop_code" + ", " + "stop_seq"
                        + ") ON CONFLICT REPLACE"
                        + ");"))
                db.execSQL("CREATE UNIQUE INDEX index_" + "follow" + "_"
                        + "type" + "_" + "company" + "_"
                        + "no" + "_" + "bound" + "_"
                        + "route_service_type" + "_"
                        + "stop_code" + "_" + "stop_seq"
                        + " ON " + "follow" + "_TEMP ("
                        + "type" + ", " + "company"
                        + ", " + "no" + ", " + "bound"
                        + ", " + "route_service_type"
                        + ", " + "stop_code" + ", " + "stop_seq"
                        + ");")
                db.execSQL("INSERT INTO " + "follow" + "_TEMP ("
                        + "company" + ", " + "date"
                        + ", " + "no" + ", " + "route_id"
                        + ", " + "bound" + ", " + "origin"
                        + ", " + "destination" + ", " + "stop_seq"
                        + ", " + "stop_code" + ", " + "stop_name"
                        + ", " + "display_order"
                        + ") SELECT "
                        + "company, date"
                        + ", no, route_id"
                        + ", bound, origin"
                        + ", destination, stop_seq"
                        + ", stop_code, stop_name"
                        + ", display_order"
                        + " FROM follow")
                db.execSQL("UPDATE " + "follow" + "_TEMP SET " + "company" + " = 'KMB' WHERE (" +
                        "company" + " IS NULL OR " + "company" + " IS ''" + ")")
                db.execSQL("UPDATE " + "follow" + "_TEMP SET " + "type" + " = '" + "route_stop" + "' WHERE (" +
                        "type" + " IS NULL OR " + "type" + " IS ''" + ")")
                db.execSQL("UPDATE " + "follow" + "_TEMP SET " + "route_service_type" + " = '01' WHERE (" +
                        "company" + " IS 'KMB') AND (" +
                        "route_service_type" + " IS NULL OR " + "route_service_type" + " IS ''" + ")")
                db.execSQL("DROP TABLE IF EXISTS " + "follow")
                db.execSQL("ALTER TABLE " + "follow" + "_TEMP RENAME TO " + "follow")
            }
        }

        private var MIGRATION_8_9 = object: Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `route_bound`")
                db.execSQL("DROP TABLE IF EXISTS `route_stop`")
                db.execSQL("DROP TABLE IF EXISTS `eta`")

                db.execSQL("""CREATE TABLE `follow_TEMP` (
                    `_id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `category_id` TEXT NOT NULL DEFAULT '',
                    `type` TEXT NOT NULL DEFAULT '',
                    `company` TEXT NOT NULL DEFAULT '',
                    `no` TEXT NOT NULL,
                    `route_id` TEXT NOT NULL DEFAULT '',
                    `bound` TEXT NOT NULL,
                    `route_service_type` TEXT NOT NULL DEFAULT '',
                    `destination` TEXT NOT NULL DEFAULT '',
                    `origin` TEXT NOT NULL DEFAULT '',
                    `stop_code` TEXT NOT NULL,
                    `stop_name` TEXT NOT NULL DEFAULT '',
                    `stop_seq` TEXT NOT NULL DEFAULT '',
                    `stop_latitude` TEXT NOT NULL DEFAULT '',
                    `stop_longitude` TEXT NOT NULL DEFAULT '',
                    `eta_get` TEXT NOT NULL DEFAULT '',
                    `display_order` INTEGER NOT NULL DEFAULT 0,
                    `date` INTEGER NOT NULL DEFAULT 0,
                    UNIQUE (`category_id`, `company`, `no`, `bound`, `route_service_type`, `stop_code`, `stop_seq`)
                    ON CONFLICT REPLACE)""")
                db.execSQL("""CREATE UNIQUE INDEX index_follow_category_id_company_no_bound_route_service_type_stop_code_stop_seq
                    ON `follow_TEMP` (`category_id`, `company`, `no`, `bound`, `route_service_type`, `stop_code`, `stop_seq`)""")
                db.execSQL("""INSERT INTO follow_TEMP (`type`, `company`, `no`, `route_id`,
                    `bound`, `route_service_type`, `destination`, `origin`, `stop_code`, `stop_name`,
                    `stop_seq`, `stop_latitude`, `stop_longitude`, `eta_get`, `display_order`, `date`)
                    SELECT `type`, `company`, `no`, `route_id`, `bound`, `route_service_type`,
                    `destination`, `origin`, `stop_code`, `stop_name`, `stop_seq`, `stop_latitude`,
                    `stop_longitude`, `eta_get`, `display_order`, `date` FROM `follow`""")
                db.execSQL("""UPDATE `follow_TEMP` SET
                        `category_id` = 'uncategorised'
                        WHERE (`category_id` IS NULL OR `category_id` IS '')""")
                db.execSQL("DROP TABLE IF EXISTS `follow`")
                db.execSQL("ALTER TABLE `follow_TEMP` RENAME TO `follow`")

                db.execSQL("""CREATE TABLE `follow_category_TEMP` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `name` TEXT NOT NULL DEFAULT '',
                    `colour` TEXT NOT NULL DEFAULT '',
                    `display_order` INTEGER NOT NULL DEFAULT 0,
                    `updated_at` INTEGER NOT NULL DEFAULT 0,
                    UNIQUE (`id`)
                    ON CONFLICT REPLACE)""")
                db.execSQL("""CREATE UNIQUE INDEX index_follow_category_id
                    ON `follow_category_TEMP` (`id`)""")
                db.execSQL("DROP TABLE IF EXISTS `follow_category`")
                db.execSQL("ALTER TABLE `follow_category_TEMP` RENAME TO `follow_category`")
                db.execSQL("INSERT INTO `follow_category` (`id`, `name`) VALUES ('uncategorised', '')")
            }
        }

        fun getInstance(context: Context): FollowDatabase? {
            if (instance == null) {
                synchronized(FollowDatabase::class.java) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            FollowDatabase::class.java, "route.db")
                            .addMigrations(MIGRATION_1_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_2_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_3_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_4_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_5_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_6_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                            .addMigrations(MIGRATION_8_9)
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return instance
        }
    }
}
