package com.alvinhkh.buseta.database;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class FavouriteProvider extends ContentProvider {

    private FavouriteOpenHelper mHelper;

    private static final String AUTHORITY = "com.alvinhkh.buseta.FavouriteProvider";
    private static final String BASE_PATH_LEFT_JOIN = "left_join";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_LEFT_JOIN);
    private static final String BASE_PATH_RIGHT_JOIN = "right_join";
    public static final Uri CONTENT_URI_ETA_JOIN = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_RIGHT_JOIN);
    private static final String BASE_PATH_FAV = "favs";
    public static final Uri CONTENT_URI_FAV = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_FAV);
    private static final String BASE_PATH_ETA = "etas";
    public static final Uri CONTENT_URI_ETA = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_ETA);

    // used for the UriMatcher
    private static final int FAV_FIRST = 10;
    private static final int ETA_FIRST = 11;
    private static final int FAV = 20;
    private static final int FAV_ID = 21;
    private static final int ETA = 30;
    private static final int ETA_ID = 31;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LEFT_JOIN, FAV_FIRST);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_RIGHT_JOIN, ETA_FIRST);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FAV, FAV);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FAV + "/#", FAV_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ETA, ETA);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_ETA + "/#", ETA_ID);
    }

    @Override
    public boolean onCreate() {
        mHelper = new FavouriteOpenHelper(getContext());
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
            case FAV_FIRST:
                // Set the table
                queryBuilder.setTables(FavouriteTable.TABLE_NAME +
                        " LEFT JOIN " + EtaTable.TABLE_NAME +
                        " ON (" +
                        FavouriteTable.COLUMN_ROUTE + "=" + EtaTable.COLUMN_ROUTE + " AND " +
                        FavouriteTable.COLUMN_BOUND + "=" + EtaTable.COLUMN_BOUND + " AND " +
                        FavouriteTable.COLUMN_STOP_SEQ + "=" + EtaTable.COLUMN_STOP_SEQ + " AND " +
                        FavouriteTable.COLUMN_STOP_CODE + "=" + EtaTable.COLUMN_STOP_CODE +
                        ")");
                break;
            case ETA_FIRST:
                // Set the table
                queryBuilder.setTables(EtaTable.TABLE_NAME +
                        " LEFT JOIN " + FavouriteTable.TABLE_NAME +
                        " ON (" +
                        FavouriteTable.COLUMN_ROUTE + "=" + EtaTable.COLUMN_ROUTE + " AND " +
                        FavouriteTable.COLUMN_BOUND + "=" + EtaTable.COLUMN_BOUND + " AND " +
                        FavouriteTable.COLUMN_STOP_SEQ + "=" + EtaTable.COLUMN_STOP_SEQ + " AND " +
                        FavouriteTable.COLUMN_STOP_CODE + "=" + EtaTable.COLUMN_STOP_CODE +
                        ")");
                break;
            case FAV:
                // Set the table
                queryBuilder.setTables(FavouriteTable.TABLE_NAME);
                break;
            case ETA:
                // Set the table
                queryBuilder.setTables(EtaTable.TABLE_NAME);
                break;
            case FAV_ID:
                // Set the table
                queryBuilder.setTables(FavouriteTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(FavouriteTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            case ETA_ID:
                // Set the table
                queryBuilder.setTables(EtaTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(EtaTable.COLUMN_ID + "=" + uri.getLastPathSegment());
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
            case FAV:
                id = sqlDB.insert(FavouriteTable.TABLE_NAME, null, values);
                break;
            case ETA:
                id = sqlDB.insert(EtaTable.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        switch (uriType) {
            case FAV:
                return Uri.parse(BASE_PATH_FAV + "/#" + id);
            case ETA:
                return Uri.parse(BASE_PATH_ETA + "/#" + id);
            default:
                return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case ETA_FIRST:
                rowsDeleted = sqlDB.delete(EtaTable.TABLE_NAME, EtaTable.COLUMN_ID +
                                " IN ( " + "SELECT " + EtaTable.COLUMN_ID + " FROM " +
                                EtaTable.TABLE_NAME +
                                " LEFT JOIN " + FavouriteTable.TABLE_NAME +
                                " ON (" +
                                FavouriteTable.COLUMN_ROUTE + "=" + EtaTable.COLUMN_ROUTE + " AND " +
                                FavouriteTable.COLUMN_BOUND + "=" + EtaTable.COLUMN_BOUND + " AND " +
                                FavouriteTable.COLUMN_STOP_SEQ + "=" + EtaTable.COLUMN_STOP_SEQ + " AND " +
                                FavouriteTable.COLUMN_STOP_CODE + "=" + EtaTable.COLUMN_STOP_CODE +
                                ")" + " WHERE " + FavouriteTable.COLUMN_ID + " IS NULL)",
                        null);
                break;
            case FAV:
                rowsDeleted = sqlDB.delete(FavouriteTable.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case FAV_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(FavouriteTable.TABLE_NAME,
                            FavouriteTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(FavouriteTable.TABLE_NAME,
                            FavouriteTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            case ETA:
                rowsDeleted = sqlDB.delete(EtaTable.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case ETA_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(EtaTable.TABLE_NAME,
                            EtaTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(EtaTable.TABLE_NAME,
                            EtaTable.COLUMN_ID + "=" + id
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
            case FAV:
                rowsUpdated = sqlDB.update(FavouriteTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case FAV_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(FavouriteTable.TABLE_NAME,
                            values,
                            FavouriteTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(FavouriteTable.TABLE_NAME,
                            values,
                            FavouriteTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            case ETA:
                rowsUpdated = sqlDB.update(EtaTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case ETA_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(EtaTable.TABLE_NAME,
                            values,
                            EtaTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(EtaTable.TABLE_NAME,
                            values,
                            EtaTable.COLUMN_ID + "=" + id
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
                FavouriteTable.COLUMN_ID,
                FavouriteTable.COLUMN_DATE,
                FavouriteTable.COLUMN_ROUTE,
                FavouriteTable.COLUMN_BOUND,
                FavouriteTable.COLUMN_ORIGIN,
                FavouriteTable.COLUMN_DESTINATION,
                FavouriteTable.COLUMN_STOP_SEQ,
                FavouriteTable.COLUMN_STOP_CODE,
                FavouriteTable.COLUMN_STOP_NAME,

                EtaTable.COLUMN_ID,
                EtaTable.COLUMN_DATE,
                EtaTable.COLUMN_ROUTE,
                EtaTable.COLUMN_BOUND,
                EtaTable.COLUMN_STOP_SEQ,
                EtaTable.COLUMN_STOP_CODE,
                EtaTable.COLUMN_ETA_API,
                EtaTable.COLUMN_ETA_TIME,
                EtaTable.COLUMN_ETA_EXPIRE,
                EtaTable.COLUMN_SERVER_TIME,
                EtaTable.COLUMN_UPDATED
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
