package com.alvinhkh.buseta.view.fragment;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.FavouriteProvider;
import com.alvinhkh.buseta.database.FavouriteTable;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopContainer;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.view.adapter.FeatureAdapter;
import com.alvinhkh.buseta.database.SuggestionsDatabase;
import com.koushikdutta.ion.Ion;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainFragment";

    private Context mContext = super.getActivity();
    private SuggestionsDatabase mDatabase_suggestion;
    private FeatureAdapter mAdapter;
    private ActionBar mActionBar = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFab;
    private MenuItem mSearchMenuItem;
    private UpdateHistoryReceiver mReceiver_history;
    private UpdateEtaReceiver mReceiver_eta;
    private ArrayList<RouteStopContainer> routeStopList = null;

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        MainFragment f = new MainFragment();
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mContext = super.getActivity();
        mDatabase_suggestion = new SuggestionsDatabase(mContext.getApplicationContext());
        Cursor cursorFav = mContext.getContentResolver().query(
                        FavouriteProvider.CONTENT_URI, null, null, null,
                FavouriteTable.COLUMN_DATE + " DESC");
        if (savedInstanceState != null) {
            routeStopList = savedInstanceState.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.MESSAGE.STATE_UPDATED, true);
            bundle.putParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
            if (null != cursorFav)
                cursorFav.setExtras(bundle);
        }
        if (null == routeStopList)
            routeStopList = new ArrayList<RouteStopContainer>();
        // Overview task
        setTaskDescription(getString(R.string.launcher_name));
        // Toolbar
        mActionBar = ((AppCompatActivity) mContext).getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
            mActionBar.setDisplayHomeAsUpEnabled(false);
        }
        setHasOptionsMenu(true);
        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);
        // RecyclerView
        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        final GridLayoutManager manager = new GridLayoutManager(mContext, 2);
        mRecyclerView.setLayoutManager(manager);
        mAdapter = new FeatureAdapter(getActivity(),
                mDatabase_suggestion.getHistory(),
                cursorFav);
        mRecyclerView.setAdapter(mAdapter);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < mAdapter.getFavouriteCount() ? manager.getSpanCount() : 1;
            }
        });
        View mEmptyView = view.findViewById(android.R.id.empty);
        mEmptyView.setVisibility(mAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        // Floating Action Button
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == mSearchMenuItem) return;
                SearchView mSearchView = (SearchView) mSearchMenuItem.getActionView();
                if (mSearchMenuItem.isActionViewExpanded() && null != mSearchView) {
                    mSearchView.requestFocus();
                } else {
                    mSearchMenuItem.expandActionView();
                }
            }
        });
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(this);
        // broadcast receiver
        if (null != mContext) {
            IntentFilter mFilter_history = new IntentFilter(Constants.MESSAGE.HISTORY_UPDATED);
            mReceiver_history = new UpdateHistoryReceiver();
            mFilter_history.addAction(Constants.MESSAGE.HISTORY_UPDATED);
            mContext.registerReceiver(mReceiver_history, mFilter_history);
            IntentFilter mFilter_eta = new IntentFilter(Constants.MESSAGE.ETA_UPDATED);
            mReceiver_eta = new UpdateEtaReceiver();
            mFilter_eta.addAction(Constants.MESSAGE.ETA_UPDATED);
            mContext.registerReceiver(mReceiver_eta, mFilter_eta);
        }
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.postDelayed(mAutoRefreshRunnable, 100);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
        }
        if (null != mAdapter) {
            Cursor oldCursor = mAdapter.swapHistoryCursor(mDatabase_suggestion.getHistory());
            if (null != oldCursor)
                oldCursor.close();
        }
        if (null != mFab)
            mFab.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != routeStopList)
            outState.putParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
    }

    @Override
    public void onDestroyView() {
        Ion.getDefault(mContext).cancelAll(mContext);
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        if (null != mContext) {
            if (null != mReceiver_history)
                mContext.unregisterReceiver(mReceiver_history);
            if (null != mReceiver_eta)
                mContext.unregisterReceiver(mReceiver_eta);
        }
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
        if (null != mDatabase_suggestion)
            mDatabase_suggestion.close();
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_search).setVisible(false);
        mSearchMenuItem = menu.findItem(R.id.action_search);
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
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.matches("eta_version")) {
            onRefresh();
        }
    }

    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        if (null != mAdapter && null != mContext) {
            // Check internet connection
            final ConnectivityManager conMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                for (int i = 0; i < mAdapter.getFavouriteCount(); i++) {
                    RouteStop object = mAdapter.getFavouriteItem(i);
                    Intent intent = new Intent(mContext, CheckEtaService.class);
                    intent.putExtra(Constants.BUNDLE.ITEM_POSITION, i);
                    intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
                    intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
                    mContext.startService(intent);
                }
            } else {
                Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.coordinator),
                        R.string.message_no_internet_connection, Snackbar.LENGTH_LONG);
                TextView tv = (TextView)
                        snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                snackbar.show();
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
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

    Handler mAutoRefreshHandler = new Handler();
    Runnable mAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            onRefresh();
            mAutoRefreshHandler.postDelayed(mAutoRefreshRunnable, 60 * 1000); // every minute
        }
    };

    public class UpdateHistoryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.HISTORY_UPDATED);
            if (null != mAdapter && null != mDatabase_suggestion && aBoolean) {
                Cursor oldCursor = mAdapter.swapHistoryCursor(mDatabase_suggestion.getHistory());
                if (null != oldCursor)
                    oldCursor.close();
            }
        }
    }

    UpdateViewHandler mViewHandler = new UpdateViewHandler(this);
    static class UpdateViewHandler extends Handler {
        WeakReference<MainFragment> mFrag;

        UpdateViewHandler(MainFragment aFragment) {
            mFrag = new WeakReference<MainFragment>(aFragment);
        }

        @Override
        public void handleMessage(Message message) {
            MainFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = message.getData();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (null != f.mAdapter && null != f.mContext && aBoolean) {
                f.routeStopList = bundle.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
                int position = bundle.getInt(Constants.BUNDLE.ITEM_POSITION, -1);
                RouteStop newObject = bundle.getParcelable(Constants.BUNDLE.STOP_OBJECT);
                if (null != newObject) {
                    for (int i = 0; i < f.routeStopList.size(); i++) {
                        RouteStop oldObject = f.routeStopList.get(i).routeStop;
                        int listPosition = f.routeStopList.get(i).position;
                        if (null != oldObject && position == listPosition) {
                            oldObject.eta = newObject.eta;
                            oldObject.eta_loading = newObject.eta_loading;
                            oldObject.eta_fail = newObject.eta_fail;
                            oldObject.favourite = newObject.favourite;
                        }
                    }
                }
                Cursor cursorFav = f.mContext.getContentResolver().query(
                        FavouriteProvider.CONTENT_URI, null, null, null,
                        FavouriteTable.COLUMN_DATE + " DESC");
                if (null != cursorFav)
                    cursorFav.setExtras(bundle);
                Cursor oldCursor = f.mAdapter.swapFavouriteCursor(cursorFav);
                if (null != oldCursor)
                    oldCursor.close();
            }
        }
    }

    public class UpdateEtaReceiver extends BroadcastReceiver {
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

}
