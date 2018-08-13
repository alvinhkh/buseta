package com.alvinhkh.buseta.nlb.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nlb.model.NlbRouteStop;
import com.alvinhkh.buseta.nlb.model.NlbStop;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.image.ImageActivity;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.RouteStopUtil;
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

import static com.alvinhkh.buseta.nlb.NlbService.TIMETABLE_URL;


public class NlbStopListFragment extends RouteStopListFragmentAbstract {

    private final NlbService nlbService = NlbService.api.create(NlbService.class);

    public NlbStopListFragment() {}

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NlbStopListFragment newInstance(@NonNull Route route,
                                                  @Nullable RouteStop routeStop) {
        NlbStopListFragment fragment = new NlbStopListFragment();
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
        disposables.add(nlbService.getDatabase()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(databaseObserver()));
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem timetableItem = menu.findItem(R.id.action_timetable);
        timetableItem.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_timetable:
                if (route != null && getContext() != null) {
                    Uri link = Uri.parse(TIMETABLE_URL + route.getCode());
                    if (link != null) {
                        try {
                            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                            builder.setToolbarColor(ContextCompat.getColor(getContext(), R.color.black));
                            CustomTabsIntent customTabsIntent = builder.build();
                            customTabsIntent.launchUrl(getContext(), link);
                        } catch (Exception ignored) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, link);
                            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }
                    }
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    DisposableObserver<NlbDatabase> databaseObserver() {
        return new DisposableObserver<NlbDatabase>() {

            List<Item> items = new ArrayList<>();

            @Override
            public void onNext(NlbDatabase database) {
                if (database != null && adapter != null) {
                    Map<String, NlbRouteStop> map = new HashMap<>();
                    for (NlbRouteStop routeStop: database.route_stops) {
                        if (!routeStop.route_id.equals(route.getSequence())) continue;
                        map.put(routeStop.stop_id, routeStop);
                    }
                    Map<Integer, RouteStop> map2 = new TreeMap<>();
                    for (NlbStop stop: database.stops) {
                        if (!map.containsKey(stop.stop_id)) continue;
                        map2.put(Integer.parseInt(map.get(stop.stop_id).stop_sequence),
                                RouteStopUtil.fromNlb(map.get(stop.stop_id), stop, route));
                    }
                    int i = 0;
                    for (RouteStop stop: map2.values()) {
                        stop.setSequence(Integer.toString(i));
                        items.add(new Item(Item.TYPE_DATA, stop));
                        i++;
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> onStopListError(e));
            }

            @Override
            public void onComplete() {
                if (getActivity() == null) return;
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
