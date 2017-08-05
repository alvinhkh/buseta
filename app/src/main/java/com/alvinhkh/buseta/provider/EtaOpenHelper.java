package com.alvinhkh.buseta.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EtaOpenHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "eta.db";
    private static final int DATABASE_VERSION = 1;

    public EtaOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        EtaContract.EtaEntry.onCreate(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        EtaContract.EtaEntry.onUpgrade(database, oldVersion, newVersion);
    }

}