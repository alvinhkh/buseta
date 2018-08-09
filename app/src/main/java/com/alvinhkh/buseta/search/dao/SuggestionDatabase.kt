package com.alvinhkh.buseta.search.dao

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
import android.content.Context


@Database(entities = [(Suggestion::class)], version = 5)
abstract class SuggestionDatabase : RoomDatabase() {

    abstract fun suggestionDao(): SuggestionDao

    companion object {
        private var instance: SuggestionDatabase? = null

        private fun MIGRATION_1_to_5(db: SupportSQLiteDatabase) {
            db.execSQL(("CREATE TABLE " + Suggestion.TABLE_NAME + "_TEMP ("
                    + Suggestion.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
                    + Suggestion.COLUMN_TEXT + " TEXT NOT NULL, "
                    + Suggestion.COLUMN_TYPE + " TEXT NOT NULL, "
                    + Suggestion.COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
                    + Suggestion.COLUMN_COMPANY + " TEXT NOT NULL DEFAULT '', "
                    + "UNIQUE (" + Suggestion.COLUMN_TEXT + ", " + Suggestion.COLUMN_COMPANY
                    + ", " + Suggestion.COLUMN_TYPE + ") ON CONFLICT REPLACE"
                    + ");"))
            db.execSQL("CREATE UNIQUE INDEX index_" + Suggestion.TABLE_NAME + "_"
                    + Suggestion.COLUMN_COMPANY + "_" + Suggestion.COLUMN_TEXT + "_"
                    + Suggestion.COLUMN_TYPE + " ON " + Suggestion.TABLE_NAME + "_TEMP ("
                    + Suggestion.COLUMN_COMPANY + ", " + Suggestion.COLUMN_TEXT + ", "
                    + Suggestion.COLUMN_TYPE + ");")
            db.execSQL("INSERT INTO " + Suggestion.TABLE_NAME + "_TEMP (" +
                    Suggestion.COLUMN_TEXT + ", " + Suggestion.COLUMN_TYPE + ", " + Suggestion.COLUMN_TIMESTAMP +
                    ") SELECT " +
                    Suggestion.COLUMN_TEXT + ", " + Suggestion.COLUMN_TYPE + ", " + Suggestion.COLUMN_TIMESTAMP +
                    " FROM " + Suggestion.TABLE_NAME)
            db.execSQL("UPDATE " + Suggestion.TABLE_NAME + "_TEMP SET " + Suggestion.COLUMN_COMPANY + " = 'KMB' WHERE (" +
                    Suggestion.COLUMN_COMPANY + " IS NULL OR " + Suggestion.COLUMN_COMPANY + " IS ''" + ")")
            db.execSQL("DROP TABLE IF EXISTS " + Suggestion.TABLE_NAME)
            db.execSQL("ALTER TABLE " + Suggestion.TABLE_NAME + "_TEMP RENAME TO " + Suggestion.TABLE_NAME)
        }

        private var MIGRATION_1_5 = object: Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_5(db)
            }
        }

        private var MIGRATION_2_5 = object: Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_5(db)
            }
        }

        private var MIGRATION_3_5 = object: Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_to_5(db)
            }
        }

        private var MIGRATION_4_5 = object: Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(("CREATE TABLE " + Suggestion.TABLE_NAME + "_TEMP ("
                        + Suggestion.COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
                        + Suggestion.COLUMN_TEXT + " TEXT NOT NULL, "
                        + Suggestion.COLUMN_TYPE + " TEXT NOT NULL, "
                        + Suggestion.COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
                        + Suggestion.COLUMN_COMPANY + " TEXT NOT NULL DEFAULT '', "
                        + "UNIQUE (" + Suggestion.COLUMN_TEXT + ", " + Suggestion.COLUMN_COMPANY
                        + ", " + Suggestion.COLUMN_TYPE + ") ON CONFLICT REPLACE"
                        + ");"))
                db.execSQL("CREATE UNIQUE INDEX index_" + Suggestion.TABLE_NAME + "_"
                        + Suggestion.COLUMN_COMPANY + "_" + Suggestion.COLUMN_TEXT + "_"
                        + Suggestion.COLUMN_TYPE + " ON " + Suggestion.TABLE_NAME + "_TEMP ("
                        + Suggestion.COLUMN_COMPANY + ", " + Suggestion.COLUMN_TEXT + ", "
                        + Suggestion.COLUMN_TYPE + ");")
                db.execSQL("INSERT INTO " + Suggestion.TABLE_NAME + "_TEMP ("
                        + Suggestion.COLUMN_TEXT + ", " + Suggestion.COLUMN_TYPE
                        + ", " + Suggestion.COLUMN_TIMESTAMP + ", " + Suggestion.COLUMN_COMPANY + ") SELECT "
                        + Suggestion.COLUMN_TEXT + ", " + Suggestion.COLUMN_TYPE
                        + ", " + Suggestion.COLUMN_TIMESTAMP + ", " + Suggestion.COLUMN_COMPANY + " FROM " + Suggestion.TABLE_NAME)
                db.execSQL("DROP TABLE IF EXISTS " + Suggestion.TABLE_NAME)
                db.execSQL("ALTER TABLE " + Suggestion.TABLE_NAME + "_TEMP RENAME TO " + Suggestion.TABLE_NAME)
            }
        }

        fun getInstance(context: Context): SuggestionDatabase? {
            if (instance == null) {
                synchronized(SuggestionDatabase::class.java) {
                    instance = Room.databaseBuilder(context.applicationContext,
                            SuggestionDatabase::class.java, "suggestions.db")
                            .addMigrations(MIGRATION_1_5, MIGRATION_2_5, MIGRATION_3_5, MIGRATION_4_5)
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return instance
        }
    }
}
