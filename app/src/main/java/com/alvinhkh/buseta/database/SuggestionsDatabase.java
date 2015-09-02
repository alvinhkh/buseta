package com.alvinhkh.buseta.database;

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

    public void close() {
        if (null != db && db.isOpen())
            db.close();
    }

    public long insert(ContentValues values) {
        if (db == null || !db.isOpen())
            db = mHelper.getWritableDatabase();
        return (db.isOpen()) ?
                db.insert(TABLE_NAME, null, values)
                : -1;
    }

    public long insertDefault(String text) {
        if (db == null || !db.isOpen())
            db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_TYPE, TYPE_DEFAULT);
        values.put(COLUMN_DATE, "0");
        return (db.isOpen()) ?
                db.insert(TABLE_NAME, null, values)
                : -1;
    }

    public boolean insertDefaults(String[] texts) {
        db = mHelper.getWritableDatabase();
        if (!db.isOpen()) return false;
        db.beginTransaction();
        boolean success = true;
        for (int i = 0; i < texts.length; i++) {
            if (insertDefault(texts[i]) < 0)
                success = false;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        return success;
    }

    public long insertHistory(String text) {
        if (db == null || !db.isOpen())
            db = mHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_TYPE, TYPE_HISTORY);
        values.put(COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
        return (db.isOpen()) ?
                db.insert(TABLE_NAME, null, values)
                : -1;
    }

    public Cursor get(String text) {
        return (db.isOpen()) ?
                db.rawQuery("SELECT * FROM (" +
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
                , null)
        : null;
    }

    public Cursor getByType(String text, String type) {
        return getByType(text, type, null);
    }
    public Cursor getByType(String text, String type, String limit) {
        String orderBy = null;
        if (type.equals(TYPE_HISTORY)) {
            orderBy = COLUMN_DATE + " DESC";
        }
        return (db.isOpen()) ? db.query(TABLE_NAME,
                new String[]{
                        COLUMN_ID,
                        COLUMN_TEXT,
                        COLUMN_TYPE,
                        COLUMN_DATE,
                },
                COLUMN_TEXT + " LIKE '" + text + "%'"
                        + " AND " + COLUMN_TYPE + " = '" + type + "'"
                , null, null, null, orderBy, limit) : null;
    }

    public Cursor getHistory() {
        return getByType("%", TYPE_HISTORY, null);
    }
    public Cursor getHistory(String limit) {
        return getByType("%", TYPE_HISTORY, limit);
    }

    public boolean clearHistory() {
        return (db.isOpen()) ?
                db.delete(TABLE_NAME, COLUMN_TYPE + "=?",
                new String[]{TYPE_HISTORY}) > 0 : false;
    }

    public boolean deleteHistory(String text) {
        return (db.isOpen()) ?
                db.delete(TABLE_NAME, COLUMN_TYPE + "=? AND " + COLUMN_TEXT + "=?",
                new String[]{TYPE_HISTORY, text}) > 0
                : false;
    }

    public boolean clearDefault() {
        return (db.isOpen()) ?
                db.delete(TABLE_NAME, COLUMN_TYPE + "=?",
                new String[]{TYPE_DEFAULT}) > 0
                : false;
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