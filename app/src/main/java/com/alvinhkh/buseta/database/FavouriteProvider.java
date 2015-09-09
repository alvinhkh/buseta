package com.alvinhkh.buseta.database;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class FavouriteProvider extends ContentProvider {

    private FavouriteOpenHelper mHelper;

    // used for the UriMatcher
    private static final int FAV = 10;
    private static final int FAV_ID = 20;
    private static final String AUTHORITY = "com.alvinhkh.buseta.FavouriteProvider";

    private static final String BASE_PATH = "favs";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/favs";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/fav";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, FAV);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", FAV_ID);
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

        // Set the table
        queryBuilder.setTables(FavouriteTable.TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case FAV:
                break;
            case FAV_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(FavouriteTable.COLUMN_ID + "="
                        + uri.getLastPathSegment());
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
        long id = 0;
        switch (uriType) {
            case FAV:
                id = sqlDB.insert(FavouriteTable.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/#" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
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
        int rowsUpdated = 0;
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
                FavouriteTable.COLUMN_STOP_NAME
        };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
