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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.FollowTable;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.view.adapter.FeatureAdapter;
import com.koushikdutta.ion.Ion;

import java.lang.ref.WeakReference;

public class MainFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = MainFragment.class.getSimpleName();

    private Context mContext = super.getActivity();
    private FeatureAdapter mAdapter;
    private Cursor mCursor_history;
    private Cursor mCursor_follow;
    private ActionBar mActionBar = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mFab;
    private MenuItem mSearchMenuItem;
    private UpdateHistoryReceiver mReceiver_history;
    private UpdateEtaReceiver mReceiver_eta;
    private UpdateFollowReceiver mReceiver_follow;

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
        mCursor_history = mContext.getContentResolver().query(SuggestionProvider.CONTENT_URI,
                null, SuggestionTable.COLUMN_TEXT + " LIKE '%%'" + " AND " +
                        SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'",
                null, SuggestionTable.COLUMN_DATE + " DESC");
        mCursor_follow = mContext.getContentResolver().query(
                FollowProvider.CONTENT_URI, null, null, null,
                FollowTable.COLUMN_DATE + " DESC");
        mAdapter = new FeatureAdapter(getActivity(), mCursor_history, mCursor_follow);
        mRecyclerView.setAdapter(mAdapter);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < mAdapter.getFollowCount() ? manager.getSpanCount() : 1;
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
                mSearchMenuItem.expandActionView();
                SearchView mSearchView = (SearchView) mSearchMenuItem.getActionView();
                mSearchView.requestFocus();
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
            mReceiver_eta = new UpdateEtaReceiver();
            IntentFilter mFilter_item = new IntentFilter(Constants.MESSAGE.STOP_UPDATED);
            mFilter_item.addAction(Constants.MESSAGE.STOP_UPDATED);
            IntentFilter mFilter_eta = new IntentFilter(Constants.MESSAGE.ETA_UPDATED);
            mFilter_eta.addAction(Constants.MESSAGE.ETA_UPDATED);
            mContext.registerReceiver(mReceiver_eta, mFilter_item);
            mContext.registerReceiver(mReceiver_eta, mFilter_eta);
            IntentFilter mFilter_follow = new IntentFilter(Constants.MESSAGE.FOLLOW_UPDATED);
            mReceiver_follow = new UpdateFollowReceiver();
            mFilter_follow.addAction(Constants.MESSAGE.FOLLOW_UPDATED);
            mContext.registerReceiver(mReceiver_follow, mFilter_follow);
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
            Cursor oldCursor = mAdapter.swapHistoryCursor(
                    mContext.getContentResolver().query(SuggestionProvider.CONTENT_URI,
                    null, SuggestionTable.COLUMN_TEXT + " LIKE '%%'" + " AND " +
                            SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'",
                    null, SuggestionTable.COLUMN_DATE + " DESC"));
            if (null != oldCursor)
                oldCursor.close();
        }
        if (null != mFab)
            mFab.show();
        int rowsDeleted_route = mContext.getContentResolver().delete(
                RouteProvider.CONTENT_URI_BOUND_FILTER, null, null);
        Log.d(TAG, "Deleted Route Records: " + rowsDeleted_route);
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
            if (null != mReceiver_follow)
                mContext.unregisterReceiver(mReceiver_follow);
        }
        // Unregister the listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
        if (null != mCursor_follow)
            mCursor_follow.close();
        if (null != mCursor_history)
            mCursor_history.close();
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Show Share button
        final MenuItem itemShare = menu.findItem(R.id.action_share);
        itemShare.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        // Search button and SearchView
        menu.findItem(R.id.action_search).setVisible(false);
        mSearchMenuItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (null != mFab)
                        mFab.hide();
                    itemShare.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                } else {
                    if (null != mFab)
                        mFab.show();
                    itemShare.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
            }
        });
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
                for (int i = 0; i < mAdapter.getFollowCount(); i++) {
                    RouteStop object = mAdapter.getFollowItem(i);
                    Intent intent = new Intent(mContext, CheckEtaService.class);
                    intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
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

    UpdateHistoryHandler mHistoryHandler = new UpdateHistoryHandler(this);
    static class UpdateHistoryHandler extends Handler {
        WeakReference<MainFragment> mFrag;

        UpdateHistoryHandler(MainFragment aFragment) {
            mFrag = new WeakReference<>(aFragment);
        }

        @Override
        public void handleMessage(Message message) {
            MainFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = message.getData();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.HISTORY_UPDATED);
            if (null != f.mAdapter && null != f.mContext && aBoolean) {
                f.mCursor_history = f.mContext.getContentResolver().query(SuggestionProvider.CONTENT_URI,
                        null, SuggestionTable.COLUMN_TEXT + " LIKE '%%'" + " AND " +
                                SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'",
                        null, SuggestionTable.COLUMN_DATE + " DESC");
                Cursor oldCursor = f.mAdapter.swapHistoryCursor(f.mCursor_history);
                if (null != oldCursor)
                    oldCursor.close();
            }
        }
    }

    public class UpdateHistoryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Message message = mHistoryHandler.obtainMessage();
                    message.setData(bundle);
                    mHistoryHandler.sendMessage(message);
                }
            };
            thread.run();
        }
    }

    UpdateEtaHandler mEtaHandler = new UpdateEtaHandler(this);
    static class UpdateEtaHandler extends Handler {
        WeakReference<MainFragment> mFrag;

        UpdateEtaHandler(MainFragment aFragment) {
            mFrag = new WeakReference<>(aFragment);
        }

        @Override
        public void handleMessage(Message message) {
            MainFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = message.getData();
            Boolean aBoolean_stop = bundle.getBoolean(Constants.MESSAGE.STOP_UPDATED);
            Boolean aBoolean_eta = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (null != f.mAdapter && null != f.mContext && (aBoolean_stop || aBoolean_eta)) {
                f.mCursor_follow = f.mContext.getContentResolver().query(
                        FollowProvider.CONTENT_URI, null, null, null,
                        FollowTable.COLUMN_DATE + " DESC");
                Cursor oldCursor = f.mAdapter.swapFollowCursor(f.mCursor_follow);
                if (null != oldCursor)
                    oldCursor.close();
                if (aBoolean_stop) {
                    f.mContext.sendBroadcast(new Intent(Constants.MESSAGE.WIDGET_TRIGGER_UPDATE));
                }
                f.mSwipeRefreshLayout.setRefreshing(false);
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
                    Message message = mEtaHandler.obtainMessage();
                    message.setData(bundle);
                    mEtaHandler.sendMessage(message);
                }
            };
            thread.run();
        }
    }

    UpdateFollowHandler mFollowHandler = new UpdateFollowHandler(this);
    static class UpdateFollowHandler extends Handler {
        WeakReference<MainFragment> mFrag;

        UpdateFollowHandler(MainFragment aFragment) {
            mFrag = new WeakReference<>(aFragment);
        }

        @Override
        public void handleMessage(Message message) {
            MainFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = message.getData();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.FOLLOW_UPDATED);
            if (null != f.mAdapter && null != f.mContext && aBoolean) {
                f.mCursor_follow = f.mContext.getContentResolver().query(
                        FollowProvider.CONTENT_URI, null, null, null,
                        FollowTable.COLUMN_DATE + " DESC");
                Cursor oldCursor = f.mAdapter.swapFollowCursor(f.mCursor_follow);
                if (null != oldCursor)
                    oldCursor.close();
                int rowsDeleted_route = f.mContext.getContentResolver().delete(
                        RouteProvider.CONTENT_URI_BOUND_FILTER, null, null);
                Log.d(TAG, "Deleted Route Records: " + rowsDeleted_route);
                int rowsDeleted_routeStop = f.mContext.getContentResolver().delete(
                        RouteProvider.CONTENT_URI_STOP_FILTER, null, null);
                Log.d(TAG, "Deleted Stops Records: " + rowsDeleted_routeStop);
                int rowsDeleted_eta = f.mContext.getContentResolver().delete(
                        FollowProvider.CONTENT_URI_ETA_JOIN, null, null);
                Log.d(TAG, "Deleted ETA Records: " + rowsDeleted_eta);
            }
        }
    }

    public class UpdateFollowReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Message message = mFollowHandler.obtainMessage();
                    message.setData(bundle);
                    mFollowHandler.sendMessage(message);
                }
            };
            thread.run();
        }
    }

}
