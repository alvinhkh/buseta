package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopContainer;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

public class EtaCheckService extends IntentService {

    private static final String TAG = "EtaCheckService";

    private SettingsHelper settingsHelper = null;
    private ArrayList<RouteStopContainer> routeStopList = new ArrayList<>();

    public EtaCheckService() {
        super("EtaCheckService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        // Log.d(TAG, "onHandleIntent");
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        settingsHelper = new SettingsHelper().parse(getApplicationContext());
        RouteStop object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
        int position = extras.getInt(Constants.BUNDLE.ITEM_POSITION, -1);
        routeStopList = extras.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
        if (null == routeStopList)
            routeStopList = new ArrayList<>();
        if (null != object)
            getETA(position, object);
    }

    private void getETA(int position, RouteStop routeStop) {
        switch (settingsHelper.getEtaApi()) {
            case 1:
                //getETAv1(position, routeStop);
                //break;
            case 2:
            default:
                getETAv2(position, routeStop);
                break;
        }
    }

    private void getETAv2(final int position, final RouteStop routeStop) {
        if (null == routeStop || null == routeStop.route_bound) return;
        routeStop.eta_loading = true;
        sendUpdate(position, routeStop);

        String route_no = routeStop.route_bound.route_no.trim().replace(" ", "").toUpperCase();
        Uri routeEtaUri = Uri.parse(Constants.URL.ETA_MOBILE_API)
                .buildUpon()
                .appendQueryParameter("action", "geteta")
                .appendQueryParameter("lang", "tc")
                .appendQueryParameter("route", route_no)
                .appendQueryParameter("bound", routeStop.route_bound.route_bound)
                .appendQueryParameter("stop", routeStop.code.replaceAll("-", ""))
                .appendQueryParameter("stop_seq", routeStop.stop_seq)
                .build();

        Ion.with(getApplicationContext())
                .load(routeEtaUri.toString())
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if (e != null)
                            Log.e(TAG, e.toString());
                        routeStop.eta_loading = true;
                        routeStop.eta_fail = false;
                        if (result != null) {
                            // Log.d(TAG, result.toString());
                            if (!result.has("response")) {
                                routeStop.eta_loading = false;
                                routeStop.eta_fail = result.has("generated") ? false : true;
                                sendUpdate(position, routeStop);
                                return;
                            }
                            JsonArray jsonArray = result.get("response").getAsJsonArray();
                            RouteStopETA routeStopETA = new RouteStopETA();
                            routeStopETA.api_version = 2;
                            routeStopETA.seq = routeStop.stop_seq;
                            routeStopETA.updated = result.get("updated").getAsString();
                            routeStopETA.server_time = result.get("generated").getAsString();
                            StringBuilder etas = new StringBuilder();
                            StringBuilder expires = new StringBuilder();
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonObject object = jsonArray.get(i).getAsJsonObject();
                                etas.append(object.get("t").getAsString());
                                expires.append(object.get("ex").getAsString());
                                if (i < jsonArray.size() - 1) {
                                    etas.append(", ");
                                    expires.append(", ");
                                }
                            }
                            routeStopETA.etas = etas.toString();
                            routeStopETA.expires = expires.toString();
                            routeStop.eta = routeStopETA;
                        }
                        routeStop.eta_loading = false;
                        sendUpdate(position, routeStop);
                    }
                });
    }

    private void sendUpdate(int position, RouteStop routeStop) {
        boolean found = false;
        for (int i = 0; i < routeStopList.size(); i++) {
            RouteStopContainer container = routeStopList.get(i);
            if (container.position == position) {
                container.routeStop = routeStop;
                found = true;
            }
        }
        if (found == false)
            routeStopList.add(new RouteStopContainer(position, routeStop));
        // Log.d(TAG, position + ": " + routeStop.name_tc + " " + routeStop.eta_loading + " " + (routeStop.eta != null && routeStop.eta.etas != null ? routeStop.eta.etas : ""));
        Intent intent = new Intent(Constants.MESSAGE.ETA_UPDATED);
        intent.putExtra(Constants.MESSAGE.ETA_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.ITEM_POSITION, position);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
        intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
        sendBroadcast(intent);
    }

}
