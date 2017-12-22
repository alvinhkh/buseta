package com.alvinhkh.buseta.nlb.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nlb.model.NlbRouteStop;
import com.alvinhkh.buseta.nlb.model.NlbStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class NlbStopListFragment extends RouteStopListFragmentAbstract {

    private final NlbService nlbService = NlbService.api.create(NlbService.class);

    public NlbStopListFragment() {}

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NlbStopListFragment newInstance(@NonNull BusRoute busRoute,
                                                  @Nullable BusRouteStop busRouteStop) {
        NlbStopListFragment fragment = new NlbStopListFragment();
        Bundle args = new Bundle();
        args.putParcelable(C.EXTRA.ROUTE_OBJECT, busRoute);
        args.putParcelable(C.EXTRA.STOP_OBJECT, busRouteStop);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        disposables.add(nlbService.getDatabase()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(databaseObserver()));
        return rootView;
    }

    DisposableObserver<NlbDatabase> databaseObserver() {
        return new DisposableObserver<NlbDatabase>() {
            @Override
            public void onNext(NlbDatabase database) {
                if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                if (database != null && adapter != null) {
                    Map<String, NlbRouteStop> map = new HashMap<>();
                    for (NlbRouteStop routeStop: database.route_stops) {
                        if (!routeStop.route_id.equals(busRoute.getSequence())) continue;
                        map.put(routeStop.stop_id, routeStop);
                    }
                    Map<Integer, BusRouteStop> map2 = new TreeMap<>();
                    for (NlbStop stop: database.stops) {
                        if (!map.containsKey(stop.stop_id)) continue;
                        map2.put(Integer.parseInt(map.get(stop.stop_id).stop_sequence),
                                BusRouteStopUtil.fromNlb(map.get(stop.stop_id), stop, busRoute));
                    }
                    List<Item> items = new ArrayList<>();
                    int i = 0;
                    for (BusRouteStop stop: map2.values()) {
                        stop.sequence = Integer.toString(i);
                        items.add(new Item(Item.TYPE_DATA, stop));
                        i++;
                    }
                    adapter.addAll(items);
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
