package com.alvinhkh.buseta.view.dialog;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.holder.RouteStopMap;
import com.alvinhkh.buseta.provider.EtaTable;
import com.alvinhkh.buseta.provider.FavouriteProvider;
import com.alvinhkh.buseta.provider.FavouriteTable;
import com.alvinhkh.buseta.holder.EtaAdapterHelper;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.preference.SettingsHelper;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.RouteStopTable;
import com.alvinhkh.buseta.service.CheckEtaService;
import com.alvinhkh.buseta.view.fragment.ScrollMapFragment;
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


public class RouteEtaDialog extends AppCompatActivity
        implements View.OnClickListener, OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = RouteEtaDialog.class.getSimpleName();

    private Context mContext;
    private ImageView iStop;
    private ImageView iStar;
    private ImageView iRefresh;
    private ProgressBar progressBar;
    private TextView tStopName;
    private TextView tEta;
    private TextView lServerTime;
    private TextView tServerTime;
    private TextView lLastUpdated;
    private TextView tLastUpdated;
    private Animation animationRotate;
    private Bitmap mBitmap = null;
    private GoogleMap mMap;
    private View mapContainer;
    private View imageContainer;

    private RouteStop object = null;
    private Boolean hideStar = false;
    private int clickCount = 0;
    private Boolean mapLoaded = false;
    private int mapVisibility = View.VISIBLE;
    private int imageVisibility = View.GONE;

    private UpdateEtaReceiver mReceiver;

    Handler mAutoRefreshHandler = new Handler();
    Runnable mAutoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            onRefresh();
            mAutoRefreshHandler.postDelayed(mAutoRefreshRunnable, 30 * 1000); // every half minute
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.dialog_eta);
        // side dialog width and height
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // prevent cancel when touch outside
        setFinishOnTouchOutside(true);
        // get context
        mContext = RouteEtaDialog.this;
        setTaskDescription(getString(R.string.launcher_name));
        // get widgets
        iStop = (ImageView) findViewById(R.id.imageView);
        iStar = (ImageView) findViewById(R.id.star);
        iRefresh = (ImageView) findViewById(R.id.refresh);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tStopName = (TextView) findViewById(R.id.stop_name);
        tEta = (TextView) findViewById(android.R.id.text1);
        lServerTime = (TextView) findViewById(R.id.label_serverTime);
        tServerTime = (TextView) findViewById(R.id.textView_serverTime);
        lLastUpdated = (TextView) findViewById(R.id.label_updated);
        tLastUpdated = (TextView) findViewById(R.id.textView_updated);
        animationRotate = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_once);
        animationRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mapContainer = findViewById(R.id.mapContainer);
        imageContainer = findViewById(R.id.imageContainer);
        //
        tStopName.setOnClickListener(this);
        iStar.setOnClickListener(this);
        iRefresh.setOnClickListener(this);
        imageContainer.setOnClickListener(this);
        // check from the saved Instance
        Bundle extras = getIntent().getExtras();
        // Or passed from the other activity
        if (extras != null) {
            hideStar = extras.getBoolean(Constants.MESSAGE.HIDE_STAR);
            object = extras.getParcelable(Constants.BUNDLE.STOP_OBJECT);
            // overview task
            setTaskDescription(null == object || null == object.route_bound ? getString(R.string.launcher_name) :
                    object.route_bound.route_no + getString(R.string.interpunct) + getString(R.string.launcher_name));
            parse();
        } else {
            finish();
        }
        //
        object = getObject(object);
        iStar.setImageResource(object.favourite ?
                R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
        // Google Map
        if (mMap == null) {
            ScrollMapFragment mMapFragment =
                    ((ScrollMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            mMap = mMapFragment.getMap();
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            final ScrollView mScrollView = (ScrollView) findViewById(R.id.scrollView);
            ((ScrollMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .setListener(new ScrollMapFragment.OnTouchListener() {
                        @Override
                        public void onTouch() {
                            mScrollView.requestDisallowInterceptTouchEvent(true);
                        }
                    });
            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                onRefresh();
                if (clickCount == 0 || clickCount % 5 == 0) {
                    final Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                            R.string.message_reminder_auto_refresh, Snackbar.LENGTH_LONG);
                    TextView tv = (TextView)
                            snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    snackbar.show();
                }
                clickCount++;
                break;
            case R.id.star:
                if (null == object || null == object.route_bound) break;
                Boolean org;
                if (isFavourite(object)) {
                    // record exist
                    org = true;
                    int rowDeleted = mContext.getContentResolver().delete(
                            FavouriteProvider.CONTENT_URI_FAV,
                            FavouriteTable.COLUMN_ROUTE + " = ?" +
                                    " AND " + FavouriteTable.COLUMN_BOUND + " = ?" +
                                    " AND " + FavouriteTable.COLUMN_STOP_CODE + " = ?",
                            new String[] {
                                    object.route_bound.route_no,
                                    object.route_bound.route_bound,
                                    object.code
                            });
                    mContext.getContentResolver().delete(
                            FavouriteProvider.CONTENT_URI_ETA,
                            EtaTable.COLUMN_ROUTE + " = ?" +
                                    " AND " + EtaTable.COLUMN_BOUND + " = ?" +
                                    " AND " + EtaTable.COLUMN_STOP_CODE + " = ?",
                            new String[] {
                                    object.route_bound.route_no,
                                    object.route_bound.route_bound,
                                    object.code
                            });
                    object.favourite = !(rowDeleted > 0);
                } else {
                    org = false;
                    ContentValues values = new ContentValues();
                    values.put(FavouriteTable.COLUMN_ROUTE, object.route_bound.route_no);
                    values.put(FavouriteTable.COLUMN_BOUND, object.route_bound.route_bound);
                    values.put(FavouriteTable.COLUMN_ORIGIN, object.route_bound.origin_tc);
                    values.put(FavouriteTable.COLUMN_DESTINATION, object.route_bound.destination_tc);
                    values.put(FavouriteTable.COLUMN_STOP_SEQ, object.stop_seq);
                    values.put(FavouriteTable.COLUMN_STOP_CODE, object.code);
                    values.put(FavouriteTable.COLUMN_STOP_NAME, object.name_tc);
                    values.put(FavouriteTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
                    Uri favUri = getContentResolver().insert(FavouriteProvider.CONTENT_URI_FAV, values);
                    object.favourite = (favUri != null);
                }
                if (org != object.favourite)
                    iStar.startAnimation(animationRotate);
                iStar.setImageResource(object.favourite ?
                        R.drawable.ic_star_black_48dp : R.drawable.ic_star_border_black_48dp);
                sendUpdate();
                break;
            case R.id.stop_name:
                mapContainer.setVisibility(View.GONE);
                getStopImage();
                break;
            case R.id.imageContainer:
                if (mapLoaded)
                    mapContainer.setVisibility(View.VISIBLE);
                imageContainer.setVisibility(View.GONE);
                break;
        }
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
    public void onPause() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        if (null != mContext) {
            if (null != mReceiver)
                mContext.unregisterReceiver(mReceiver);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (null != mAutoRefreshHandler && null != mAutoRefreshRunnable)
            mAutoRefreshHandler.removeCallbacks(mAutoRefreshRunnable);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroy();
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != object)
            outState.putParcelable(Constants.BUNDLE.STOP_OBJECT, object);
        mBitmap = Ion.with(iStop).getBitmap();
        outState.putParcelable("stop_image_bitmap", mBitmap);
        outState.putBoolean("mapLoaded", mapLoaded);
        outState.putInt("mapVisibility", mapContainer.getVisibility());
        outState.putInt("imageVisibility", imageContainer.getVisibility());
    }

    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mBitmap = savedInstanceState.getParcelable("stop_image_bitmap");
        mapLoaded = savedInstanceState.getBoolean("mapLoaded");
        mapVisibility = savedInstanceState.getInt("mapVisibility", View.GONE);
        imageVisibility = savedInstanceState.getInt("imageVisibility", View.GONE);
        imageContainer.setVisibility(View.GONE);
        if (null != mBitmap) {
            iStop.setImageBitmap(mBitmap);
            iStop.setVisibility(View.VISIBLE);
            if (imageVisibility == View.VISIBLE) {
                imageContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        View container = findViewById(R.id.mapContainer);
        if (null == object || null == object.details) {
            mapContainer.setVisibility(View.GONE);
            return;
        }
        map.setTrafficEnabled(true);
        map.setOnInfoWindowClickListener(this);
        // move
        LatLng place = getStopLanLng();
        CameraPosition cameraPosition =
                new CameraPosition.Builder()
                        .target(place)
                        .zoom(16)
                        .build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        // map.animateCamera(CameraUpdateFactory.zoomTo(16), 1000, null);
        // mark
        Marker marker = map.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_bus_black_24dp))
                .position(place)
                .title(object.stop_seq + ": " + object.name_tc));
        marker.showInfoWindow();
        mapLoaded = true;
        if (mapVisibility == View.VISIBLE)
            container.setVisibility(View.VISIBLE);
        //
        SettingsHelper settingsHelper = new SettingsHelper().parse(mContext.getApplicationContext());
        if (settingsHelper.getLoadStopImage()) {
            container.setVisibility(View.GONE);
            getStopImage();
        }
    }

    private LatLng getStopLanLng() {
        if (null == object || null == object.details) return null;
        Float lat = Float.valueOf(object.details.lat);
        Float lng = Float.valueOf(object.details.lng);
        return new LatLng(lat, lng);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.getPosition().equals(getStopLanLng())) {
            View container = findViewById(R.id.mapContainer);
            container.setVisibility(View.GONE);
            getStopImage();
        }
    }

    private void onRefresh() {
        iRefresh.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, CheckEtaService.class);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        startService(intent);
    }

    private void setTaskDescription(String title) {
        // overview task
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(title, bm,
                            ContextCompat.getColor(mContext, R.color.primary_600));
            ((AppCompatActivity) mContext).setTaskDescription(taskDesc);
        }
    }

    private void sendUpdate() {
        Intent intent = new Intent(Constants.MESSAGE.STOP_UPDATED);
        intent.putExtra(Constants.MESSAGE.STOP_UPDATED, true);
        intent.putExtra(Constants.BUNDLE.STOP_OBJECT, object);
        getApplication().sendBroadcast(intent);
    }

    private Boolean isFavourite(RouteStop object) {
        Cursor c = getContentResolver().query(FavouriteProvider.CONTENT_URI_FAV,
                null,
                FavouriteTable.COLUMN_ROUTE + " =?" +
                        " AND " + FavouriteTable.COLUMN_BOUND + " =?" +
                        " AND " + FavouriteTable.COLUMN_STOP_CODE + " =?",
                new String[]{
                        object.route_bound.route_no,
                        object.route_bound.route_bound,
                        object.code
                },
                FavouriteTable.COLUMN_DATE + " DESC");
        Boolean isFavourite = false;
        if (null != c) {
            isFavourite = c.getCount() > 0;
            c.close();
        }
        return isFavourite;
    }

    private RouteStop getObject(RouteStop object) {
       final Cursor c = getContentResolver().query(
                RouteProvider.CONTENT_URI,
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
        if (null != c && c.getCount() > 0) {
            c.moveToFirst();
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
        if (null != c)
            c.close();
        routeStop.favourite = isFavourite(object);
        Cursor cEta = getContentResolver().query(FavouriteProvider.CONTENT_URI_ETA_JOIN,
                null,
                FavouriteTable.COLUMN_ROUTE + " =?" +
                        " AND " + FavouriteTable.COLUMN_BOUND + " =?" +
                        " AND " + FavouriteTable.COLUMN_STOP_CODE + " =?",
                new String[]{
                        object.route_bound.route_no,
                        object.route_bound.route_bound,
                        object.code
                },
                FavouriteTable.COLUMN_DATE + " DESC");
        if (null != cEta && cEta.getCount() > 0) {
            cEta.moveToFirst();
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
            cEta.close();
        }
        return routeStop;
    }

    private String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    private void parse() {
        if (null == object) {
            finish();
            return;
        }
        iStar.setVisibility(hideStar ? View.GONE : View.VISIBLE);
        tStopName.setText(object.name_tc);
        tEta.setVisibility(View.VISIBLE);
        if (object.eta_loading != null && object.eta_loading) {
            if (tEta.getText().equals(""))
                tEta.setText(R.string.message_loading);
        } else if (object.eta_fail != null && object.eta_fail) {
            tEta.setText(R.string.message_fail_to_request);
        } else if (null == object.eta || object.eta.etas.equals("")) {
            tEta.setText(R.string.message_no_data);
        }
        if (null == object.eta || object.eta.etas.equals("")) {
            lServerTime.setVisibility(View.GONE);
            tServerTime.setVisibility(View.GONE);
            lLastUpdated.setVisibility(View.GONE);
            tLastUpdated.setVisibility(View.GONE);
            return;
        } else {
            lServerTime.setVisibility(View.VISIBLE);
            tServerTime.setVisibility(View.VISIBLE);
            lLastUpdated.setVisibility(View.VISIBLE);
            tLastUpdated.setVisibility(View.VISIBLE);
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
            if (i == 0) {
                String text = etas[i].replaceAll(" ?　?預定班次", "");
                setTaskDescription(null == object || null == object.route_bound ?
                        getString(R.string.launcher_name) :
                        text + " " + object.route_bound.route_no + " " + object.name_tc +
                                getString(R.string.interpunct) + getString(R.string.launcher_name));
            }
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
    }

    private void getStopImage() {
        if (null == object) return;
        final String stopCode = object.code;
        progressBar.setVisibility(View.VISIBLE);
        iStop.setVisibility(View.VISIBLE);
        imageContainer.setVisibility(View.VISIBLE);
        Ion.with(mContext)
                .load(Constants.URL.ROUTE_STOP_IMAGE + stopCode)
                .progressBar(progressBar)
                .withBitmap()
                .error(R.drawable.ic_error_outline_black_48dp)
                .resize(340, 255)
                .centerCrop()
                .animateLoad(R.anim.fade_in)
                .intoImageView(iStop)
                .setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        mBitmap = Ion.with(iStop).getBitmap();
                        iStop.setVisibility(View.VISIBLE);
                        if (null != progressBar)
                            progressBar.setVisibility(View.GONE);
                    }
                });
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
                        progressBar.setVisibility(View.GONE);
                    }
                }
                iRefresh.setVisibility(View.VISIBLE);
            }
        }
    }

}
