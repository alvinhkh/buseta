package com.alvinhkh.buseta.mtr.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.mtr.dao.AESBusDatabase;
import com.alvinhkh.buseta.mtr.model.AESBusRoute;
import com.alvinhkh.buseta.mtr.model.AESBusStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.DatabaseUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;


public class AESBusStopListFragment extends RouteStopListFragmentAbstract {

    public AESBusStopListFragment() {}

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AESBusStopListFragment newInstance(@NonNull BusRoute busRoute,
                                                     @Nullable BusRouteStop busRouteStop) {
        AESBusStopListFragment fragment = new AESBusStopListFragment();
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
        if (busRoute == null || TextUtils.isEmpty(busRoute.getName())) {
            Toast.makeText(getContext(), R.string.missing_input, Toast.LENGTH_SHORT).show();
        } else {
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
            if (getContext() != null) {
                AESBusDatabase database = DatabaseUtil.Companion.getAESBusDatabase(getContext());
                disposables.add(database.aesBusDao().getAllRoutes()
                        .subscribe(aesBusRoutes -> {
                            LinkedList<String> stopIds = new LinkedList<>();
                            if (aesBusRoutes != null) {
                                for (AESBusRoute aesBusRoute: aesBusRoutes) {
                                    if (!aesBusRoute.getBusNumber().equals(busRoute.getName())) continue;
                                    stopIds.addAll(Arrays.asList(aesBusRoute.getRoute().split("\\+")));
                                }
                            }
                            disposables.add(database.aesBusDao().getAllStops()
                                    .subscribe(aesBusStops -> {
                                        List<Item> items = new ArrayList<>();
                                        if (aesBusStops != null && adapter != null) {
                                            Map<String, BusRouteStop> map = new HashMap<>();
                                            for (AESBusStop aesBusStop: aesBusStops) {
                                                if (TextUtils.isEmpty(aesBusStop.getBusNumber()) || !aesBusStop.getBusNumber().equals(busRoute.getName())) continue;
                                                if (!stopIds.contains(aesBusStop.getStopId())) continue;
                                                BusRouteStop stop = BusRouteStopUtil.fromAESBus(aesBusStop, busRoute);
                                                if (stop == null) continue;
                                                map.put(aesBusStop.getStopId(), stop);
                                            }
                                            List<BusRouteStop> stops = new ArrayList<>();
                                            for (String stopId: stopIds) {
                                                if (map.get(stopId) == null) continue;
                                                stops.add(map.get(stopId));
                                            }
                                            int i = 0;
                                            for (BusRouteStop stop: stops) {
                                                if (stop == null) continue;
                                                stop.sequence = Integer.toString(i);
                                                items.add(new Item(Item.TYPE_DATA, stop));
                                                i++;
                                            }
                                        }
                                        getActivity().runOnUiThread(() -> {
                                            if (adapter != null) {
                                                adapter.addAll(items);
                                            }
                                            onStopListComplete();
                                        });
                                    }, e -> {
                                        Timber.d(e);
                                        getActivity().runOnUiThread(() -> onStopListError(e));
                                    }));
                        }, e -> {
                            Timber.d(e);
                            getActivity().runOnUiThread(() -> onStopListError(e));
                        }));
            }
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem noticeMenuItem = menu.findItem(R.id.action_notice);
        noticeMenuItem.setVisible(false);
    }
}
