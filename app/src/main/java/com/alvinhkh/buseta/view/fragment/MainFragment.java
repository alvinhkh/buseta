package com.alvinhkh.buseta.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.AbstractCursor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.FavouriteDatabase;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopContainer;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.view.adapter.FeatureAdapter;
import com.alvinhkh.buseta.database.SuggestionsDatabase;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

public class MainFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainFragment";

    private Context mContext = super.getActivity();
    private SuggestionsDatabase mDatabase_suggestion;
    private FavouriteDatabase mDatabase_favourite;
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
        mContext = super.getActivity();
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mDatabase_suggestion = new SuggestionsDatabase(mContext.getApplicationContext());
        mDatabase_favourite = new FavouriteDatabase(mContext.getApplicationContext());
        AbstractCursor cursor_fav = (AbstractCursor) mDatabase_favourite.get();
        if (savedInstanceState != null) {
            routeStopList = savedInstanceState.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.MESSAGE.STATE_UPDATED, true);
            bundle.putParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
            if (null != cursor_fav)
                cursor_fav.setExtras(bundle);
        }
        if (null == routeStopList)
            routeStopList = new ArrayList<RouteStopContainer>();
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
                cursor_fav);
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
            mAutoRefreshHandler.post(mAutoRefreshRunnable);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
        }
        if (null != mAdapter)
            mAdapter.swapHistoryCursor(mDatabase_suggestion.getHistory());
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
        if (null != mDatabase_favourite)
            mDatabase_favourite.close();
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
        if (null != mAdapter && null != mContext)
            for (int i = 0; i < mAdapter.getFavouriteCount(); i++) {
                RouteStop object = mAdapter.getFavouriteItem(i);
                Intent intent = new Intent(mContext, CheckEtaService.class);
                intent.putExtra(Constants.BUNDLE.ITEM_POSITION, i);
                intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
                intent.putParcelableArrayListExtra(Constants.BUNDLE.STOP_OBJECTS, routeStopList);
                mContext.startService(intent);
            }
        mSwipeRefreshLayout.setRefreshing(false);
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
                mAdapter.swapHistoryCursor(mDatabase_suggestion.getHistory());
            }
        }
    }

    public class UpdateEtaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (null != mAdapter && null != mDatabase_favourite && aBoolean) {
                routeStopList = bundle.getParcelableArrayList(Constants.BUNDLE.STOP_OBJECTS);
                int position = bundle.getInt(Constants.BUNDLE.ITEM_POSITION, -1);
                RouteStop newObject = bundle.getParcelable(Constants.BUNDLE.STOP_OBJECT);
                if (null != newObject) {
                    for (int i = 0; i < routeStopList.size(); i++) {
                        RouteStop oldObject = routeStopList.get(i).routeStop;
                        int listPosition = routeStopList.get(i).position;
                        if (null != oldObject && position == listPosition) {
                            oldObject.eta = newObject.eta;
                            oldObject.eta_loading = newObject.eta_loading;
                            oldObject.eta_fail = newObject.eta_fail;
                            oldObject.favourite = newObject.favourite;
                        }
                    }
                }
                AbstractCursor cursor = (AbstractCursor) mDatabase_favourite.get();
                if (null != cursor)
                    cursor.setExtras(bundle);
                mAdapter.swapFavouriteCursor(cursor);
            }
        }
    }

}
