package com.alvinhkh.buseta.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RouteOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "route.db";
    private static final int DATABASE_VERSION = 2;

    public RouteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        RouteStopTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        RouteStopTable.onUpgrade(database, oldVersion, newVersion);
    }

}