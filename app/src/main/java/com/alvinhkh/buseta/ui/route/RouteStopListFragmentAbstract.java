package com.alvinhkh.buseta.ui.route;

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
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
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
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


// TODO: better way to find nearest stop
// TODO: keep (nearest) stop on top
public abstract class RouteStopListFragmentAbstract extends Fragment implements
        ArrayListRecyclerViewAdapter.OnClickItemListener,
        SwipeRefreshLayout.OnRefreshListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        AppBarLayout.OnOffsetChangedListener {

    protected final CompositeDisposable disposables = new CompositeDisposable();

    protected RouteStopListAdapter adapter;

    protected SwipeRefreshLayout swipeRefreshLayout;

    protected RecyclerView recyclerView;

    protected View emptyView;

    protected ProgressBar progressBar;

    protected TextView emptyText;

    protected BusRoute busRoute;

    protected BusRouteStop navToStop = null;

    protected GoogleMap map;

    protected List<Pair<Double, Double>> mapCoordinates = new ArrayList<>();

    protected Boolean hasMapCoordinates = false;

    private List<BusRouteStop> busRouteStops = new ArrayList<>();

    private Boolean isShowMapFragment = true;

    public RouteStopListFragmentAbstract() {}

    protected final Handler refreshHandler = new Handler();

    protected final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) {
                if (getContext() != null) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    if (preferences != null && preferences.getBoolean("load_etas", false)) {
                        onRefresh();
                        refreshHandler.postDelayed(this, 60000);  // refresh every 60 sec
                        return;
                    }
                }
                adapter.notifyDataSetChanged();
            }
            refreshHandler.postDelayed(this, 30000);  // refresh every 30 sec
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            busRoute = getArguments().getParcelable(C.EXTRA.ROUTE_OBJECT);
        }
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.ETA_UPDATE))
                .share().subscribeWith(etaObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.FOLLOW_UPDATE))
                .share().subscribeWith(followObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.LOCATION_UPDATE))
                .share().subscribeWith(locationObserver()));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        emptyView = rootView.findViewById(R.id.empty_view);
        emptyView.setVisibility(View.VISIBLE);
        progressBar = rootView.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        emptyText = rootView.findViewById(R.id.empty_text);
        emptyText.setText(R.string.message_loading);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setVisibility(View.GONE);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new RouteStopListAdapter(getFragmentManager(), recyclerView, busRoute);
        adapter.setOnClickItemListener(this);
        if (!TextUtils.isEmpty(busRoute.getDescription())) {
            adapter.add(new Item(Item.TYPE_HEADER, busRoute.getDescription()));
        }
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

        if (getArguments() != null) {
            navToStop = getArguments().getParcelable(C.EXTRA.STOP_OBJECT);
        }
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isShowMapFragment = preferences != null && preferences.getBoolean("load_map", true);
        showMapFragment();

        return rootView;
    }

    private void showMapFragment() {
        if (getContext() == null) return;
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            if (isShowMapFragment) {
                ft.show(mapFragment);
            } else {
                ft.hide(mapFragment);
            }
            ft.commitAllowingStateLoss();
        }

        if (getActivity() != null &&
                getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
            if (appBar != null) {
                if (isShowMapFragment) {
                    appBar.addOnOffsetChangedListener(this);
                } else {
                    appBar.removeOnOffsetChangedListener(this);
                }
            }
        }

        if (getView() != null) {
            Guideline guideTopInfo = getView().findViewById(R.id.guideline);
            if (guideTopInfo != null) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                params.guidePercent = isShowMapFragment ? .4f : .0f;
                guideTopInfo.setLayoutParams(params);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getView() != null) {
            AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
            if (appBar != null) {
                Guideline guideTopInfo = getView().findViewById(R.id.guideline);
                if (guideTopInfo != null) {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                    params.guidePercent = isShowMapFragment ? .45f : .0f;
                    guideTopInfo.setLayoutParams(params);
                }
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isUserVisible) {
        super.setUserVisibleHint(isUserVisible);
        if (getView() != null) {
            if (isUserVisible) {
                refreshHandler.post(refreshRunnable);
                if (getActivity() != null) {
                    FloatingActionButton fab = getActivity().findViewById(R.id.fab);
                    if (fab != null) {
                        fab.setOnClickListener(v -> onRefresh());
                    }
                }
            } else {
                refreshHandler.removeCallbacksAndMessages(null);
            }
        }
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        if (getContext() != null) {
            PreferenceManager.getDefaultSharedPreferences(getContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_route, menu);
        MenuItem showMapMenuItem = menu.findItem(R.id.action_show_map);
        if (getContext() != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            showMapMenuItem.setVisible(preferences == null || !preferences.getBoolean("load_map", true));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_notice:
                if (busRoute != null) {
                    Intent intent = new Intent(getContext(), RouteAnnounceActivity.class);
                    intent.putExtra(C.EXTRA.ROUTE_OBJECT, busRoute);
                    startActivity(intent);
                    return true;
                }
                break;
            case R.id.action_show_map:
                isShowMapFragment = !isShowMapFragment;
                showMapFragment();
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
        } else if (key.equals("load_map")) {
            isShowMapFragment = sharedPreferences != null && sharedPreferences.getBoolean("load_map", true);
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
            showMapFragment();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
    }

    @Override
    public void onRefresh() {
        if (getContext() != null && adapter != null && adapter.getDataItemCount() > 0) {
            if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(true);
            }
            ArrayList<BusRouteStop> busRouteStopList = new ArrayList<>();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getItem(i).getType() == TYPE_DATA &&
                        adapter.getItem(i).getObject() instanceof BusRouteStop) {
                    busRouteStopList.add((BusRouteStop) adapter.getItem(i).getObject());
                }
            }
            try {
                Intent intent = new Intent(getContext(), EtaService.class);
                intent.putParcelableArrayListExtra(C.EXTRA.STOP_LIST, busRouteStopList);
                getContext().startService(intent);
            } catch (IllegalStateException ignored) {}
        }
    }

    @Override
    public void onClickItem(Item item, int position) {
        if (item.getType() == Item.TYPE_DATA) {
            BusRouteStop stop = (BusRouteStop) item.getObject();
            if (map != null && stop != null && isShowMapFragment &&
                    !TextUtils.isEmpty(stop.latitude) && !TextUtils.isEmpty(stop.longitude)) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude)), 18));
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
        if (map == null || getContext() == null) return;
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.map_style));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.3964, 114.1095), 10));
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapToolbarEnabled(false);
        options.compassEnabled(true);
        options.rotateGesturesEnabled(true);
        options.scrollGesturesEnabled(false);
        options.tiltGesturesEnabled(true);
        options.zoomControlsEnabled(false);
        options.zoomGesturesEnabled(true);
        googleMap.setBuildingsEnabled(false);
        googleMap.setIndoorEnabled(false);
        googleMap.setTrafficEnabled(false);
        googleMap.setOnMarkerClickListener(this);
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        if (busRouteStops.size() > 0) {
            BusRouteStop stop = busRouteStops.get(0);
            if (!TextUtils.isEmpty(stop.latitude) && !TextUtils.isEmpty(stop.longitude)) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(Double.parseDouble(stop.latitude),
                                Double.parseDouble(stop.longitude)), 16));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() instanceof BusRouteStop) {
            BusRouteStop stop = (BusRouteStop) marker.getTag();
            if (stop != null && getContext() != null) {
                if (recyclerView != null) {
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                        @Override protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }
                    };
                    int i = 0;
                    for (Item item: adapter.getDataItems()) {
                        if (((BusRouteStop) item.getObject()).sequence.equals(stop.sequence)) {
                            smoothScroller.setTargetPosition(i);
                        }
                        i++;
                    }
                    recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                }
                if (map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude)), 16));
                }
                try {
                    Intent intent = new Intent(getContext(), EtaService.class);
                    intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
                    getContext().startService(intent);
                } catch (IllegalStateException ignored) {}
            }
        }
        return true;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // TODO: CoordinatorLayout.Behavior?
        if (getView() == null || recyclerView == null || recyclerView.getVisibility() != View.VISIBLE) return;
        Guideline guideline = getView().findViewById(R.id.guideline);
        if (guideline == null) return;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();
        if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
            params.guidePercent = .2f;
        } else if (verticalOffset == 0) {
            params.guidePercent = .45f;
        } else {
            float p = 0.2f + (1.0f - (Math.abs(verticalOffset * 1.0f) / appBarLayout.getTotalScrollRange() * 1.0f)) * 0.25f;
            params.guidePercent = params.guidePercent * .9f + p *.1f;
        }
        guideline.setLayoutParams(params);
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                BusRouteStop busRouteStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (busRouteStop == null) return;
                if (!busRouteStop.route.equals(busRoute.getName())) return;
                if (!busRouteStop.direction.equals(busRoute.getSequence())) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED) || bundle.getBoolean(C.EXTRA.FAIL)) {
                    if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    int i = 0;
                    for (Item item : adapter.getItems()) {
                        if (item.getType() == TYPE_DATA &&
                                ((BusRouteStop) item.getObject()).sequence.equals(busRouteStop.sequence)) {
                            adapter.notifyItemChanged(i);
                            break;
                        }
                        i++;
                    }
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

    protected void onStopListError(Throwable e) {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            emptyText.setText(e.getMessage());
        }
    }

    protected void onStopListComplete() {
        Boolean isScrollToPosition = false;
        Integer scrollToPosition = 0;
        busRouteStops.clear();
        if (map != null && adapter != null) {
            Boolean isSnapToRoad = false;
            if (getActivity() != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                isSnapToRoad = preferences != null && preferences.getBoolean("map_direction_api", false);

                FloatingActionButton fab = getActivity().findViewById(R.id.fab);
                if (fab != null) {
                    if (preferences == null || !preferences.getBoolean("load_etas", false)) {
                        fab.show();
                    }
                }
            }
            if (hasMapCoordinates) {
                isSnapToRoad = false;
            }

            // note: there is an api free limit
            GeoApiContext geoApiContext = null;
            if (isSnapToRoad) {
                geoApiContext = new GeoApiContext.Builder()
                        .apiKey(getString(R.string.DIRECTION_API_KEY))
                        .build();
            }
            map.clear();
            if (!hasMapCoordinates) {
                mapCoordinates.clear();
            }
            if (adapter.getItemCount() > 0) {
                for (int i = 0, j = 0; i < adapter.getItemCount(); i++) {
                    Item item = adapter.getItem(i);
                    if (item.getType() != Item.TYPE_DATA) continue;
                    BusRouteStop stop = (BusRouteStop) item.getObject();
                    busRouteStops.add(stop);
                    if (
                            navToStop != null &&
                                    stop.companyCode.equals(navToStop.companyCode) &&
                                    stop.route.equals(navToStop.route) &&
                                    stop.name.equals(navToStop.name) &&
                                    stop.direction.equals(navToStop.direction) &&
                                    stop.sequence.equals(navToStop.sequence)
                            ) {
                        scrollToPosition = j;
                        isScrollToPosition = true;
                    }
                    if (!TextUtils.isEmpty(stop.latitude) && !TextUtils.isEmpty(stop.longitude)) {
                        if (!hasMapCoordinates) {
                            mapCoordinates.add(new Pair<>(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude)));
                        }
                        IconGenerator iconFactory = new IconGenerator(getContext());
                        Bitmap bmp = iconFactory.makeIcon(stop.sequence + ": " + stop.name);
                        map.addMarker(new MarkerOptions()
                                .position(new LatLng(Double.parseDouble(stop.latitude), Double.parseDouble(stop.longitude)))
                                .icon(BitmapDescriptorFactory.fromBitmap(bmp))).setTag(stop);
                    }
                    j++;
                }
                PolylineOptions singleLine = new PolylineOptions().width(20).zIndex(1)
                        .color(ContextCompat.getColor(getContext(), R.color.grey))
                        .startCap(new RoundCap()).endCap(new RoundCap());
                for (Pair<Double, Double> pair: mapCoordinates) {
                    if (pair.first == null || pair.second == null) continue;
                    singleLine.add(new LatLng(pair.first, pair.second));
                }
                Boolean hasError = false;
                if (isSnapToRoad) {
                    for (int i = 0; i < mapCoordinates.size(); i++) {
                        if (hasError) break;
                        List<LatLng> path = new ArrayList<>();
                        Pair<Double, Double> pair = mapCoordinates.get(i);
                        if (i + 1 < mapCoordinates.size() && mapCoordinates.get(i + 1) != null) {
                            Pair<Double, Double> nextPair = mapCoordinates.get(i + 1);
                            // https://stackoverflow.com/a/47556917/2411672
                            DirectionsApiRequest req = DirectionsApi.getDirections(geoApiContext,
                                    pair.first + "," + pair.second, nextPair.first + "," + nextPair.second);
                            try {
                                DirectionsResult res = req.await();
                                // loop through legs and steps to get encoded polylines of each step
                                if (res.routes != null && res.routes.length > 0) {
                                    DirectionsRoute route = res.routes[0];
                                    if (route.legs != null) {
                                        for (int j = 0; j < route.legs.length; j++) {
                                            DirectionsLeg leg = route.legs[j];
                                            if (leg.steps != null) {
                                                for (int k = 0; k < leg.steps.length; k++) {
                                                    DirectionsStep step = leg.steps[k];
                                                    if (step.steps != null && step.steps.length > 0) {
                                                        for (int l = 0; l < step.steps.length; l++) {
                                                            DirectionsStep step1 = step.steps[l];
                                                            EncodedPolyline points1 = step1.polyline;
                                                            if (points1 != null) {
                                                                // decode polyline and add points to list of route coordinates
                                                                List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                                                for (com.google.maps.model.LatLng coord1 : coords1) {
                                                                    path.add(new LatLng(coord1.lat, coord1.lng));
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        EncodedPolyline points = step.polyline;
                                                        if (points != null) {
                                                            // decode polyline and add points to list of route coordinates
                                                            List<com.google.maps.model.LatLng> coords = points.decodePath();
                                                            for (com.google.maps.model.LatLng coord : coords) {
                                                                path.add(new LatLng(coord.lat, coord.lng));
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (InterruptedException|IOException e) {
                                Timber.d(e);
                                hasError = true;
                            } catch (ApiException e) {
                                hasError = true;
                            }
                        }

                        if (!hasError && path.size() > 0) {
                            PolylineOptions line = new PolylineOptions().width(20).zIndex(1)
                                    .color(ContextCompat.getColor(getContext(), R.color.grey))
                                    .startCap(new RoundCap()).endCap(new RoundCap());
                            map.addPolyline(line.addAll(path));
                        }
                    }
                }
                if (hasMapCoordinates || !isSnapToRoad || hasError) {
                    map.addPolyline(singleLine);
                }
                if (busRouteStops.size() > 0 && scrollToPosition < busRouteStops.size()) {
                    BusRouteStop stop = busRouteStops.get(scrollToPosition);
                    if (!TextUtils.isEmpty(stop.latitude) && !TextUtils.isEmpty(stop.longitude)) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(Double.parseDouble(stop.latitude),
                                        Double.parseDouble(stop.longitude)), 16));
                    }
                }
            }
        }
        refreshHandler.post(refreshRunnable);
        if (adapter != null) {
            if (adapter.getItemCount() > 0) {
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (isScrollToPosition) {
                        recyclerView.scrollToPosition(scrollToPosition);
                    }
                }
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        }
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
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
                    int i = 0;
                    for (Item item : adapter.getItems()) {
                        if (item.getType() == TYPE_DATA &&
                                ((BusRouteStop) item.getObject()).sequence.equals(followStop.sequence)) {
                            adapter.notifyItemChanged(i);
                            break;
                        }
                        i++;
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
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
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onComplete() {
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };
    }
}
