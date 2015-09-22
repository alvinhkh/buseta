package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class RouteBoundTable {

    public final static String TABLE_NAME = "route_bound";
    public final static String PREFIX = "rb_";
    public final static String COLUMN_ID = PREFIX + "_id";
    public final static String COLUMN_DATE = PREFIX + "date";
    public final static String COLUMN_ROUTE = PREFIX + "route_no";
    public final static String COLUMN_BOUND = PREFIX + "route_bound";
    public final static String COLUMN_ORIGIN = PREFIX + "route_origin";
    public final static String COLUMN_ORIGIN_EN = PREFIX + "route_origin_en";
    public final static String COLUMN_DESTINATION = PREFIX + "route_destination";
    public final static String COLUMN_DESTINATION_EN = PREFIX + "route_destination_en";

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
            + "UNIQUE (" + COLUMN_ROUTE + ", " + COLUMN_BOUND
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
