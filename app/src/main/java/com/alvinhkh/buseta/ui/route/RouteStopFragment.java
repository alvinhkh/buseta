package com.alvinhkh.buseta.ui.route;

import android.Manifest;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.Api;
import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.service.GeofenceTransitionsIntentService;
import com.alvinhkh.buseta.service.NotificationService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;


public class RouteStopFragment extends BottomSheetDialogFragment implements OnCompleteListener<Void> {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Tracks whether the user requested to add or remove geofences, or to do neither.
     */
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    private GeofencingClient mGeofencingClient;

    private ArrayList<Geofence> mGeofenceList;

    private PendingIntent mGeofencePendingIntent;

    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;

    private Location currentLocation;

    private BusRouteStop busRouteStop;

    private ViewHolder vh;

    private final Handler refreshHandler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (busRouteStop != null && getContext() != null) {
                    Intent intent = new Intent(getContext(), EtaService.class);
                    intent.putExtra(C.EXTRA.STOP_OBJECT, busRouteStop);
                    getContext().startService(intent);
                }
            } catch (IllegalStateException ignored) {}
            refreshHandler.postDelayed(this, 30000);  // refresh every 30 sec
        }
    };

    public RouteStopFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static RouteStopFragment newInstance(@NonNull BusRouteStop busRouteStop) {
        RouteStopFragment fragment = new RouteStopFragment();
        Bundle args = new Bundle();
        args.putParcelable(C.EXTRA.STOP_OBJECT, busRouteStop);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGeofenceList = new ArrayList<>();
        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(getContext());

        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.ETA_UPDATE))
                .share()
                .subscribeWith(etaObserver()));
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.LOCATION_UPDATE))
                .share()
                .subscribeWith(locationObserver()));  // Note: rely on others to call location request
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(refreshRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    /**
     * Runs when the result of calling {@link #addGeofences()} and/or {@link #removeGeofences()}
     * is available.
     * @param task the resulting Task, containing either a result or error.
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
                updateGeofencesAdded(String.format(Locale.ENGLISH, "%s-%s-%s-%s", busRouteStop.companyCode,
                        busRouteStop.route, busRouteStop.direction, busRouteStop.code));
                showSnackbar(getString(R.string.arrival_alert_added));
            } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
                updateGeofencesAdded("");
                showSnackbar(getString(R.string.arrival_alert_removed));
            }
        } else {
            Timber.w(task.getException());
        }
        if (vh != null && vh.arrivalAlertButton != null) {
            vh.arrivalAlertButton.setCompoundDrawablesWithIntrinsicBounds(null,
                    ContextCompat.getDrawable(getContext(), isThisGeofencesAdded() ?
                            R.drawable.ic_alarm_on_black_48dp : R.drawable.ic_alarm_black_48dp),
                    null, null);
        }
        mPendingGeofenceTask = PendingGeofenceTask.NONE;
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(getContext(), GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        if (vh.buttonContainer == null) return;
        Snackbar.make(vh.buttonContainer, text, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        if (vh.buttonContainer == null) return;
        Snackbar.make(
                vh.buttonContainer,
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Returns true if geofences were added, otherwise false.
     */
    private boolean isGeofencesAdded() {
        return !TextUtils.isEmpty(PreferenceManager.getDefaultSharedPreferences(getContext()).getString(C.PREF.GEOFENCES_KEY, ""));
    }

    /**
     * Returns true if this geofences were added, otherwise false.
     */
    private boolean isThisGeofencesAdded() {
        return PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(C.PREF.GEOFENCES_KEY, "")
                .equals(String.format(Locale.ENGLISH, "%s-%s-%s-%s", busRouteStop.companyCode,
                        busRouteStop.route, busRouteStop.direction, busRouteStop.code));
    }

    /**
     * Stores whether geofences were added or removed in {@link SharedPreferences};
     */
    private void updateGeofencesAdded(String key) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putString(C.PREF.GEOFENCES_KEY, key)
                .apply();
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     */
    private void performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeofences();
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    view -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_PERMISSIONS_REQUEST_CODE));
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofencesButtonHandler() {
        mPendingGeofenceTask = PendingGeofenceTask.ADD;
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }
        addGeofences();
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }

        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofencesButtonHandler() {
        mPendingGeofenceTask = PendingGeofenceTask.REMOVE;
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }
        removeGeofences();
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void removeGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }

        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Timber.i("onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Timber.i("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.i("Permission granted.");
                performPendingGeofenceTask();
            } else {
                showSnackbar(R.string.permission_denied_explanation, R.string.title_settings,
                        view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
                mPendingGeofenceTask = PendingGeofenceTask.NONE;
            }
        }
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback =
            new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        // Timber.d("STATE_COLLAPSED");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        // Timber.d("STATE_DRAGGING");
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        // Timber.d("STATE_EXPANDED");
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        // Timber.d("STATE_HIDDEN");
                        dismiss();
                        break;
                    default:
                        // Timber.d("STATE_SETTLING");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
    };

    @Override
    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.fragment_route_stop, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        if(behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }

        Bundle bundle = getArguments();
        if (bundle == null) {
            dialog.cancel();
            return;
        }
        busRouteStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
        if (busRouteStop == null) {
            dialog.cancel();
            return;
        }

        vh = new ViewHolder();
        vh.contentView = contentView;
        vh.stopImageButton = contentView.findViewById(R.id.show_image_button);
        vh.stopImageButton.setVisibility(View.GONE);
        vh.stopImage = contentView.findViewById(R.id.stop_image);
        vh.stopImage.setVisibility(View.GONE);

        vh.buttonContainer = contentView.findViewById(R.id.button_container);
        vh.followButton = contentView.findViewById(R.id.follow_button);
        vh.mapButton = contentView.findViewById(R.id.open_map_button);
        vh.notificationButton = contentView.findViewById(R.id.notification_button);
        vh.streetviewButton = contentView.findViewById(R.id.open_streetview_button);
        vh.arrivalAlertButton = contentView.findViewById(R.id.arrival_alert_button);
        vh.arrivalAlertButton.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);

        vh.nameText = contentView.findViewById(R.id.stop_name);
        vh.routeNoText = contentView.findViewById(R.id.route_no);
        vh.routeLocationText = contentView.findViewById(R.id.route_location);
        vh.stopLocationText = contentView.findViewById(R.id.stop_location);
        vh.fareText = contentView.findViewById(R.id.fare);
        vh.distanceText = contentView.findViewById(R.id.distance);
        vh.etaView = contentView.findViewById(R.id.eta_container);
        vh.etaText = contentView.findViewById(R.id.eta_text);
        vh.etaText.setText("\n\n\n");
        vh.etaServerTimeText = contentView.findViewById(R.id.eta_server_time);
        vh.etaLastUpdateText = contentView.findViewById(R.id.eta_last_update);

        vh.mapView = contentView.findViewById(R.id.map);

        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(getContext())
                    .getLastLocation().addOnSuccessListener(location -> {
                this.currentLocation = location;
                updateDistanceDisplay();
            });

            // TODO: alert in last few stops
            mGeofenceList.add(new Geofence.Builder()
                    .setRequestId(String.format(Locale.ENGLISH, "%s %s", busRouteStop.route, busRouteStop.name))
                    .setCircularRegion(
                            Double.parseDouble(busRouteStop.latitude),
                            Double.parseDouble(busRouteStop.longitude),
                            C.GEOFENCE.RADIUS_IN_METERS
                    )
                    .setExpirationDuration(C.GEOFENCE.EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
        }

        onRefresh();
    }

    private void updateDistanceDisplay() {
        if (busRouteStop.latitude != null && busRouteStop.longitude != null) {
            Location location = new Location("");
            location.setLatitude(Double.parseDouble(busRouteStop.latitude));
            location.setLongitude(Double.parseDouble(busRouteStop.longitude));
            if (currentLocation != null) {
                Float distance = currentLocation.distanceTo(location);
                vh.distanceText.setText(new DecimalFormat("~#.##km").format(distance / 1000));
            }
        }
    }

    private void onRefresh() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        vh.followButton.setOnClickListener(null);
        vh.mapButton.setOnClickListener(null);
        vh.notificationButton.setOnClickListener(null);
        vh.stopImageButton.setOnClickListener(null);
        vh.streetviewButton.setOnClickListener(null);
        vh.mapButton.setVisibility(View.GONE);
        vh.streetviewButton.setVisibility(View.GONE);

        if (busRouteStop != null) {
            if (!TextUtils.isEmpty(busRouteStop.latitude) && !TextUtils.isEmpty(busRouteStop.longitude)) {
                vh.mapView.setVisibility(View.VISIBLE);
                vh.mapView.setTileSource(TileSourceFactory.MAPNIK);
                vh.mapView.setBuiltInZoomControls(false);
                vh.mapView.setMultiTouchControls(false);
                vh.mapView.setTilesScaledToDpi(true);
                vh.mapView.setMaxZoomLevel(20);
                vh.mapView.setMinZoomLevel(14);
                IMapController mapController = vh.mapView.getController();
                mapController.setZoom(18);
                GeoPoint startPoint = new GeoPoint(Double.parseDouble(busRouteStop.latitude), Double.parseDouble(busRouteStop.longitude));
                mapController.setCenter(startPoint);

                Marker startMarker1 = new Marker(vh.mapView);
                startMarker1.setPosition(startPoint);
                startMarker1.setTitle(busRouteStop.name);
                vh.mapView.getOverlays().add(startMarker1);

                CompassOverlay mCompassOverlay = new CompassOverlay(getContext(),
                        new InternalCompassOrientationProvider(getContext()),
                        vh.mapView);
                mCompassOverlay.enableCompass();
                vh.mapView.getOverlays().add(mCompassOverlay);
                // vh.mapView.getOverlays().add(new CopyrightOverlay(getContext()));

                vh.mapView.setOnTouchListener((v, event) -> true);
            } else {
                vh.mapView.setVisibility(View.GONE);
            }


            Drawable followDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_border_black_48dp);
            Drawable unfollowDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_bookmark_black_48dp);
            vh.followButton.setCompoundDrawablesWithIntrinsicBounds(null, followDrawable, null, null);
            vh.followButton.setText(R.string.follow);
            FollowStopUtil.query(getContext(), BusRouteStopUtil.toFollowStop(busRouteStop)).subscribe(cursor -> {
                vh.followButton.setText(cursor.getCount() > 0 ? R.string.action_unfollow : R.string.follow);
                vh.followButton.setCompoundDrawablesWithIntrinsicBounds(null, cursor.getCount() > 0 ? unfollowDrawable : followDrawable, null, null);
            });

            if (!TextUtils.isEmpty(busRouteStop.latitude) && !TextUtils.isEmpty(busRouteStop.longitude)) {
                vh.mapButton.setVisibility(View.VISIBLE);
                vh.streetviewButton.setVisibility(View.VISIBLE);
                vh.mapButton.setOnClickListener(v -> {
                    Uri uri = new Uri.Builder().scheme("geo")
                            .appendPath(busRouteStop.latitude + "," + busRouteStop.longitude)
                            .appendQueryParameter("q", busRouteStop.latitude + "," + busRouteStop.longitude + "(" + busRouteStop.name + ")")
                            .build();
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    if (mapIntent.resolveActivity(v.getContext().getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        showSnackbar(getString(R.string.message_no_geo_app));
                    }
                });
                vh.streetviewButton.setOnClickListener(v -> {
                    Uri gmmIntentUri = Uri.parse("google.streetview:cbll=" + busRouteStop.latitude + "," + busRouteStop.longitude + "&cbp=1,0,,-90,1");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(v.getContext().getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        showSnackbar(getString(R.string.message_no_geo_app));
                    }
                });
            }
            vh.followButton.setOnClickListener(v -> {
                FollowStop followStop = BusRouteStopUtil.toFollowStop(busRouteStop);
                FollowStopUtil.query(v.getContext(), followStop).count().subscribe(count -> {
                    if (count > 0) {
                        // followed, remove
                        int rowDeleted = FollowStopUtil.delete(v.getContext(), followStop);
                        if (rowDeleted > 0) {
                            vh.followButton.setCompoundDrawablesWithIntrinsicBounds(null, followDrawable, null, null);
                            vh.followButton.setText(R.string.follow);
                        }
                    } else {
                        // follow
                        Uri uriInserted = FollowStopUtil.insert(v.getContext(), followStop);
                        if (uriInserted != null) {
                            vh.followButton.setCompoundDrawablesWithIntrinsicBounds(null, unfollowDrawable, null, null);
                            vh.followButton.setText(R.string.action_unfollow);
                        }
                    }
                    Intent intent = new Intent(C.ACTION.FOLLOW_UPDATE);
                    intent.putExtra(C.EXTRA.STOP_OBJECT, followStop);
                    intent.putExtra(C.EXTRA.UPDATED, true);
                    v.getContext().sendBroadcast(intent);
                });
            });
            vh.notificationButton.setOnClickListener(v -> {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(v.getContext());
                if (!notificationManager.areNotificationsEnabled()) {
                    showSnackbar("Notification disabled in system settings.");
                    return;
                }
                Intent intent = new Intent(v.getContext(), NotificationService.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT, busRouteStop);
                ContextCompat.startForegroundService(v.getContext(), intent);

                /*
                if (dispatcher != null) {
                    Integer notificationId = NotificationUtil.showArrivalTime(v.getContext(), busRouteStop);
                    Bundle bundle = new Bundle();
                    bundle.putString(C.EXTRA.STOP_OBJECT_STRING, new Gson().toJson(busRouteStop));
                    bundle.putInt(C.EXTRA.NOTIFICATION_ID, notificationId);
                    Job job = dispatcher.newJobBuilder()
                            .setService(EtaJobService.class)  // the JobService that will be called
                            .setTag(String.format(Locale.ENGLISH, "eta-notification-%d", notificationId))  // uniquely identifies the job
                            .setTrigger(Trigger.executionWindow(0, 15))  // start between 0 and 15 seconds from now
                            .setRecurring(true)  // repeat job
                            .setLifetime(Lifetime.UNTIL_NEXT_BOOT)  // don't persist past a device reboot
                            .setReplaceCurrent(true)  // overwrite an existing job with the same tag
                            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)  // retry with exponential backoff
                            .setConstraints(Constraint.ON_ANY_NETWORK)
                            .setExtras(bundle)
                            .build();
                    dispatcher.mustSchedule(job);
                }
                */

                showSnackbar(getString(R.string.message_shown_as_notification));
            });
            vh.arrivalAlertButton.setCompoundDrawablesWithIntrinsicBounds(null,
                    ContextCompat.getDrawable(getContext(),
                            isThisGeofencesAdded() ? R.drawable.ic_alarm_on_black_48dp : R.drawable.ic_alarm_black_48dp),
                    null, null);
            vh.arrivalAlertButton.setOnClickListener(v -> {
                Timber.d("isThisGeofencesAdded: %s", isThisGeofencesAdded());
                if (isThisGeofencesAdded()) {
                    Timber.d("removeGeofencesButtonHandler");
                    removeGeofencesButtonHandler();
                } else if (isGeofencesAdded()) {
                    showSnackbar(R.string.arrival_alert_existed, R.string.replace, view -> {
                        Timber.d("addGeofencesButtonHandler");
                        addGeofencesButtonHandler();
                    });
                } else {
                    Timber.d("addGeofencesButtonHandler");
                    addGeofencesButtonHandler();
                }
            });

            vh.nameText.setText(TextUtils.isEmpty(busRouteStop.name) ? "" : busRouteStop.name.trim());
            vh.routeNoText.setText(TextUtils.isEmpty(busRouteStop.route) ? "" : busRouteStop.route.trim());
            if (!TextUtils.isEmpty(busRouteStop.origin) && !TextUtils.isEmpty(busRouteStop.destination)) {
                vh.routeLocationText.setText(getString(R.string.destination, busRouteStop.destination));
            }
            vh.stopLocationText.setText(TextUtils.isEmpty(busRouteStop.location) ? "" : busRouteStop.location.trim());
            StringBuilder fareText = new StringBuilder();
            fareText.append(String.format(Locale.ENGLISH, "$%1$,.1f", Float.valueOf(busRouteStop.fare)));
            if (!TextUtils.isEmpty(busRouteStop.fareHoliday)) {
                fareText.append(String.format(Locale.ENGLISH, "/$%1$,.1f", Float.valueOf(busRouteStop.fareHoliday)));
            }
            if (!TextUtils.isEmpty(busRouteStop.fareChild)) {
                fareText.append(String.format(Locale.ENGLISH, "/$%1$,.1f", Float.valueOf(busRouteStop.fareChild)));
            }
            if (!TextUtils.isEmpty(busRouteStop.fareSenior)) {
                fareText.append(String.format(Locale.ENGLISH, "/$%1$,.1f", Float.valueOf(busRouteStop.fareSenior)));
            }
            vh.fareText.setText(fareText);
            updateDistanceDisplay();
            // ETA
            vh.etaView.setVisibility(View.INVISIBLE);
            Intent intent = new Intent(getContext(), EtaService.class);
            intent.putExtra(C.EXTRA.STOP_OBJECT, busRouteStop);
            getContext().startService(intent);
            // Stop image
            vh.stopImage.setVisibility(View.GONE);
            vh.stopImageButton.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(busRouteStop.imageUrl)) {
                if (preferences != null && preferences.getBoolean("load_stop_image", false)) {
                    disposables.add(Api.raw.create(Api.class).get(busRouteStop.imageUrl)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(getImage()));
                } else {
                    vh.stopImageButton.setVisibility(View.VISIBLE);
                    vh.stopImageButton.setOnClickListener(v ->
                            disposables.add(Api.raw.create(Api.class).get(busRouteStop.imageUrl)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(getImage())));
                }
            }
        }
    }

    private static class ViewHolder {
        View contentView;

        Bitmap stopBitmap;
        ImageView stopImage;
        Button stopImageButton;

        View buttonContainer;
        Button followButton;
        Button mapButton;
        Button notificationButton;
        Button streetviewButton;
        Button arrivalAlertButton;

        TextView nameText;
        TextView routeNoText;
        TextView routeLocationText;
        TextView stopLocationText;
        TextView fareText;
        TextView distanceText;
        View etaView;
        TextView etaText;
        TextView etaServerTimeText;
        TextView etaLastUpdateText;

        MapView mapView;
    }

    DisposableObserver<Intent> etaObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                BusRouteStop stop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (stop == null) return;
                if (!stop.equals(busRouteStop)) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    Timber.d("eta updated: %s", stop.toString());

                    Context context = getContext();
                    vh.etaText.setText(null);
                    vh.etaServerTimeText.setText(null);
                    vh.etaLastUpdateText.setText(null);

                    ArrivalTimeUtil.query(context, stop).subscribe(cursor -> {
                        // Cursor has been moved +1 position forward.

                        ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
                        arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime);
                        if (arrivalTime.id != null) {
                            SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.text);
                            Integer pos = Integer.parseInt(arrivalTime.id);
                            Integer colorInt = ContextCompat.getColor(context,
                                    arrivalTime.expired ? R.color.textDiminish :
                                            (pos > 0 ? R.color.textPrimary : R.color.textHighlighted));
                            if (arrivalTime.isSchedule) {
                                etaText.append(" ").append(getString(R.string.scheduled_bus));
                            }
                            if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                                etaText.append(" (").append(arrivalTime.estimate).append(")");
                            }
                            if (arrivalTime.distanceKM >= 0) {
                                etaText.append(" ").append(context.getString(R.string.km, arrivalTime.distanceKM));
                            }
                            if (arrivalTime.capacity >= 0) {
                                Drawable drawable = null;
                                String capacity = "";
                                if (arrivalTime.capacity == 0) {
                                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_0_black);
                                    capacity = getString(R.string.capacity_empty);
                                } else if (arrivalTime.capacity > 0 && arrivalTime.capacity <= 3) {
                                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_20_black);
                                    capacity = "¼";
                                } else if (arrivalTime.capacity > 3 && arrivalTime.capacity <= 6) {
                                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_50_black);
                                    capacity = "½";
                                } else if (arrivalTime.capacity > 6 && arrivalTime.capacity <= 9) {
                                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_80_black);
                                    capacity = "¾";
                                } else if (arrivalTime.capacity >= 10) {
                                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_100_black);
                                    capacity = getString(R.string.capacity_full);
                                }
                                if (drawable != null) {
                                    drawable = DrawableCompat.wrap(drawable);
                                    drawable.setBounds(0, 0, vh.etaText.getLineHeight(), vh.etaText.getLineHeight());
                                    DrawableCompat.setTint(drawable.mutate(), colorInt);
                                    ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                                    etaText.append(" ");
                                    if (etaText.length() > 0) {
                                        etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                    }
                                }
                                if (!TextUtils.isEmpty(capacity)) {
                                    etaText.append(capacity);
                                }
                            }
                            if (arrivalTime.hasWheelchair) {
                                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_accessible_black_18dp);
                                drawable = DrawableCompat.wrap(drawable);
                                drawable.setBounds(0, 0, vh.etaText.getLineHeight(), vh.etaText.getLineHeight());
                                DrawableCompat.setTint(drawable.mutate(), colorInt);
                                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                                etaText.append(" ");
                                if (etaText.length() > 0) {
                                    etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            }
                            if (arrivalTime.hasWifi) {
                                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_network_wifi_black_18dp);
                                drawable = DrawableCompat.wrap(drawable);
                                drawable.setBounds(0, 0, vh.etaText.getLineHeight(), vh.etaText.getLineHeight());
                                DrawableCompat.setTint(drawable.mutate(), colorInt);
                                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                                etaText.append(" ");
                                if (etaText.length() > 0) {
                                    etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            }
                            if (etaText.length() > 0) {
                                etaText.setSpan(new ForegroundColorSpan(colorInt), 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (TextUtils.isEmpty(vh.etaText.getText())) {
                                vh.etaText.setText(etaText);
                            } else {
                                etaText.insert(0, "\n");
                                etaText.insert(0, vh.etaText.getText());
                                vh.etaText.setText(etaText);
                            }

                            if (arrivalTime.generatedAt != null && arrivalTime.generatedAt > 0) {
                                Date date = new Date(arrivalTime.generatedAt);
                                vh.etaServerTimeText.setText(ArrivalTimeUtil.displayDateFormat.format(date));
                            }
                            if (arrivalTime.updatedAt != null && arrivalTime.updatedAt > 0) {
                                Date date = new Date(arrivalTime.updatedAt);
                                vh.etaLastUpdateText.setText(ArrivalTimeUtil.displayDateFormat.format(date));
                            }
                            if (vh.etaServerTimeText.getText().equals(vh.etaLastUpdateText.getText())) {
                                vh.etaServerTimeText.setText(null);
                            }

                            vh.etaView.setVisibility(View.VISIBLE);
                        }
                    });
                }
                // if (bundle.getBoolean(C.EXTRA.UPDATING)) { }
                if (bundle.getBoolean(C.EXTRA.FAIL)) {
                    vh.etaView.setVisibility(View.INVISIBLE);
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
                if (bundle.getBoolean(C.EXTRA.REQUEST)) return;
                Location location = bundle.getParcelable(C.EXTRA.LOCATION_OBJECT);
                if (location == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    currentLocation = location;
                }
                if (bundle.getBoolean(C.EXTRA.FAIL)) {
                    currentLocation = null;
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
                updateDistanceDisplay();
            }
        };
    }

    DisposableObserver<ResponseBody> getImage() {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                if (body == null) return;
                if (body.contentType() != null) {
                    String contentType = body.contentType().toString();
                    if (contentType.contains("image")) {
                        vh.stopBitmap = BitmapFactory.decodeStream(body.byteStream());
                    } else {
                        Timber.d(contentType);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                vh.stopBitmap = null;
                Timber.d(e);
            }

            @Override
            public void onComplete() {
                if (vh.stopImage != null && vh.stopBitmap != null) {
                    vh.stopImage.setImageBitmap(vh.stopBitmap);
                    vh.stopImage.setVisibility(View.VISIBLE);
                    vh.stopImageButton.setVisibility(View.GONE);
                }
            }
        };
    }
}
