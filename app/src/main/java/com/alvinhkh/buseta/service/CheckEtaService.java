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
import com.alvinhkh.buseta.provider.EtaTable;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.holder.RouteStop;
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

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckEtaService extends IntentService {

    private static final String TAG = CheckEtaService.class.getSimpleName();

    SharedPreferences mPrefs;
    SettingsHelper settingsHelper = null;
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
        sendUpdating = extras.getBoolean(Constants.MESSAGE.SEND_UPDATING, true);
        isWidget = extras.getBoolean(Constants.MESSAGE.WIDGET_UPDATE, false);
        int nId = extras.getInt(Constants.MESSAGE.NOTIFICATION_UPDATE);

        RouteStop object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
        if (null != object) {
            // Check internet connection
            final ConnectivityManager conMgr =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnected()) {
                object.eta_loading = false;
                object.eta_fail = true;
                sendUpdate(object, nId);
                return;
            }
            getETA(object, nId);
        }
    }

    private void getETA(final RouteStop routeStop, final int nId) {
        switch (settingsHelper.getEtaApi()) {
            case 1:
                try {
                    getETAv1(routeStop, nId);
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, e.getMessage());
                }
                break;
            case 2:
            default:
                getETAv2(routeStop, nId);
                break;
        }
    }

    private void getETAv2(final RouteStop routeStop, final int nId) {
        if (null == routeStop || null == routeStop.route_bound) return;
        routeStop.eta_loading = true;
        sendUpdating(routeStop, nId);

        String route_no = routeStop.route_bound.route_no.trim().replace(" ", "").toUpperCase();
        String stop_code = routeStop.code;
        if (null == stop_code) return;
        Uri routeEtaUri = Uri.parse(Constants.URL.ETA_MOBILE_API)
                .buildUpon()
                .appendQueryParameter("action", "geteta")
                .appendQueryParameter("lang", "tc")
                .appendQueryParameter("route", route_no)
                .appendQueryParameter("bound", routeStop.route_bound.route_bound)
                .appendQueryParameter("stop", stop_code.replaceAll("-", ""))
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
                                routeStop.eta_fail = !result.has("generated");
                                routeStop.eta = result.has("generated") ? new RouteStopETA() : null;
                                sendUpdate(routeStop, nId);
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
                        sendUpdate(routeStop, nId);
                    }
                });
    }

    private void getETAv1(final RouteStop routeStop, int nId)
            throws ExecutionException, InterruptedException {
        getETAv1(routeStop, nId, 0);
    }

    private void getETAv1(final RouteStop routeStop, int nId, int attempt)
            throws ExecutionException, InterruptedException {
        if (null == routeStop || null == routeStop.route_bound) return;
        routeStop.eta_loading = true;
        sendUpdating(routeStop, nId);

        _id = mPrefs.getString(Constants.PREF.REQUEST_ID, null);
        _token = mPrefs.getString(Constants.PREF.REQUEST_TOKEN, null);
        String etaApi = mPrefs.getString(Constants.PREF.REQUEST_API_ETA, null);
        if (null == _id || null == _token || _id.equals("") || _token.equals("")) {
            findToken(routeStop,
                    mPrefs.getString(Constants.PREF.REQUEST_API_INFO, Constants.URL.ROUTE_INFO));
            etaApi = null;
        }
        String route_no = routeStop.route_bound.route_no.trim().replace(" ", "").toUpperCase();
        String _random_t = ((Double) Math.random()).toString();
        if (etaApi == null || etaApi.equals(""))
            etaApi = findEtaApi();
        if (etaApi == null) {
            routeStop.eta_loading = false;
            routeStop.eta_fail = true;
            sendUpdate(routeStop, nId);
            return;
        }
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
            editor.apply();
            if (attempt < 3) {
                getETAv1(routeStop, attempt + 1);
            } else {
                routeStop.eta_loading = false;
                routeStop.eta_fail = false;
                sendUpdate(routeStop, nId);
            }
        } else if (null != response && response.getHeaders().code() == 200) {
            String result = response.getResult();
            if (result != null) {
                // Log.d(TAG, result);
                if (!result.contains("ETA_TIME")) {
                    // delete record
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(Constants.PREF.REQUEST_API_ETA, null);
                    editor.apply();
                    if (attempt < 5) {
                        getETAv1(routeStop, attempt);
                    } else {
                        routeStop.eta_loading = false;
                        routeStop.eta_fail = true;
                        sendUpdate(routeStop, nId);
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
                        getETAv1(routeStop, attempt + 1);
                        return;
                    }
                }
                routeStop.eta = routeStopETA;
                routeStop.eta_loading = false;
                sendUpdate(routeStop, nId);
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
        String etaJs = null;
        if (null != response && response.getHeaders().code() == 200) {
            String result = response.getResult();
            if (result != null && !result.equals("")) {
                Pattern p = Pattern.compile("\"(" + Constants.URL.PATH_ETA_JS + "[a-zA-Z0-9_.]*\\.js\\?[a-zA-Z0-9]*)\"");
                Matcher m = p.matcher(result);
                if (m.find()) {
                    etaJs = Constants.URL.KMB + m.group(1);
                    // Log.d(TAG, "etaJs: " + etaJs);
                } else {
                    Log.e(TAG, "etaJs: fail " + result);
                }
            }
        }
        if (null == etaJs || etaJs.equals("")) return null;
        String etaApi = "";
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

                if (etaApi.equals("")) {
                    // unencrypted
                    Pattern p = Pattern.compile("\"(" + Constants.URL.PATH_ETA_API + "[a-zA-Z0-9_.]*\\.php\\?[a-zA-Z0-9]*=)\"");
                    Matcher m = p.matcher(result);
                    if (m.find()) {
                        etaApi = Constants.URL.KMB + m.group(1);
                        Log.d(TAG, "etaApi: easy " + etaApi);
                    }
                }

                if (etaApi.equals("")) {
                    // 21 Oct 2015
                    Pattern p = Pattern.compile("eq\\|([^\\|]*)\\|(t[a-zA-Z0-9_.]*)\\|");
                    Matcher m = p.matcher(result);
                    if (m.find() && m.groupCount() == 2) {
                        etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                + m.group(1) + ".php?" + m.group(2);
                        Log.d(TAG, "etaApi: found-1021 " + etaApi);
                    }
                }

                if (etaApi.equals("")) {
                    // 25 Sept 2015
                    Pattern p = Pattern.compile("\\|([^\\|]*)\\|eq\\|(t[a-zA-Z0-9_.]*)\\|");
                    Matcher m = p.matcher(result);
                    if (m.find() && m.groupCount() == 2) {
                        etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                + m.group(1) + ".php?" + m.group(2);
                        Log.d(TAG, "etaApi: found-0925 " + etaApi);
                    }
                }

                if (etaApi.equals("")) {
                    // 5 Sept 2015
                    Pattern p = Pattern.compile("\\|([^\\|]*)\\|80K\\|\\|80M\\|8A\\|(t[a-zA-Z0-9_.]*)\\|75X");
                    Matcher m = p.matcher(result);
                    if (m.find() && m.groupCount() == 2) {
                        etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                + m.group(1) + ".php?" + m.group(2);
                        Log.d(TAG, "etaApi: found-0905 " + etaApi);
                    }
                }

                if (etaApi.equals("")) {
                    Pattern p = Pattern.compile("\\|([^\\|]*)\\|\\|(t[a-zA-Z0-9_.]*)\\|prod");
                    Matcher m = p.matcher(result);
                    if (m.find() && m.groupCount() == 2) {
                        etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                + m.group(1) + ".php?" + m.group(2);
                        Log.d(TAG, "etaApi: found-nd " + etaApi);
                    }
                }

                if (etaApi.equals("")) {
                    Pattern p = Pattern.compile("\\|([^\\|]*)\\|(t[a-zA-Z0-9_.]*)\\|eq");
                    Matcher m = p.matcher(result);
                    if (m.find() && m.groupCount() == 2) {
                        etaApi = Constants.URL.KMB + Constants.URL.PATH_ETA_API
                                + m.group(1) + ".php?" + m.group(2);
                        Log.d(TAG, "etaApi: found-rd " + etaApi);
                    }
                }

                if (etaApi.equals("")) {
                    Log.d(TAG, "etaJs: " + etaJs);
                    Log.e(TAG, "etaApi: fail " + etaApi);
                    etaApi = null;
                }

            }
        }
        // save record
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(Constants.PREF.REQUEST_API_ETA, etaApi);
        editor.apply();
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
                if (result.get("valid").getAsBoolean()) {
                    String id = result.get("id").getAsString();
                    String token = result.get("token").getAsString();
                    // Log.d(TAG, "id: " + id + " token: " + token);
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
            findToken(routeStop, routeInfoApi);
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
