package com.alvinhkh.buseta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SuggestionsDatabase {

    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "suggestions.db";
    public final static String TABLE_NAME = "suggestions";
    public final static String COLUMN_ID = "_id";
    public final static String COLUMN_TEXT = "text";
    public final static String COLUMN_TYPE = "type";
    public final static String COLUMN_DATE = "date";
    public final static String TYPE_DEFAULT = "default";
    public final static String TYPE_HISTORY = "history";

    private Helper mHelper;
    private SQLiteDatabase db;

    public SuggestionsDatabase(Context context) {
        mHelper = new Helper(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = mHelper.getWritableDatabase();
    }

    public long insert(ContentValues values) {
        return db.insert(TABLE_NAME, null, values);
    }

    public void insertDefault(String text) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_TYPE, TYPE_DEFAULT);
        values.put(COLUMN_DATE, "0");
        db.insert(TABLE_NAME, null, values);
    }

    public long insertHistory(String text) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_TYPE, TYPE_HISTORY);
        values.put(COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
        return db.insert(TABLE_NAME, null, values);
    }

    public Cursor get(String text) {
        return db.rawQuery("SELECT * FROM (" +
                // 3 history
                " SELECT * " + " FROM " + TABLE_NAME +
                " WHERE " + COLUMN_TEXT + " LIKE '" + text + "%'" +
                " AND " + COLUMN_TYPE + " = '" + TYPE_HISTORY + "'" +
                " ORDER BY " + COLUMN_DATE + " DESC" +
                " LIMIT 0,3" +
                " ) UNION SELECT * FROM (" +
                // All others
                " SELECT * FROM " + TABLE_NAME +
                " WHERE " + COLUMN_TEXT + " LIKE '" + text + "%'" +
                " AND " + COLUMN_TYPE + " = '" + TYPE_DEFAULT + "'" +
                " AND " + COLUMN_TEXT + " NOT IN (" +
                // exclude 3 history
                " SELECT " + COLUMN_TEXT + " FROM " + TABLE_NAME +
                " WHERE " + COLUMN_TEXT + " LIKE '" + text + "%'" +
                " AND " + COLUMN_TYPE + " = '" + TYPE_HISTORY + "'" +
                " ORDER BY " + COLUMN_DATE + " DESC" +
                " LIMIT 0,3" +
                " )" +
                " ORDER BY " + COLUMN_TEXT + " ASC" +
                " )" +
                " ORDER BY " + COLUMN_DATE + " DESC"
                , null);
    }

    public Cursor getByType(String text, String type) {
        return getByType(text, type, null);
    }
    public Cursor getByType(String text, String type, String limit) {
        String orderBy = null;
        if (type.equals(TYPE_HISTORY)) {
            orderBy = COLUMN_DATE + " DESC";
        }
        return db.query(TABLE_NAME,
                new String[]{
                        COLUMN_ID,
                        COLUMN_TEXT,
                        COLUMN_TYPE,
                        COLUMN_DATE,
                },
                COLUMN_TEXT + " LIKE '" + text + "%'"
                        + " AND " + COLUMN_TYPE + " = '" + type + "'"
                , null, null, null, orderBy, limit);
    }

    public Cursor getHistory() {
        return getByType("%", TYPE_HISTORY, null);
    }
    public Cursor getHistory(String limit) {
        return getByType("%", TYPE_HISTORY, limit);
    }

    public boolean clearHistory() {
        return db.delete(TABLE_NAME, COLUMN_TYPE + "=?",
                new String[]{TYPE_HISTORY}) > 0;
    }

    public boolean deleteHistory(String text) {
        return db.delete(TABLE_NAME, COLUMN_TYPE + "=? AND " + COLUMN_TEXT + "=?",
                new String[]{TYPE_HISTORY, text}) > 0;
    }

    public boolean clearDefault() {
        return db.delete(TABLE_NAME, COLUMN_TYPE + "=?",
                new String[]{TYPE_DEFAULT}) > 0;
    }

    private class Helper extends SQLiteOpenHelper {

        public Helper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                      int version) {
            super(context, name, factory, version);
        }

        private static final String DATABASE_CREATE = "CREATE TABLE "
                + TABLE_NAME
                + "("
                + COLUMN_ID + " INTEGER primary key autoincrement, "
                + COLUMN_TEXT + " TEXT not NULL, "
                + COLUMN_TYPE + " TEXT not NULL, "
                + COLUMN_DATE + " TEXT not NULL, "
                + "UNIQUE (" + COLUMN_TEXT + ", " + COLUMN_TYPE + ") ON CONFLICT REPLACE"
                + ");";

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(DATABASE_NAME,
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

    }

}