package com.alvinhkh.buseta.datagovhk.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.datagovhk.DataGovHkService;
import com.alvinhkh.buseta.datagovhk.model.MtrBusFare;
import com.alvinhkh.buseta.datagovhk.model.MtrBusStop;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;


public class MtrBusStopListFragment extends RouteStopListFragmentAbstract {

    private final DataGovHkService dataGovHkService = DataGovHkService.resource.create(DataGovHkService.class);

    public MtrBusStopListFragment() {}

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MtrBusStopListFragment newInstance(@NonNull Route route,
                                                     @Nullable RouteStop routeStop) {
        MtrBusStopListFragment fragment = new MtrBusStopListFragment();
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
        disposables.add(dataGovHkService.mtrBusFares()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(mtrBusFareObserver()));
        return rootView;
    }

    DisposableObserver<ResponseBody> mtrBusFareObserver() {
        return new DisposableObserver<ResponseBody>() {

            MtrBusFare mtrBusFare = null;

            @Override
            public void onNext(ResponseBody body) {
                if (body != null && adapter != null) {
                    try {
                        List<MtrBusFare> fares = MtrBusFare.Companion.fromCSV(body.string());
                        for (MtrBusFare fare: fares) {
                            if (!route.getName().equals(fare.getRouteId())) continue;
                            mtrBusFare = fare;
                            break;
                        }
                    } catch (IOException e) {
                        Timber.d(e);
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
                disposables.add(dataGovHkService.mtrBusStops()
                        .retryWhen(new RetryWithDelay(5, 3000))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(mtrBusStopsObserver(mtrBusFare)));
            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem noticeMenuItem = menu.findItem(R.id.action_notice);
        noticeMenuItem.setVisible(false);
    }

    DisposableObserver<ResponseBody> mtrBusStopsObserver(MtrBusFare mtrBusFare) {
        return new DisposableObserver<ResponseBody>() {

            List<Item> items = new ArrayList<>();

            @Override
            public void onNext(ResponseBody body) {
                if (body != null && adapter != null) {
                    try {
                        List<MtrBusStop> mtrBusStops = MtrBusStop.Companion.fromCSV(body.string());
                        List<RouteStop> stops = new ArrayList<>();
                        for (MtrBusStop mtrBusStop: mtrBusStops) {
                            if (!route.getName().equals(mtrBusStop.getRouteId())) continue;
                            stops.add(RouteStopUtil.fromMtrBus(mtrBusStop, mtrBusFare, route));
                        }
                        int i = 0;
                        for (RouteStop stop: stops) {
                            stop.setSequence(Integer.toString(i));
                            items.add(new Item(Item.TYPE_DATA, stop));
                            i++;
                        }
                    } catch (IOException e) {
                        Timber.d(e);
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
