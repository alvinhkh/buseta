package com.alvinhkh.buseta.nwst.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
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
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstLatLong;
import com.alvinhkh.buseta.nwst.model.NwstStop;
import com.alvinhkh.buseta.nwst.model.NwstVariant;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.ui.webview.WebViewActivity;
import com.alvinhkh.buseta.utils.RouteStopUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;
import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


public class NwstStopListFragment extends RouteStopListFragmentAbstract {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    private String timetableHtml = "";

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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        String qInfo = NwstRequestUtil.paramInfo(route);
        if (!TextUtils.isEmpty(qInfo)) {
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
            disposables.add(nwstService.stopList(qInfo, LANGUAGE_TC, NwstRequestUtil.syscode(),
                    PLATFORM, APP_VERSION, NwstRequestUtil.syscode2(),
                    preferences.getString("nwst_tk", ""),
                    preferences.getString("nwst_syscode3", ""))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(stopListObserver()));
        } else {
            onStopListError(new Error(getString(R.string.message_fail_to_request)));
        }
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
                if (!TextUtils.isEmpty(timetableHtml)) {
                    Intent intent = new Intent(getContext(), WebViewActivity.class);
                    intent.putExtra(WebViewActivity.TITLE, route == null || TextUtils.isEmpty(route.getName()) ?
                            getString(R.string.timetable) : route.getName() + " " + getString(R.string.timetable));
                    intent.putExtra(WebViewActivity.HTML, timetableHtml);
                    startActivity(intent);
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    DisposableObserver<ResponseBody> stopListObserver() {
        return new DisposableObserver<ResponseBody>() {

            List<Item> items = new ArrayList<>();

            @Override
            public void onNext(ResponseBody body) {
                if (route == null || route.getStopsStartSequence() == null) return;
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
                });
                String routeInfo = route.getInfoKey();
                if (TextUtils.isEmpty(routeInfo)) {
                    onStopListComplete();
                    return;
                }
                NwstVariant variant = NwstVariant.Companion.parseInfo(routeInfo);
                if (variant == null) {
                    onStopListComplete();
                    return;
                }
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                disposables.add(nwstService.latlongList(variant.getRdv(), LANGUAGE_TC,
                        NwstRequestUtil.syscode(), PLATFORM, APP_VERSION, NwstRequestUtil.syscode2(),
                        preferences.getString("nwst_tk", ""), preferences.getString("nwst_syscode3", ""))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(latlongListObserver()));

                String rdv = route.getInfoKey();
                if (!TextUtils.isEmpty(rdv)) {
                    String[] temp = rdv.substring(1).split("\\*{3}");
                    if (temp.length >= 4) {
                        rdv = temp[0] + "||" + temp[1] + "||" + temp[2] + "||" + temp[3];
                    }
                }
                disposables.add(nwstService.timetable(rdv, route.getSequence(), LANGUAGE_TC,
                        NwstRequestUtil.syscode(), PLATFORM, APP_VERSION, NwstRequestUtil.syscode2(),
                        preferences.getString("nwst_tk", ""), preferences.getString("nwst_syscode3", ""))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(timetableObserver()));
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
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> onStopListError(e));
            }

            @Override
            public void onComplete() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> onStopListComplete());
            }
        };
    }

    DisposableObserver<ResponseBody> timetableObserver() {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                timetableHtml = "";
                try {
                    timetableHtml = body.string();
                } catch (IOException e) {
                    Timber.d(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                timetableHtml = "";
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }
}
