package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;

import timber.log.Timber;

public class FollowTable {

    public final static String TABLE_NAME = "follow";
    public final static String COLUMN_ID = "_id";
    public final static String COLUMN_COMPANY = "company";
    public final static String COLUMN_DATE = "date";
    public final static String COLUMN_ROUTE = "no";
    public final static String COLUMN_ROUTE_ID = "route_id";
    public final static String COLUMN_BOUND = "bound";
    public final static String COLUMN_ORIGIN = "origin";
    public final static String COLUMN_DESTINATION = "destination";
    public final static String COLUMN_STOP_SEQ = "stop_seq";
    public final static String COLUMN_STOP_CODE = "stop_code";
    public final static String COLUMN_STOP_NAME = "stop_name";
    public final static String COLUMN_DISPLAY_ORDER = "display_order";

    private static String DATABASE_CREATE(String tableName) {
        return "CREATE TABLE "
                + tableName
                + "("
                + COLUMN_ID + " INTEGER primary key autoincrement, "
                + COLUMN_COMPANY + " TEXT not NULL DEFAULT '', "
                + COLUMN_DATE + " TEXT not NULL, "
                + COLUMN_ROUTE + " TEXT not NULL, "
                + COLUMN_ROUTE_ID + " TEXT not NULL DEFAULT '', "
                + COLUMN_BOUND + " TEXT not NULL, "
                + COLUMN_ORIGIN + " TEXT, "
                + COLUMN_DESTINATION + " TEXT, "
                + COLUMN_STOP_SEQ + " TEXT not NULL, "
                + COLUMN_STOP_CODE + " TEXT not NULL, "
                + COLUMN_STOP_NAME + " TEXT not NULL, "
                + COLUMN_DISPLAY_ORDER + " INTEGER DEFAULT 0, "
                + "UNIQUE (" + COLUMN_COMPANY + ", " + COLUMN_ROUTE + ", "
                + COLUMN_BOUND + ", " + COLUMN_STOP_CODE
                + ") ON CONFLICT REPLACE"
                + ");";
    };

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE(TABLE_NAME));
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 6) {
            Timber.d("Upgrading database from version %s to %s, try to preserve old data", oldVersion, newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            db.execSQL(DATABASE_CREATE(TABLE_NAME));
            String OLD_TABLE_NAME = "favourite";
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" +
                    COLUMN_DATE + ", " + COLUMN_ROUTE + ", " + COLUMN_BOUND + ", " + COLUMN_ORIGIN + ", " +
                    COLUMN_DESTINATION + ", " + COLUMN_STOP_SEQ + ", " + COLUMN_STOP_CODE + ", " + COLUMN_STOP_NAME +
                    ") SELECT " +
                    COLUMN_DATE + ", " + COLUMN_ROUTE + ", " + COLUMN_BOUND + ", " + COLUMN_ORIGIN + ", " +
                    COLUMN_DESTINATION + ", " + COLUMN_STOP_SEQ + ", " + COLUMN_STOP_CODE + ", " + COLUMN_STOP_NAME +
                    " FROM " + OLD_TABLE_NAME);
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_COMPANY + " = 'KMB' WHERE (" +
                    COLUMN_COMPANY + " IS NULL OR " + COLUMN_COMPANY + " IS ''" + ")");
        } else {
            Timber.d("Upgrading database from version %s to %s, which will destroy all old data", oldVersion, newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

}
