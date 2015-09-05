package com.alvinhkh.buseta.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.FavouriteDatabase;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopContainer;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.view.adapter.RouteStopAdapter;
import com.alvinhkh.buseta.holder.RouteStopMap;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.alvinhkh.buseta.view.dialog.RouteEtaDialog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;

import java.util.ArrayList;
import java.util.List;

public class RouteStopFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RouteStopFragment";
    private static final String KEY_LIST_VIEW_STATE = "KEY_LIST_VIEW_STATE_ROUTE_STOP";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFab;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;
    private RouteStopAdapter mAdapter;
    private UpdateViewReceiver mReceiver;

    private ArrayList<RouteStopContainer> routeStopList = null;
    private RouteBound _routeBound;
    private String _route_no = null;
    private String _route_bound = null;
    private String _route_origin = null;
    private String _route_destination = null;
    private String _id = null;
    private String _token = null;
    private String etaApi = "";
    private String getRouteInfoApi = "";
    private Boolean fabHidden = true;
    private FavouriteDatabase mDatabase;
    private SettingsHelper settingsHelper = null;
    private SharedPreferences mPrefs;

    // Runnable to get all stops eta
    int iEta = 0;
    Handler mEtaHandler = new Handler();
    Runnable mEtaRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != mAdapter && iEta < mAdapter.getCount()) {
                RouteStop routeStop = mAdapter.getItem(iEta);
                Intent intent = new Intent(mContext, CheckEtaService.class);
                intent.putExtra(Constants.BUNDLE.ITEM_POSITION, iEta);
                intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
                mContext.startService(intent);
                iEta++;
                if (iEta < mAdapter.getCount() - 1) {
                    int interval = 800;
                    if (null != settingsHelper && settingsHelper.getEtaApi() == 2)
                        interval = 150;
                    mEtaHandler.postDelayed(mEtaRunnable, interval);
                } else {
                    if (mSwipeRefreshLayout != null)
                        mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        }
    };

    Handler mAutoRefreshHandler = new Handler();
    Runnable mAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != mAdapter)
                mAdapter.notifyDataSetChanged();
            mAutoRefreshHandler.postDelayed(mAutoRefreshRunnable, 30 * 1000); // every half minute
        }
    };

    public RouteStopFragment() {
    }

    public static RouteStopFragment newInstance(RouteBound routeBound) {
        RouteStopFragment f = new RouteStopFragment();
        Bundle args = new Bundle();
        args.putParcelable("route", routeBound);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routestop, container, false);
        mContext = super.getActivity();
        // set database
        mDatabase = new FavouriteDatabase(mContext);
        settingsHelper = new SettingsHelper().parse(mContext.getApplicationContext());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
        // Get arguments
        _routeBound = getArguments().getParcelable("route");
        if (null != _routeBound) {
            _route_no = _routeBound.route_no.trim().replace(" ", "").toUpperCase();
            _route_bound = _routeBound.route_bound;
            _route_origin = _routeBound.origin_tc;
            _route_destination = _routeBound.destination_tc;
        }
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
            routeStopList = savedInstanceState.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
            _id = savedInstanceState.getString("_id");
            _token = savedInstanceState.getString("_token");
            etaApi = savedInstanceState.getString("etaApi");
            getRouteInfoApi = savedInstanceState.getString("getRouteInfoApi");
        }
        if (null == routeStopList)
            routeStopList = new ArrayList<RouteStopContainer>();
        // SwipeRefreshLayout
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
            fabHidden = false;
        } else {
            getRouteInfoApi = Constants.URL.ROUTE_INFO;
            // Get Route Stops
            getRouteStops(_route_no, _route_bound);
        }
        mListView.setAdapter(mAdapter);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
        // FloatingActionButton
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        fabHidden = true;
        mFab.attachToListView(mListView, new ScrollDirectionListener() {
            @Override
            public void onScrollDown() {
                if (fabHidden == false)
                    mFab.show();
            }

            @Override
            public void onScrollUp() {
                mFab.hide();
            }
        }, new AbsListView.OnScrollListener() {

            int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (view.getId() == mListView.getId()) {
                    final int currentFirstVisibleItem = mListView.getFirstVisiblePosition();
                    if (currentFirstVisibleItem > mLastFirstVisibleItem) {
                        // mActionBar.hide();
                    } else if (currentFirstVisibleItem < mLastFirstVisibleItem) {
                        // mActionBar.show();
                    }
                    mLastFirstVisibleItem = currentFirstVisibleItem;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean enable = false;
                if (mListView != null && mListView.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = mListView.getFirstVisiblePosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = mListView.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                // mSwipeRefreshLayout.setEnabled(enable);
            }

        });
        // Broadcast Receiver
        if (null != mContext) {
            mReceiver = new UpdateViewReceiver();
            IntentFilter mFilter = new IntentFilter(Constants.MESSAGE.STOP_UPDATED);
            mFilter.addAction(Constants.MESSAGE.STOP_UPDATED);
            IntentFilter mFilter_eta = new IntentFilter(Constants.MESSAGE.ETA_UPDATED);
            mFilter_eta.addAction(Constants.MESSAGE.ETA_UPDATED);
            mContext.registerReceiver(mReceiver, mFilter);
            mContext.registerReceiver(mReceiver, mFilter_eta);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mAdapter) {
            mAdapter.onSaveInstanceState(outState);
            outState.putParcelable(KEY_LIST_VIEW_STATE, mListView.onSaveInstanceState());
        }
        if (null != routeStopList)
            outState.putParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
        outState.putParcelable("route", _routeBound);
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
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.post(mAutoRefreshRunnable);
    }

    @Override
    public void onPause() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (null != mContext && null != mReceiver)
            mContext.unregisterReceiver(mReceiver);
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(false);
        if (null != mFab)
            mFab.hide();
        if (null != mListView)
            mListView.setAdapter(null);
        if (null != mProgressBar)
            mProgressBar.setVisibility(View.GONE);
        if (null != mEmptyText)
            mEmptyText.setVisibility(View.GONE);
        if (null != mDatabase)
            mDatabase.close();
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        if (null != mEtaHandler && null != mEtaRunnable)
            mEtaHandler.removeCallbacks(mEtaRunnable);
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view,
                            final int position, long id) {
        if (view != null) {
            RouteStop routeStop = mAdapter.getItem(position);
            Intent intent = new Intent(mContext, CheckEtaService.class);
            intent.putExtra(Constants.BUNDLE.ITEM_POSITION, position);
            intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
            intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
            mContext.startService(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        RouteStop object = mAdapter.getItem(position);
        if (null == object)
            return false;
        Intent intent = new Intent(mContext, RouteEtaDialog.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.BUNDLE.ITEM_POSITION, position);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        startActivity(intent);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            getRouteStops(_route_no, _route_bound);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
     public void onRefresh() {
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(true);
        iEta = 0;
        if (null != mEtaHandler && null != mEtaRunnable)
            mEtaHandler.post(mEtaRunnable);
    }

    private void getRouteStops(final String route_no, final String route_bound) {

        if (mEmptyText != null)
            mEmptyText.setText(R.string.message_loading);
        if (mProgressBar != null)
            mProgressBar.setVisibility(View.VISIBLE);
        if (null != mEtaHandler && null != mEtaRunnable)
            mEtaHandler.removeCallbacks(mEtaRunnable);
        if (null != mAdapter) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
        }

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
                .setHeader("Referer", Constants.URL.REQUEST_REFERRER)
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
                            mAdapter.clear();
                            if (null != result)
                            if (result.get("valid").getAsBoolean() == true) {
                                //  Got Bus Line Stops
                                JsonArray _bus_arr = result.getAsJsonArray("bus_arr");
                                int seq = 0;
                                for (JsonElement element : _bus_arr) {
                                    Gson gson = new Gson();
                                    RouteStop routeStop = gson.fromJson(element.getAsJsonObject(), RouteStop.class);
                                    routeStop.route_bound = _routeBound;
                                    routeStop.stop_seq = String.valueOf(seq);
                                    Cursor cursor = mDatabase.getExist(routeStop);
                                    routeStop.favourite = (null != cursor && cursor.getCount() > 0);
                                    mAdapter.add(routeStop);
                                    seq++;
                                }
                                _id = result.get("id").getAsString();
                                _token = result.get("token").getAsString();
                                getRouteFares(route_no, route_bound, "01");
                                if (mEmptyText != null)
                                    mEmptyText.setText("");

                                fabHidden = false;

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
                        if (null != mSwipeRefreshLayout)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });

    }

    private void switchGetRouteInfoApi() {
        if (getRouteInfoApi.equals(Constants.URL.ROUTE_INFO)) {
            getRouteInfoApi = Constants.URL.ROUTE_INFO_V1;
        } else {
            getRouteInfoApi = Constants.URL.ROUTE_INFO;
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(Constants.PREF.REQUEST_API_INFO, getRouteInfoApi);
        editor.putString(Constants.PREF.REQUEST_ID, _id);
        editor.putString(Constants.PREF.REQUEST_TOKEN, _token);
        editor.commit();
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

    public class UpdateViewReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean_stop = bundle.getBoolean(Constants.MESSAGE.STOP_UPDATED);
            Boolean aBoolean_eta = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (null != mAdapter && (aBoolean_stop ==  true || aBoolean_eta == true)) {
                RouteStop newObject = bundle.getParcelable(Constants.BUNDLE.STOP_OBJECT);
                if (null != newObject) {
                    int position = Integer.parseInt(newObject.stop_seq);
                    if (position < mAdapter.getCount()) {
                        RouteStop oldObject = mAdapter.getItem(position);
                        oldObject.favourite = newObject.favourite;
                        oldObject.eta = newObject.eta;
                        oldObject.eta_loading = newObject.eta_loading;
                        oldObject.eta_fail = newObject.eta_fail;
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

}