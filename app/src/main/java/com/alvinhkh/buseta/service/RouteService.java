package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alvinhkh.buseta.Connectivity;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopMap;
import com.alvinhkh.buseta.provider.RouteBoundTable;
import com.alvinhkh.buseta.provider.RouteStopTable;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RouteService extends IntentService {

    private static final String TAG = RouteService.class.getSimpleName();
    private static final int TIME_OUT = 60 * 1000;

    SharedPreferences mPrefs;
    List<ContentValues> valuesList = null;
    String vHost = Constants.URL.LWB;

    public RouteService() {
        super("RouteService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = mPrefs.edit();
        String routeInfoApi = mPrefs.getString(Constants.PREF.REQUEST_API_INFO, "");
        if (routeInfoApi.equals("") || !routeInfoApi.contains(vHost))
            editor.putString(Constants.PREF.REQUEST_API_INFO,
                    vHost + Constants.URL.ROUTE_INFO_V1);
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

        String routeNo = extras.getString(Constants.BUNDLE.ROUTE_NO);
        if (null != routeNo) {
            synchronized(this) {
                Log.d(TAG, "Get Bounds");
                // request route bounds
                if (!Connectivity.isConnected(this)) {
                    // Check internet connection
                    sendUpdate(routeNo, Constants.STATUS.CONNECTIVITY_INVALID);
                    return;
                }
                try {
                    getRouteBound(routeNo);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    sendUpdate(routeNo, Constants.STATUS.CONNECT_FAIL);
                    Log.e(TAG, e.getMessage());
                    // second attempt
                    try {
                        getRouteBound(routeNo);
                    } catch (InterruptedException | ExecutionException | TimeoutException e2) {
                        sendUpdate(routeNo, Constants.STATUS.CONNECT_FAIL);
                        Log.e(TAG, e2.getMessage());
                    }
                }
            }
        }
        RouteBound object = extras.getParcelable(Constants.BUNDLE.BOUND_OBJECT);
        if (null != object) {
            Log.d(TAG, "Get Stops");
            // request route stops
            if (!Connectivity.isConnected(this)) {
                // Check internet connection
                sendUpdate(object, Constants.STATUS.CONNECTIVITY_INVALID);
                return;
            }
            try {
                getRouteStop(object);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                sendUpdate(routeNo, Constants.STATUS.CONNECT_FAIL);
                Log.e(TAG, e.getMessage());
                // second attempt
                try {
                    getRouteStop(object);
                } catch (InterruptedException | ExecutionException | TimeoutException e2) {
                    sendUpdate(routeNo, Constants.STATUS.CONNECT_FAIL);
                    Log.e(TAG, e2.getMessage());
                }
            }
        }
    }

    private void getRouteBound(final String routeNo) throws ExecutionException, InterruptedException, TimeoutException {
        sendUpdate(routeNo, Constants.STATUS.UPDATING_BOUNDS);
        Uri routeStopUri = Uri.parse(mPrefs.getString(Constants.PREF.REQUEST_API_INFO,
                vHost + Constants.URL.ROUTE_INFO))
                .buildUpon()
                .appendQueryParameter("t", ((Double) Math.random()).toString())
                .appendQueryParameter("field9", routeNo)
                .build();
        Headers headers = new Headers();
        headers.add("Referer", Constants.URL.REQUEST_REFERRER);
        headers.add("X-Requested-With", "XMLHttpRequest");
        headers.add("Pragma", "no-cache");
        headers.add("User-Agent", Constants.URL.REQUEST_UA);
        Future<Response<String>> conn = Ion.with(this)
                .load(routeStopUri.toString())
                .setLogging(TAG, Log.VERBOSE)
                .addHeaders(headers.getMultiMap())
                .setTimeout(TIME_OUT)
                .asString()
                .withResponse();
        Response<String> response = conn.get();
        if (null != response && response.getHeaders().code() == 200) {
            String responseResult = response.getResult();
            // Log.d(TAG, "responseResult: " + responseResult);
            JsonObject result = null;
            if (null != responseResult) {
                try {
                    result = new JsonParser().parse(responseResult).getAsJsonObject();
                    if (null == result) {
                        Log.d(TAG, "bound: null");
                    }
                } catch (Exception e) {
                    result = null;
                    Log.d(TAG, e.getMessage());
                }
            }
            valuesList = new ArrayList<>();
            if (null != result && result.get("valid").getAsBoolean()) {
                if (result.has("id") && result.has("token")) {
                    // token and id
                    String id = result.get("id").getAsString();
                    String token = result.get("token").getAsString();
                    // Log.d(TAG, "id: " + id + " token: " + token);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF.REQUEST_ID, id);
                    editor.putString(Constants.PREF.REQUEST_TOKEN, token);
                    editor.apply();
                }
                //  Got Bus Line Bounds
                JsonArray _bus_arr = result.getAsJsonArray("bus_arr");
                int seq = 1;
                for (JsonElement element : _bus_arr) {
                    Gson gson = new Gson();
                    RouteBound routeBound = gson.fromJson(element.getAsJsonObject(), RouteBound.class);
                    routeBound.route_no = routeNo;
                    routeBound.route_bound = String.valueOf(seq);
                    valuesList.add(toContentValues(routeBound));
                    seq++;
                }
                int rowInserted = getContentResolver().bulkInsert(RouteProvider.CONTENT_URI_BOUND,
                        valuesList.toArray(new ContentValues[valuesList.size()]));
                if (rowInserted > 0) {
                    // Log.d(TAG, "Route Bound: " + rowInserted);
                    sendUpdate(routeNo, Constants.STATUS.UPDATED_BOUNDS);
                }
            } else if (null != result &&
                    !result.get("valid").getAsBoolean() &&
                    !result.get("message").getAsString().equals("")) {
                // Invalid request with output message
                sendUpdate(routeNo, result.get("message").getAsString());
            } else {
                sendUpdate(routeNo, Constants.STATUS.CONNECT_FAIL);
            }
        } else if (null != response && response.getHeaders().code() == 404) {
            sendUpdate(routeNo, Constants.STATUS.CONNECT_404);
        } else {
            sendUpdate(routeNo, Constants.STATUS.CONNECT_FAIL);
        }
    }

    private void getRouteStop(final RouteBound object) throws ExecutionException, InterruptedException, TimeoutException {
        sendUpdate(object, Constants.STATUS.UPDATING_STOPS);
        Uri routeStopUri = Uri.parse(mPrefs.getString(Constants.PREF.REQUEST_API_INFO,
                vHost + Constants.URL.ROUTE_INFO))
                .buildUpon()
                .appendQueryParameter("t", ((Double) Math.random()).toString())
                .appendQueryParameter("chkroutebound", "true")
                .appendQueryParameter("field9", object.route_no)
                .appendQueryParameter("routebound", object.route_bound)
                .build();
        Headers headers = new Headers();
        headers.add("Referer", Constants.URL.REQUEST_REFERRER);
        headers.add("X-Requested-With", "XMLHttpRequest");
        headers.add("Pragma", "no-cache");
        headers.add("User-Agent", Constants.URL.REQUEST_UA);
        Future<Response<String>> conn = Ion.with(this)
                .load(routeStopUri.toString())
                .setLogging(TAG, Log.DEBUG)
                .addHeaders(headers.getMultiMap())
                .setTimeout(TIME_OUT)
                .asString()
                .withResponse();
        Response<String> response = conn.get();
        if (null != response && response.getHeaders().code() == 200) {
            String responseResult = response.getResult();
            // Log.d(TAG, "responseResult: " + responseResult);
            JsonObject result = null;
            if (null != responseResult) {
                try {
                    result = new JsonParser().parse(responseResult).getAsJsonObject();
                    if (null == result) {
                        Log.d(TAG, "stop: null");
                    }
                } catch (Exception e) {
                    result = null;
                    Log.d(TAG, e.getMessage());
                }
            }
            // Log.d(TAG, result.toString());
            valuesList = new ArrayList<>();
            if (null != result && result.get("valid").getAsBoolean()) {
                if (result.has("id") && result.has("token")) {
                    // token and id
                    String id = result.get("id").getAsString();
                    String token = result.get("token").getAsString();
                    Log.d(TAG, "id: " + id + " token: " + token);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF.REQUEST_ID, id);
                    editor.putString(Constants.PREF.REQUEST_TOKEN, token);
                    editor.apply();
                }
                //  Got Bus Line Stops
                JsonArray _bus_arr = result.getAsJsonArray("bus_arr");
                int seq = 0;
                for (JsonElement element : _bus_arr) {
                    Gson gson = new Gson();
                    RouteStop routeStop = gson.fromJson(element.getAsJsonObject(), RouteStop.class);
                    routeStop.route_bound = object;
                    routeStop.stop_seq = String.valueOf(seq);
                    valuesList.add(toContentValues(routeStop));
                    seq++;
                }
                int rowInserted = getContentResolver().bulkInsert(RouteProvider.CONTENT_URI_STOP,
                        valuesList.toArray(new ContentValues[valuesList.size()]));
                if (rowInserted > 0) {
                    // Log.d(TAG, "Route Stop: " + rowInserted);
                    sendUpdate(object, Constants.STATUS.UPDATED_STOPS);
                }
                getRouteFares(object);
            } else if (null != result &&
                    !result.get("valid").getAsBoolean() &&
                    !result.get("message").getAsString().equals("")) {
                // Invalid request with output message
                sendUpdate(object, result.get("message").getAsString());
            } else {
                sendUpdate(object, Constants.STATUS.CONNECT_FAIL);
            }
        } else if (null != response && response.getHeaders().code() == 404) {
            sendUpdate(object, Constants.STATUS.CONNECT_404);
        } else {
            sendUpdate(object, Constants.STATUS.CONNECT_FAIL);
        }
    }

    private void getRouteFares(final RouteBound object) throws ExecutionException, InterruptedException {
        sendUpdate(object, Constants.STATUS.UPDATING_FARE);
        final String route_no = object.route_no;
        final String route_bound = object.route_bound;
        final String route_st = "01"; // TODO: selectable
        Headers headers = new Headers();
        headers.add("Referer", Constants.URL.HTML_SEARCH);
        headers.add("X-Requested-With", "XMLHttpRequest");
        headers.add("Pragma", "no-cache");
        headers.add("User-Agent", Constants.URL.REQUEST_UA);
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        params.put("bn", Collections.singletonList(route_no));
        params.put("dir", Collections.singletonList(route_bound));
        params.put("ST", Collections.singletonList(route_st));
        Future<Response<JsonArray>> conn = Ion.with(this)
                .load(Constants.URL.KMB + Constants.URL.ROUTE_MAP)
                .addHeaders(headers.getMultiMap())
                .setTimeout(TIME_OUT)
                .setBodyParameters(params)
                .asJsonArray()
                .withResponse();
        Response<JsonArray> response = conn.get();
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
                int rowInserted = getContentResolver().bulkInsert(RouteProvider.CONTENT_URI_STOP,
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

    private ContentValues toContentValues(RouteBound object) {
        ContentValues values = null;
        if (null != object) {
            values = new ContentValues();
            values.put(RouteBoundTable.COLUMN_ROUTE, object.route_no);
            values.put(RouteBoundTable.COLUMN_BOUND, object.route_bound);
            values.put(RouteBoundTable.COLUMN_ORIGIN, object.origin_tc);
            values.put(RouteBoundTable.COLUMN_ORIGIN_EN, object.origin_en);
            values.put(RouteBoundTable.COLUMN_DESTINATION, object.destination_tc);
            values.put(RouteBoundTable.COLUMN_DESTINATION_EN, object.destination_en);
            values.put(RouteBoundTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
        }
        return values;
    }

    private ContentValues toContentValues(RouteStop object) {
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

    private void sendUpdate(String routeNo, String message) {
        Log.d(TAG, "sendUpdate: " + routeNo + " " + message);
        Intent intent = new Intent(Constants.MESSAGE.BOUNDS_UPDATED);
        intent.putExtra(Constants.MESSAGE.BOUNDS_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.ROUTE_NO, routeNo);
        intent.putExtra(Constants.BUNDLE.UPDATE_MESSAGE, message);
        sendBroadcast(intent);
    }

    private void sendUpdate(RouteBound object, String message) {
        if (null != object && null != object.route_no)
            Log.d(TAG, "sendUpdate:: " + object.route_no + " " + message);
        Intent intent = new Intent(Constants.MESSAGE.STOPS_UPDATED);
        intent.putExtra(Constants.MESSAGE.STOPS_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.BOUND_OBJECT, object);
        intent.putExtra(Constants.BUNDLE.UPDATE_MESSAGE, message);
        sendBroadcast(intent);
    }

}
