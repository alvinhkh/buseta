package com.alvinhkh.buseta.provider;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class FollowProvider extends ContentProvider {

    private FollowOpenHelper mHelper;

    private static final String AUTHORITY = "com.alvinhkh.buseta.FollowProvider";
    private static final String BASE_PATH_FOLLOW = "follows";
    public static final Uri CONTENT_URI_FOLLOW = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_FOLLOW);

    // used for the UriMatcher
    private static final int FOLLOW = 20;
    private static final int FOLLOW_ID = 21;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FOLLOW, FOLLOW);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_FOLLOW + "/#", FOLLOW_ID);
    }

    @Override
    public boolean onCreate() {
        mHelper = new FollowOpenHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case FOLLOW:
                // Set the table
                queryBuilder.setTables(FollowTable.TABLE_NAME);
                break;
            case FOLLOW_ID:
                // Set the table
                queryBuilder.setTables(FollowTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(FollowTable.COLUMN_ID + "=" + uri.getLastPathSegment());
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
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        long id;
        switch (uriType) {
            case FOLLOW:
                id = sqlDB.insert(FollowTable.TABLE_NAME, null, values);
                notifyChange(uri, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        switch (uriType) {
            case FOLLOW:
                return Uri.parse(BASE_PATH_FOLLOW + "/#" + id);
            default:
                return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case FOLLOW:
                rowsDeleted = sqlDB.delete(FollowTable.TABLE_NAME, selection,
                        selectionArgs);
                notifyChange(uri, true);
                break;
            case FOLLOW_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(FollowTable.TABLE_NAME,
                            FollowTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(FollowTable.TABLE_NAME,
                            FollowTable.COLUMN_ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                notifyChange(uri, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case FOLLOW:
                rowsUpdated = sqlDB.update(FollowTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                notifyChange(uri, true);
                break;
            case FOLLOW_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(FollowTable.TABLE_NAME,
                            values,
                            FollowTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(FollowTable.TABLE_NAME,
                            values,
                            FollowTable.COLUMN_ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                notifyChange(uri, true);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return rowsUpdated;
    }

    private void notifyChange(Uri uri, Boolean widgetUpdate) {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
            if (widgetUpdate) {
                // TODO: Widgets can't register content observers so we refresh widgets separately.
            }
        }
    }

    private void checkColumns(String[] projection) {
        String[] available = {
                FollowTable.COLUMN_ID,
                FollowTable.COLUMN_COMPANY,
                FollowTable.COLUMN_DATE,
                FollowTable.COLUMN_ROUTE,
                FollowTable.COLUMN_BOUND,
                FollowTable.COLUMN_ORIGIN,
                FollowTable.COLUMN_DESTINATION,
                FollowTable.COLUMN_STOP_SEQ,
                FollowTable.COLUMN_STOP_CODE,
                FollowTable.COLUMN_STOP_NAME,
                FollowTable.COLUMN_ORDER
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
