package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class EtaTable {

    public final static String TABLE_NAME = "eta";
    public final static String PREFIX = "eta_";
    public final static String COLUMN_ID = PREFIX + "_id";
    public final static String COLUMN_DATE = PREFIX + "date";
    public final static String COLUMN_ROUTE = PREFIX + "route_no";
    public final static String COLUMN_BOUND = PREFIX + "bound";
    public final static String COLUMN_STOP_SEQ = PREFIX + "stop_seq";
    public final static String COLUMN_STOP_CODE = PREFIX + "stop_code";
    public final static String COLUMN_ETA_API = PREFIX + "api_version";
    public final static String COLUMN_ETA_TIME = PREFIX + "time";
    public final static String COLUMN_ETA_WHEELCHAIR = PREFIX + "wheelchair";
    public final static String COLUMN_ETA_EXPIRE = PREFIX + "expire";
    public final static String COLUMN_SERVER_TIME = PREFIX + "server_time";
    public final static String COLUMN_UPDATED = PREFIX + "updated";
    public final static String COLUMN_LOADING = PREFIX + "loading";
    public final static String COLUMN_FAIL = PREFIX + "loading_fail";

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
            + COLUMN_ETA_WHEELCHAIR + " TEXT, "
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
        Log.w(RouteOpenHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
