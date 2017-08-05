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
import android.widget.Toast;

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
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
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


// TODO: better way to find nearest stop
// TODO: keep (nearest) stop on top
public class LwbStopListFragment extends Fragment implements
        ArrayListRecyclerViewAdapter.OnClickItemListener,
        SwipeRefreshLayout.OnRefreshListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String ARG_ROUTE_NUMBER = "route_number";

    private static final String ARG_ROUTE_BOUND = "route_bound";

    private static final String ARG_ROUTE_SERVICE_TYPE = "route_service_type";

    private static final String ARG_ROUTE_LOCATION_START = "route_location_start";

    private static final String ARG_ROUTE_LOCATION_END = "route_location_end";

    private final LwbService lwbService = LwbService.retrofit.create(LwbService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private RouteStopListAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;

    private RecyclerView recyclerView;

    private BusRoute busRoute;

    private Integer goToStopPos;

    private List<BusRouteStop> busRouteStops;

    private GoogleMap map;

    public LwbStopListFragment() {}

    private final Handler refreshHandler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyDataSetChanged();
            }
            refreshHandler.postDelayed(this, 30000);  // refresh eta every half minute
        }
    };

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LwbStopListFragment newInstance(@NonNull BusRoute busRoute,
                                                  @Nullable BusRouteStop busRouteStop) {
        LwbStopListFragment fragment = new LwbStopListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROUTE_NUMBER, busRoute.getName());
        args.putString(ARG_ROUTE_BOUND, busRoute.getSequence());
        args.putString(ARG_ROUTE_SERVICE_TYPE, busRoute.getServiceType());
        args.putString(ARG_ROUTE_LOCATION_START, busRoute.getLocationStartName());
        args.putString(ARG_ROUTE_LOCATION_END, busRoute.getLocationEndName());
        args.putParcelable(C.EXTRA.STOP_OBJECT, busRouteStop);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            busRoute = new BusRoute();
            busRoute.setName(getArguments().getString(ARG_ROUTE_NUMBER));
            busRoute.setCompanyCode(BusRoute.COMPANY_KMB);
            busRoute.setSequence(getArguments().getString(ARG_ROUTE_BOUND));
            busRoute.setServiceType(getArguments().getString(ARG_ROUTE_SERVICE_TYPE));
            busRoute.setLocationStartName(getArguments().getString(ARG_ROUTE_LOCATION_START));
            busRoute.setLocationEndName(getArguments().getString(ARG_ROUTE_LOCATION_END));
        }

        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(this);
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.ETA_UPDATE))
                .share().subscribeWith(etaObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.FOLLOW_UPDATE))
                .share().subscribeWith(followObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.LOCATION_UPDATE))
                .share().subscribeWith(locationObserver()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new RouteStopListAdapter(getFragmentManager(), recyclerView, busRoute);
        adapter.setOnClickItemListener(this);
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(getContext())
                    .getLastLocation()
                    .addOnSuccessListener(location -> adapter.setCurrentLocation(location));
        }
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(this);

        goToStopPos = 0;
        if (getArguments() != null) {
            BusRouteStop routeStop = getArguments().getParcelable(C.EXTRA.STOP_OBJECT);
            if (routeStop != null) {
                goToStopPos = Integer.parseInt(routeStop.sequence);
            }
        }
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
            if (appBar != null) {
                Guideline guideTopInfo = rootView.findViewById(R.id.guideline);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                    if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                        params.guidePercent = .2f;
                    } else if (verticalOffset == 0) {
                        params.guidePercent = .45f;
                    } else {
                        float guidePercent;
                        guidePercent = 0.2f + (1.0f - (Math.abs(verticalOffset * 1.0f) / appBarLayout.getTotalScrollRange() * 1.0f)) * 0.25f;
                        params.guidePercent = params.guidePercent * .9f + guidePercent *.1f;
                    }
                    guideTopInfo.setLayoutParams(params);
                });
            }
        }

        disposables.add(lwbService.getRouteMap(busRoute.getName(), busRoute.getSequence(), busRoute.getServiceType())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeMapObserver(goToStopPos)));
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint()) {
            if (getView() != null) {
                swipeRefreshLayout = getView().findViewById(R.id.swipe_refresh_layout);
            }
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this);
        }
        AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
        if (appBar != null) {
            Guideline guideTopInfo = getView().findViewById(R.id.guideline);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
            params.guidePercent = .45f;
            guideTopInfo.setLayoutParams(params);
        }
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_route, menu);
        MenuItem noticeMenuItem = menu.findItem(R.id.action_notice);
        noticeMenuItem.setEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_notice:
                if (busRoute != null) {
                    Intent intent = new Intent(getContext(), RouteAnnounceActivity.class);
                    intent.putExtra(RouteAnnounceActivity.ROUTE_COMPANY, busRoute.getCompanyCode());
                    intent.putExtra(RouteAnnounceActivity.ROUTE_NO, busRoute.getName());
                    intent.putExtra(RouteAnnounceActivity.ROUTE_SEQ, busRoute.getSequence());
                    startActivity(intent);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("load_wheelchair_icon") || key.equals("load_wifi_icon")) {
            // to reflect changes when toggle display icon
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onRefresh() {
        Context context = getContext();
        if (context != null) {
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
            ArrayList<BusRouteStop> busRouteStopList = new ArrayList<>();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getItem(i).getObject() instanceof BusRouteStop) {
                    busRouteStopList.add((BusRouteStop) adapter.getItem(i).getObject());
                }
            }
            try {
                Intent intent = new Intent(context, EtaService.class);
                intent.putParcelableArrayListExtra(C.EXTRA.STOP_LIST, busRouteStopList);
                context.startService(intent);
            } catch (IllegalStateException ignored) {}
        }
    }

    @Override
    public void onClickItem(Item item) {
        if (item.getType() == Item.TYPE_DATA) {
            if (map != null) {
                BusRouteStop stop = (BusRouteStop) item.getObject();
                if (stop != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude)), 18));
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (map == null) return;
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.3964, 114.1095), 10));
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapToolbarEnabled(false);
        options.compassEnabled(true);
        options.rotateGesturesEnabled(true);
        options.scrollGesturesEnabled(false);
        options.tiltGesturesEnabled(true);
        options.zoomControlsEnabled(false);
        options.zoomGesturesEnabled(true);
        map.setBuildingsEnabled(false);
        map.setIndoorEnabled(false);
        map.setTrafficEnabled(false);
        map.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        if (busRouteStops != null && busRouteStops.size() > 0) {
            PolylineOptions line = new PolylineOptions().width(20).zIndex(1)
                    .color(ContextCompat.getColor(getContext(), R.color.colorAccent));
            for (BusRouteStop stop: busRouteStops) {
                LatLng latLng = new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude));
                line.add(latLng);
                IconGenerator iconFactory = new IconGenerator(getContext());
                Bitmap bmp = iconFactory.makeIcon(stop.sequence + ": " + stop.name);
                map.addMarker(new MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.fromBitmap(bmp))).setTag(stop);
            }
            line.startCap(new RoundCap());
            line.endCap(new RoundCap());
            map.addPolyline(line);
            if (busRouteStops.size() < goToStopPos) {
                goToStopPos = 0;
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(Double.parseDouble(busRouteStops.get(goToStopPos).latitude),
                            Double.parseDouble(busRouteStops.get(goToStopPos).longitude)), 16));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() instanceof BusRouteStop) {
            BusRouteStop stop = (BusRouteStop) marker.getTag();
            if (stop != null) {
                if (recyclerView != null) {
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                        @Override protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }
                    };
                    smoothScroller.setTargetPosition(Integer.parseInt(stop.sequence));
                    recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude)), 16));
                Intent intent = new Intent(getContext(), EtaService.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
                getContext().startService(intent);
            }
        }
        return true;
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                BusRouteStop busRouteStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (busRouteStop == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    adapter.notifyItemChanged(Integer.parseInt(busRouteStop.sequence));
                }
                if (Integer.parseInt(busRouteStop.sequence) >= adapter.getDataItemCount() - 1) {
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
            }
        };
    }

    DisposableObserver<Intent> followObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                FollowStop followStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (followStop == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    adapter.notifyItemChanged(Integer.parseInt(followStop.sequence));
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {}
        };
    }

    DisposableObserver<Intent> locationObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Location location = bundle.getParcelable(C.EXTRA.LOCATION_OBJECT);
                if (location == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    if (adapter != null) {
                        adapter.setCurrentLocation(location);
                    }
                }
                if (bundle.getBoolean(C.EXTRA.FAIL)) {
                    if (adapter != null) {
                        adapter.setCurrentLocation(null);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {}
        };
    }

    DisposableObserver<List<LwbRouteStop>> routeMapObserver(Integer scrollToPosition) {
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
                        items.add(new Item(Item.TYPE_DATA, BusRouteStopUtil.fromLwb(data.get(i), busRoute, i)));
                    }
                    adapter.addAll(items);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                if (recyclerView != null) {
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                        @Override protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }
                    };
                    smoothScroller.setTargetPosition(scrollToPosition);
                    recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                }
                if (adapter != null) {
                    adapter.setLoaded();
                    if (adapter.getItemCount() < 1) {
                        Toast.makeText(getContext(), R.string.message_fail_to_request, Toast.LENGTH_SHORT).show();
                    } else {
                        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
                        if (fab != null) {
                            fab.show();
                        }
                    }
                }
                busRouteStops = new ArrayList<>();
                if (map != null) {
                    map.clear();
                }
                PolylineOptions line = new PolylineOptions().width(20).zIndex(1)
                        .color(ContextCompat.getColor(getContext(), R.color.colorAccent));
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    Item item = adapter.getItem(i);
                    if (item.getType() != Item.TYPE_DATA) continue;
                    BusRouteStop stop = (BusRouteStop) item.getObject();
                    busRouteStops.add(stop);

                    if (map != null) {
                        LatLng latLng = new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude));
                        line.add(latLng);
                        IconGenerator iconFactory = new IconGenerator(getContext());
                        Bitmap bmp = iconFactory.makeIcon(stop.sequence + ": " + stop.name);
                        map.addMarker(new MarkerOptions().position(latLng)
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp))).setTag(stop);
                    }
                }
                if (map != null && busRouteStops.size() > 0 && scrollToPosition < busRouteStops.size()) {
                    line.startCap(new RoundCap());
                    line.endCap(new RoundCap());
                    map.addPolyline(line);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Double.parseDouble(busRouteStops.get(scrollToPosition).latitude),
                                    Double.parseDouble(busRouteStops.get(scrollToPosition).longitude)), 16));
                }
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };
    }
}
