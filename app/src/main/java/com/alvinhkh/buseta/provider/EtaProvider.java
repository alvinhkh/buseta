package com.alvinhkh.buseta.provider;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

// This provide should only provide the requirements needed
public class EtaProvider extends ContentProvider {

    // used for the UriMatcher
    private static final int ETA = 10;

    private static final UriMatcher sURIMatcher = buildUriMatcher();

    private EtaOpenHelper openHelper;

    private Context context;

    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = EtaContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, EtaContract.PATH_ETA, ETA);
        matcher.addURI(authority, EtaContract.PATH_ETA + "/*", ETA);
        matcher.addURI(authority, EtaContract.PATH_ETA + "/#", ETA);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        context = getContext();
        openHelper = new EtaOpenHelper(context);
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case ETA: {
                retCursor = openHelper.getReadableDatabase().query(
                        EtaContract.EtaEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // make sure that potential listeners are getting notified
        retCursor.setNotificationUri(context.getContentResolver(), uri);

        return retCursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = openHelper.getWritableDatabase();
        Uri returnUri;
        long _id;
        switch (uriType) {
            case ETA:
                _id = sqlDB.insert(EtaContract.EtaEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = EtaContract.EtaEntry.buildUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        context.getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values){
        final SQLiteDatabase db = openHelper.getWritableDatabase();
        final int match = sURIMatcher.match(uri);
        switch (match) {
            case ETA: {
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(EtaContract.EtaEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                context.getContentResolver().notifyChange(uri, null);
                return returnCount;
            }
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = openHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case ETA:
                rowsDeleted = sqlDB.delete(EtaContract.EtaEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        context.getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = openHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriType) {
            case ETA:
                rowsUpdated = sqlDB.update(EtaContract.EtaEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (rowsUpdated != 0) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

}