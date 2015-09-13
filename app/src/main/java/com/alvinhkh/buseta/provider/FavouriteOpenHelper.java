package com.alvinhkh.buseta.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavouriteOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "favourite.db";
    private static final int DATABASE_VERSION = 5;

    public FavouriteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        FavouriteTable.onCreate(database);
        EtaTable.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (!(newVersion == 5 && oldVersion < 5))
            FavouriteTable.onUpgrade(database, oldVersion, newVersion);
        EtaTable.onUpgrade(database, oldVersion, newVersion);
    }

}