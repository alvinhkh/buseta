package com.alvinhkh.buseta.lwb.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.lwb.LwbService;
import com.alvinhkh.buseta.lwb.model.LwbRouteStop;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class LwbStopListFragment extends RouteStopListFragmentAbstract {

    private final LwbService lwbService = LwbService.retrofit.create(LwbService.class);

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LwbStopListFragment newInstance(@NonNull Route route,
                                                  @Nullable RouteStop routeStop) {
        LwbStopListFragment fragment = new LwbStopListFragment();
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
        disposables.add(lwbService.getRouteMap(route.getName(), route.getSequence(), route.getServiceType())
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeMapObserver()));
        return rootView;
    }

    DisposableObserver<List<LwbRouteStop>> routeMapObserver() {
        return new DisposableObserver<List<LwbRouteStop>>() {

            List<Item> items = new ArrayList<>();

            @Override
            public void onNext(List<LwbRouteStop> data) {
                if (data != null && adapter != null) {
                    if (data.size() < 1) {
                        Timber.d("empty route map.");
                    }
                    for (int i = 0; i < data.size(); i++) {
                        items.add(new Item(Item.TYPE_DATA, RouteStopUtil.fromLwb(data.get(i), route, i, i >= data.size() - 1)));
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
