package com.alvinhkh.buseta.route.ui

import android.Manifest
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabsIntent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.BuildConfig
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.follow.ui.FollowGroupDialogFragment
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.service.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

import timber.log.Timber


class RouteStopFragment : BottomSheetDialogFragment(), OnCompleteListener<Void> {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase

    private lateinit var followDatabase: FollowDatabase

    private lateinit var routeDatabase: RouteDatabase

    private var mGeofencingClient: GeofencingClient? = null

    private var mGeofenceList: ArrayList<Geofence>? = null

    private var mGeofencePendingIntent: PendingIntent? = null

    private var mPendingGeofenceTask = PendingGeofenceTask.NONE

    private var currentLocation: Location? = null

    private var routeStop: RouteStop? = null

    private val vh = ViewHolder()

    private var refreshInterval: Int? = 30

    private val refreshHandler = Handler()

    private val refreshRunnable = object : Runnable {
        override fun run() {
            try {
                if (routeStop != null && context != null) {
                    val intent = Intent(context, EtaService::class.java)
                    intent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                    requireContext().startService(intent)
                }
            } catch (ignored: IllegalStateException) {
            }

            refreshHandler.postDelayed(this, (refreshInterval!! * 1000).toLong())
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private// Reuse the PendingIntent if we already have it.
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
    // addGeofences() and removeGeofences().
    val geofencePendingIntent: PendingIntent
        get() {
            if (mGeofencePendingIntent != null) {
                return mGeofencePendingIntent as PendingIntent
            }
            val intent = Intent(context, GeofenceTransitionsIntentService::class.java)
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    /**
     * Returns true if geofences were added, otherwise false.
     */
    private val isGeofencesAdded: Boolean
        get() = if (context == null) false else !PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(C.PREF.GEOFENCES_KEY, "").isNullOrEmpty()

    /**
     * Returns true if this geofences were added, otherwise false.
     */
    private val isThisGeofencesAdded: Boolean
        get() = if (context == null) false else PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(C.PREF.GEOFENCES_KEY, "") == String.format(Locale.ENGLISH, "%s-%s-%s-%s", routeStop!!.companyCode,
                routeStop!!.routeNo, routeStop!!.routeSequence, routeStop!!.stopId)


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private// The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
    // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
    // is already inside that geofence.
    // Add the geofences to be monitored by geofencing service.
    // Return a GeofencingRequest.
    val geofencingRequest: GeofencingRequest?
        get() {
            if (mGeofenceList!!.size < 1) return null
            val builder = GeofencingRequest.Builder()
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            builder.addGeofences(mGeofenceList)
            return builder.build()
        }

    private val mBottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                }
                BottomSheetBehavior.STATE_DRAGGING -> {
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                }
                BottomSheetBehavior.STATE_HIDDEN ->
                    dismiss()
                else -> {
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    /**
     * Tracks whether the user requested to add or remove geofences, or to do neither.
     */
    private enum class PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(requireContext())!!
        followDatabase = FollowDatabase.getInstance(requireContext())!!
        routeDatabase = RouteDatabase.getInstance(requireContext())!!

        mGeofenceList = ArrayList()
        mGeofencePendingIntent = null
        mGeofencingClient = LocationServices.getGeofencingClient(requireContext())

        if (activity != null &&
                ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener(requireActivity()) { location ->
                        if (location != null) {
                            currentLocation = location
                        }
                    }
        }
    }

    override fun onResume() {
        super.onResume()
        if (context != null) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            if (preferences != null) {
                val i = preferences.getString("load_eta", "0")?.toInt()?:0
                if (i > 0) {
                    refreshInterval = i
                }
            }
        }
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacksAndMessages(refreshRunnable)
    }

    /**
     * Runs when the result of calling [.addGeofences] and/or [.removeGeofences]
     * is available.
     * @param task the resulting Task, containing either a result or error.
     */
    override fun onComplete(task: Task<Void>) {
        if (task.isSuccessful) {
            if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
                updateGeofencesAdded(String.format(Locale.ENGLISH, "%s-%s-%s-%s", routeStop!!.companyCode,
                        routeStop!!.routeNo, routeStop!!.routeSequence, routeStop!!.stopId))
                showSnackbar(getString(R.string.arrival_alert_added))
            } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
                updateGeofencesAdded("")
                showSnackbar(getString(R.string.arrival_alert_removed))
            }
        } else {
            Timber.w(task.exception)
        }
        vh.arrivalAlertButton?.setIconResource(if (isThisGeofencesAdded)
            R.drawable.ic_outline_alarm_on_36dp
        else
            R.drawable.ic_outline_alarm_add_36dp)
        mPendingGeofenceTask = PendingGeofenceTask.NONE
    }

    /**
     * Shows a [Snackbar] using `text`.
     *
     * @param text The Snackbar text.
     */
    private fun showSnackbar(text: String) {
        if (vh.coordinatorLayout == null) return
        Snackbar.make(vh.coordinatorLayout!!, text, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Shows a [Snackbar].
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int, listener: (Any) -> Unit) {
        if (vh.coordinatorLayout == null) return
        Snackbar.make(
                vh.coordinatorLayout!!,
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show()
    }

    /**
     * Stores whether geofences were added or removed in [SharedPreferences];
     */
    private fun updateGeofencesAdded(key: String) {
        if (context == null) return
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit()
                .putString(C.PREF.GEOFENCES_KEY, key)
                .apply()
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     */
    private fun performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences()
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeofences()
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        if (context == null) return false
        val permissionState = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

        if (shouldProvideRationale) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok
            ) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    private fun addGeofencesButtonHandler() {
        mPendingGeofenceTask = PendingGeofenceTask.ADD
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        addGeofences()
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    private fun addGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }

//        mGeofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
//                .addOnCompleteListener(this)
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    private fun removeGeofencesButtonHandler() {
        mPendingGeofenceTask = PendingGeofenceTask.REMOVE
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        removeGeofences()
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    private fun removeGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions))
            return
        }

        mGeofencingClient?.removeGeofences(geofencePendingIntent)?.addOnCompleteListener(this)
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        Timber.i("onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Timber.i("User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    Timber.i("Permission granted.")
                    performPendingGeofenceTask()
                }
                else -> {
                    showSnackbar(R.string.permission_denied_explanation, R.string.title_settings
                    ) {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    mPendingGeofenceTask = PendingGeofenceTask.NONE
                }
            }
        }
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        val contentView = View.inflate(context, R.layout.fragment_route_stop, null)
        dialog.setContentView(contentView)

        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior

        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
        }

        val bundle = arguments
        if (bundle == null) {
            dialog.cancel()
            return
        }
        routeStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT)
        var follow: Follow? = bundle.getParcelable(C.EXTRA.FOLLOW_OBJECT)
        if (follow != null && routeStop == null) {
            routeStop = follow.toRouteStop()
        }
        if (routeStop == null) {
            dialog.cancel()
            return
        }
        val route = routeDatabase.routeDao().get(routeStop?.companyCode?:"", routeStop?.routeId?:"", routeStop?.routeNo?:"", routeStop?.routeSequence?:"", routeStop?.routeServiceType?:"")
        routeStop = routeDatabase.routeStopDao().get(routeStop?.companyCode?:"", routeStop?.routeNo?:"", routeStop?.routeSequence?:"", routeStop?.routeServiceType?:"", routeStop?.stopId?:"", routeStop?.sequence?:"")
        if (routeStop == null) {
            dialog.cancel()
            return
        }
        if (follow == null) {
            follow = Follow.createInstance(route, routeStop)
        }
        val companyColor: Int = if (!route?.colour.isNullOrBlank()) {
            Color.parseColor(route?.colour)
        } else {
            Route.companyColour(requireContext(), routeStop?.companyCode?:"", routeStop?.routeNo?:"")?: ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        }

        vh.contentView = contentView
        vh.headerLayout = contentView.findViewById(R.id.header_layout)
        vh.headerLayout?.setBackgroundColor(companyColor)
        vh.stopImageButton = contentView.findViewById(R.id.show_image_button)
        vh.stopImageButton?.visibility = View.GONE
        vh.stopImage = contentView.findViewById(R.id.stop_image)
        vh.stopImage?.visibility = View.GONE

        vh.coordinatorLayout = contentView.findViewById(R.id.coordinator_layout)
        vh.followButton = contentView.findViewById(R.id.follow_button)
        vh.mapButton = contentView.findViewById(R.id.open_map_button)
        vh.notificationButton = contentView.findViewById(R.id.notification_button)
        vh.streetviewButton = contentView.findViewById(R.id.open_streetview_button)
        vh.arrivalAlertButton = contentView.findViewById(R.id.arrival_alert_button)
        vh.arrivalAlertButton?.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            vh.followButton?.iconTint = ColorStateList.valueOf(companyColor)
            vh.mapButton?.iconTint = ColorStateList.valueOf(companyColor)
            vh.notificationButton?.iconTint = ColorStateList.valueOf(companyColor)
            vh.streetviewButton?.iconTint = ColorStateList.valueOf(companyColor)
            vh.arrivalAlertButton?.iconTint = ColorStateList.valueOf(companyColor)
            vh.followButton?.setTextColor(companyColor)
            vh.mapButton?.setTextColor(companyColor)
            vh.notificationButton?.setTextColor(companyColor)
            vh.streetviewButton?.setTextColor(companyColor)
            vh.arrivalAlertButton?.setTextColor(companyColor)
        }

        vh.nameText = contentView.findViewById(R.id.stop_name)
        vh.routeNoText = contentView.findViewById(R.id.route_no)
        vh.routeLocationText = contentView.findViewById(R.id.route_location)
        vh.stopLocationText = contentView.findViewById(R.id.stop_location)
        vh.fareText = contentView.findViewById(R.id.fare)
        vh.distanceText = contentView.findViewById(R.id.distance)
        vh.etaView = contentView.findViewById(R.id.eta_container)
        vh.etaText = contentView.findViewById(R.id.eta_text)
        vh.etaText?.text = "\n\n\n"
        vh.etaServerTimeText = contentView.findViewById(R.id.eta_server_time)
        vh.etaLastUpdateText = contentView.findViewById(R.id.eta_last_update)

        vh.mapView = contentView.findViewById(R.id.map)

        if (context != null && ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(requireContext())
                    .lastLocation.addOnSuccessListener { location ->
                this.currentLocation = location
                updateDistanceDisplay()
            }

            // TODO: alert in last few stops
            /*
            if (!TextUtils.isEmpty(routeStop.latitude) && !TextUtils.isEmpty(routeStop.longitude)) {
                mGeofenceList.add(new Geofence.Builder()
                        .setRequestId(String.format(Locale.ENGLISH, "%s %s", routeStop.routeNo, routeStop.name))
                        .setCircularRegion(
                                Double.parseDouble(routeStop.latitude),
                                Double.parseDouble(routeStop.longitude),
                                C.GEOFENCE.RADIUS_IN_METERS
                        )
                        .setExpirationDuration(C.GEOFENCE.EXPIRATION_IN_MILLISECONDS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
            }
             */
        }


        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        vh.followButton?.setOnClickListener(null)
        vh.mapButton?.setOnClickListener(null)
        vh.notificationButton?.setOnClickListener(null)
        vh.stopImageButton?.setOnClickListener(null)
        vh.streetviewButton?.setOnClickListener(null)
        vh.mapButton?.visibility = View.GONE
        vh.streetviewButton?.visibility = View.GONE
        vh.arrivalAlertButton?.visibility = View.GONE

        if (routeStop != null) {
            if (!routeStop?.latitude.isNullOrEmpty() && !routeStop?.longitude.isNullOrEmpty()) {
                vh.mapView?.visibility = View.VISIBLE
                vh.mapView?.setTileSource(TileSourceFactory.MAPNIK)
                vh.mapView?.zoomController?.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                vh.mapView?.setMultiTouchControls(false)
                vh.mapView?.isTilesScaledToDpi = true
                vh.mapView?.maxZoomLevel = 20.0
                vh.mapView?.minZoomLevel = 14.0
                val mapController = vh.mapView?.controller
                mapController?.setZoom(18.0)
                val startPoint = GeoPoint(routeStop?.latitude?.toDouble()?:0.0, routeStop?.longitude?.toDouble()?:0.0)
                mapController?.setCenter(startPoint)

                val startMarker1 = Marker(vh.mapView)
                startMarker1.position = startPoint
                startMarker1.title = routeStop?.name
                vh.mapView?.overlays?.add(startMarker1)

                val mCompassOverlay = CompassOverlay(requireContext(),
                        InternalCompassOrientationProvider(requireContext()),
                        vh.mapView)
                mCompassOverlay.enableCompass()
                vh.mapView?.overlays?.add(mCompassOverlay)
            } else {
                vh.mapView?.visibility = View.GONE
            }

            val followCount = followDatabase.followDao().liveCount(
                    routeStop?.companyCode?:"", routeStop?.routeNo?:"",
                    routeStop?.routeSequence?:"", routeStop?.routeServiceType?:"",
                    routeStop?.stopId?:"", routeStop?.sequence?:"")
            followCount.removeObservers(this)
            followCount.observe(this, { count ->
                vh.followButton?.removeCallbacks {  }
                if (count == null) {
                    return@observe
                }
                vh.followButton?.setIconResource(if (count > 0) R.drawable.ic_outline_bookmark_36dp else R.drawable.ic_outline_bookmark_border_36dp)
                vh.followButton?.setOnClickListener {
                    if (count > 0) {
                        val fragment = FollowGroupDialogFragment.newInstance(follow)
                        fragment.show(childFragmentManager, "follow_group_dialog_fragment")
                    } else {
                        val noGroup = followDatabase.followGroupDao().get(FollowGroup.UNCATEGORISED)
                        if (noGroup == null) {
                            followDatabase.followGroupDao().insert(FollowGroup(FollowGroup.UNCATEGORISED, "", ""))
                        }
                        val f = follow.copy()
                        f._id = 0
                        f.groupId = FollowGroup.UNCATEGORISED
                        f.updatedAt = System.currentTimeMillis()
                        followDatabase.followDao().insert(f)
                    }
                }
            })

            if (!routeStop?.latitude.isNullOrEmpty() && !routeStop?.longitude.isNullOrEmpty()) {
                vh.mapButton?.visibility = View.VISIBLE
                vh.streetviewButton?.visibility = View.VISIBLE
                vh.mapButton?.setOnClickListener { v ->
                    val uri = Uri.Builder().scheme("geo")
                            .appendPath(routeStop?.latitude + "," + routeStop?.longitude)
                            .appendQueryParameter("q", routeStop?.latitude + "," + routeStop?.longitude + "(" + routeStop?.name + ")")
                            .build()
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    if (mapIntent.resolveActivity(v.context.packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        showSnackbar(getString(R.string.message_no_geo_app))
                    }
                }
                vh.streetviewButton?.setOnClickListener { v ->
                    val gmmIntentUri = Uri.parse("google.streetview:cbll=" + routeStop?.latitude + "," + routeStop?.longitude + "&cbp=1,0,,-90,1")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(v.context.packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        showSnackbar(getString(R.string.message_no_geo_app))
                    }
                }
                vh.arrivalAlertButton?.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
                vh.arrivalAlertButton?.setIconResource(if (isThisGeofencesAdded) R.drawable.ic_outline_alarm_on_36dp else R.drawable.ic_outline_alarm_add_36dp)
                vh.arrivalAlertButton?.setOnClickListener {
                    Timber.d("isThisGeofencesAdded: %s", isThisGeofencesAdded)
                    when {
                        isThisGeofencesAdded -> {
                            Timber.d("removeGeofencesButtonHandler")
                            removeGeofencesButtonHandler()
                        }
                        isGeofencesAdded -> {
                            showSnackbar(R.string.arrival_alert_existed, R.string.replace) {
                                Timber.d("addGeofencesButtonHandler")
                                addGeofencesButtonHandler()
                            }
                        }
                        else -> {
                            Timber.d("addGeofencesButtonHandler")
                            addGeofencesButtonHandler()
                        }
                    }
                }
            } else {
                if (routeStop?.companyCode?:"" == C.PROVIDER.MTR) {
                    vh.mapButton?.visibility = View.VISIBLE
                    vh.mapButton?.setText(R.string.location_map)
                    vh.mapButton?.setOnClickListener { v ->
                        openLink(v.context,
                                "https://www.mtr.com.hk/archive/en/services/maps/${routeStop?.stopId?.toLowerCase(Locale.ROOT)}.pdf",
                                companyColor)
                    }
                    vh.streetviewButton?.visibility = View.VISIBLE
                    vh.streetviewButton?.setText(R.string.station_layout)
                    vh.streetviewButton?.setOnClickListener { v ->
                        openLink(v.context,
                                "https://www.mtr.com.hk/archive/en/services/layouts/${routeStop?.stopId?.toLowerCase(Locale.ROOT)}.pdf",
                                companyColor)
                    }
                }
            }
            vh.notificationButton?.setOnClickListener { v ->
                val notificationManager = NotificationManagerCompat.from(v.context)
                if (!notificationManager.areNotificationsEnabled()) {
                    showSnackbar("Notification disabled in system settings.")
                    return@setOnClickListener
                }
                val intent = Intent(v.context, EtaNotificationService::class.java)
                intent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                ContextCompat.startForegroundService(v.context, intent)
                showSnackbar(getString(R.string.message_shown_as_notification))
            }

            vh.nameText?.text = if (routeStop?.name.isNullOrEmpty()) "" else routeStop?.name?.trim { it <= ' ' }
            vh.routeNoText?.text = if (routeStop?.routeNo.isNullOrEmpty()) "" else routeStop?.routeNo?.trim { it <= ' ' }
            if (!routeStop?.routeOrigin.isNullOrEmpty() && !routeStop?.routeDestination.isNullOrEmpty()) {
                vh.routeLocationText?.text = getString(R.string.destination, routeStop?.routeDestination)
            }
            vh.stopLocationText?.text = if (routeStop?.location.isNullOrEmpty()) "" else routeStop?.location?.trim { it <= ' ' }
            val fareText = StringBuilder()
            if (!routeStop?.fareFull.isNullOrEmpty()) {
                fareText.append(String.format(Locale.ENGLISH, "$%1$,.1f", routeStop?.fareFull?.toFloat()))
            }
            if (!routeStop?.fareHoliday.isNullOrEmpty()) {
                fareText.append(String.format(Locale.ENGLISH, "/$%1$,.1f", routeStop?.fareHoliday?.toFloat()))
            }
            if (!routeStop?.fareChild.isNullOrEmpty()) {
                fareText.append(String.format(Locale.ENGLISH, "/$%1$,.1f", routeStop?.fareChild?.toFloat()))
            }
            if (!routeStop?.fareSenior.isNullOrEmpty()) {
                fareText.append(String.format(Locale.ENGLISH, "/$%1$,.1f", routeStop?.fareSenior?.toFloat()))
            }
            vh.fareText?.text = fareText
            updateDistanceDisplay()

            // ETA
            vh.etaView?.visibility = View.INVISIBLE
            val intent = Intent(context, EtaService::class.java)
            intent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
            requireContext().startService(intent)
            val arrivalTimeLiveData = arrivalTimeDatabase.arrivalTimeDao().getLiveData(routeStop?.companyCode?:"", routeStop?.routeNo?:"", routeStop?.routeSequence?:"", routeStop?.stopId?:"", routeStop?.sequence?:"")
            arrivalTimeLiveData.removeObservers(this)
            arrivalTimeLiveData.observe(this, { list ->
                vh.etaText?.text = ""
                if (list?.size?:0 < 1) {
                    vh.etaView?.visibility = View.GONE
                }
                list?.forEach {
                    val arrivalTime = ArrivalTime.estimate(requireContext(), it)
                    if (arrivalTime.updatedAt > System.currentTimeMillis() - 600000 && arrivalTime.order.isNotEmpty()) {
                        val etaText = SpannableStringBuilder(arrivalTime.text)
                        val pos = Integer.parseInt(arrivalTime.order)
                        val colorInt: Int = ContextCompat.getColor(requireContext(),
                                if (arrivalTime.companyCode == C.PROVIDER.MTR) {
                                    if (arrivalTime.expired)
                                        R.color.textDiminish
                                    else
                                        R.color.textPrimary
                                } else {
                                    when {
                                        arrivalTime.expired -> R.color.textDiminish
                                        pos > 0 -> R.color.textPrimary
                                        else -> R.color.textHighlighted
                                    }
                                })
                        if (arrivalTime.platform.isNotEmpty()) {
                            etaText.insert(0, "[" + arrivalTime.platform + "] ")
                        }
                        if (arrivalTime.isSchedule) {
                            etaText.append(" ").append(getString(R.string.scheduled_bus))
                        }
                        if (arrivalTime.estimate.isNotEmpty()) {
                            etaText.append(" (").append(arrivalTime.estimate).append(")")
                        }
                        if (arrivalTime.distanceKM >= 0) {
                            etaText.append(" ").append(getString(R.string.km, arrivalTime.distanceKM))
                        }
                        if (arrivalTime.plate.isNotEmpty()) {
                            etaText.append(" ").append(arrivalTime.plate)
                        }
                        if (arrivalTime.capacity >= 0) {
                            val drawableId = when {
                                arrivalTime.capacity == 0L -> {
                                    R.drawable.ic_capacity_0_black
                                }
                                arrivalTime.capacity in 1..3 -> {
                                    R.drawable.ic_capacity_20_black
                                }
                                arrivalTime.capacity in 4..6 -> {
                                    R.drawable.ic_capacity_50_black
                                }
                                arrivalTime.capacity in 7..9 -> {
                                    R.drawable.ic_capacity_80_black
                                }
                                arrivalTime.capacity >= 10 -> {
                                    R.drawable.ic_capacity_100_black
                                }
                                else -> 0
                            }
                            var drawable: Drawable? = ContextCompat.getDrawable(requireContext(), drawableId)
                            val capacity = when {
                                arrivalTime.capacity == 0L -> {
                                    getString(R.string.capacity_empty)
                                }
                                arrivalTime.capacity in 1..3 -> {
                                    "¼"
                                }
                                arrivalTime.capacity in 4..6 -> {
                                    "½"
                                }
                                arrivalTime.capacity in 7..9 -> {
                                    "¾"
                                }
                                arrivalTime.capacity >= 10 -> {
                                    getString(R.string.capacity_full)
                                }
                                else -> ""
                            }
                            if (drawable != null) {
                                drawable = DrawableCompat.wrap(drawable)
                                drawable?.setBounds(0, 0, vh.etaText?.lineHeight?:0, vh.etaText?.lineHeight?:0)
                                DrawableCompat.setTint(drawable.mutate(), colorInt)
                                val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
                                etaText.append(" ")
                                if (etaText.isNotEmpty()) {
                                    etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                                }
                            }
                            if (capacity.isNotEmpty()) {
                                etaText.append(capacity)
                            }
                        }
                        if (arrivalTime.hasWheelchair) {
                            var drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_accessible_18dp)
                            drawable = DrawableCompat.wrap(drawable!!)
                            drawable?.setBounds(0, 0, vh.etaText?.lineHeight?:0, vh.etaText?.lineHeight?:0)
                            DrawableCompat.setTint(drawable.mutate(), colorInt)
                            val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
                            etaText.append(" ")
                            if (etaText.isNotEmpty()) {
                                etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                            }
                        }
                        if (arrivalTime.hasWifi) {
                            var drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_wifi_18dp)
                            if (drawable != null) {
                                drawable = DrawableCompat.wrap(drawable)
                                drawable?.setBounds(0, 0, vh.etaText?.lineHeight?:0, vh.etaText?.lineHeight?:0)
                                DrawableCompat.setTint(drawable.mutate(), colorInt)
                                val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
                                etaText.append(" ")
                                if (etaText.isNotEmpty()) {
                                    etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                                }
                            }
                        }
                        if (arrivalTime.note.isNotEmpty()) {
                            etaText.append(" ").append(arrivalTime.note)
                        }
                        if (etaText.isNotEmpty()) {
                            etaText.setSpan(ForegroundColorSpan(colorInt), 0, etaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (vh.etaText?.text?.isEmpty() == true) {
                            vh.etaText?.text = etaText
                        } else {
                            etaText.insert(0, "\n")
                            etaText.insert(0, vh.etaText?.text)
                            vh.etaText?.text = etaText
                        }

                        val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM", Locale.ENGLISH)
                        if (arrivalTime.generatedAt > 0) {
                            val date = Date(arrivalTime.generatedAt)
                            vh.etaServerTimeText?.text = dateFormat.format(date)
                        }
                        if (arrivalTime.updatedAt > 0) {
                            val date = Date(arrivalTime.updatedAt)
                            vh.etaLastUpdateText?.text = dateFormat.format(date)
                        }
                        if (vh.etaServerTimeText?.text == vh.etaLastUpdateText?.text) {
                            vh.etaServerTimeText?.text = null
                        }

                        vh.etaView?.visibility = View.VISIBLE
                    }
                }
            })

            // Stop raw
            vh.stopImage?.visibility = View.GONE
            vh.stopImageButton?.visibility = View.GONE
            if (!routeStop?.imageUrl.isNullOrEmpty()) {
                val tag = "StopImage_${routeStop?.imageUrl}"
                WorkManager.getInstance().cancelAllWorkByTag(tag)
                val request = OneTimeWorkRequest.Builder(ImageDownloadWorker::class.java)
                        .setInputData(Data.Builder().putString("url", routeStop?.imageUrl).build())
                        .addTag(tag)
                        .build()
                if (preferences != null && preferences.getBoolean("load_stop_image", false)) {
                    WorkManager.getInstance().enqueue(request)
                } else {
                    vh.stopImageButton?.visibility = View.VISIBLE
                    vh.stopImageButton?.setOnClickListener {
                        WorkManager.getInstance().enqueue(request)
                    }
                }
                WorkManager.getInstance().getWorkInfoByIdLiveData(request.id).observe(this,
                        { workInfo ->
                            if (vh.stopImage != null) {
                                if (workInfo?.state == WorkInfo.State.FAILED) {
                                    vh.stopImage?.visibility = View.GONE
                                    showSnackbar(getString(R.string.message_fail_to_request))
                                }
                                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                                    try {
                                        vh.stopImage?.setImageBitmap(BitmapFactory.decodeFile(workInfo.outputData.getString("filepath")))
                                        vh.stopImage?.visibility = View.VISIBLE
                                        vh.stopImageButton?.visibility = View.GONE
                                    } catch (e: Throwable) {
                                        Timber.e(e)
                                        vh.stopImage?.visibility = View.GONE
                                    }
                                }
                            }
                        })
            }
        }
    }

    private fun updateDistanceDisplay() {
        if (routeStop?.latitude.isNullOrEmpty() || routeStop?.longitude.isNullOrEmpty()) return
        val location = Location("")
        location.latitude = routeStop?.latitude?.toDouble()?:0.0
        location.longitude = routeStop?.longitude?.toDouble()?:0.0
        if (currentLocation != null) {
            val distance = currentLocation!!.distanceTo(location)
            vh.distanceText?.text = DecimalFormat("~#.##km").format((distance / 1000).toDouble())
        }
    }

    private fun openLink(context: Context, url: String, @ColorInt colorInt: Int) {
        val link = Uri.parse(url)
        try {
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(colorInt)
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, link)
        } catch (ignored: Throwable) {
            val intent = Intent(Intent.ACTION_VIEW, link)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }

    private class ViewHolder {
        var contentView: View? = null
        var headerLayout: RelativeLayout? = null

        var stopImage: ImageView? = null
        var stopImageButton: MaterialButton? = null

        var coordinatorLayout: View? = null
        var followButton: MaterialButton? = null
        var mapButton: MaterialButton? = null
        var notificationButton: MaterialButton? = null
        var streetviewButton: MaterialButton? = null
        var arrivalAlertButton: MaterialButton? = null

        var nameText: TextView? = null
        var routeNoText: TextView? = null
        var routeLocationText: TextView? = null
        var stopLocationText: TextView? = null
        var fareText: TextView? = null
        var distanceText: TextView? = null
        var etaView: View? = null
        var etaText: TextView? = null
        var etaServerTimeText: TextView? = null
        var etaLastUpdateText: TextView? = null

        var mapView: MapView? = null
    }

    companion object {

        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route, routeStop: RouteStop): RouteStopFragment {
            val fragment = RouteStopFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(follow: Follow): RouteStopFragment {
            val fragment = RouteStopFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.FOLLOW_OBJECT, follow)
            fragment.arguments = args
            return fragment
        }
    }
}
