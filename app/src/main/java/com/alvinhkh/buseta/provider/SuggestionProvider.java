package com.alvinhkh.buseta.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

public class SuggestionProvider extends ContentProvider {

    private SuggestionOpenHelper mHelper;

    private static final String AUTHORITY = "com.alvinhkh.buseta.SuggestionProvider";
    private static final String BASE_PATH = "suggestion";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);
    private static final String BASE_PATH_SUGGESTIONS = "query";
    public static final Uri CONTENT_URI_SUGGESTIONS = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_SUGGESTIONS);

    // used for the UriMatcher
    private static final int SUGGESTION = 10;
    private static final int SUGGESTION_ID = 11;
    private static final int SUGGESTIONS = 20;
    private static final int SUGGESTIONS_ANY = 21;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, SUGGESTION);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SUGGESTION_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_SUGGESTIONS, SUGGESTIONS_ANY);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_SUGGESTIONS + "/#", SUGGESTIONS);
    }

    @Override
    public boolean onCreate() {
        mHelper = new SuggestionOpenHelper(getContext());
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
        String queryText;
        switch (uriType) {
            case SUGGESTIONS_ANY:
                queryText = "%";
                break;
            default:
                queryText = uri.getLastPathSegment();
                break;
        }
        switch (uriType) {
            case SUGGESTIONS:
            case SUGGESTIONS_ANY:
                queryText = queryText.toString().trim().replace(" ", "");
                // Set the table
                queryBuilder.setTables("(" +
                        // 3 history
                        " SELECT * " + " FROM " + SuggestionTable.TABLE_NAME +
                        " WHERE " + SuggestionTable.COLUMN_TEXT + " LIKE '" + queryText + "%'" +
                        " AND " + SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'" +
                        " ORDER BY " + SuggestionTable.COLUMN_DATE + " DESC" +
                        " LIMIT 0,3" +
                        " ) UNION SELECT * FROM (" +
                        // All others
                        " SELECT * FROM " + SuggestionTable.TABLE_NAME +
                        " WHERE " + SuggestionTable.COLUMN_TEXT + " LIKE '" + queryText + "%'" +
                        " AND " + SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_DEFAULT + "'" +
                        " AND " + SuggestionTable.COLUMN_TEXT + " NOT IN (" +
                        // exclude 3 history
                        " SELECT " + SuggestionTable.COLUMN_TEXT + " FROM " + SuggestionTable.TABLE_NAME +
                        " WHERE " + SuggestionTable.COLUMN_TEXT + " LIKE '" + queryText + "%'" +
                        " AND " + SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'" +
                        " ORDER BY " + SuggestionTable.COLUMN_DATE + " DESC" +
                        " LIMIT 0,3" +
                        " )" +
                        " ORDER BY " + SuggestionTable.COLUMN_TEXT + " ASC" +
                        " )");
                if (null == sortOrder) {
                    sortOrder = SuggestionTable.COLUMN_DATE + " DESC";
                }
                break;
            case SUGGESTION:
                // Set the table
                queryBuilder.setTables(SuggestionTable.TABLE_NAME);
                break;
            case SUGGESTION_ID:
                // Set the table
                queryBuilder.setTables(SuggestionTable.TABLE_NAME);
                // adding the ID to the original query
                queryBuilder.appendWhere(SuggestionTable.COLUMN_ID + "=" + uri.getLastPathSegment());
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
            case SUGGESTION:
                id = sqlDB.insert(SuggestionTable.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        switch (uriType) {
            case SUGGESTION:
                return Uri.parse(BASE_PATH + "/#" + id);
            default:
                return null;
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values){
        int uriType = sURIMatcher.match(uri);
        int numInserted = 0;
        String tableName;
        switch (uriType) {
            case SUGGESTION:
                tableName = SuggestionTable.TABLE_NAME;
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
            case SUGGESTION:
                rowsDeleted = sqlDB.delete(SuggestionTable.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case SUGGESTION_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SuggestionTable.TABLE_NAME,
                            SuggestionTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(SuggestionTable.TABLE_NAME,
                            SuggestionTable.COLUMN_ID + "=" + id
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
            case SUGGESTION:
                rowsUpdated = sqlDB.update(SuggestionTable.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case SUGGESTION_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(SuggestionTable.TABLE_NAME,
                            values,
                            SuggestionTable.COLUMN_ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(SuggestionTable.TABLE_NAME,
                            values,
                            SuggestionTable.COLUMN_ID + "=" + id
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
                SuggestionTable.COLUMN_ID,
                SuggestionTable.COLUMN_TEXT,
                SuggestionTable.COLUMN_TYPE,
                SuggestionTable.COLUMN_DATE,
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