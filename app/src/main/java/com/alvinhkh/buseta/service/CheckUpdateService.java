package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Connectivity;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.AppUpdate;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class CheckUpdateService extends IntentService {

    private static final String TAG = CheckUpdateService.class.getSimpleName();

    SharedPreferences mPrefs;

    public CheckUpdateService() {
        super("CheckUpdateService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Log.d(TAG, "onHandleIntent");
        Boolean triggerUpdateSuggestion = false;
        if (null != intent) {
            triggerUpdateSuggestion =
                    intent.getBooleanExtra(Constants.MESSAGE.SUGGESTION_FORCE_UPDATE, false);
        }
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int routeVersion = mPrefs.getInt(Constants.PREF.VERSION_RECORD, 0);
        if (Constants.ROUTES.VERSION > routeVersion) {
            Log.d(TAG, "hardcode trigger update suggestion database");
            triggerUpdateSuggestion = true;
        }
        Boolean checkWithoutPush = true;
        // Check internet connection
        if (!Connectivity.isConnected(this)) {
            sendUpdate(R.string.message_no_internet_connection);
            return;
        }
        try {
            Response<JsonArray> response = Ion.with(getApplicationContext())
                    .load(Constants.URL.RELEASE)
                    .asJsonArray()
                    .withResponse()
                    .get();
            if (null != response && response.getHeaders().code() == 404) {
                Log.d(TAG, "DotCom not found");
            } else if (null != response && response.getHeaders().code() == 200) {
                JsonArray result = response.getResult();
                if (null != result && result.size() > 0) {
                    JsonObject object = result.get(0).getAsJsonObject();
                    if (null != object && object.has("suggestion_database")) {
                        AppUpdate appUpdate = new Gson().fromJson(object, AppUpdate.class);
                        if (null != appUpdate) {
                            if (!triggerUpdateSuggestion)
                                sendAppUpdated(appUpdate);
                            Log.d(TAG, "SuggestionDB: " + appUpdate.suggestion_database + " " + routeVersion);
                            if (appUpdate.suggestion_database > routeVersion) {
                                Log.d(TAG, "DotCom trigger update suggestion database");
                                triggerUpdateSuggestion = true;
                            }
                            Log.d(TAG, "SuggestionCheck: " + appUpdate.suggestion_check);
                            checkWithoutPush = appUpdate.suggestion_check;
                        }
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        // start fetch available bus route
        if (triggerUpdateSuggestion)
            sendUpdate(R.string.message_database_updating);
        if (triggerUpdateSuggestion || checkWithoutPush)
        try {
            Response<JsonArray> response = Ion.with(getApplicationContext())
                    .load(Constants.URL.ROUTE_AVAILABLE)
                    .setHeader("X-Requested-With", "XMLHttpRequest")
                    .asJsonArray()
                    .withResponse()
                    .get();
            if (null != response && response.getHeaders().code() == 200) {
                JsonArray result = response.getResult();
                if (null != result && result.size() > 0) {
                    JsonObject object = result.get(0).getAsJsonObject();
                    if (null != object && object.has("r_no")) {
                        // count existing routes
                        Cursor mCursor_suggestion = getContentResolver().query(SuggestionProvider.CONTENT_URI,
                                null, SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_DEFAULT + "'",
                                null, SuggestionTable.COLUMN_DATE + " DESC");
                        int count = 0;
                        if (null != mCursor_suggestion) {
                            count = mCursor_suggestion.getCount();
                            mCursor_suggestion.close();
                        }
                        //
                        String routes = object.get("r_no").getAsString();
                        String[] routeArray = routes.split(",");
                        Log.d(TAG, "Suggestion In DB: " + count + " Available: " + routeArray.length);
                        if (triggerUpdateSuggestion || routeArray.length > count || count == 0) {
                            // clear existing suggested routes
                            getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                                    SuggestionTable.COLUMN_TYPE + "=?",
                                    new String[]{SuggestionTable.TYPE_DEFAULT});
                            //
                            ContentValues[] contentValues = new ContentValues[routeArray.length];
                            for (int i = 0; i < routeArray.length; i++) {
                                ContentValues values = new ContentValues();
                                values.put(SuggestionTable.COLUMN_TEXT, routeArray[i]);
                                values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_DEFAULT);
                                values.put(SuggestionTable.COLUMN_DATE, "0");
                                contentValues[i] = values;
                            }
                            int insertedRows = getContentResolver().bulkInsert(
                                    SuggestionProvider.CONTENT_URI, contentValues);
                            if (null != mPrefs) {
                                // update record version number
                                Calendar now = Calendar.getInstance();
                                String nowYear = String.format("%02d", now.get(Calendar.YEAR));
                                String nowMonth = String.format("%02d", now.get(Calendar.MONTH) + 1);
                                String nowDay = String.format("%02d", now.get(Calendar.DAY_OF_MONTH));
                                String date = nowYear + nowMonth + nowDay;
                                Integer version = Integer.valueOf(date);
                                SharedPreferences.Editor editor = mPrefs.edit();
                                editor.putInt(Constants.PREF.VERSION_RECORD, version);
                                editor.apply();
                            }
                            if (insertedRows > 0) {
                                Log.d(TAG, "updated available routes suggestion: " + insertedRows);
                            } else {
                                Log.d(TAG, "error when inserting available routes to database");
                            }
                            sendUpdate(R.string.message_database_updated);
                        }
                    }
                }
            } else {
                if (triggerUpdateSuggestion)
                    sendUpdate(R.string.message_fail_to_request);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, e.toString());
            if (triggerUpdateSuggestion)
                sendUpdate(R.string.message_fail_to_request);
        }
    }

    private void sendUpdate(int id) {
        Intent intent = new Intent(Constants.MESSAGE.CHECKING_UPDATED);
        intent.putExtra(Constants.STATUS.UPDATED_SUGGESTION, true);
        intent.putExtra(Constants.BUNDLE.MESSAGE_ID, id);
        sendBroadcast(intent);
    }

    private void sendAppUpdated(AppUpdate data) {
        Intent intent = new Intent(Constants.MESSAGE.CHECKING_UPDATED);
        intent.putExtra(Constants.STATUS.UPDATED_APP_FOUND, true);
        intent.putExtra(Constants.BUNDLE.APP_UPDATE_OBJECT, data);
        sendBroadcast(intent);
    }

}
