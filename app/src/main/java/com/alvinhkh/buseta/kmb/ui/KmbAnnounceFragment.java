package com.alvinhkh.buseta.kmb.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.KmbAnnounce;
import com.alvinhkh.buseta.kmb.model.network.KmbAnnounceRes;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class KmbAnnounceFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_ROUTE_NUMBER = "route_number";

    private static final String ARG_ROUTE_BOUND = "route_bound";

    private final KmbService kmbService = KmbService.webSearch.create(KmbService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private SwipeRefreshLayout swipeRefreshLayout;

    private KmbAnnounceAdapter adapter;

    private FloatingActionButton fab;

    private String routeBound;

    private String routeNo;

    private RecyclerView recyclerView;

    private View emptyView;

    public KmbAnnounceFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static KmbAnnounceFragment newInstance(BusRoute busRoute) {
        Timber.d(busRoute.toString());
        KmbAnnounceFragment fragment = new KmbAnnounceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROUTE_NUMBER, busRoute.getName());
        args.putString(ARG_ROUTE_BOUND, Integer.toString(Integer.parseInt(busRoute.getSequence()) + 1));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        routeNo = getArguments().getString(ARG_ROUTE_NUMBER);
        routeBound = getArguments().getString(ARG_ROUTE_BOUND);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new KmbAnnounceAdapter(recyclerView);
        recyclerView.setAdapter(adapter);
        emptyView = rootView.findViewById(R.id.empty_view);
        TextView emptyText = rootView.findViewById(R.id.empty_text);
        emptyText.setText(R.string.message_no_passenger_notice);
        if (adapter.getDataItemCount() > 0) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.INVISIBLE);
            emptyView.setVisibility(View.VISIBLE);
        }
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        fab = rootView.findViewById(R.id.fab);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fab != null) {
            fab.hide();
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(true);
            swipeRefreshLayout.setRefreshing(false);
        }
        onRefresh();
    }

    @Override
    public void onRefresh() {
        if (!TextUtils.isEmpty(routeNo) && !TextUtils.isEmpty(routeBound)) {
            loadRouteAnnounce(routeNo, routeBound);
            return;
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                onRefresh();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }

    private void loadRouteAnnounce(@NonNull String route, @NonNull String bound) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (adapter != null) {
            adapter.clear();
        }
        disposables.add(kmbService.getAnnounce(route, bound)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(getRouteAnnounceObserver()));
    }

    public DisposableObserver<KmbAnnounceRes> getRouteAnnounceObserver() {
        return new DisposableObserver<KmbAnnounceRes>() {
            @Override
            public void onNext(KmbAnnounceRes res) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                if (adapter != null) {
                    if (res != null && res.data != null) {
                        for (KmbAnnounce announce : res.data) {
                            adapter.add(new Item(Item.TYPE_DATA, announce));
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (adapter != null) {
                    adapter.setLoaded();
                    if (recyclerView != null) {
                        if (adapter.getDataItemCount() > 0) {
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.INVISIBLE);
                        }
                    }
                    if (emptyView != null) {
                        if (adapter.getDataItemCount() > 0) {
                            emptyView.setVisibility(View.GONE);
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        };
    }
}
