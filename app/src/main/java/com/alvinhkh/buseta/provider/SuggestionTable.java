package com.alvinhkh.buseta.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SuggestionTable {

    public final static String TABLE_NAME = "suggestions";
    public final static String COLUMN_ID = "_id";
    public final static String COLUMN_TEXT = "text";
    public final static String COLUMN_TYPE = "type";
    public final static String COLUMN_DATE = "date";
    public final static String TYPE_DEFAULT = "default";
    public final static String TYPE_HISTORY = "history";

    private static final String DATABASE_CREATE = "CREATE TABLE "
            + TABLE_NAME
            + "("
            + COLUMN_ID + " INTEGER primary key autoincrement, "
            + COLUMN_TEXT + " TEXT not NULL, "
            + COLUMN_TYPE + " TEXT not NULL, "
            + COLUMN_DATE + " TEXT not NULL, "
            + "UNIQUE (" + COLUMN_TEXT + ", " + COLUMN_TYPE + ") ON CONFLICT REPLACE"
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
