package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alvinhkh.buseta.utils.Connectivity;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.utils.EtaAdapterHelper;
import com.alvinhkh.buseta.provider.EtaTable;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.util.Charsets;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckEtaService extends IntentService {

    private static final String TAG = CheckEtaService.class.getSimpleName();
    private static final int TIME_OUT = 30 * 1000;

    SharedPreferences mPrefs;
    String _id = null;
    String _token = null;
    Boolean sendUpdating = true;
    Boolean isWidget = false;

    public CheckEtaService() {
        super("CheckEtaService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _id = null;
        _token = null;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = mPrefs.edit();
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
        sendUpdating = extras.getBoolean(Constants.MESSAGE.SEND_UPDATING, true);
        isWidget = extras.getBoolean(Constants.MESSAGE.WIDGET_UPDATE, false);
        int nId = extras.getInt(Constants.MESSAGE.NOTIFICATION_UPDATE);

        RouteStop object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
        if (null != object) {
            // Check internet connection
            if (!Connectivity.isConnected(this)) {
                object.eta_loading = false;
                object.eta_fail = true;
                sendUpdate(object, nId);
                return;
            }
            getETA(object, nId);
        }
    }

    private void getETA(final RouteStop routeStop, final int nId) {
        getETAv2(routeStop, nId);
    }

    private void getETAv2(final RouteStop routeStop, final int nId) {
        if (null == routeStop || null == routeStop.route_bound) return;
        routeStop.eta_loading = true;
        sendUpdating(routeStop, nId);

        String route_no = routeStop.route_bound.route_no.trim().replace(" ", "").toUpperCase();
        String stop_code = routeStop.code;
        if (null == stop_code) return;
        try {

            Uri routeEtaUri = Uri.parse(Constants.URL.ETA_API_HOST)
                    .buildUpon()
                    .appendQueryParameter("action", "geteta")
                    .appendQueryParameter("lang", "tc")
                    .appendQueryParameter("route", route_no)
                    .appendQueryParameter("bound", routeStop.route_bound.route_bound)
                    .appendQueryParameter("stop", stop_code.replaceAll("-", ""))
                    .appendQueryParameter("stop_seq", routeStop.stop_seq)
                    .build();
            Headers headers = new Headers();
            headers.add("X-Requested-With", "XMLHttpRequest");
            Response<String> response = Ion.with(getApplicationContext())
                    .load(routeEtaUri.toString())
                    .addHeaders(headers.getMultiMap())
                    .setTimeout(TIME_OUT)
                    .asString(Charsets.UTF_8)
                    .withResponse()
                    .get();

            if (null != response && response.getHeaders().code() == 200) {
                String str = response.getResult();
                routeStop.eta_loading = true;
                routeStop.eta_fail = false;
                if (str != null) {
                    JsonParser parser = new JsonParser();
                    JsonObject result = parser.parse(str).getAsJsonObject();
                    if (!result.has("response") || !result.get("response").isJsonArray()) {
                        routeStop.eta_loading = false;
                        routeStop.eta_fail = !result.has("generated");
                        routeStop.eta = result.has("generated") ? new RouteStopETA() : null;
                        sendUpdate(routeStop, nId);
                        return;
                    }
                    JsonArray jsonArray = result.get("response").getAsJsonArray();
                    RouteStopETA routeStopETA = new RouteStopETA();
                    routeStopETA.api_version = 2;
                    routeStopETA.seq = routeStop.stop_seq;
                    routeStopETA.updated = result.has("updated") ?
                            result.get("updated").getAsString() : "";
                    routeStopETA.server_time = result.has("generated") ?
                            result.get("generated").getAsString() : "";
                    StringBuilder etas = new StringBuilder();
                    StringBuilder expires = new StringBuilder();
                    StringBuilder scheduled = new StringBuilder();
                    StringBuilder wheelchair = new StringBuilder();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        if (!jsonArray.get(i).isJsonNull()) {
                            JsonObject object = jsonArray.get(i).getAsJsonObject();
                            etas.append(object.has("t") && !object.get("t").isJsonNull() ? object.get("t").getAsString() : "");
                            expires.append(object.has("ex") && !object.get("ex").isJsonNull() ? object.get("ex").getAsString() : "");
                            scheduled.append(object.has("ei") && !object.get("ei").isJsonNull() ? object.get("ei").getAsString() : "");
                            wheelchair.append(object.has("w") && !object.get("w").isJsonNull() ? object.get("w").getAsString() : "");
                            if (i < jsonArray.size() - 1) {
                                etas.append(", ");
                                expires.append(", ");
                                scheduled.append(", ");
                                wheelchair.append(", ");
                            }
                        }
                    }
                    routeStopETA.etas = etas.toString();
                    routeStopETA.scheduled = scheduled.toString();
                    routeStopETA.wheelchair = wheelchair.toString();
                    routeStopETA.expires = expires.toString();
                    routeStop.eta = routeStopETA;
                }
                routeStop.eta_loading = false;
                sendUpdate(routeStop, nId);
            } else {
                routeStop.eta_loading = false;
                routeStop.eta_fail = true;
                sendUpdate(routeStop, nId);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, e.toString());
            routeStop.eta_loading = false;
            routeStop.eta_fail = true;
            sendUpdate(routeStop, nId);
        }
    }

    private void sendUpdating(RouteStop object, int nId) {
        if (!sendUpdating || isWidget || nId > 0) return;
        sendUpdate(object, nId);
    }

    private void sendUpdate(RouteStop object, int nId) {
        if (null != object && null != object.route_bound) {
            ContentValues values = new ContentValues();
            values.put(EtaTable.COLUMN_LOADING, object.eta_loading);
            values.put(EtaTable.COLUMN_FAIL, object.eta_fail);
            values.put(EtaTable.COLUMN_ROUTE, object.route_bound.route_no);
            values.put(EtaTable.COLUMN_BOUND, object.route_bound.route_bound);
            values.put(EtaTable.COLUMN_STOP_SEQ, object.stop_seq);
            values.put(EtaTable.COLUMN_STOP_CODE, object.code);
            if (null != object.eta) {
                values.put(EtaTable.COLUMN_ETA_API, String.valueOf(object.eta.api_version));
                values.put(EtaTable.COLUMN_ETA_TIME, object.eta.etas);
                values.put(EtaTable.COLUMN_ETA_SCHEDULED, object.eta.scheduled);
                values.put(EtaTable.COLUMN_ETA_WHEELCHAIR, object.eta.wheelchair);
                values.put(EtaTable.COLUMN_ETA_EXPIRE, object.eta.expires);
                values.put(EtaTable.COLUMN_SERVER_TIME, object.eta.server_time);
                values.put(EtaTable.COLUMN_UPDATED, object.eta.updated);
            }
            values.put(EtaTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
            Uri etaUri = getContentResolver().insert(FollowProvider.CONTENT_URI_ETA, values);
            Boolean inserted = (null != etaUri);
            if (inserted) {
                Intent intent = new Intent(Constants.MESSAGE.ETA_UPDATED);
                intent.putExtra(Constants.MESSAGE.ETA_UPDATED, true);
                intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
                if (isWidget)
                    intent.putExtra(Constants.MESSAGE.WIDGET_UPDATE, true);
                if (nId > 0)
                    intent.putExtra(Constants.MESSAGE.NOTIFICATION_UPDATE, nId);
                sendBroadcast(intent);
            }
        }
    }

}
