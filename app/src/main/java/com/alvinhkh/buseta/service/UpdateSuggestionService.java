package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.SuggestionsDatabase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class UpdateSuggestionService extends IntentService {

    private static final String TAG = "UpdateSuggestion";

    SharedPreferences mPrefs;
    SuggestionsDatabase mDatabase;

    public UpdateSuggestionService() {
        super("UpdateSuggestionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mDatabase = new SuggestionsDatabase(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        if (null != mDatabase)
            mDatabase.close();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Log.d(TAG, "onHandleIntent");
        sendUpdate(R.string.message_database_updating);
        Ion.with(getApplicationContext())
                .load(Constants.URL.ROUTE_AVAILABLE)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override
                    public void onCompleted(Exception e, JsonArray result) {
                        // do stuff with the result or error
                        if (null != e)
                            Log.e(TAG, e.toString());
                        if (null != result && result.size() > 0) {
                            JsonObject object = result.get(0).getAsJsonObject();
                            if (null != object && object.has("r_no")) {
                                mDatabase.clearDefault(); // clear existing suggested routes
                                String routes = object.get("r_no").getAsString();
                                String[] routeArray = routes.split(",");
                                Boolean success = true;
                                success = mDatabase.insertDefaults(routeArray);
                                if (null != mPrefs) {
                                    // update record version number
                                    // this number is to trigger update automatically
                                    SharedPreferences.Editor editor = mPrefs.edit();
                                    editor.putInt(Constants.PREF.VERSION_RECORD, Constants.ROUTES.VERSION);
                                    editor.commit();
                                }
                                if (success) {
                                    Log.d(TAG, "updated available routes suggestion");
                                } else {
                                    Log.d(TAG, "error when inserting available routes to database");
                                }
                                sendUpdate(R.string.message_database_updated);
                            }
                        }
                    }
                });
    }

    private void sendUpdate(int resourceId) {
        Intent intent = new Intent(Constants.ROUTES.SUGGESTION_UPDATE);
        intent.putExtra(Constants.ROUTES.SUGGESTION_UPDATE, true);
        intent.putExtra(Constants.ROUTES.MESSAGE_ID, resourceId);
        sendBroadcast(intent);
    }

}
