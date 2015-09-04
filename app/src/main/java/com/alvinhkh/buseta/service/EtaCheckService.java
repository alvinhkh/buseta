package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopContainer;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EtaCheckService extends IntentService {

    private static final String TAG = "EtaCheckService";

    SharedPreferences mPrefs;
    private SettingsHelper settingsHelper = null;
    private ArrayList<RouteStopContainer> routeStopList = new ArrayList<>();
    String _id = null;
    String _token = null;

    public EtaCheckService() {
        super("EtaCheckService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        routeStopList = null;
        _id = null;
        _token = null;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settingsHelper = new SettingsHelper().parse(getApplicationContext());
        SharedPreferences.Editor editor = mPrefs.edit();
        String routeInfoApi = mPrefs.getString(Constants.PREF.REQUEST_API_INFO, "");
        if (null == routeInfoApi || routeInfoApi.equals("")) {
            editor.putString(Constants.PREF.REQUEST_API_INFO, Constants.URL.ROUTE_INFO);
        }
        editor.putString(Constants.PREF.REQUEST_ID, null);
        editor.putString(Constants.PREF.REQUEST_TOKEN, null);
        editor.commit();
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

        RouteStop object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
        int position = extras.getInt(Constants.BUNDLE.ITEM_POSITION, -1);
        if (null == routeStopList)
            routeStopList = extras.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
        if (null == routeStopList)
            routeStopList = new ArrayList<>();
        if (null != object)
            getETA(position, object);
    }

    private void getETA(int position, RouteStop routeStop) {
        switch (settingsHelper.getEtaApi()) {
            case 1:
                try {
                    getETAv1(position, routeStop);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                } catch (ExecutionException e) {
                    Log.e(TAG, e.getMessage());
                }
                break;
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
                                routeStop.eta = result.has("generated") ? new RouteStopETA() : null;
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

    private void getETAv1(final int position, final RouteStop routeStop) throws ExecutionException, InterruptedException {
        getETAv1(position, routeStop, 0);
        return;
    }

    private void getETAv1(final int position, final RouteStop routeStop, int attempt) throws ExecutionException, InterruptedException {
        if (null == routeStop || null == routeStop.route_bound) return;
        routeStop.eta_loading = true;
        sendUpdate(position, routeStop);

        _id = mPrefs.getString(Constants.PREF.REQUEST_ID, null);
        _token = mPrefs.getString(Constants.PREF.REQUEST_TOKEN, null);
        String etaApi = mPrefs.getString(Constants.PREF.REQUEST_API_ETA, null);
        if (null == _id || null == _token || _id.equals("") || _token.equals("")) {
            findToken(routeStop, mPrefs.getString(Constants.PREF.REQUEST_API_INFO, Constants.URL.ROUTE_INFO));
            etaApi = null;
        }
        String route_no = routeStop.route_bound.route_no.trim().replace(" ", "").toUpperCase();
        String _random_t = ((Double) Math.random()).toString();
        if (etaApi == null || etaApi.equals(""))
            etaApi = findEtaApi();
        Response<String> response = Ion.with(getApplicationContext())
                .load(etaApi + _random_t)
                .setHeader("Referer", Constants.URL.REQUEST_REFERRER)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .setBodyParameter("route", route_no)
                .setBodyParameter("route_no", route_no)
                .setBodyParameter("bound", routeStop.route_bound.route_bound)
                .setBodyParameter("busstop", routeStop.code.replaceAll("-", ""))
                .setBodyParameter("lang", "tc")
                .setBodyParameter("stopseq", routeStop.stop_seq)
                .setBodyParameter("id", _id)
                .setBodyParameter("token", _token)
                .asString()
                .withResponse()
                .get();
        routeStop.eta_loading = true;
        routeStop.eta_fail = false;
        if (null != response && response.getHeaders().code() == 404) {
            // delete record
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(Constants.PREF.REQUEST_ID, null);
            editor.putString(Constants.PREF.REQUEST_TOKEN, null);
            editor.putString(Constants.PREF.REQUEST_API_ETA, null);
            editor.commit();
            if (attempt < 3) {
                getETAv1(position, routeStop, attempt + 1);
            } else {
                routeStop.eta_loading = false;
                routeStop.eta_fail = false;
                sendUpdate(position, routeStop);
            }
            return;
        } else if (null != response && response.getHeaders().code() == 200) {
            String result = response.getResult();
            if (result != null) {
                // Log.d(TAG, result);
                if (!result.contains("ETA_TIME")) {
                    // delete record
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF.REQUEST_API_ETA, null);
                    editor.commit();
                    if (attempt < 5) {
                        getETAv1(position, routeStop, attempt);
                    } else {
                        routeStop.eta_loading = false;
                        routeStop.eta_fail = true;
                        sendUpdate(position, routeStop);
                    }
                    return;
                }

                RouteStopETA routeStopETA = new RouteStopETA();
                // TODO: parse result [], ignore php error
                JsonParser jsonParser = new JsonParser();
                JsonArray jsonArray = jsonParser.parse(result).getAsJsonArray();
                for (final JsonElement element : jsonArray) {
                    routeStopETA = new Gson().fromJson(element.getAsJsonObject(), RouteStopETA.class);
                    routeStopETA.api_version = 1;
                }
                if (null != routeStopETA.etas) {
                    Document doc = Jsoup.parse(routeStopETA.etas);
                    String text = doc.text().replaceAll(" ?　?預定班次", "");
                    String[] etas = text.split(", ?");
                    Pattern pattern = Pattern.compile("到達([^/離開]|$)");
                    Matcher matcher = pattern.matcher(text);
                    int count = 0;
                    while (matcher.find())
                        count++; //count any matched pattern
                    if (count > 1 && count == etas.length && attempt < 8) {
                        // more than one and all same, more likely error
                        Log.d(TAG, "sam.sam.sam.");
                        getETAv1(position, routeStop, attempt + 1);
                        return;
                    }
                }
                routeStop.eta = routeStopETA;
                routeStop.eta_loading = false;
                sendUpdate(position, routeStop);
            }
        }
    }

    private String findEtaApi() throws ExecutionException, InterruptedException {
        // Find ETA API URL, by first finding the js file use to call eta api on web
        Response<String> response = Ion.with(getApplicationContext())
                .load(Constants.URL.HTML_ETA)
                .setHeader("Referer", Constants.URL.KMB)
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .withResponse()
                .get();
        String etaJs = "";
        if (null != response && response.getHeaders().code() == 200) {
            String result = response.getResult();
            if (result != null && !result.equals("")) {
                Pattern p = Pattern.compile("\"(" + Constants.URL.PATH_ETA_JS + "[a-zA-Z0-9_.]*\\.js\\?[a-zA-Z0-9]*)\"");
                Matcher m = p.matcher(result);
                if (m.find()) {
                    etaJs = Constants.URL.KMB + m.group(1);
                    Log.d(TAG, "etaJs: " + etaJs);
                }
            }
        }
        if (null == etaJs || etaJs.equals("")) return "";
        String etaApi = null;
        // Find ETA API Url in found JS file
        Response<String> response2 = Ion.with(getApplicationContext())
                .load(etaJs)
                .setHeader("Referer", Constants.URL.REQUEST_REFERRER)
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .withResponse()
                .get();
        if (null != response2 && response2.getHeaders().code() == 200) {
            String result = response2.getResult();
            if (result != null && !result.equals("")) {
                Pattern p = Pattern.compile("\"(" + Constants.URL.PATH_ETA_API + "[a-zA-Z0-9_.]*\\.php\\?[a-zA-Z0-9]*=)\"");
                Matcher m = p.matcher(result);
                if (m.find()) {
                    etaApi = Constants.URL.KMB + m.group(1);
                    Log.d(TAG, "etaApi: easy found " + etaApi);
                } else {

                    Pattern p2 = Pattern.compile("\\|([^\\|]*)\\|\\|(t[a-zA-Z0-9_.]*)\\|prod");
                    Matcher m2 = p2.matcher(result);

                    if (m2.find() && m2.groupCount() == 2) {
                        etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                + m2.group(1) + ".php?" + m2.group(2);
                        Log.d(TAG, "etaApi: found-nd " + etaApi);
                    } else {

                        Pattern p3 = Pattern.compile("\\|([^\\|]*)\\|(t[a-zA-Z0-9_.]*)\\|eq");
                        Matcher m3 = p3.matcher(result);

                        if (m3.find() && m3.groupCount() == 2) {
                            etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                    + m3.group(1) + ".php?" + m3.group(2);
                            Log.d(TAG, "etaApi: found-rd " + etaApi);
                        } else {
                            Log.d(TAG, "etaApi: fail " + etaApi);
                        }

                    }

                }
            }
        }
        // save record
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(Constants.PREF.REQUEST_API_ETA, etaApi);
        editor.commit();
        return etaApi;
    }

    private void findToken(final RouteStop routeStop, String routeInfoApi) throws ExecutionException, InterruptedException {
        if (null == routeStop || null == routeStop.route_bound) return;

        String _random_t = ((Double) Math.random()).toString();
        Uri routeStopUri = Uri.parse(routeInfoApi)
                .buildUpon()
                .appendQueryParameter("t", _random_t)
                .appendQueryParameter("chkroutebound", "true")
                .appendQueryParameter("field9", routeStop.route_bound.route_no)
                .appendQueryParameter("routebound", routeStop.route_bound.route_bound)
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
                if (result.get("valid").getAsBoolean() == true) {
                    String id = result.get("id").getAsString();
                    String token = result.get("token").getAsString();
                    Log.d(TAG, "id: " + id + " token: " + token);
                    // save record
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF.REQUEST_ID, id);
                    editor.putString(Constants.PREF.REQUEST_TOKEN, token);
                    editor.commit();
                } else if (result.get("valid").getAsBoolean() == false &&
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
            editor.commit();
            findToken(routeStop, routeInfoApi);
        }
        return;
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
