package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopMap;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.alvinhkh.buseta.provider.RouteStopTable;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class RouteService extends IntentService {

    private static final String TAG = RouteService.class.getSimpleName();

    SharedPreferences mPrefs;
    SettingsHelper settingsHelper = null;
    String _id = null;
    String _token = null;
    List<ContentValues> valuesList = null;

    public RouteService() {
        super("RouteService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settingsHelper = new SettingsHelper().parse(getApplicationContext());
        SharedPreferences.Editor editor = mPrefs.edit();
        String routeInfoApi = mPrefs.getString(Constants.PREF.REQUEST_API_INFO, "");
        if (routeInfoApi.equals(""))
            editor.putString(Constants.PREF.REQUEST_API_INFO, Constants.URL.ROUTE_INFO);
        editor.putString(Constants.PREF.REQUEST_ID, null);
        editor.putString(Constants.PREF.REQUEST_TOKEN, null);
        editor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Log.d(TAG, "onHandleIntent");
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        RouteBound object = extras.getParcelable(Constants.BUNDLE.BOUND_OBJECT);
        if (null != object) {
            // Check internet connection
            final ConnectivityManager conMgr =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                sendUpdate(object, Constants.STATUS.CONNECTIVITY_INVALID);
                return;
            }
            try {
                getRouteStop(object);
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void getRouteStop(final RouteBound object) throws ExecutionException, InterruptedException {
        sendUpdate(object, Constants.STATUS.UPDATING_ROUTE_STOPS);
        _id = mPrefs.getString(Constants.PREF.REQUEST_ID, null);
        _token = mPrefs.getString(Constants.PREF.REQUEST_TOKEN, null);
        if (null == _id || null == _token || _id.equals("") || _token.equals("")) {
            findToken(object, mPrefs.getString(Constants.PREF.REQUEST_API_INFO, Constants.URL.ROUTE_INFO));
        }
        Uri routeStopUri = Uri.parse(mPrefs.getString(Constants.PREF.REQUEST_API_INFO, Constants.URL.ROUTE_INFO))
                .buildUpon()
                .appendQueryParameter("t", ((Double) Math.random()).toString())
                .appendQueryParameter("chkroutebound", "true")
                .appendQueryParameter("field9", object.route_no)
                .appendQueryParameter("routebound", object.route_bound)
                .build();
       Response<JsonObject> response = Ion.with(this)
                .load(routeStopUri.toString())
                .setHeader("Referer", Constants.URL.REQUEST_REFERRER)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asJsonObject()
                .withResponse()
                .get();
        if (null != response && response.getHeaders().code() == 200) {
            JsonObject result = response.getResult();
            // Log.d(TAG, result.toString());
            valuesList = new ArrayList<>();
            if (null != result)
                if (result.get("valid").getAsBoolean()) {
                    //  Got Bus Line Stops
                    JsonArray _bus_arr = result.getAsJsonArray("bus_arr");
                    int seq = 0;
                    for (JsonElement element : _bus_arr) {
                        Gson gson = new Gson();
                        RouteStop routeStop = gson.fromJson(element.getAsJsonObject(), RouteStop.class);
                        routeStop.route_bound = object;
                        routeStop.stop_seq = String.valueOf(seq);
                        valuesList.add(convertToContentValues(routeStop));
                        seq++;
                    }
                    int rowInserted = getContentResolver().bulkInsert(RouteProvider.CONTENT_URI,
                            valuesList.toArray(new ContentValues[valuesList.size()]));
                    if (rowInserted > 0) {
                        // Log.d(TAG, "Route Stop: " + rowInserted);
                        sendUpdate(object, Constants.STATUS.UPDATED_ROUTE_STOPS);
                    }
                    getRouteFares(object);
                } else if (!result.get("valid").getAsBoolean() &&
                        !result.get("message").getAsString().equals("")) {
                    // Invalid request with output message
                    sendUpdate(object, result.get("message").getAsString());
                }
        } else if (null != response && response.getHeaders().code() == 404) {
            sendUpdate(object, Constants.STATUS.CONNECT_404);
        } else {
            sendUpdate(object, Constants.STATUS.CONNECT_FAIL);
        }
    }

    private void findToken(final RouteBound object, String routeInfoApi) throws ExecutionException, InterruptedException {
        if (null == object) return;
        String _random_t = ((Double) Math.random()).toString();
        Uri routeStopUri = Uri.parse(routeInfoApi)
                .buildUpon()
                .appendQueryParameter("t", _random_t)
                .appendQueryParameter("chkroutebound", "true")
                .appendQueryParameter("field9", object.route_no)
                .appendQueryParameter("routebound", object.route_bound)
                .build();

        Response<JsonObject> response = Ion.with(getApplicationContext())
                .load(routeStopUri.toString())
                .setHeader("Referer", Constants.URL.REQUEST_REFERRER)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asJsonObject()
                .withResponse()
                .get();
        if (null != response && response.getHeaders().code() == 200) {
            JsonObject result = response.getResult();
            //Log.d(TAG, result.toString());
            if (null != result)
                if (result.get("valid").getAsBoolean()) {
                    String id = result.get("id").getAsString();
                    String token = result.get("token").getAsString();
                    Log.d(TAG, "id: " + id + " token: " + token);
                    // save record
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF.REQUEST_ID, id);
                    editor.putString(Constants.PREF.REQUEST_TOKEN, token);
                    editor.apply();
                } else if (!result.get("valid").getAsBoolean() &&
                        !result.get("message").getAsString().equals("")) {
                    // Invalid request with output message
                    Log.d(TAG, result.get("message").getAsString());
                }
        } else {
            if (routeInfoApi.equals(Constants.URL.ROUTE_INFO)) {
                routeInfoApi = Constants.URL.ROUTE_INFO_V1;
            } else {
                routeInfoApi = Constants.URL.ROUTE_INFO;
            }
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(Constants.PREF.REQUEST_API_INFO, routeInfoApi);
            editor.putString(Constants.PREF.REQUEST_ID, null);
            editor.putString(Constants.PREF.REQUEST_TOKEN, null);
            editor.apply();
            findToken(object, routeInfoApi);
        }
    }

    private void getRouteFares(final RouteBound object) throws ExecutionException, InterruptedException {
        sendUpdate(object, Constants.STATUS.UPDATING_FARE);
        final String route_no = object.route_no;
        final String route_bound = object.route_bound;
        final String route_st = "01"; // TODO: selectable
        Response<JsonArray> response = Ion.with(this)
                .load(Constants.URL.ROUTE_MAP)
                .setHeader("Referer", Constants.URL.HTML_SEARCH)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .setBodyParameter("bn", route_no)
                .setBodyParameter("dir", route_bound)
                .setBodyParameter("ST", route_st)
                .asJsonArray()
                .withResponse().get();
        if (null != response && response.getHeaders().code() == 200) {
            JsonArray result = response.getResult();
            // Log.d(TAG, result.toString());
            if (null != result) {
                for (int i = 0; i < result.size(); i++) {
                    JsonObject jsonObject = result.get(i).getAsJsonObject();
                    if (null != jsonObject) {
                        Gson gson = new Gson();
                        RouteStopMap routeStopMap = gson.fromJson(jsonObject, RouteStopMap.class);
                        if (null != routeStopMap.subarea) {
                            for (int j = 0; j < valuesList.size(); j++) {
                                ContentValues values = valuesList.get(j);
                                String stopCode = values.getAsString(RouteStopTable.COLUMN_STOP_CODE);
                                if (stopCode.equals(routeStopMap.subarea)) {
                                    if (null != routeStopMap.air_cond_fare &&
                                            !routeStopMap.air_cond_fare.equals("") &&
                                            !routeStopMap.air_cond_fare.equals("0.00"))
                                        values.put(RouteStopTable.COLUMN_STOP_FARE,
                                                getString(R.string.hkd, routeStopMap.air_cond_fare));
                                    if (null != routeStopMap.lat && !routeStopMap.lat.equals(""))
                                        values.put(RouteStopTable.COLUMN_STOP_LAT, routeStopMap.lat);
                                    if (null != routeStopMap.lng && !routeStopMap.lng.equals(""))
                                        values.put(RouteStopTable.COLUMN_STOP_LONG, routeStopMap.lng);
                                }
                            }
                        }
                    }
                }
                int rowInserted = getContentResolver().bulkInsert(RouteProvider.CONTENT_URI,
                        valuesList.toArray(new ContentValues[valuesList.size()]));
                if (rowInserted > 0) {
                    // Log.d(TAG, "Route Fare: " + rowInserted);
                    sendUpdate(object, Constants.STATUS.UPDATED_FARE);
                }
            }
        } else if (null != response && response.getHeaders().code() == 404) {
            Log.e(TAG, "Route Fare 404");
        }
    }

    private ContentValues convertToContentValues(RouteStop object) {
        ContentValues values = null;
        if (null != object && null != object.route_bound) {
            values = new ContentValues();
            values.put(RouteStopTable.COLUMN_ROUTE, object.route_bound.route_no);
            values.put(RouteStopTable.COLUMN_BOUND, object.route_bound.route_bound);
            values.put(RouteStopTable.COLUMN_ORIGIN, object.route_bound.origin_tc);
            values.put(RouteStopTable.COLUMN_ORIGIN_EN, object.route_bound.origin_en);
            values.put(RouteStopTable.COLUMN_DESTINATION, object.route_bound.destination_tc);
            values.put(RouteStopTable.COLUMN_DESTINATION_EN, object.route_bound.destination_en);
            values.put(RouteStopTable.COLUMN_STOP_SEQ, object.stop_seq);
            values.put(RouteStopTable.COLUMN_STOP_CODE, object.code);
            values.put(RouteStopTable.COLUMN_STOP_NAME, object.name_tc);
            values.put(RouteStopTable.COLUMN_STOP_NAME_EN, object.name_en);
            if (null != object.details) {
                values.put(RouteStopTable.COLUMN_STOP_FARE, object.details.air_cond_fare);
                values.put(RouteStopTable.COLUMN_STOP_LAT, object.details.lat);
                values.put(RouteStopTable.COLUMN_STOP_LONG, object.details.lng);
            }
            values.put(RouteStopTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
        }
        return values;
    }

    private void sendUpdate(RouteBound object, String message) {
        Intent intent = new Intent(Constants.MESSAGE.STOPS_UPDATED);
        intent.putExtra(Constants.MESSAGE.STOPS_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.BOUND_OBJECT, object);
        intent.putExtra(Constants.BUNDLE.UPDATE_MESSAGE, message);
        sendBroadcast(intent);
    }

}
