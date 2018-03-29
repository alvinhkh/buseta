package com.alvinhkh.buseta.kmb.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.KmbRouteStop;
import com.alvinhkh.buseta.kmb.model.network.KmbStopsRes;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.HKSCSUtil;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    public static KmbStopListFragment newInstance(@NonNull Route route,
                                                  @Nullable RouteStop routeStop) {
        KmbStopListFragment fragment = new KmbStopListFragment();
        Bundle args = new Bundle();
        args.putParcelable(C.EXTRA.ROUTE_OBJECT, route);
        args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
        disposables.add(kmbService.getStops(route.getName(), route.getSequence(), route.getServiceType())
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .doOnNext(res -> {
                    for (int i = 0; i < res.data.routeStops.size(); i++) {
                        KmbRouteStop kmbRouteStop = res.data.routeStops.get(i);
                        kmbRouteStop.nameTc = HKSCSUtil.convert(kmbRouteStop.nameTc);
                        kmbRouteStop.locationTc = HKSCSUtil.convert(kmbRouteStop.locationTc);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeStopsObserver()));
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    DisposableObserver<KmbStopsRes> routeStopsObserver() {
        return new DisposableObserver<KmbStopsRes>() {

            List<Item> items = new ArrayList<>();

            @Override
            public void onNext(KmbStopsRes res) {
                if (res != null && res.data != null && adapter != null) {
                    if (res.data.routeStops != null) {
                        for (int i = 0; i < res.data.routeStops.size(); i++) {
                            RouteStop stop = RouteStopUtil.fromKmbRouteStop(res.data.routeStops.get(i),
                                    route, i, i >= res.data.routeStops.size() - 1);
                            items.add(new Item(Item.TYPE_DATA, stop));
                        }
                    }
                    hasMapCoordinates = false;
                    mapCoordinates.clear();
                    if (!TextUtils.isEmpty(res.data.route.lineGeometry)) {
                        try {
                            hasMapCoordinates = true;
                            Gson gson = new Gson();
                            KmbStopsRes.Data.Route.LineGeometry lineGeometry = gson.fromJson(res.data.route.lineGeometry,
                                    KmbStopsRes.Data.Route.LineGeometry.class);
                            for (int i = 0; i < lineGeometry.paths.size(); i++) {
                                List<List<Double>> path = lineGeometry.paths.get(i);
                                for (int j = 0; j < path.size(); j++) {
                                    List<Double> p = path.get(j);
                                    mapCoordinates.add(RouteStopUtil.fromHK80toWGS84(new Pair<>(p.get(0), p.get(1))));
                                }
                            }
                        } catch (JsonParseException e) {
                            hasMapCoordinates = false;
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                getActivity().runOnUiThread(() -> onStopListError(e));
            }

            @Override
            public void onComplete() {
                getActivity().runOnUiThread(() -> {
                    if (adapter != null && items != null) {
                        adapter.addAll(items);
                    }
                    onStopListComplete();
                });
            }
        };
    }
}
