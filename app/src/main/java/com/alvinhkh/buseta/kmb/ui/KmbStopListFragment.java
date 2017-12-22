package com.alvinhkh.buseta.kmb.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.network.KmbStopsRes;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class KmbStopListFragment extends RouteStopListFragmentAbstract {

    private final KmbService kmbService = KmbService.webSearch.create(KmbService.class);

    public KmbStopListFragment() {}

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static KmbStopListFragment newInstance(@NonNull BusRoute busRoute,
                                                  @Nullable BusRouteStop busRouteStop) {
        KmbStopListFragment fragment = new KmbStopListFragment();
        Bundle args = new Bundle();
        args.putParcelable(C.EXTRA.ROUTE_OBJECT, busRoute);
        args.putParcelable(C.EXTRA.STOP_OBJECT, busRouteStop);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        disposables.add(kmbService.getStops(busRoute.getName(), busRoute.getSequence(), busRoute.getServiceType())
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeStopsObserver()));
        return rootView;
    }

    DisposableObserver<KmbStopsRes> routeStopsObserver() {
        return new DisposableObserver<KmbStopsRes>() {
            @Override
            public void onNext(KmbStopsRes res) {
                if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                if (res != null && res.data != null && adapter != null) {
                    if (res.data.routeStops != null) {
                        List<Item> items = new ArrayList<>();
                        int i = busRoute.getStopsStartSequence();
                        for (int j = 0; j < res.data.routeStops.size(); j++) {
                            BusRouteStop stop = BusRouteStopUtil.fromKmbRouteStop(res.data.routeStops.get(j),
                                    busRoute, j, j >= res.data.routeStops.size() - 1);
                            items.add(new Item(Item.TYPE_DATA, stop));
                        }
                        adapter.addAll(items);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                onStopListError(e);
            }

            @Override
            public void onComplete() {
                onStopListComplete();
            }
        };
    }
}
