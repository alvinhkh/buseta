package com.alvinhkh.buseta.nwst.ui;

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
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstStop;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;
import com.alvinhkh.buseta.ui.route.RouteAnnounceActivity;
import com.alvinhkh.buseta.ui.route.RouteStopListAdapter;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.APP_VERSION;
import static com.alvinhkh.buseta.nwst.NwstService.LANGUAGE_TC;
import static com.alvinhkh.buseta.nwst.NwstService.PLATFORM;
import static com.alvinhkh.buseta.nwst.NwstService.QUERY_APP_VERSION;
import static com.alvinhkh.buseta.nwst.NwstService.QUERY_INFO;
import static com.alvinhkh.buseta.nwst.NwstService.QUERY_LANGUAGE;
import static com.alvinhkh.buseta.nwst.NwstService.QUERY_PLATFORM;
import static com.alvinhkh.buseta.nwst.NwstService.QUERY_SYSCODE;
import static com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item.TYPE_DATA;


public class NwstStopListFragment extends Fragment implements
        ArrayListRecyclerViewAdapter.OnClickItemListener,
        SwipeRefreshLayout.OnRefreshListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private RouteStopListAdapter adapter;

    private SwipeRefreshLayout swipeRefreshLayout;

    private RecyclerView recyclerView;

    private BusRoute busRoute;

    private Integer goToStopSequence;

    private List<BusRouteStop> busRouteStops;

    private GoogleMap map;

    public NwstStopListFragment() {}

    private final Handler refreshHandler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (getContext() != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (preferences != null && preferences.getBoolean("load_etas", false)) {
                    onRefresh();
                    refreshHandler.postDelayed(this, 30000);  // refresh every 30 sec
                    return;
                }
            }
            if (adapter != null && adapter.getItemCount() > 0) {
                adapter.notifyDataSetChanged();
            }
            refreshHandler.postDelayed(this, 30000);  // refresh every 30 sec
        }
    };

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static NwstStopListFragment newInstance(@NonNull BusRoute busRoute,
                                                   @Nullable BusRouteStop busRouteStop) {
        NwstStopListFragment fragment = new NwstStopListFragment();
        Bundle args = new Bundle();
        args.putParcelable(C.EXTRA.ROUTE_OBJECT, busRoute);
        args.putParcelable(C.EXTRA.STOP_OBJECT, busRouteStop);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            busRoute = getArguments().getParcelable(C.EXTRA.ROUTE_OBJECT);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        adapter = new RouteStopListAdapter(getFragmentManager(), recyclerView, busRoute);
        adapter.setOnClickItemListener(this);
        if (!TextUtils.isEmpty(busRoute.getDescription()) && !busRoute.getDescription().equals("正常路線")) {
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

        goToStopSequence = busRoute.getStopsStartSequence();
        if (getArguments() != null) {
            BusRouteStop routeStop = getArguments().getParcelable(C.EXTRA.STOP_OBJECT);
            if (routeStop != null) {
                goToStopSequence = Integer.parseInt(routeStop.sequence);
            }
        }
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE
                && getActivity() != null) {
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
        Map<String, String> options = new LinkedHashMap<>();
        options.put(QUERY_INFO, NwstRequestUtil.paramInfo(busRoute));
        options.put(QUERY_LANGUAGE, LANGUAGE_TC);
        options.put(QUERY_PLATFORM, PLATFORM);
        options.put(QUERY_APP_VERSION, APP_VERSION);
        options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
        disposables.add(nwstService.stopList(options)
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(stopListObserver(goToStopSequence)));
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
        if (getActivity() != null) {
            AppBarLayout appBar = getActivity().findViewById(R.id.appbar);
            if (appBar != null) {
                Guideline guideTopInfo = getView().findViewById(R.id.guideline);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideTopInfo.getLayoutParams();
                params.guidePercent = .45f;
                guideTopInfo.setLayoutParams(params);
            }
        }
        refreshHandler.postDelayed(refreshRunnable, 100);
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
        if (item.getType() == TYPE_DATA) {
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
        if (map == null || getContext() == null) return;
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
            if (busRouteStops.size() + busRoute.getStopsStartSequence() < goToStopSequence) {
                goToStopSequence = busRoute.getStopsStartSequence();
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(Double.parseDouble(busRouteStops.get(goToStopSequence).latitude),
                            Double.parseDouble(busRouteStops.get(goToStopSequence).longitude)), 16));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (getContext() == null) return false;
        if (marker.getTag() instanceof BusRouteStop) {
            BusRouteStop stop = (BusRouteStop) marker.getTag();
            if (stop != null) {
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

    DisposableObserver<ResponseBody> stopListObserver(Integer scrollToStopSequence) {
        return new DisposableObserver<ResponseBody>() {

            int scrollToPosition = 0;

            Boolean isScrollToPosition = false;

            @Override
            public void onNext(ResponseBody body) {
                if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(true);
                }
                try {
                    List<Item> items = new ArrayList<>();
                    String[] routes = body.string().split("<br>", -1);
                    int i = busRoute.getStopsStartSequence();
                    for (String route : routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstStop nwstStop = NwstStop.Companion.fromString(text);
                        if (nwstStop != null) {
                            BusRouteStop stop = BusRouteStopUtil.fromNwst(nwstStop, busRoute);
                            stop.sequence = Integer.toString(i);
                            items.add(new Item(TYPE_DATA, stop));
                            if (busRoute != null && busRoute.getSequence().equals(stop.direction) &&
                                    i == scrollToStopSequence) {
                                scrollToPosition = i - busRoute.getStopsStartSequence();
                                isScrollToPosition = true;
                            }
                            i++;
                        }
                    }
                    adapter.addAll(items);
                } catch (IOException e) {
                    Timber.d(e);
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
                if (getContext() == null) return;
                if (recyclerView != null && isScrollToPosition) {
                    recyclerView.getLayoutManager().scrollToPosition(scrollToPosition);
                }
                if (adapter != null) {
                    adapter.setLoaded();
                    if (adapter.getItemCount() < 1) {
                        Toast.makeText(getContext(), R.string.message_fail_to_request, Toast.LENGTH_SHORT).show();
                    } else if (getActivity() != null) {
                        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
                        if (fab != null) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            if (preferences == null || !preferences.getBoolean("load_etas", false)) {
                                fab.show();
                            }
                        }
                    }
                }
                busRouteStops = new ArrayList<>();
                if (map != null) {
                    map.clear();
                }
                if (adapter.getItemCount() > 0) {
                    PolylineOptions line = new PolylineOptions().width(20).zIndex(1)
                            .color(ContextCompat.getColor(getContext(), R.color.colorAccent));
                    for (int j = 0; j < adapter.getItemCount(); j++) {
                        Item item = adapter.getItem(j);
                        if (item.getType() != TYPE_DATA) continue;
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

                }
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                refreshHandler.post(refreshRunnable);
            }
        };
    }
}
