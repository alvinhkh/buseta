package com.alvinhkh.buseta.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.alvinhkh.buseta.holder.RouteStop;

public class FavouriteDatabase {

    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "favourite.db";
    public final static String TABLE_NAME = "favourite";
    public final static String COLUMN_ID = "_id";
    public final static String COLUMN_DATE = "date";
    public final static String COLUMN_ROUTE = "no";
    public final static String COLUMN_BOUND = "bound";
    public final static String COLUMN_ORIGIN = "origin";
    public final static String COLUMN_DESTINATION = "destination";
    public final static String COLUMN_STOP_SEQ = "stop_seq";
    public final static String COLUMN_STOP_CODE = "stop_code";
    public final static String COLUMN_STOP_NAME = "stop_name";

    private Helper mHelper;
    private SQLiteDatabase db;

    public FavouriteDatabase(Context context) {
        mHelper = new Helper(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = mHelper.getWritableDatabase();
    }

    public void close() {
        if (null != db && db.isOpen())
            db.close();
    }

    public long insert(ContentValues values) {
        return db.isOpen() ? db.insert(TABLE_NAME, null, values) : -1;
    }

    public long insertStop(RouteStop routeStop) {
        if (null == routeStop || null == routeStop.route_bound) return -1;
        ContentValues values = new ContentValues();
        values.put(COLUMN_ROUTE, routeStop.route_bound.route_no);
        values.put(COLUMN_BOUND, routeStop.route_bound.route_bound);
        values.put(COLUMN_ORIGIN, routeStop.route_bound.origin_tc);
        values.put(COLUMN_DESTINATION, routeStop.route_bound.destination_tc);
        values.put(COLUMN_STOP_SEQ, routeStop.stop_seq);
        values.put(COLUMN_STOP_CODE, routeStop.code);
        values.put(COLUMN_STOP_NAME, routeStop.name_tc);
        values.put(COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
        return insert(values);
    }

    public Cursor get() {
        return (db.isOpen()) ?
                db.rawQuery(
                        " SELECT * " + " FROM " + TABLE_NAME +
                                " ORDER BY " + COLUMN_DATE + " DESC"
                        , null)
                : null;
    }

    public Cursor getExist(RouteStop routeStop) {
        if (null == routeStop || null == routeStop.route_bound) return null;
        String route_no = routeStop.route_bound.route_no;
        String route_bound = routeStop.route_bound.route_bound;
        String stop_code = routeStop.code;
        return (db.isOpen()) ?
                db.rawQuery(
                        " SELECT * " + " FROM " + TABLE_NAME +
                                " WHERE " + COLUMN_ROUTE + " = '" + route_no + "'" +
                                " AND " + COLUMN_BOUND + " = '" + route_bound + "'" +
                                " AND " + COLUMN_STOP_CODE + " = '" + stop_code + "'" +
                                " ORDER BY " + COLUMN_DATE + " DESC"
                        , null)
                : null;
    }

    public boolean deleteAll() {
        return (db.isOpen()) ?
                db.delete(TABLE_NAME, null, null) > 0 : false;
    }

    public boolean delete(RouteStop routeStop) {
        if (null == routeStop || null == routeStop.route_bound) return false;
        return (db.isOpen()) ?
                db.delete(TABLE_NAME, COLUMN_ROUTE + "=? AND " +
                                COLUMN_BOUND + "=?  AND " +
                                COLUMN_STOP_CODE + "=? ",
                new String[]{
                        routeStop.route_bound.route_no,
                        routeStop.route_bound.route_bound,
                        routeStop.code}) > 0
                : false;
    }

    public boolean delete(String route_no, String route_bound, String stop_code) {
        return (db.isOpen()) ?
                db.delete(TABLE_NAME, COLUMN_ROUTE + "=? AND " +
                                COLUMN_BOUND + "=?  AND " +
                                COLUMN_STOP_CODE + "=? ",
                        new String[]{
                                route_no,
                                route_bound,
                                stop_code}) > 0
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
                + COLUMN_DATE + " TEXT not NULL, "
                + COLUMN_ROUTE + " TEXT not NULL, "
                + COLUMN_BOUND + " TEXT not NULL, "
                + COLUMN_ORIGIN + " TEXT not NULL, "
                + COLUMN_DESTINATION + " TEXT not NULL, "
                + COLUMN_STOP_SEQ + " TEXT not NULL, "
                + COLUMN_STOP_CODE + " TEXT not NULL, "
                + COLUMN_STOP_NAME + " TEXT not NULL, "
                + "UNIQUE (" + COLUMN_ROUTE + ", "
                + COLUMN_BOUND + ", " + COLUMN_STOP_CODE
                + ") ON CONFLICT REPLACE"
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