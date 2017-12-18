package com.alvinhkh.buseta.ui.follow;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.SearchHistory;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.search.SearchActivity;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.google.gson.Gson;

import java.util.ArrayList;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;


public class FollowFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private FloatingActionButton mFab;

    private FollowAndHistoryAdapter followAndHistoryAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private View emptyView;

    private final Handler refreshHandler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (followAndHistoryAdapter != null && followAndHistoryAdapter.getFollowCount() > 0) {
                followAndHistoryAdapter.notifyItemRangeChanged(0, followAndHistoryAdapter.getFollowCount());
            }
            refreshHandler.postDelayed(this, 30000);  // refresh every half minute
        }
    };

    private final Handler refreshEtaHandler = new Handler();

    private final Runnable refreshEtaRunnable = new Runnable() {
        @Override
        public void run() {
            onRefresh();
            refreshEtaHandler.postDelayed(this, 60000);  // refresh eta every minute
        }
    };

    public FollowFragment() {
    }

    /**
     * Returns a new instance of this fragment
     */
    public static FollowFragment newInstance() {
        FollowFragment fragment = new FollowFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        followAndHistoryAdapter = new FollowAndHistoryAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_follow, container, false);
        setHasOptionsMenu(true);

        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        if (null != mSwipeRefreshLayout) {
            mSwipeRefreshLayout.setOnRefreshListener(this);
            mSwipeRefreshLayout.setRefreshing(false);
        }
        RecyclerView recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        final GridLayoutManager manager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(followAndHistoryAdapter);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < followAndHistoryAdapter.getFollowCount() ? manager.getSpanCount() : 1;
            }
        });
        emptyView = rootView.findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(followAndHistoryAdapter != null &&
                    followAndHistoryAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        }
        mFab = getActivity().findViewById(R.id.fab);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }
        if (followAndHistoryAdapter != null) {
            followAndHistoryAdapter.updateHistory();
            followAndHistoryAdapter.updateFollow();
            followAndHistoryAdapter.notifyDataSetChanged();
            if (emptyView != null) {
                emptyView.setVisibility(followAndHistoryAdapter != null &&
                        followAndHistoryAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
            }
            updateAppShortcuts();
        }
        if (mFab != null) {
            mFab.show();
        }
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.ETA_UPDATE))
                .share().subscribeWith(etaObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.FOLLOW_UPDATE))
                .share().subscribeWith(followObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.HISTORY_UPDATE))
                .share().subscribeWith(historyObserver()));
        refreshEtaHandler.post(refreshEtaRunnable);
        refreshHandler.postDelayed(refreshRunnable, 15000);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(refreshRunnable);
        refreshEtaHandler.removeCallbacksAndMessages(refreshEtaRunnable);
        disposables.clear();
    }

    @Override
    public void onDestroyView() {
        onPause();
        if (followAndHistoryAdapter != null) {
            followAndHistoryAdapter.close();
        }
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_follow, menu);
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
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        if (followAndHistoryAdapter != null && getContext() != null) {
            // Check internet connection
            if (ConnectivityUtil.isConnected(getContext())) {
                if (followAndHistoryAdapter.getFollowCount() > 0) {
                    for (int i = 0; i < followAndHistoryAdapter.getFollowCount(); i++) {
                        BusRouteStop object = BusRouteStopUtil.fromFollowStop(followAndHistoryAdapter.followItem(i));
                        Intent intent = new Intent(getContext(), EtaService.class);
                        intent.putExtra(C.EXTRA.ROW, i);
                        intent.putExtra(C.EXTRA.STOP_OBJECT, object);
                        try {
                            getContext().startService(intent);
                        } catch (IllegalStateException ignored) {}
                    }
                }
            } else {
                if (getActivity() != null) {
                    Snackbar.make(getActivity().findViewById(R.id.coordinator_layout),
                            R.string.message_no_internet_connection, Snackbar.LENGTH_LONG).show();
                }
            }
        }
        if (emptyView != null) {
            emptyView.setVisibility(followAndHistoryAdapter != null &&
                    followAndHistoryAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        }
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateAppShortcuts() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
        // Dynamic App Shortcut
        try {
            ShortcutManager shortcutManager = getContext().getSystemService(ShortcutManager.class);
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            for (int i = 0; i < shortcutManager.getMaxShortcutCountPerActivity()-1 && i < followAndHistoryAdapter.getItemCount(); i++) {
                Object object = followAndHistoryAdapter.getItem(i);
                if (object instanceof FollowStop) {
                    BusRouteStop busRouteStop = BusRouteStopUtil.fromFollowStop((FollowStop) object);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClass(getContext(), SearchActivity.class);
                    intent.putExtra(C.EXTRA.STOP_OBJECT_STRING, new Gson().toJson(busRouteStop));
                    shortcuts.add(new ShortcutInfo.Builder(getContext(), "buseta-" + busRouteStop.companyCode + busRouteStop.route + busRouteStop.direction + busRouteStop.code)
                            .setShortLabel(busRouteStop.route + " " + busRouteStop.name)
                            .setLongLabel(busRouteStop.route + " " + busRouteStop.name + " " + getString(R.string.destination, busRouteStop.destination))
                            .setIcon(Icon.createWithResource(getContext(), R.drawable.ic_shortcut_directions_bus))
                            .setIntent(intent)
                            .build());
                } else if (object instanceof SearchHistory) {
                    SearchHistory searchHistory = (SearchHistory) object;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClass(getContext(), SearchActivity.class);
                    intent.putExtra(C.EXTRA.ROUTE_NO, searchHistory.route);
                    intent.putExtra(C.EXTRA.COMPANY_CODE, searchHistory.companyCode);
                    shortcuts.add(new ShortcutInfo.Builder(getContext(), "buseta-q-" + searchHistory.companyCode + searchHistory.route)
                            .setShortLabel(searchHistory.route)
                            .setLongLabel(searchHistory.route)
                            .setIcon(Icon.createWithResource(getContext(), R.drawable.ic_shortcut_search))
                            .setIntent(intent)
                            .build());
                }
            }
            shortcutManager.setDynamicShortcuts(shortcuts);
        } catch (NoClassDefFoundError|NoSuchMethodError ignored) {}
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Integer row = bundle.getInt(C.EXTRA.ROW, -1);
                BusRouteStop busRouteStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (busRouteStop == null) return;
                if (followAndHistoryAdapter == null) return;
                //FollowStopUtil.query(getContext(), BusRouteStopUtil.toFollowStop(busRouteStop)).subscribe(cursor -> {
                //  FollowStop followStop = FollowStopUtil.fromCursor(cursor);
                //  followAndHistoryAdapter.notifyItemChanged(followStop.order);
                //});
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    Timber.d("eta updated: [%d] %s", row, busRouteStop.toString());
                    if (row >= 0 && row < followAndHistoryAdapter.getFollowCount()) {
                        followAndHistoryAdapter.notifyItemChanged(row);
                    } else {
                        // followAndHistoryAdapter.notifyItemRangeChanged(0, followAndHistoryAdapter.getFollowCount());
                    }
                }
                // if (bundle.getBoolean(C.EXTRA.UPDATING)) { }
                // if (bundle.getBoolean(C.EXTRA.FAIL)) { }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    DisposableObserver<Intent> followObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    if (followAndHistoryAdapter != null) {
                        followAndHistoryAdapter.updateFollow();
                        followAndHistoryAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
                if (emptyView != null) {
                    emptyView.setVisibility(followAndHistoryAdapter != null && followAndHistoryAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                }
                updateAppShortcuts();
            }
        };
    }

    DisposableObserver<Intent> historyObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    if (followAndHistoryAdapter != null) {
                        followAndHistoryAdapter.updateHistory();
                        followAndHistoryAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
                if (emptyView != null) {
                    emptyView.setVisibility(followAndHistoryAdapter != null && followAndHistoryAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                }
                updateAppShortcuts();
            }
        };
    }
}
