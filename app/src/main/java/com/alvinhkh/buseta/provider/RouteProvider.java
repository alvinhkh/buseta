package com.alvinhkh.buseta.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

public class RouteProvider extends ContentProvider {

    private RouteOpenHelper mHelper;

    private static final String AUTHORITY = "com.alvinhkh.buseta.RouteProvider";
    private static final String BASE_PATH = "route";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);

    // used for the UriMatcher
    private static final int ROUTES = 10;
    private static final int ROUTE_ID = 11;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, ROUTES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", ROUTE_ID);
    }

    @Override
    public boolean onCreate() {
        mHelper = new RouteOpenHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case ROUTES:
                // Set the table
                queryBuilder.setTables(RouteStopTable.TABLE_NAME);
                break;
            case ROUTE_ID:
                // Set the table
                queryBuilder.setTables(RouteStopTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(RouteStopTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case ROUTES:
                id = sqlDB.insert(RouteStopTable.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        switch (uriType) {
            case ROUTES:
                return Uri.parse(BASE_PATH + "/#" + id);
            default:
                return null;
        }
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values){
        int uriType = sURIMatcher.match(uri);
        int numInserted = 0;
        String tableName;
        switch (uriType) {
            case ROUTES:
                tableName = RouteStopTable.TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        try {
            for (ContentValues cv : values) {
                long newID = sqlDB.insertOrThrow(tableName, null, cv);
                if (newID <= 0)
                    throw new SQLException("Failed to insert row into " + uri);
                numInserted++;
            }
            sqlDB.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(uri, null);
        } finally {
            sqlDB.endTransaction();
        }
        return numInserted;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case ROUTES:
                rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case ROUTE_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME,
                            RouteStopTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(RouteStopTable.TABLE_NAME,
                            RouteStopTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case ROUTES:
                rowsUpdated = sqlDB.update(RouteStopTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case ROUTE_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(RouteStopTable.TABLE_NAME,
                            values,
                            RouteStopTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(RouteStopTable.TABLE_NAME,
                            values,
                            RouteStopTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                RouteStopTable.COLUMN_ID,
                RouteStopTable.COLUMN_DATE,
                RouteStopTable.COLUMN_ROUTE,
                RouteStopTable.COLUMN_BOUND,
                RouteStopTable.COLUMN_ORIGIN,
                RouteStopTable.COLUMN_ORIGIN_EN,
                RouteStopTable.COLUMN_DESTINATION,
                RouteStopTable.COLUMN_DESTINATION_EN,
                RouteStopTable.COLUMN_STOP_SEQ,
                RouteStopTable.COLUMN_STOP_CODE,
                RouteStopTable.COLUMN_STOP_NAME,
                RouteStopTable.COLUMN_STOP_NAME_EN,
                RouteStopTable.COLUMN_STOP_FARE,
                RouteStopTable.COLUMN_STOP_LAT,
                RouteStopTable.COLUMN_STOP_LONG,
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}