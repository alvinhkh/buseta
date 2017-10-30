package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import timber.log.Timber;

public class SuggestionTable {

    public final static String TABLE_NAME = "suggestions";
    public final static String COLUMN_ID = "_id";
    public final static String COLUMN_TEXT = "text";
    public final static String COLUMN_TYPE = "type";
    public final static String COLUMN_DATE = "date";
    public final static String COLUMN_COMPANY = "company";
    public final static String TYPE_DEFAULT = "default";
    public final static String TYPE_HISTORY = "history";

    private static String DATABASE_CREATE(String tableName) {
        return "CREATE TABLE "
                + tableName
                + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TEXT + " TEXT NOT NULL, "
                + COLUMN_TYPE + " TEXT NOT NULL, "
                + COLUMN_DATE + " TEXT NOT NULL, "
                + COLUMN_COMPANY + " TEXT NOT NULL DEFAULT '', "
                + "UNIQUE (" + COLUMN_TEXT + ", " + COLUMN_COMPANY + ", " + COLUMN_TYPE + ") ON CONFLICT REPLACE"
                + ");";
    };

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE(TABLE_NAME));
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 2) {
            Timber.d("Upgrading database from version %s to %s, try to preserve old data", oldVersion, newVersion);
            db.execSQL(DATABASE_CREATE(TABLE_NAME + "_TEMP"));
            db.execSQL("INSERT INTO " + TABLE_NAME + "_TEMP (" + COLUMN_TEXT + ", " + COLUMN_TYPE + ", " + COLUMN_DATE + ") SELECT " + COLUMN_TEXT + ", " + COLUMN_TYPE + ", " + COLUMN_DATE + " FROM " + TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            db.execSQL("ALTER TABLE " + TABLE_NAME + "_TEMP RENAME TO " + TABLE_NAME);
        } else {
            Timber.d("Upgrading database from version %s to %s, which will destroy all old data", oldVersion, newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

}
