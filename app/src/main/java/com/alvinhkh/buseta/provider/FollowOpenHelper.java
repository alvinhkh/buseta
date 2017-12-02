package com.alvinhkh.buseta.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FollowOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "route.db";
    private static final int DATABASE_VERSION = 7;

    public FollowOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        FollowTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        FollowTable.onUpgrade(database, oldVersion, newVersion);
    }

}