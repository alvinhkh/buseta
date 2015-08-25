package com.alvinhkh.buseta.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.adapter.RouteStopAdapter;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.holder.RouteStopMap;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteStopFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RouteStopFragment";
    private static final String KEY_LIST_VIEW_STATE = "KEY_LIST_VIEW_STATE_ROUTE_STOP";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private Menu mMenu = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;
    private RouteStopAdapter mAdapter;

    private String _route_no = null;
    private String _route_bound = null;
    private String _route_origin = null;
    private String _route_destination = null;
    private String _id = null;
    private String _token = null;
    private String etaApi = "";
    private String getRouteInfoApi = "";
    private Boolean savedState = false;
    private SettingsHelper settingsHelper = null;

    // Runnable to get all stops eta
    int iEta = 0;
    Handler mEtaHandler = new Handler();
    Runnable mEtaRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != mSwipeRefreshLayout)
                mSwipeRefreshLayout.setRefreshing(true);
            if (null != mAdapter && iEta < mAdapter.getCount()) {
                RouteStop routeStop = mAdapter.getItem(iEta);
                getETA(iEta, routeStop.code);
                iEta++;
                if (iEta < mAdapter.getCount() - 1) {
                    mEtaHandler.postDelayed(mEtaRunnable, 250);
                } else {
                    if (mSwipeRefreshLayout != null)
                        mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        }
    };

    public RouteStopFragment() {
    }

    public static RouteStopFragment newInstance(String _route_no,
                                                   String _route_bound,
                                                   String _route_origin,
                                                   String _route_destination) {
        RouteStopFragment f = new RouteStopFragment();
        Bundle args = new Bundle();
        args.putString("route_no", _route_no);
        args.putString("route_bound", _route_bound);
        args.putString("route_origin", _route_origin);
        args.putString("route_destination", _route_destination);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listview, container, false);
        mContext = super.getActivity();
        settingsHelper = new SettingsHelper().parse(mContext.getApplicationContext());
        // Get arguments
        _route_no = getArguments().getString("route_no");
        _route_bound = getArguments().getString("route_bound");
        _route_origin = getArguments().getString("route_origin");
        _route_destination = getArguments().getString("route_destination");
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mActionBar.setTitle(_route_no);
        mActionBar.setSubtitle(getString(R.string.destination, _route_destination));
        mActionBar.setDisplayHomeAsUpEnabled(false);
        setHasOptionsMenu(true);
        // Set List Adapter
        mAdapter = new RouteStopAdapter(mContext);
        if (savedInstanceState != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState);
            _id = savedInstanceState.getString("_id");
            _token = savedInstanceState.getString("_token");
            etaApi = savedInstanceState.getString("etaApi");
            getRouteInfoApi = savedInstanceState.getString("getRouteInfoApi");
        }
        //
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_route);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(false); // disable pull-to-refresh
        mSwipeRefreshLayout.setRefreshing(false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
        // Set Listview
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setDividerHeight(2);
        mEmptyText = (TextView) view.findViewById(android.R.id.empty);
        mEmptyText.setText("");
        mListView.setEmptyView(view.findViewById(R.id.empty));
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_LIST_VIEW_STATE)) {
            mListView.onRestoreInstanceState(savedInstanceState
                    .getParcelable(KEY_LIST_VIEW_STATE));
            savedState = true;
        } else {
            getRouteInfoApi = Constants.URL.ROUTE_INFO;
            // Get Route Stops
            getRouteStops(_route_no, _route_bound);
            if (settingsHelper.getEtaApi() == 1)
                findEtaApiUrl();
        }
        mListView.setAdapter(mAdapter);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mAdapter) {
            mAdapter.onSaveInstanceState(outState);
            outState.putParcelable(KEY_LIST_VIEW_STATE, mListView.onSaveInstanceState());
        }
        outState.putString("_id", _id);
        outState.putString("_token", _token);
        outState.putString("etaApi", etaApi);
        outState.putString("getRouteInfoApi", getRouteInfoApi);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mActionBar) {
            mActionBar.setTitle(_route_no);
            mActionBar.setSubtitle(getString(R.string.destination, _route_destination));
        }
    }

    @Override
    public void onDestroyView() {
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(false);
        if (null != mListView)
            mListView.setAdapter(null);
        if (null != mProgressBar)
            mProgressBar.setVisibility(View.GONE);
        if (null != mEmptyText)
            mEmptyText.setVisibility(View.GONE);
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        if (null != mEtaHandler && null != mEtaRunnable)
            mEtaHandler.removeCallbacks(mEtaRunnable);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view,
                            final int position, long id) {
        if (view != null) {
            TextView textView_code = (TextView) view.findViewById(R.id.stop_code);
            getETA(position, textView_code.getText().toString());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view != null) {
            TextView textView_name = (TextView) view.findViewById(R.id.stop_name);
            RouteStop object = mAdapter.getItem(position);
            if (null != object.eta) {
                // Request Time
                SimpleDateFormat display_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String server_time = "";
                Date server_date = null;
                if (null != object.eta.server_time && !object.eta.server_time.equals("")) {
                    if (object.eta.api_version == 2) {
                        server_date = new Date(Long.parseLong(object.eta.server_time));
                    } else if (object.eta.api_version == 1) {
                        SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        try {
                            server_date = date_format.parse(object.eta.server_time);
                        } catch (ParseException ep) {
                            ep.printStackTrace();
                        }
                    }
                    server_time = (null != server_date) ?
                            display_format.format(server_date) : object.eta.server_time;
                }
                // last updated
                String updated_time = "";
                Date updated_date = null;
                if (null != object.eta.updated && !object.eta.updated.equals("")) {
                    if (object.eta.api_version == 2) {
                        updated_date = new Date(Long.parseLong(object.eta.updated));
                    }
                    updated_time = (null != updated_date) ?
                            display_format.format(updated_date) : object.eta.updated;
                }
                // ETAs
                RouteStopETA routeStopETA = object.eta;
                String eta = Jsoup.parse(routeStopETA.etas).text();
                String[] etas = eta.replaceAll("　", " ").split(", ?");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < etas.length; i++) {
                    sb.append(etas[i]);
                    if (object.eta.api_version == 1) {
                        // API v1 from Web, with minutes no time
                        String minutes = etas[i].replaceAll("[^0123456789]", "");
                        if (null != server_date && !minutes.equals("") &&
                                etas[i].contains("分鐘")) {
                            Long t = server_date.getTime();
                            Date etaDate = new Date(t + (Integer.parseInt(minutes) * 60000));
                            SimpleDateFormat eta_time_format = new SimpleDateFormat("HH:mm");
                            String etaTime = eta_time_format.format(etaDate);
                            sb.append(" (");
                            sb.append(etaTime);
                            sb.append(")");
                        }
                    } else if (object.eta.api_version == 2) {
                        // API v2 from Mobile v2, with exact time
                        if (etas[i].matches(".*\\d.*")) {
                            // if text has digit
                            String etaMinutes = "";
                            long differences = new Date().getTime() - server_date.getTime(); // get device time and compare to server time
                            try {
                                SimpleDateFormat time_format =
                                        new SimpleDateFormat("yyyy/MM/dd HH:mm");
                                Date etaDateCompare = server_date;
                                // first assume eta time and server time is on the same date
                                Date etaDate = time_format.parse(
                                        new SimpleDateFormat("yyyy").format(etaDateCompare) + "/" +
                                                new SimpleDateFormat("MM").format(etaDateCompare) + "/" +
                                                new SimpleDateFormat("dd").format(etaDateCompare) + " " +
                                                etas[i]);
                                // if not minutes will get negative integer
                                int minutes = (int) ((etaDate.getTime() / 60000) -
                                        ((server_date.getTime() + differences) / 60000));
                                if (minutes < -12 * 60) {
                                    // plus one day to get correct eta date
                                    etaDateCompare = new Date(server_date.getTime() + 1 * 24 * 60 * 60 * 1000);
                                    etaDate = time_format.parse(
                                            new SimpleDateFormat("yyyy").format(etaDateCompare) + "/" +
                                                    new SimpleDateFormat("MM").format(etaDateCompare) + "/" +
                                                    new SimpleDateFormat("dd").format(etaDateCompare) + " " +
                                                    etas[i]);
                                    minutes = (int) ((etaDate.getTime() / 60000) -
                                            ((server_date.getTime() + differences) / 60000));
                                }
                                // minutes should be 0 to within a day
                                if (minutes >= 0 && minutes < 1 * 24 * 60 * 60 * 1000)
                                    etaMinutes = String.valueOf(minutes);
                            } catch (ParseException ep) {
                                ep.printStackTrace();
                            }
                            if (!etaMinutes.equals("")) {
                                sb.append(" (");
                                if (etaMinutes.equals("0")) {
                                    sb.append("現在");
                                } else {
                                    sb.append(etaMinutes);
                                    sb.append("分鐘");
                                }
                                sb.append(")");
                            }
                        }
                    }
                    if (i < etas.length - 1)
                        sb.append("\n");
                }
                sb.append("\n\n");
                if (null != server_time && !server_time.equals("")) {
                    sb.append("\n");
                    sb.append(getString(R.string.message_server_time, server_time));
                }
                if (null != updated_time && !updated_time.equals("")) {
                    sb.append("\n");
                    sb.append(getString(R.string.message_last_updated, updated_time));
                }
                new AlertDialog.Builder(mContext)
                        .setTitle(textView_name.getText())
                        .setMessage(sb.toString()).show();
            }
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(savedState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            onRefresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
     public void onRefresh() {
        iEta = 0;
        if (null != mEtaHandler && null != mEtaRunnable)
            mEtaHandler.post(mEtaRunnable);
    }

    private void getRouteStops(final String route_no, final String route_bound) {

        if (mEmptyText != null)
            mEmptyText.setText(R.string.message_loading);
        if (mProgressBar != null)
            mProgressBar.setVisibility(View.VISIBLE);

        String _random_t = ((Double) Math.random()).toString();

        Uri routeStopUri = Uri.parse(getRouteInfoApi)
                .buildUpon()
                .appendQueryParameter("t", _random_t)
                .appendQueryParameter("chkroutebound", "true")
                .appendQueryParameter("field9", route_no)
                .appendQueryParameter("routebound", route_bound)
                .build();

        Ion.with(mContext)
                .load(routeStopUri.toString())
                //.setLogging("Ion", Log.DEBUG)
                .progressBar(mProgressBar)
                .setHeader("Referer", Constants.URL.REQUEST_REFERER)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asJsonObject()
                .withResponse()
                .setCallback(new FutureCallback<Response<JsonObject>>() {
                    @Override
                    public void onCompleted(Exception e, Response<JsonObject> response) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                            if (mEmptyText != null)
                                mEmptyText.setText(R.string.message_fail_to_request);
                        }
                        if (null != response && response.getHeaders().code() == 200) {
                            JsonObject result = response.getResult();
                            //Log.d(TAG, result.toString());
                            if (null != result)
                            if (result.get("valid").getAsBoolean() == true) {
                                //  Got Bus Line Stops
                                JsonArray _bus_arr = result.getAsJsonArray("bus_arr");
                                for (JsonElement element : _bus_arr) {
                                    Gson gson = new Gson();
                                    RouteStop routeStop = gson.fromJson(element.getAsJsonObject(), RouteStop.class);
                                    mAdapter.add(routeStop);
                                }
                                _id = result.get("id").getAsString();
                                _token = result.get("token").getAsString();
                                getRouteFares(route_no, route_bound, "01");
                                if (mEmptyText != null)
                                    mEmptyText.setText("");

                                if (null != mMenu)
                                    mMenu.findItem(R.id.action_refresh).setVisible(true);

                            } else if (result.get("valid").getAsBoolean() == false &&
                                    !result.get("message").getAsString().equals("")) {
                                // Invalid request with output message
                                if (mEmptyText != null)
                                    mEmptyText.setText(result.get("message").getAsString());
                            }
                        } else {
                            switchGetRouteInfoApi();
                            getRouteStops(route_no, route_bound);
                        }
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.GONE);
                    }
                });

    }

    private void switchGetRouteInfoApi() {
        if (getRouteInfoApi.equals(Constants.URL.ROUTE_INFO)) {
            getRouteInfoApi = Constants.URL.ROUTE_INFO_V1;
        } else {
            getRouteInfoApi = Constants.URL.ROUTE_INFO;
        }
    }

    private void getRouteFares(final String route_no, final String route_bound, final String route_st) {

        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
        List<RouteStop> routeStopList = mAdapter.getAllItems();
        for (int j = 0; j < routeStopList.size(); j++) {
            RouteStop routeStop = routeStopList.get(j);
            routeStop.fare = mContext.getString(R.string.dots);
        }
        mAdapter.notifyDataSetChanged();

        Ion.with(mContext)
                .load(Constants.URL.ROUTE_MAP)
                .setHeader("Referer", Constants.URL.HTML_SEARCH)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .setBodyParameter("bn", route_no)
                .setBodyParameter("dir", route_bound)
                .setBodyParameter("ST", route_st)
                .asJsonArray()
                .setCallback(new FutureCallback<JsonArray>() {
                    @Override
                    public void onCompleted(Exception e, JsonArray jsonArray) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                        }
                        List<RouteStop> routeStopList = mAdapter.getAllItems();
                        if (null != jsonArray) {
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonObject object = jsonArray.get(i).getAsJsonObject();
                                if (null != object) {
                                    Gson gson = new Gson();
                                    RouteStopMap routeStopMap = gson.fromJson(object, RouteStopMap.class);
                                    if (null != routeStopMap.subarea) {
                                        for (int j = 0; j < routeStopList.size(); j++) {
                                            RouteStop routeStop = routeStopList.get(j);
                                            String stopCode = routeStop.code;
                                            if (stopCode.equals(routeStopMap.subarea)) {
                                                if (null != routeStopMap.air_cond_fare &&
                                                        !routeStopMap.air_cond_fare.equals("") &&
                                                        !routeStopMap.air_cond_fare.equals("0.00"))
                                                routeStop.fare = mContext.getString(R.string.hkd, routeStopMap.air_cond_fare);
                                            }
                                        }
                                    }
                                }
                            }
                            for (int j = 0; j < routeStopList.size(); j++) {
                                RouteStop routeStop = routeStopList.get(j);
                                if (null != routeStop.fare &&
                                        routeStop.fare.equals(mContext.getString(R.string.dots)))
                                    routeStop.fare = "";
                            }
                            mAdapter.notifyDataSetChanged();
                            if (mSwipeRefreshLayout != null)
                                mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });

    }

    private void getETA(final int position, final String stop_code) {
        switch (settingsHelper.getEtaApi()) {
            case 2:
                getETAv2(position, stop_code);
                break;
            case 1:
            default:
                getETAv1(position, stop_code);
                break;
        }
    }

    private void getETAv2(final int position, final String stop_code) {
        RouteStop routeStop = mAdapter.getItem(position);
        routeStop.eta_loading = true;
        mAdapter.notifyDataSetChanged();
        final String stopCode = stop_code.replaceAll("-", "");
        final String stopSeq = String.valueOf(position);

        Uri routeEtaUri = Uri.parse(Constants.URL.ETA_MOBILE_API)
                .buildUpon()
                .appendQueryParameter("action", "geteta")
                .appendQueryParameter("lang", "tc")
                .appendQueryParameter("route", _route_no)
                .appendQueryParameter("bound", _route_bound)
                .appendQueryParameter("stop", stopCode)
                .appendQueryParameter("stop_seq", stopSeq)
                .build();

        Ion.with(mContext)
                .load(routeEtaUri.toString())
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                        }
                        RouteStop routeStop = mAdapter.getItem(position);
                        routeStop.eta_loading = true;
                        routeStop.eta_fail = false;
                        if (result != null) {
                            //Log.d(TAG, result);
                            if (!result.has("response")) {
                                routeStop.eta_loading = false;
                                routeStop.eta_fail = true;
                                mAdapter.notifyDataSetChanged();
                                getETAv1(position, stopCode);
                                return;
                            }

                            RouteStopETA routeStopETA = new RouteStopETA();
                            JsonArray jsonArray = result.get("response").getAsJsonArray();
                            routeStopETA = new RouteStopETA();
                            routeStopETA.api_version = 2;
                            routeStopETA.seq = stopSeq;
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
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void getETAv1(final int position, String bus_stop) {
        RouteStop routeStop = mAdapter.getItem(position);
        routeStop.eta_loading = true;
        mAdapter.notifyDataSetChanged();

        String stop_seq = String.valueOf(position);
        String _random_t = ((Double) Math.random()).toString();

        if (etaApi.equals("")) {
            findEtaApiUrl();
            routeStop.eta_loading = false;
            routeStop.eta_fail = true;
            mAdapter.notifyDataSetChanged();
        } else
        Ion.with(mContext)
                .load(etaApi + _random_t)
                //.setLogging("Ion", Log.DEBUG)
                .setHeader("Referer", Constants.URL.REQUEST_REFERER)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .setBodyParameter("route", _route_no)
                .setBodyParameter("route_no", _route_no)
                .setBodyParameter("bound", _route_bound)
                .setBodyParameter("busstop", bus_stop)
                .setBodyParameter("lang", "tc")
                .setBodyParameter("stopseq", stop_seq)
                .setBodyParameter("id", _id)
                .setBodyParameter("token", _token)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                        }
                        RouteStop routeStop = mAdapter.getItem(position);
                        routeStop.eta_loading = true;
                        routeStop.eta_fail = false;
                        if (result != null) {
                            //Log.d(TAG, result);
                            if (!result.contains("ETA_TIME")) {
                                findEtaApiUrl();
                                routeStop.eta_loading = false;
                                routeStop.eta_fail = true;
                                mAdapter.notifyDataSetChanged();
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
                            routeStop.eta = routeStopETA;
                        }
                        routeStop.eta_loading = false;
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void findEtaApiUrl() {
        // Find ETA API URL, by first finding the js file use to call eta api on web
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
        Ion.with(mContext)
                .load(Constants.URL.HTML_ETA)
                .setHeader("Referer", Constants.URL.KMB)
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                        }
                        if (result != null && !result.equals("")) {
                            Pattern p = Pattern.compile("\"(" + Constants.URL.PATH_ETA_JS + "[a-zA-Z0-9_.]*\\.js\\?[a-zA-Z0-9]*)\"");
                            Matcher m = p.matcher(result);
                            if (m.find()) {
                                String etaJs = Constants.URL.KMB + m.group(1);
                                findEtaApi(etaJs);
                                Log.d(TAG, "etaJs: " + etaJs);
                            }
                        }
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void findEtaApi(String JS_ETA) {
        // Find ETA API Url in found JS file
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
        Ion.with(mContext)
                .load(JS_ETA)
                .setHeader("Referer", Constants.URL.REQUEST_REFERER)
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                        }
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
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

}