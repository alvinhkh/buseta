package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RouteStopTable {

    public final static String TABLE_NAME = "route_stop";
    public final static String PREFIX = "rs_";
    public final static String COLUMN_ID = PREFIX + "_id";
    public final static String COLUMN_DATE = PREFIX + "date";
    public final static String COLUMN_ROUTE = PREFIX + "route_no";
    public final static String COLUMN_BOUND = PREFIX + "route_bound";
    public final static String COLUMN_ORIGIN = PREFIX + "route_origin";
    public final static String COLUMN_ORIGIN_EN = PREFIX + "route_origin_en";
    public final static String COLUMN_DESTINATION = PREFIX + "route_destination";
    public final static String COLUMN_DESTINATION_EN = PREFIX + "route_destination_en";
    public final static String COLUMN_STOP_SEQ = PREFIX + "stop_seq";
    public final static String COLUMN_STOP_CODE = PREFIX + "stop_code";
    public final static String COLUMN_STOP_NAME = PREFIX + "stop_name";
    public final static String COLUMN_STOP_NAME_EN = PREFIX + "stop_name_en";
    public final static String COLUMN_STOP_FARE = PREFIX + "stop_fare";
    public final static String COLUMN_STOP_LAT = PREFIX + "stop_latitude";
    public final static String COLUMN_STOP_LONG = PREFIX + "stop_longitude";

    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_NAME
            + "("
            + COLUMN_ID + " INTEGER primary key autoincrement, "
            + COLUMN_DATE + " TEXT not NULL, "
            + COLUMN_ROUTE + " TEXT not NULL, "
            + COLUMN_BOUND + " TEXT not NULL, "
            + COLUMN_ORIGIN + " TEXT not NULL, "
            + COLUMN_ORIGIN_EN + " TEXT, "
            + COLUMN_DESTINATION + " TEXT not NULL, "
            + COLUMN_DESTINATION_EN + " TEXT, "
            + COLUMN_STOP_SEQ + " TEXT not NULL, "
            + COLUMN_STOP_CODE + " TEXT not NULL, "
            + COLUMN_STOP_NAME + " TEXT not NULL, "
            + COLUMN_STOP_NAME_EN + " TEXT, "
            + COLUMN_STOP_FARE + " TEXT, "
            + COLUMN_STOP_LAT + " TEXT, "
            + COLUMN_STOP_LONG + " TEXT, "
            + "UNIQUE (" + COLUMN_ROUTE + ", " + COLUMN_BOUND + ", "
            + COLUMN_STOP_SEQ + ", " + COLUMN_STOP_CODE
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
