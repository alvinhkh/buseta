package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class EtaTable {

    public final static String TABLE_NAME = "eta";
    public final static String COLUMN_ID = "eta__id";
    public final static String COLUMN_DATE = "eta_date";
    public final static String COLUMN_ROUTE = "eta_route_no";
    public final static String COLUMN_BOUND = "eta_bound";
    public final static String COLUMN_STOP_SEQ = "eta_stop_seq";
    public final static String COLUMN_STOP_CODE = "eta_stop_code";
    public final static String COLUMN_ETA_API = "eta_api_version";
    public final static String COLUMN_ETA_TIME = "eta_time";
    public final static String COLUMN_ETA_EXPIRE = "eta_expire";
    public final static String COLUMN_SERVER_TIME = "eta_server_time";
    public final static String COLUMN_UPDATED = "eta_updated";
    public final static String COLUMN_LOADING = "eta_loading";
    public final static String COLUMN_FAIL = "eta_loading_fail";

    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_NAME
            + "("
            + COLUMN_ID + " INTEGER primary key autoincrement, "
            + COLUMN_DATE + " TEXT not NULL, "
            + COLUMN_ROUTE + " TEXT not NULL, "
            + COLUMN_BOUND + " TEXT not NULL, "
            + COLUMN_STOP_SEQ + " TEXT not NULL, "
            + COLUMN_STOP_CODE + " TEXT not NULL, "
            + COLUMN_ETA_API + " TEXT, "
            + COLUMN_ETA_TIME + " TEXT, "
            + COLUMN_ETA_EXPIRE + " TEXT, "
            + COLUMN_SERVER_TIME + " TEXT, "
            + COLUMN_UPDATED + " TEXT, "
            + COLUMN_LOADING + " TEXT not NULL, "
            + COLUMN_FAIL + " TEXT not NULL, "
            + "UNIQUE (" +
            COLUMN_ROUTE + ", " + COLUMN_BOUND + ", " +
            COLUMN_STOP_SEQ + ", " + COLUMN_STOP_CODE
            + ") ON CONFLICT REPLACE"
            + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(FavouriteOpenHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
