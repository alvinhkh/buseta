package com.alvinhkh.buseta;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

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
                if (iEta < mAdapter.getCount()) {
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
            Log.d(TAG, "See: " + etaApi);
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
        } else {
            // Get Route Stops
            getRouteStops(_route_no, _route_bound);
            findEtaApiUrl();
        }
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        //mListView.setOnItemLongClickListener(this);

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
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
        menu.findItem(R.id.action_clear_history).setVisible(false);
        menu.findItem(R.id.action_about).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
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

        Uri routeStopUri = Uri.parse(Constants.URL.ROUTE_INFO)
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
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.e(TAG, e.toString());
                            if (mEmptyText != null)
                                mEmptyText.setText(R.string.message_fail_to_request);
                        }
                        if (result != null) {
                            //Log.d(TAG, result.toString());
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
                        }
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.GONE);
                    }
                });

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
                        if (null != jsonArray) {
                            //Log.d(TAG, result.toString());
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonObject object = jsonArray.get(i).getAsJsonObject();
                                if (null != object) {
                                    //Log.d(TAG, object.toString());
                                    Gson gson = new Gson();
                                    RouteStopMap routeStopMap = gson.fromJson(object, RouteStopMap.class);
                                    if (null != routeStopMap.subarea) {
                                        List<RouteStop> routeStopList = mAdapter.getAllItems();
                                        for (int j = 0; j < routeStopList.size(); j++) {
                                            RouteStop routeStop = routeStopList.get(j);
                                            String stopCode = routeStop.code;
                                            if (stopCode.equals(routeStopMap.subarea)) {
                                                if (null != routeStopMap.air_cond_fare &&
                                                        !routeStopMap.air_cond_fare.equals(""))
                                                routeStop.fare = mContext.getString(R.string.hkd, routeStopMap.air_cond_fare);
                                            }
                                        }
                                    }
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                            if (mSwipeRefreshLayout != null)
                                mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });

    }

    private void getETA(final int position, String bus_stop) {
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
                                    Log.d(TAG, "etaApi: found " + etaApi);
                                } else {
                                    Log.d(TAG, "etaApi: fail " + etaApi);
                                }
                            }
                        }
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

}