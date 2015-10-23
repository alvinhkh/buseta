package com.alvinhkh.buseta.view.fragment;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteStopMap;
import com.alvinhkh.buseta.provider.EtaTable;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.FollowTable;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.RouteStopTable;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.service.RouteService;
import com.alvinhkh.buseta.view.adapter.RouteStopAdapter;
import com.alvinhkh.buseta.view.dialog.RouteEtaActivity;
import com.koushikdutta.ion.Ion;
import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;

import java.lang.ref.WeakReference;

public class RouteStopFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = RouteStopFragment.class.getSimpleName();
    private static final String KEY_LIST_VIEW_STATE = "KEY_LIST_VIEW_STATE_ROUTE_STOP";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFab;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;
    private RouteStopAdapter mAdapter;
    private UpdateViewReceiver mReceiver_view;
    private UpdateItemReceiver mReceiver_item;

    private RouteBound _routeBound;
    private Boolean fabHidden = true;

    // Runnable to get all stops eta
    int iEta = 0;
    Handler mEtaHandler = new Handler();
    Runnable mEtaRunnable = new Runnable() {
        @Override
        public void run() {
            if (null != mAdapter && iEta < mAdapter.getCount()) {
                RouteStop routeStop = mAdapter.getItem(iEta);
                routeStop.eta_loading = true;
                mAdapter.notifyDataSetChanged();
                Intent intent = new Intent(mContext, CheckEtaService.class);
                intent.putExtra(Constants.MESSAGE.SEND_UPDATING, false);
                intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                mContext.startService(intent);
                iEta++;
                if (iEta < mAdapter.getCount() - 1) {
                    mEtaHandler.postDelayed(mEtaRunnable, 120);
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

    public RouteStopFragment() {}

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
        // Get arguments
        _routeBound = getArguments().getParcelable("route");
        if (null != _routeBound) {
            _routeBound.route_no = _routeBound.route_no.trim().replace(" ", "").toUpperCase();
        } else {
            _routeBound = new RouteBound();
        }
        // Overview task
        setTaskDescription(_routeBound.route_no +
                getString(R.string.interpunct) + getString(R.string.app_name));
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(_routeBound.route_no);
            mActionBar.setSubtitle(getString(R.string.destination, _routeBound.destination_tc));
            mActionBar.setDisplayHomeAsUpEnabled(false);
        }
        setHasOptionsMenu(true);
        // Set List Adapter
        mAdapter = new RouteStopAdapter(mContext);
        if (savedInstanceState != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState);
        }
        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
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
        mListView.setAdapter(mAdapter);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
        // Broadcast Receiver
        if (null != mContext) {
            mReceiver_view = new UpdateViewReceiver();
            IntentFilter mFilter_view = new IntentFilter(Constants.MESSAGE.STOPS_UPDATED);
            mFilter_view.addAction(Constants.MESSAGE.STOPS_UPDATED);
            mContext.registerReceiver(mReceiver_view, mFilter_view);
            mReceiver_item = new UpdateItemReceiver();
            IntentFilter mFilter_item = new IntentFilter(Constants.MESSAGE.FOLLOW_UPDATED);
            mFilter_item.addAction(Constants.MESSAGE.FOLLOW_UPDATED);
            IntentFilter mFilter_eta = new IntentFilter(Constants.MESSAGE.ETA_UPDATED);
            mFilter_eta.addAction(Constants.MESSAGE.ETA_UPDATED);
            mContext.registerReceiver(mReceiver_item, mFilter_item);
            mContext.registerReceiver(mReceiver_item, mFilter_eta);
        }
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
                if (!fabHidden)
                    mFab.show();
            }

            @Override
            public void onScrollUp() {
                mFab.hide();
            }
        }, null/*new AbsListView.OnScrollListener() {

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

        }*/);
        // load data
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_LIST_VIEW_STATE)) {
            mListView.onRestoreInstanceState(savedInstanceState
                    .getParcelable(KEY_LIST_VIEW_STATE));
            mEmptyText.setText(savedInstanceState.getString("EmptyText", ""));
            fabHidden = false;
        } else {
            final Cursor c = mContext.getContentResolver().query(
                    RouteProvider.CONTENT_URI_STOP,
                    null,
                    RouteStopTable.COLUMN_ROUTE + "=? AND " +
                            RouteStopTable.COLUMN_BOUND + "=? ",
                    new String[]{
                            _routeBound.route_no,
                            _routeBound.route_bound,
                    }, RouteStopTable.COLUMN_STOP_SEQ + "* 1 ASC");
            if (null != c && c.getCount() > 0) {
                Intent intent = new Intent(Constants.MESSAGE.STOPS_UPDATED);
                intent.putExtra(Constants.MESSAGE.STOPS_UPDATED, true);
                intent.putExtra(Constants.BUNDLE.BOUND_OBJECT, _routeBound);
                intent.putExtra(Constants.BUNDLE.UPDATE_MESSAGE, Constants.STATUS.UPDATED_STOPS);
                mContext.sendBroadcast(intent);
            } else {
                requestRouteStop();
            }
            if (null != c)
                c.close();
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
        if (null != mEmptyText)
            outState.putString("EmptyText", mEmptyText.getText().toString());
        outState.putParcelable("route", _routeBound);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mActionBar) {
            mActionBar.setTitle(_routeBound.route_no);
            mActionBar.setSubtitle(getString(R.string.destination, _routeBound.destination_tc));
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
        Ion.getDefault(mContext).cancelAll(mContext);
        if (null != mContext && null != mReceiver_view)
            mContext.unregisterReceiver(mReceiver_view);
        if (null != mContext && null != mReceiver_item)
            mContext.unregisterReceiver(mReceiver_item);
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
        if (null != mEtaHandler && null != mEtaRunnable)
            mEtaHandler.removeCallbacks(mEtaRunnable);
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        requestEta(position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        return openInfoWindow(position);
    }

    private boolean requestEta(int position) {
        RouteStop object = mAdapter.getItem(position);
        if (null == object)
            return false;
        object.eta_loading = true;
        mAdapter.notifyDataSetChanged();
        Intent intent = new Intent(mContext, CheckEtaService.class);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        mContext.startService(intent);
        return true;
    }

    private boolean openInfoWindow(int position) {
        RouteStop object = mAdapter.getItem(position);
        if (null == object)
            return false;
        Intent intent = new Intent(mContext, RouteEtaActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        getActivity().startActivity(intent);
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
            if (null != mContext) {
                int rowsDeleted_route = mContext.getContentResolver().delete(
                        RouteProvider.CONTENT_URI_BOUND_FILTER, null, null);
                Log.d(TAG, "Deleted Route Records: " + rowsDeleted_route);
                int rowsDeleted_routeStop = mContext.getContentResolver().delete(
                        RouteProvider.CONTENT_URI_STOP_FILTER, null, null);
                Log.d(TAG, "Deleted Stops Records: " + rowsDeleted_routeStop);
                int rowsDeleted_eta = mContext.getContentResolver().delete(
                        FollowProvider.CONTENT_URI_ETA_JOIN, null, null);
                Log.d(TAG, "Deleted ETA Records: " + rowsDeleted_eta);
            }
            requestRouteStop();
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

    private void setTaskDescription(String title) {
        // overview task
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(title, bm,
                            ContextCompat.getColor(mContext, R.color.primary_600));
            ((AppCompatActivity) mContext).setTaskDescription(taskDesc);
        }
    }

    private void requestRouteStop() {
        if (null != mAdapter) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
        }
        if (null != mProgressBar)
            mProgressBar.setVisibility(View.VISIBLE);
        Intent intent = new Intent(mContext, RouteService.class);
        intent.putExtra(Constants.BUNDLE.BOUND_OBJECT, _routeBound);
        mContext.startService(intent);
    }

    UpdateViewHandler mViewHandler = new UpdateViewHandler(this);
    static class UpdateViewHandler extends Handler {
        WeakReference<RouteStopFragment> mFrag;

        UpdateViewHandler(RouteStopFragment aFragment) {
            mFrag = new WeakReference<>(aFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            RouteStopFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = msg.getData();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.STOPS_UPDATED);
            if (null != f.mAdapter && aBoolean) {
                if (null != f.mEtaHandler && null != f.mEtaRunnable)
                    f.mEtaHandler.removeCallbacks(f.mEtaRunnable);
                final ConnectivityManager conMgr =
                        (ConnectivityManager) f.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                if (activeNetwork == null || !activeNetwork.isConnected()) {
                    // Check internet connection
                    if (null != f.getView()) {
                        if (null != f.getView().findViewById(android.R.id.content)) {
                            Snackbar snackbar = Snackbar.make(f.getView().findViewById(android.R.id.content),
                                    R.string.message_no_internet_connection, Snackbar.LENGTH_LONG);
                            TextView tv = (TextView)
                                    snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            tv.setTextColor(Color.WHITE);
                            snackbar.show();
                        } else if (null != f.getView().getContext()) {
                            Toast.makeText(f.getView().getContext(),
                                    R.string.message_no_internet_connection,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    if (null != f.mProgressBar)
                        f.mProgressBar.setVisibility(View.GONE);
                    if (null != f.mEmptyText)
                        f.mEmptyText.setText(R.string.message_fail_to_request);
                    if (null != f.mFab)
                        f.mFab.hide();
                    return;
                }
                RouteBound object = bundle.getParcelable(Constants.BUNDLE.BOUND_OBJECT);
                String message = bundle.getString(Constants.BUNDLE.UPDATE_MESSAGE, "");
                switch (message) {
                    case Constants.STATUS.UPDATED_STOPS:
                        if (null != object && null != f._routeBound &&
                                object.route_no.equals(f._routeBound.route_no) &&
                                object.route_bound.equals(f._routeBound.route_bound)) {
                            final Cursor c = f.mContext.getContentResolver().query(
                                    RouteProvider.CONTENT_URI_STOP,
                                    null,
                                    RouteStopTable.COLUMN_ROUTE + "=? AND " +
                                            RouteStopTable.COLUMN_BOUND + "=? ",
                                    new String[]{
                                            f._routeBound.route_no,
                                            f._routeBound.route_bound,
                                    }, RouteStopTable.COLUMN_STOP_SEQ + "* 1 ASC");
                            f.mAdapter.clear();
                            while (null != c && c.moveToNext()) {
                                RouteStop routeStop = new RouteStop();
                                routeStop.route_bound = f._routeBound;
                                routeStop.stop_seq = getColumnString(c, RouteStopTable.COLUMN_STOP_SEQ);
                                routeStop.name_tc = getColumnString(c, RouteStopTable.COLUMN_STOP_NAME);
                                routeStop.name_en = getColumnString(c, RouteStopTable.COLUMN_STOP_NAME_EN);
                                routeStop.code = getColumnString(c, RouteStopTable.COLUMN_STOP_CODE);
                                Cursor cFollow = f.mContext.getContentResolver().query(FollowProvider.CONTENT_URI_FOLLOW,
                                        null,
                                        FollowTable.COLUMN_ROUTE + " =?" +
                                                " AND " + FollowTable.COLUMN_BOUND + " =?" +
                                                " AND " + FollowTable.COLUMN_STOP_CODE + " =?",
                                        new String[]{
                                                routeStop.route_bound.route_no,
                                                routeStop.route_bound.route_bound,
                                                routeStop.code
                                        },
                                        FollowTable.COLUMN_DATE + " DESC");
                                routeStop.follow = (null != cFollow && cFollow.getCount() > 0);
                                if (null != cFollow)
                                    cFollow.close();
                                RouteStopMap routeStopMap = new RouteStopMap();
                                routeStopMap.air_cond_fare = getColumnString(c, RouteStopTable.COLUMN_STOP_FARE);
                                routeStopMap.lat = getColumnString(c, RouteStopTable.COLUMN_STOP_LAT);
                                routeStopMap.lng = getColumnString(c, RouteStopTable.COLUMN_STOP_LONG);
                                routeStop.details = routeStopMap;
                                f.mAdapter.add(routeStop);
                            }
                            f.mAdapter.notifyDataSetChanged();
                            // Get ETA records in database
                            Intent intent = new Intent(Constants.MESSAGE.ETA_UPDATED);
                            intent.putExtra(Constants.MESSAGE.ETA_UPDATED, true);
                            f.mContext.sendBroadcast(intent);
                            if (null != c)
                                c.close();
                            f.fabHidden = false;
                            if (null != f.mFab)
                                f.mFab.show();
                            if (null != f.mEmptyText)
                                f.mEmptyText.setText("");
                            if (null != f.mProgressBar)
                                f.mProgressBar.setVisibility(View.GONE);
                            if (null != f.mSwipeRefreshLayout)
                                f.mSwipeRefreshLayout.setRefreshing(false);
                        }
                        break;
                    case Constants.STATUS.UPDATED_FARE:
                        if (null != object && null != f._routeBound &&
                                object.route_no.equals(f._routeBound.route_no) &&
                                object.route_bound.equals(f._routeBound.route_bound)) {
                            final Cursor c = f.mContext.getContentResolver().query(
                                    RouteProvider.CONTENT_URI_STOP,
                                    null,
                                    RouteStopTable.COLUMN_ROUTE + "=? AND " +
                                            RouteStopTable.COLUMN_BOUND + "=? ",
                                    new String[]{
                                            f._routeBound.route_no,
                                            f._routeBound.route_bound,
                                    }, RouteStopTable.COLUMN_STOP_SEQ + "* 1 ASC");
                            while (null != c && c.moveToNext()) {
                                String route_no = getColumnString(c, RouteStopTable.COLUMN_ROUTE);
                                String route_bound = getColumnString(c, RouteStopTable.COLUMN_BOUND);
                                String stop_seq = getColumnString(c, RouteStopTable.COLUMN_STOP_SEQ);
                                String stop_code = getColumnString(c, RouteStopTable.COLUMN_STOP_CODE);
                                for (int i = 0; i < f.mAdapter.getCount(); i++) {
                                    RouteStop routeStop = f.mAdapter.getItem(i);
                                    if (routeStop.route_bound.route_no.equals(route_no) &&
                                            routeStop.route_bound.route_bound.equals(route_bound) &&
                                            routeStop.stop_seq.equals(stop_seq) &&
                                            routeStop.code.equals(stop_code)) {
                                        if (null == routeStop.details)
                                            routeStop.details = new RouteStopMap();
                                        routeStop.details.air_cond_fare = getColumnString(c, RouteStopTable.COLUMN_STOP_FARE);
                                        routeStop.details.lat = getColumnString(c, RouteStopTable.COLUMN_STOP_LAT);
                                        routeStop.details.lng = getColumnString(c, RouteStopTable.COLUMN_STOP_LONG);
                                    }
                                }
                            }
                            f.mAdapter.notifyDataSetChanged();
                            if (null != c)
                                c.close();
                            if (null != f.mEmptyText)
                                f.mEmptyText.setText("");
                            if (null != f.mSwipeRefreshLayout)
                                f.mSwipeRefreshLayout.setRefreshing(false);
                        }
                        break;
                    case Constants.STATUS.CONNECTIVITY_INVALID:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(R.string.message_no_internet_connection);
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.GONE);
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(false);
                        break;
                    case Constants.STATUS.CONNECT_FAIL:
                    case Constants.STATUS.CONNECT_404:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(R.string.message_fail_to_request);
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.GONE);
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(false);
                        break;
                    case Constants.STATUS.UPDATING_FARE:
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(true);
                    case Constants.STATUS.UPDATING_STOPS:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(R.string.message_loading);
                        break;
                    default:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(message);
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.GONE);
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(false);
                        break;
                }
            }
        }

        private String getColumnString(Cursor cursor, String column) {
            int index = cursor.getColumnIndex(column);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        }
    }

    class UpdateViewReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Message message = mViewHandler.obtainMessage();
                    message.setData(bundle);
                    mViewHandler.sendMessage(message);
                }
            };
            thread.run();
        }
    }

    UpdateItemHandler mItemHandler = new UpdateItemHandler(this);
    static class UpdateItemHandler extends Handler {
        WeakReference<RouteStopFragment> mFrag;

        UpdateItemHandler(RouteStopFragment aFragment) {
            mFrag = new WeakReference<>(aFragment);
        }

        @Override
        public void handleMessage(Message message) {
            RouteStopFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = message.getData();
            Boolean aBoolean_follow = bundle.getBoolean(Constants.MESSAGE.FOLLOW_UPDATED);
            Boolean aBoolean_eta = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (null != f.mAdapter && (aBoolean_follow || aBoolean_eta)) {
                RouteStop newObject = bundle.getParcelable(Constants.BUNDLE.STOP_OBJECT);
                if (null != newObject) {
                    int position = Integer.parseInt(newObject.stop_seq);
                    if (position < f.mAdapter.getCount()) {
                        RouteStop oldObject = f.mAdapter.getItem(position);
                        if (oldObject.route_bound.route_no.equals(newObject.route_bound.route_no) &&
                                oldObject.route_bound.route_bound.equals(newObject.route_bound.route_bound) &&
                                oldObject.stop_seq.equals(newObject.stop_seq) &&
                                oldObject.code.equals(newObject.code)) {
                            oldObject.follow = newObject.follow;
                            oldObject.eta = newObject.eta;
                            oldObject.eta_loading = newObject.eta_loading;
                            oldObject.eta_fail = newObject.eta_fail;
                            f.mAdapter.notifyDataSetChanged();
                        }
                    }
                } else {

                    Cursor cursor = f.mContext.getContentResolver().query(FollowProvider.CONTENT_URI_ETA_JOIN,
                            null,
                            EtaTable.COLUMN_ROUTE + " =?" + " AND " + EtaTable.COLUMN_BOUND + " =?",
                            new String[]{
                                    f._routeBound.route_no,
                                    f._routeBound.route_bound
                            },
                            EtaTable.COLUMN_DATE + " DESC");
                    if (null != cursor) {
                        while (cursor.moveToNext()) {
                            // Load data from dataCursor and return it...
                            RouteStopETA routeStopETA = null;
                            String apiVersion = getColumnString(cursor, EtaTable.COLUMN_ETA_API);
                            if (null != apiVersion && !apiVersion.equals("")) {
                                routeStopETA = new RouteStopETA();
                                routeStopETA.api_version = Integer.valueOf(apiVersion);
                                routeStopETA.seq = getColumnString(cursor, EtaTable.COLUMN_STOP_SEQ);
                                routeStopETA.etas = getColumnString(cursor, EtaTable.COLUMN_ETA_TIME);
                                routeStopETA.expires = getColumnString(cursor, EtaTable.COLUMN_ETA_EXPIRE);
                                routeStopETA.server_time = getColumnString(cursor, EtaTable.COLUMN_SERVER_TIME);
                                routeStopETA.updated = getColumnString(cursor, EtaTable.COLUMN_UPDATED);
                            }
                            String route_no = getColumnString(cursor, EtaTable.COLUMN_ROUTE);
                            String route_bound = getColumnString(cursor, EtaTable.COLUMN_BOUND);
                            String stop_seq = getColumnString(cursor, EtaTable.COLUMN_STOP_SEQ);
                            String stop_code = getColumnString(cursor, EtaTable.COLUMN_STOP_CODE);
                            for (int i = 0; i < f.mAdapter.getCount(); i++) {
                                RouteStop object = f.mAdapter.getItem(i);
                                if (object.route_bound.route_no.equals(route_no) &&
                                        object.route_bound.route_bound.equals(route_bound) &&
                                        object.stop_seq.equals(stop_seq) &&
                                        object.code.equals(stop_code)) {
                                    String dateText = getColumnString(cursor, EtaTable.COLUMN_DATE);
                                    if ((System.currentTimeMillis() / 1000L) > (Long.valueOf(dateText) + 3600)) {
                                        // show nothing if obtain time is over an hour ago
                                        object.eta = null;
                                    } else {
                                        object.eta = routeStopETA;
                                    }
                                    object.eta_loading = getColumnString(cursor, EtaTable.COLUMN_LOADING).equals("true");
                                    object.eta_fail = getColumnString(cursor, EtaTable.COLUMN_FAIL).equals("true");
                                }
                            }
                            f.mAdapter.notifyDataSetChanged();
                        }
                        cursor.close();
                    }

                }
            }
        }

        private String getColumnString(Cursor cursor, String column) {
            int index = cursor.getColumnIndex(column);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        }
    }

    class UpdateItemReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Message message = mItemHandler.obtainMessage();
                    message.setData(bundle);
                    mItemHandler.sendMessage(message);
                }
            };
            thread.run();
        }
    }

}