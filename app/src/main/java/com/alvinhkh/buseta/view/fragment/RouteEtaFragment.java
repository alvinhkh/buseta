package com.alvinhkh.buseta.view.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.EtaAdapterHelper;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.holder.RouteStopMap;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.alvinhkh.buseta.provider.EtaTable;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.FollowTable;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.RouteStopTable;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.view.ControllableAppBarLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jsoup.Jsoup;

import java.util.Date;

public class RouteEtaFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener,
        OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = RouteEtaFragment.class.getSimpleName();

    private Context mContext = super.getActivity();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private View mImageContainer;
    private ImageView mImageView;
    private TextView tSubtitle;
    private TextView tEta;
    private TextView lServerTime;
    private TextView tServerTime;
    private TextView lLastUpdated;
    private TextView tLastUpdated;
    private Bitmap mBitmap = null;
    private Menu mMenu;
    private MenuItem mRefresh;
    private MenuItem mFollow;

    private SettingsHelper settingsHelper;
    private GoogleMap mMap;
    private Marker mStopMarker = null;
    private RouteStop object;
    private int clickCount = 0;
    private boolean imageVisible = false;
    private UpdateEtaReceiver mReceiver;

    public RouteEtaFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_eta, container, false);
        mContext = super.getActivity();
        // get widgets
        if (null != view.getRootView())
            mImageView = (ImageView) view.getRootView().findViewById(R.id.imageView);
        tSubtitle = (TextView) view.findViewById(R.id.textView_subtitle);
        tEta = (TextView) view.findViewById(android.R.id.text1);
        lServerTime = (TextView) view.findViewById(R.id.label_serverTime);
        tServerTime = (TextView) view.findViewById(R.id.textView_serverTime);
        lLastUpdated = (TextView) view.findViewById(R.id.label_updated);
        tLastUpdated = (TextView) view.findViewById(R.id.textView_updated);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);
        // Get arguments
        settingsHelper = new SettingsHelper().parse(mContext.getApplicationContext());
        if (null != savedInstanceState) {
            object = savedInstanceState.getParcelable(Constants.BUNDLE.STOP_OBJECT);
            mBitmap = savedInstanceState.getParcelable("stop_image_bitmap");
            imageVisible = savedInstanceState.getBoolean("imageVisibility", false);
        } else {
            object = getArguments().getParcelable(Constants.BUNDLE.STOP_OBJECT);
            if (null == object)
                object = new RouteStop();
            if (null == object.route_bound)
                object.route_bound = new RouteBound();
            object = getObject(object);
            object.eta_loading = true;
            parse();
        }
        // Set Toolbar
        ActionBar mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(object.name_tc);
            mActionBar.setSubtitle(object.route_bound.route_no + " " +
                    getString(R.string.destination, object.route_bound.destination_tc));
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setHasOptionsMenu(true);
        if (mMap == null) {
            // Google Map
            ScrollMapFragment mMapFragment =
                    ((ScrollMapFragment) getFragmentManager().findFragmentById(R.id.map));
            mMap = mMapFragment.getMap();
            final NestedScrollView mScrollView = (NestedScrollView) view.findViewById(R.id.NestedScrollView);
            ((ScrollMapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .setListener(new ScrollMapFragment.OnTouchListener() {
                        @Override
                        public void onTouch() {
                            mScrollView.requestDisallowInterceptTouchEvent(true);
                        }
                    });
            mMapFragment.getMapAsync(this);
        }
        checkGooglePlayServices(getActivity());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.post(mAutoRefreshRunnable);
        if (null != mContext) {
            IntentFilter mFilter_eta = new IntentFilter(Constants.MESSAGE.ETA_UPDATED);
            mReceiver = new UpdateEtaReceiver();
            mFilter_eta.addAction(Constants.MESSAGE.ETA_UPDATED);
            mContext.registerReceiver(mReceiver, mFilter_eta);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != object)
            outState.putParcelable(Constants.BUNDLE.STOP_OBJECT, object);
        if (null != mImageContainer)
            outState.putBoolean("imageVisibility", mImageContainer.getVisibility() == View.VISIBLE);
        if (null != mImageView)
            mBitmap = Ion.with(mImageView).getBitmap();
        if (null != mBitmap)
            outState.putParcelable("stop_image_bitmap", mBitmap);
    }

    @Override
    public void onPause() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        if (null != mContext && null != mReceiver)
                mContext.unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        Ion.getDefault(mContext).cancelAll(mContext);
        if (null != mMap)
            mMap.clear();
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_route_eta, menu);
        mMenu = menu;
        mRefresh = menu.findItem(R.id.action_refresh);
        mFollow = menu.findItem(R.id.action_follow);
        Boolean loadImage = null != settingsHelper && settingsHelper.getLoadStopImage();
        menu.findItem(R.id.action_show_map).setVisible(loadImage);
        menu.findItem(R.id.action_show_photo).setVisible(!loadImage);
        getHeaderView(loadImage || imageVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            case R.id.action_refresh:
                onRefresh();
                if (clickCount == 0 || clickCount % 5 == 0) {
                    if (null != getView() && null != getView().getRootView()) {
                        final Snackbar snackbar = Snackbar.make(getView().getRootView().findViewById(android.R.id.content),
                                R.string.message_reminder_auto_refresh, Snackbar.LENGTH_LONG);
                        TextView tv = (TextView)
                                snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        snackbar.show();
                    }
                }
                clickCount++;
                break;
            case R.id.action_show_map:
            case R.id.action_show_photo:
                if (null != getView() && null != getView().getRootView()) {
                    ControllableAppBarLayout appBarLayout =
                            (ControllableAppBarLayout) getView().getRootView().findViewById(R.id.AppBar);
                    if (null != appBarLayout)
                        appBarLayout.expandToolbar(true);
                }
                getHeaderView(id == R.id.action_show_photo);
                break;
            case R.id.action_send:
                if (null == object.details) break;
                Uri uri = new Uri.Builder().scheme("geo").appendPath(object.details.lat +","+ object.details.lng)
                        .appendQueryParameter("q", object.name_tc).build();
                startActivity(new Intent(android.content.Intent.ACTION_VIEW, uri));
                break;
            case R.id.action_follow:
                // TODO: deal with situation where stop seq changed, a problem for followed stops
                if (null == object || null == object.route_bound) break;
                Boolean org;
                if (isFollowing(object)) {
                    // record exist
                    org = true;
                    int rowDeleted = mContext.getContentResolver().delete(
                            FollowProvider.CONTENT_URI_FOLLOW,
                            FollowTable.COLUMN_ROUTE + " = ?" +
                                    " AND " + FollowTable.COLUMN_BOUND + " = ?" +
                                    " AND " + FollowTable.COLUMN_STOP_CODE + " = ?",
                            new String[] {
                                    object.route_bound.route_no,
                                    object.route_bound.route_bound,
                                    object.code
                            });
                    object.follow = !(rowDeleted > 0);
                } else {
                    org = false;
                    ContentValues values = new ContentValues();
                    values.put(FollowTable.COLUMN_ROUTE, object.route_bound.route_no);
                    values.put(FollowTable.COLUMN_BOUND, object.route_bound.route_bound);
                    values.put(FollowTable.COLUMN_ORIGIN, object.route_bound.origin_tc);
                    values.put(FollowTable.COLUMN_DESTINATION, object.route_bound.destination_tc);
                    values.put(FollowTable.COLUMN_STOP_SEQ, object.stop_seq);
                    values.put(FollowTable.COLUMN_STOP_CODE, object.code);
                    values.put(FollowTable.COLUMN_STOP_NAME, object.name_tc);
                    values.put(FollowTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
                    Uri followUri = mContext.getContentResolver().insert(FollowProvider.CONTENT_URI_FOLLOW, values);
                    object.follow = (followUri != null);
                }
                item.setIcon(object.follow ?
                        R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp);
                LayoutInflater layoutInflater =
                        (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final ImageView iv = (ImageView) layoutInflater.inflate(R.layout.action_view_image, null);
                iv.setImageResource(object.follow ?
                        R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp);
                if (org != object.follow) {
                    Animation rotate = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_once);
                    iv.startAnimation(rotate);
                    if (null != getView() && null != getView().getRootView()) {
                        final Snackbar snackbar = Snackbar.make(
                                getView().getRootView().findViewById(android.R.id.content),
                                object.follow ? R.string.message_follow : R.string.message_unfollow,
                                Snackbar.LENGTH_SHORT);
                        TextView tv = (TextView)
                                snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        snackbar.show();
                    }
                }
                mFollow.setActionView(iv);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        mFollow.setActionView(null);
                    }
                }, 500);
                Intent intent = new Intent(Constants.MESSAGE.FOLLOW_UPDATED);
                intent.putExtra(Constants.MESSAGE.FOLLOW_UPDATED, true);
                intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
                mContext.sendBroadcast(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if (null == object || null == object.details)
            return;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setTrafficEnabled(false);
        map.setBuildingsEnabled(false);
        map.setIndoorEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setScrollGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(false);
        map.setOnInfoWindowClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerClickListener(this);
        // mark
        mStopMarker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_bus_black_24dp))
                .position(getStopLanLng())
                .title(object.name_tc)
                .snippet(object.route_bound.route_no + " " +
                        getString(R.string.destination, object.route_bound.destination_tc)));
        resetMap(map);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.getPosition().equals(getStopLanLng())) {
            getHeaderView(true);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (null == mMap) return;
        resetMap(mMap);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    private void resetMap(final GoogleMap map) {
        if (null == map) return;
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(getStopLanLng())
                        .zoom(16)
                        .build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        map.moveCamera(CameraUpdateFactory.scrollBy(0, -128));
        if (null != mStopMarker)
            mStopMarker.showInfoWindow();
    }

    private LatLng getStopLanLng() {
        if (null == object || null == object.details) return null;
        Float lat = Float.valueOf(object.details.lat);
        Float lng = Float.valueOf(object.details.lng);
        return new LatLng(lat, lng);
    }

    public void onRefresh() {
        if (null != mRefresh)
            mRefresh.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(true);
        Intent intent = new Intent(mContext, CheckEtaService.class);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        mContext.startService(intent);
    }

    private Boolean isFollowing(RouteStop object) {
        final Cursor c = mContext.getContentResolver().query(FollowProvider.CONTENT_URI_FOLLOW,
                null,
                FollowTable.COLUMN_ROUTE + " =?" +
                        " AND " + FollowTable.COLUMN_BOUND + " =?" +
                        " AND " + FollowTable.COLUMN_STOP_CODE + " =?",
                new String[]{
                        object.route_bound.route_no,
                        object.route_bound.route_bound,
                        object.code
                },
                FollowTable.COLUMN_DATE + " DESC");
        Boolean isFollowing = false;
        if (null != c) {
            c.moveToFirst();
            isFollowing = c.getCount() > 0;
            c.close();
        }
        return isFollowing;
    }

    private RouteStop getObject(RouteStop object) {
        final Cursor c = mContext.getContentResolver().query(
                RouteProvider.CONTENT_URI_STOP,
                null,
                RouteStopTable.COLUMN_ROUTE + " =?" +
                        " AND " + RouteStopTable.COLUMN_BOUND + " =?" +
                        " AND " + RouteStopTable.COLUMN_STOP_CODE + " =?",
                new String[]{
                        object.route_bound.route_no,
                        object.route_bound.route_bound,
                        object.code
                }, RouteStopTable.COLUMN_STOP_SEQ + "* 1 ASC");
        RouteStop routeStop = new RouteStop();
        if (null != c) {
            c.moveToFirst();
            if (c.getCount() > 0) {
                routeStop.route_bound = object.route_bound;
                routeStop.stop_seq = getColumnString(c, RouteStopTable.COLUMN_STOP_SEQ);
                routeStop.name_tc = getColumnString(c, RouteStopTable.COLUMN_STOP_NAME);
                routeStop.name_en = getColumnString(c, RouteStopTable.COLUMN_STOP_NAME_EN);
                routeStop.code = getColumnString(c, RouteStopTable.COLUMN_STOP_CODE);
                RouteStopMap routeStopMap = new RouteStopMap();
                routeStopMap.air_cond_fare = getColumnString(c, RouteStopTable.COLUMN_STOP_FARE);
                routeStopMap.lat = getColumnString(c, RouteStopTable.COLUMN_STOP_LAT);
                routeStopMap.lng = getColumnString(c, RouteStopTable.COLUMN_STOP_LONG);
                routeStop.details = routeStopMap;
            }
            c.close();
        }
        routeStop.follow = isFollowing(object);
        Cursor cEta = mContext.getContentResolver().query(FollowProvider.CONTENT_URI_ETA_JOIN,
                null,
                FollowTable.COLUMN_ROUTE + " =?" +
                        " AND " + FollowTable.COLUMN_BOUND + " =?" +
                        " AND " + FollowTable.COLUMN_STOP_CODE + " =?",
                new String[]{
                        object.route_bound.route_no,
                        object.route_bound.route_bound,
                        object.code
                },
                FollowTable.COLUMN_DATE + " DESC");
        if (null != c) {
            cEta.moveToFirst();
            if (cEta.getCount() > 0) {
                RouteStopETA routeStopETA = null;
                String apiVersion = getColumnString(cEta, EtaTable.COLUMN_ETA_API);
                if (null != apiVersion && !apiVersion.equals("")) {
                    routeStopETA = new RouteStopETA();
                    routeStopETA.api_version = Integer.valueOf(apiVersion);
                    routeStopETA.seq = getColumnString(cEta, EtaTable.COLUMN_STOP_SEQ);
                    routeStopETA.etas = getColumnString(cEta, EtaTable.COLUMN_ETA_TIME);
                    routeStopETA.expires = getColumnString(cEta, EtaTable.COLUMN_ETA_EXPIRE);
                    routeStopETA.server_time = getColumnString(cEta, EtaTable.COLUMN_SERVER_TIME);
                    routeStopETA.updated = getColumnString(cEta, EtaTable.COLUMN_UPDATED);
                }
                routeStop.eta = routeStopETA;
                routeStop.eta_loading = getColumnString(cEta, EtaTable.COLUMN_LOADING).equals("true");
                routeStop.eta_fail = getColumnString(cEta, EtaTable.COLUMN_FAIL).equals("true");
            }
            cEta.close();
        }
        return routeStop;
    }

    private String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    private void parse() {
        if (null == object || null == object.route_bound) return;
        tEta.setVisibility(View.VISIBLE);
        lServerTime.setVisibility(View.VISIBLE);
        tServerTime.setVisibility(View.VISIBLE);
        lLastUpdated.setVisibility(View.VISIBLE);
        tLastUpdated.setVisibility(View.VISIBLE);
        tSubtitle.setText(object.name_tc + "\n" + object.route_bound.route_no + " " +
                getString(R.string.destination, object.route_bound.destination_tc));
        if (null != mFollow)
            mFollow.setIcon(object.follow ?
                    R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp);
        if (object.eta_loading != null && object.eta_loading) {
            mSwipeRefreshLayout.setRefreshing(true);
            if (null == object.eta || object.eta.etas.equals("") || tEta.getText().equals(""))
                tEta.setText(getString(R.string.message_loading) + "\n\n\n");
        } else if (object.eta_fail != null && object.eta_fail) {
            tEta.setText(R.string.message_fail_to_request);
            mSwipeRefreshLayout.setRefreshing(false);
        } else if (null == object.eta || object.eta.etas.equals("")) {
            tEta.setText(R.string.message_no_data);
            mSwipeRefreshLayout.setRefreshing(false);
        }
        if (null == object.eta || object.eta.etas.equals("")) {
            lServerTime.setVisibility(View.GONE);
            tServerTime.setVisibility(View.GONE);
            lLastUpdated.setVisibility(View.GONE);
            tLastUpdated.setVisibility(View.GONE);
            return;
        }
        // Request Time
        String server_time = "";
        Date server_date = null;
        if (null != object.eta.server_time && !object.eta.server_time.equals("")) {
            server_date = EtaAdapterHelper.serverDate(object);
            server_time = (null != server_date) ?
                    EtaAdapterHelper.display_format.format(server_date) : object.eta.server_time;
        }
        // last updated
        String updated_time = "";
        Date updated_date;
        if (null != object.eta.updated && !object.eta.updated.equals("")) {
            updated_date = EtaAdapterHelper.updatedDate(object);
            updated_time = (null != updated_date) ?
                    EtaAdapterHelper.display_format.format(updated_date) : object.eta.updated;
        }
        // ETAs
        String eta = Jsoup.parse(object.eta.etas).text();
        String[] etas = eta.replaceAll("　", " ").split(", ?");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < etas.length; i++) {
            sb.append(etas[i]);
            String estimate = EtaAdapterHelper.etaEstimate(object, etas, i, server_date, null, null, null);
            sb.append(estimate);
            if (i < etas.length - 1)
                sb.append("\n");
        }
        tEta.setText(sb.toString());
        if (null != server_time && !server_time.equals("")) {
            tServerTime.setText(server_time);
        }
        if (null != updated_time && !updated_time.equals("")) {
            lLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setText(updated_time);
        } else {
            lLastUpdated.setVisibility(View.GONE);
            tLastUpdated.setVisibility(View.GONE);
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void getHeaderView(Boolean showPhoto) {
        if (null != getView() && null != getView().getRootView() && null != mContext) {
            final View root = getView().getRootView();
            final View mapContainer = root.findViewById(R.id.mapContainer);
            mImageContainer = root.findViewById(R.id.imageContainer);
            mImageView = (ImageView) root.findViewById(R.id.imageView);
            Animation fadeIn = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
            if (showPhoto) {
                if (null != mMenu) {
                    mMenu.findItem(R.id.action_show_map).setVisible(true);
                    mMenu.findItem(R.id.action_show_photo).setVisible(false);
                }
                mapContainer.setVisibility(View.GONE);
                mImageContainer.setVisibility(View.VISIBLE);
                if (null != mBitmap) {
                    mImageView.setImageBitmap(mBitmap);
                    mImageView.setAnimation(fadeIn);
                } else {
                    if (null != object)
                    Ion.with(mContext)
                            .load(Constants.URL.ROUTE_STOP_IMAGE + object.code)
                            .withBitmap()
                            .error(R.drawable.ic_error_outline_black_48dp)
                            .resize(340, 255)
                            .centerCrop()
                            .animateLoad(R.anim.fade_in)
                            .intoImageView(mImageView)
                            .setCallback(new FutureCallback<ImageView>() {
                                @Override
                                public void onCompleted(Exception e, ImageView result) {
                                    mBitmap = Ion.with(mImageView).getBitmap();
                                }
                            });
                }
            } else {
                if (null != mMenu) {
                    mMenu.findItem(R.id.action_show_map).setVisible(false);
                    mMenu.findItem(R.id.action_show_photo).setVisible(true);
                }
                mapContainer.setVisibility(View.VISIBLE);
                mapContainer.setAnimation(fadeIn);
                mImageContainer.setVisibility(View.GONE);
            }
        }
    }

    public static boolean checkGooglePlayServices(final Activity activity) {
        final int available = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        switch (available) {
            case ConnectionResult.SUCCESS:
                return true;
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_MISSING:
                if (GooglePlayServicesUtil.isUserRecoverableError(available)) {
                    Toast.makeText(activity,
                            GooglePlayServicesUtil.getErrorString(available), Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Log.d(TAG, GooglePlayServicesUtil.getErrorString(available));
                }
                break;
        }
        return false;
    }

    public class UpdateEtaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.ETA_UPDATED);
            if (aBoolean) {
                RouteStop routeStop = bundle.getParcelable(Constants.BUNDLE.STOP_OBJECT);
                if (null != routeStop && null != routeStop.route_bound
                        && null != object && null != object.route_bound) {
                    if (object.route_bound.route_no.equals(routeStop.route_bound.route_no) &&
                            object.route_bound.route_bound.equals(routeStop.route_bound.route_bound) &&
                            object.stop_seq.equals(routeStop.stop_seq) &&
                            object.code.equals(routeStop.code)){
                        object.eta = routeStop.eta;
                        object.eta_loading = routeStop.eta_loading;
                        object.eta_fail = routeStop.eta_fail;
                        parse();
                    }
                }
                if (null != mSwipeRefreshLayout)
                    mSwipeRefreshLayout.setRefreshing(false);
                if (null != mRefresh)
                    mRefresh.setEnabled(true);
            }
        }
    }

    Handler mAutoRefreshHandler = new Handler();
    Runnable mAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            onRefresh();
            mAutoRefreshHandler.postDelayed(mAutoRefreshRunnable, 30 * 1000); // every half minute
        }
    };

}
