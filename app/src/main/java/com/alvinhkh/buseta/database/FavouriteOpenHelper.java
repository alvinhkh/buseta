package com.alvinhkh.buseta.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavouriteOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "favourite.db";
    private static final int DATABASE_VERSION = 2;

    public FavouriteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        FavouriteTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        FavouriteTable.onUpgrade(database, oldVersion, newVersion);
    }

}