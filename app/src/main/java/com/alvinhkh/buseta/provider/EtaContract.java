package com.alvinhkh.buseta.provider;


import android.content.ContentUris;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.alvinhkh.buseta.BuildConfig;

import timber.log.Timber;

public class EtaContract {
  // The "Content authority" is a name for the entire content provider, similar to the
  // relationship between a domain name and its website.  A convenient string to use for the
  // content authority is the package name for the app, which is guaranteed to be unique on the
  // device.
  public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".EtaProvider";

  // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
  // the content provider.
  public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

  public static final String PATH_ETA = "eta";

  public static final class EtaEntry implements BaseColumns {

    public static final Uri CONTENT_URI =
        BASE_CONTENT_URI.buildUpon().appendPath(PATH_ETA).build();

    public final static String TABLE_NAME = "eta";

    public final static String COLUMN_GENERATED_AT = "generated_at";

    public final static String COLUMN_UPDATED_AT = "updated_at";

    public final static String COLUMN_ROUTE_COMPANY = "company";

    public final static String COLUMN_ROUTE_NO = "route_no";

    public final static String COLUMN_ROUTE_SEQ = "route_seq";  // round bound

    public final static String COLUMN_STOP_SEQ = "stop_seq";

    public final static String COLUMN_STOP_ID = "stop_id"; // stop code

    public final static String COLUMN_ETA_EXPIRE = "eta_expire";

    public final static String COLUMN_ETA_ID = "eta_id";

    public final static String COLUMN_ETA_MISC = "eta_misc";  // eg. wheelchair, eot, ol

    public final static String COLUMN_ETA_SCHEDULED = "eta_scheduled";

    public final static String COLUMN_ETA_TIME = "eta_time";

    public final static String COLUMN_ETA_URL = "eta_url";


    public static Uri buildUri(long id) {
      return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    private static final String DATABASE_CREATE = "CREATE TABLE "
        + TABLE_NAME
        + "("
        + _ID + " INTEGER primary key autoincrement, "
        + COLUMN_ROUTE_COMPANY + " TEXT not NULL, "
        + COLUMN_ROUTE_NO + " TEXT not NULL, "
        + COLUMN_ROUTE_SEQ + " TEXT not NULL, "
        + COLUMN_STOP_SEQ + " TEXT not NULL, "
        + COLUMN_STOP_ID + " TEXT, "
        + COLUMN_ETA_EXPIRE + " TEXT, "
        + COLUMN_ETA_ID + " TEXT, "
        + COLUMN_ETA_MISC + " TEXT, "
        + COLUMN_ETA_SCHEDULED + " TEXT, "
        + COLUMN_ETA_TIME + " TEXT, "
        + COLUMN_ETA_URL + " TEXT, "
        + COLUMN_GENERATED_AT + " TEXT not NULL, "
        + COLUMN_UPDATED_AT + " TEXT not NULL, "
        + "UNIQUE (" + COLUMN_ROUTE_COMPANY + ", " + COLUMN_ROUTE_NO + ", " + COLUMN_ROUTE_SEQ
        + ", " + COLUMN_STOP_SEQ + ", " + COLUMN_ETA_ID + ") ON CONFLICT REPLACE"
        + ");";

    public static void onCreate(SQLiteDatabase database) {
      database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Timber.w("Upgrading database from version %s to %s, which will destroy all old data",
          oldVersion, newVersion);
      db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
      onCreate(db);
    }

    public static Uri buildQueryUri(String constraint) {
      return CONTENT_URI.buildUpon().appendPath(String.valueOf(constraint)).build();
    }

    public static String getQueryFromUri(Uri uri) {
      if (uri.getPathSegments().size() < 2)
        return null;
      return uri.getPathSegments().get(1);
    }
  }
}