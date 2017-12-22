package com.alvinhkh.buseta.lwb.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.lwb.LwbService;
import com.alvinhkh.buseta.lwb.model.LwbRouteStop;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteStopListAdapter;
import com.alvinhkh.buseta.ui.route.RouteAnnounceActivity;
import com.alvinhkh.buseta.ui.route.RouteStopListFragmentAbstract;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;
import com.alvinhkh.buseta.utils.Utils;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


public class LwbStopListFragment extends RouteStopListFragmentAbstract {

    private final LwbService lwbService = LwbService.retrofit.create(LwbService.class);

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LwbStopListFragment newInstance(@NonNull BusRoute busRoute,
                                                  @Nullable BusRouteStop busRouteStop) {
        LwbStopListFragment fragment = new LwbStopListFragment();
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
        disposables.add(lwbService.getRouteMap(busRoute.getName(), busRoute.getSequence(), busRoute.getServiceType())
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeMapObserver()));
        return rootView;
    }

    DisposableObserver<List<LwbRouteStop>> routeMapObserver() {
        return new DisposableObserver<List<LwbRouteStop>>() {
            @Override
            public void onNext(List<LwbRouteStop> data) {
                if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                if (data != null && adapter != null) {
                    if (data.size() < 1) {
                        Timber.d("empty route map.");
                    }
                    List<Item> items = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        items.add(new Item(Item.TYPE_DATA, BusRouteStopUtil.fromLwb(data.get(i), busRoute, i, i >= data.size() - 1)));
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
