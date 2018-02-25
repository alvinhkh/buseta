package com.alvinhkh.buseta.nwst.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstLatLong;
import com.alvinhkh.buseta.nwst.model.NwstStop;
import com.alvinhkh.buseta.nwst.model.NwstVariant;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.observers.DisposableObserver;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;
import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


public class NwstStopListFragment extends RouteStopListFragmentAbstract {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NwstStopListFragment newInstance(@NonNull Route route,
                                                   @Nullable RouteStop routeStop) {
        NwstStopListFragment fragment = new NwstStopListFragment();
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
        String qInfo = NwstRequestUtil.paramInfo(route);
        if (!TextUtils.isEmpty(qInfo)) {
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
            Map<String, String> options = new LinkedHashMap<>();
            options.put(QUERY_INFO, qInfo);
            options.put(QUERY_LANGUAGE, LANGUAGE_TC);
            options.put(QUERY_PLATFORM, PLATFORM);
            options.put(QUERY_APP_VERSION, APP_VERSION);
            options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
            disposables.add(nwstService.stopList(options)
                    .retryWhen(new RetryWithDelay(5, 3000))
                    .subscribeWith(stopListObserver()));
        } else {
            onStopListError(new Error(getString(R.string.message_fail_to_request)));
        }
        return rootView;
    }

    DisposableObserver<ResponseBody> stopListObserver() {
        return new DisposableObserver<ResponseBody>() {

            List<Item> items = new ArrayList<>();

            @Override
            public void onNext(ResponseBody body) {
                try {
                    String[] routes = body.string().split("<br>", -1);
                    int i = route.getStopsStartSequence();
                    for (String route : routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstStop nwstStop = NwstStop.Companion.fromString(text);
                        if (nwstStop != null) {
                            RouteStop stop = RouteStopUtil.fromNwst(nwstStop, NwstStopListFragment.this.route);
                            stop.setSequence(Integer.toString(i));
                            items.add(new Item(TYPE_DATA, stop));
                            i++;
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
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
                });
                Map<String, String> options = new LinkedHashMap<>();
                String routeInfo = route.getInfoKey();
                NwstVariant variant = NwstVariant.Companion.parseInfo(routeInfo);
                if (variant == null) {
                    onStopListComplete();
                    return;
                }
                options.put(QUERY_R, variant.getRdv());
                options.put(QUERY_LANGUAGE, LANGUAGE_TC);
                options.put(QUERY_PLATFORM, PLATFORM);
                options.put(QUERY_APP_VERSION, APP_VERSION);
                options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
                disposables.add(nwstService.latlongList(options)
                        .retryWhen(new RetryWithDelay(5, 3000))
                        .subscribeWith(latlongListObserver()));
            }
        };
    }

    DisposableObserver<ResponseBody> latlongListObserver() {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                mapCoordinates.clear();
                try {
                    hasMapCoordinates = true;
                    String[] data = body.string().split("\\|\\*\\|");
                    for (String text: data) {
                        if (TextUtils.isEmpty(text)) continue;
                        NwstLatLong nwstLatLong = NwstLatLong.Companion.fromString(text);
                        if (nwstLatLong != null && nwstLatLong.getPath().size() > 0) {
                            for (kotlin.Pair<Double, Double> pair : nwstLatLong.getPath()) {
                                mapCoordinates.add(new Pair<>(pair.getFirst(), pair.getSecond()));
                            }
                        }
                    }
                } catch (IOException e) {
                    hasMapCoordinates = false;
                    Timber.d(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                getActivity().runOnUiThread(() -> onStopListError(e));
            }

            @Override
            public void onComplete() {
                getActivity().runOnUiThread(() -> onStopListComplete());
            }
        };
    }
}
