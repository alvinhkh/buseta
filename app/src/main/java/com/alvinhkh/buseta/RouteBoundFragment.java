package com.alvinhkh.buseta;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;


public class RouteBoundFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RouteBoundFragment";
    private static final String KEY_LIST_VIEW_STATE = "KEY_LIST_VIEW_STATE_ROUTE_BOUND";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private TextView mTextView_routeNo;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;
    private Button mButton_routeNews;

    private RouteBoundAdapter mAdapter;
    private String _route_no = null;
    private String _route_bound = null;
    private String _id = null;
    private String _token = null;
    private String getRouteInfoApi = "";

    private SuggestionsDatabase mDatabase;

    public RouteBoundFragment() {
    }

    public static RouteBoundFragment newInstance(String _route_no) {
        RouteBoundFragment f = new RouteBoundFragment();
        Bundle args = new Bundle();
        args.putString("route_no", _route_no);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route, container, false);
        mContext = super.getActivity();
        // Get arguments
        _route_no = getArguments().getString("route_no");
        // Set Database for inserting search history
        mDatabase = new SuggestionsDatabase(getActivity().getApplicationContext());
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setSubtitle(null);
        mActionBar.setDisplayHomeAsUpEnabled(false);
        // Set List Adapter
        mAdapter = new RouteBoundAdapter(mContext);
        if (savedInstanceState != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState);
            _id = savedInstanceState.getString("_id");
            _token = savedInstanceState.getString("_token");
            getRouteInfoApi = savedInstanceState.getString("getRouteInfoApi");
        }
        //
        mTextView_routeNo = (TextView) view.findViewById(R.id.route_no);
        mTextView_routeNo.setText(_route_no);
        //
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_route);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(true);
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
            getRouteInfoApi = Constants.URL.ROUTE_INFO;
            // Get Route Bounds
            getRouteBounds(_route_no);
        }
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        //mListView.setOnItemLongClickListener(this);
        // Button
        mButton_routeNews = (Button) view.findViewById(R.id.button_news);
        mButton_routeNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showRouteNewsFragment(_route_no);
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("_route_no", _route_no);
        outState.putString("_id", _id);
        outState.putString("_token", _token);
        outState.putString("getRouteInfoApi", getRouteInfoApi);
        if (null != mAdapter) {
            mAdapter.onSaveInstanceState(outState);
            outState.putParcelable(KEY_LIST_VIEW_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
        }
    }

    @Override
    public void onDestroyView() {
        if (null != mDatabase)
            mDatabase.close();
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
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view != null) {
            TextView textView_origin_tc = (TextView) view.findViewById(R.id.origin);
            TextView textView_destination_tc = (TextView) view.findViewById(R.id.destination);
            ((MainActivity) getActivity()).showRouteStopFragment(_route_no,
                    String.valueOf(position + 1),
                    textView_origin_tc.getText().toString(),
                    textView_destination_tc.getText().toString());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    @Override
    public void onRefresh() {
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(true);
        if (null != mAdapter)
            mAdapter.clear();
        getRouteBounds(_route_no);
    }



    private void getRouteBounds(final String _route_no) {

        if (mEmptyText != null)
            mEmptyText.setText(R.string.message_loading);
        if (mProgressBar != null)
            mProgressBar.setVisibility(View.VISIBLE);

        String _random_t = ((Double) Math.random()).toString();

        Uri routeInfoUri = Uri.parse(getRouteInfoApi)
                .buildUpon()
                .appendQueryParameter("t", _random_t)
                .appendQueryParameter("field9", _route_no)
                .build();

        Ion.with(mContext)
                .load(routeInfoUri.toString())
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
                            Log.d(TAG, e.toString());
                            if (mEmptyText != null)
                                mEmptyText.setText(R.string.message_fail_to_request);
                        }
                        JsonObject result = response.getResult();
                        if (response.getHeaders().code() == 200 && null != result) {
                            //Log.d(TAG, result.toString());
                            if (result.get("valid").getAsBoolean() == true) {
                                //  Got Bus Routes

                                JsonArray _bus_arr = result.getAsJsonArray("bus_arr");
                                for (JsonElement element : _bus_arr) {
                                    Gson gson = new Gson();
                                    RouteBound routeBound = gson.fromJson(element.getAsJsonObject(), RouteBound.class);
                                    mAdapter.add(routeBound);
                                }
                                _id = result.get("id").getAsString();
                                _token = result.get("token").getAsString();
                                if (mEmptyText != null)
                                    mEmptyText.setText("");
                                if (mDatabase != null)
                                    mDatabase.insertHistory(_route_no);

                            } else if (result.get("valid").getAsBoolean() == false &&
                                    !result.get("message").getAsString().equals("")) {
                                // Invalid request with output message
                                if (mEmptyText != null)
                                    mEmptyText.setText(result.get("message").getAsString());
                            }
                        } else {
                            Log.d(TAG, "Response: " + response.getHeaders().code() + " ");
                            switchGetRouteInfoApi();
                            getRouteBounds(_route_no);
                        }
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.GONE);
                        if (mSwipeRefreshLayout != null)
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
    }

}