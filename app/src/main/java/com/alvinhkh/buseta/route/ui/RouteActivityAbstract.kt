package com.alvinhkh.buseta.route.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.DataSetObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Guideline
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.datagovhk.RtNwstWorker
import com.alvinhkh.buseta.kmb.KmbRouteWorker
import com.alvinhkh.buseta.kmb.ui.KmbStopListFragment
import com.alvinhkh.buseta.lwb.LwbRouteWorker
import com.alvinhkh.buseta.lwb.ui.LwbStopListFragment
import com.alvinhkh.buseta.mtr.ui.MtrBusStopListFragment
import com.alvinhkh.buseta.mtr.ui.MtrStationListFragment
import com.alvinhkh.buseta.nlb.ui.NlbStopListFragment
import com.alvinhkh.buseta.nwst.NwstRouteWorker
import com.alvinhkh.buseta.nwst.ui.NwstStopListFragment
import com.alvinhkh.buseta.route.UpdateAppShortcutWorker
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.LatLong
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.service.EtaService
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.ui.setting.SettingActivity
import com.alvinhkh.buseta.utils.AdViewUtil
import com.alvinhkh.buseta.utils.ColorUtil
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.PreferenceUtil
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.maps.android.ui.IconGenerator
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

abstract class RouteActivityAbstract : BaseActivity(),
        OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    lateinit var arrivalTimeDatabase: ArrivalTimeDatabase

    lateinit var routeDatabase: RouteDatabase

    lateinit var suggestionDatabase: SuggestionDatabase

    lateinit var firebaseAnalytics: FirebaseAnalytics

    /**
     * The [ViewPager] that will host the section contents.
     */
    lateinit var viewPager: ViewPager

    lateinit var pagerAdapter: RoutePagerAdapter

    lateinit var tabLayout: TabLayout

    lateinit var fab: FloatingActionButton

    lateinit var appBarLayout: AppBarLayout

    lateinit var emptyView: View

    lateinit var progressBar: ProgressBar

    lateinit var emptyText: TextView

    private var showMapMenuItem: MenuItem? = null

    private var mapFragment: SupportMapFragment? = null

    private var map: GoogleMap? = null

    private var currentRoute: Route? = null

    private var suggestion: Suggestion? = null

    private var isScrolledToPage = false

    private var fragNo: Int = 0

    private var companyCode: String? = null

    private var routeNo: String? = null

    private var stopFromIntent: RouteStop? = null

    private var stopIdFromIntent: String? = null

    private var requestId: UUID? = null

    private var isShowMap: Boolean = false

    private val markerMap = HashMap<String, Marker>()

    private val mapHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras
        if (bundle != null) {
            companyCode = bundle.getString(C.EXTRA.COMPANY_CODE)
            routeNo = bundle.getString(C.EXTRA.ROUTE_NO)
            stopFromIntent = bundle.getParcelable(C.EXTRA.STOP_OBJECT)
            stopIdFromIntent = bundle.getString(C.EXTRA.STOP_ID)
        }
        if (routeNo.isNullOrEmpty() && stopFromIntent != null) {
            companyCode = stopFromIntent?.companyCode
            routeNo = stopFromIntent?.routeNo
        }

        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(this)!!
        routeDatabase = RouteDatabase.getInstance(this)!!
        suggestionDatabase = SuggestionDatabase.getInstance(this)!!
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        WorkManager.getInstance().cancelAllWorkByTag("RouteList")

        setContentView(R.layout.activity_route)

        // set action bar
        setToolbar()
        val actionBar = supportActionBar
        actionBar?.title = getString(R.string.app_name)
        actionBar?.subtitle = null
        actionBar?.setDisplayHomeAsUpEnabled(true)

        adViewContainer = findViewById(R.id.adView_container)
        with(adViewContainer) {
            adView = AdViewUtil.banner(this)
        }
        fab = findViewById(R.id.fab)

        emptyView = findViewById(android.R.id.empty)
        progressBar = findViewById(R.id.progressBar)
        progressBar.isIndeterminate = true
        emptyText = findViewById(R.id.empty_text)
        showLoadingView()

        // Disable vertical scroll
        appBarLayout = findViewById(R.id.app_bar_layout)
        appBarLayout.setExpanded(true)
        val params = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = AppBarLayout.Behavior()
        behavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
        params.behavior = behavior

        // Create the adapter that will return a fragment
        pagerAdapter = RoutePagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = pagerAdapter
        tabLayout = findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.addOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (!companyCode.isNullOrEmpty() && !routeNo.isNullOrEmpty()) {
                    val fragment = RouteSelectDialogFragment.newInstance(companyCode ?: "", routeNo
                            ?: "", WeakReference(viewPager))
                    fragment.show(supportFragmentManager, "route_select_dialog_fragment")
                }
            }
        })
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                currentRoute = pagerAdapter.routeList[position]
                if (isShowMap) {
                    mapHandler.removeCallbacksAndMessages(null)
                    mapHandler.postDelayed({
                        loadMapMarkers(currentRoute!!)
                    }, 200)
                }
            }
        })

        pagerAdapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                if (pagerAdapter.count > 0) {
                    emptyView.visibility = View.GONE
                    if (pagerAdapter.count > 2) {
                        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
                    } else {
                        tabLayout.tabMode = TabLayout.MODE_FIXED
                    }
                }
            }
        })

        if (routeNo?.isNotBlank() == true) {
            loadRoute(companyCode ?: "", routeNo ?: "")
        } else {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        isShowMap = preferences.getBoolean("load_map", false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_route, menu)
        showMapMenuItem = menu?.findItem(R.id.action_show_map)
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        showMapMenuItem?.isVisible = preferences == null || !preferences.getBoolean("load_map", false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                when (companyCode) {
                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                        val closeIntent = Intent(this, SettingActivity::class.java)
                        closeIntent.putExtra("close_RouteActivityAbstract", object : ResultReceiver(null) {
                            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                                if (resultCode == RESULT_OK) {
                                    val newIntent = intent
                                    finish()
                                    startActivity(newIntent)
                                }
                            }
                        })
                        startActivityForResult(closeIntent, 1)
                        return true
                    }
                }
            }
            R.id.action_refresh -> {
                val timeNow = System.currentTimeMillis() / 1000
                val cCode = companyCode ?: ""
                val rNo = routeNo ?: ""
                val provider = if (PreferenceUtil.isUsingNwstDataGovHkApi(applicationContext)) {
                    C.PROVIDER.DATAGOVHK_NWST
                } else {
                    ""
                }
                routeDatabase.routeDao().deleteBySource(provider, cCode, rNo, timeNow)
                routeDatabase.routeStopDao().deleteBySource(provider, cCode, rNo, timeNow)
                loadRoute(cCode, rNo)
                // Returning true to avoid redundant action_refresh in RouteStopListFragmentAbstract.onOptionsItemSelected
                return true
            }
            R.id.action_show_map -> {
                isShowMap = !isShowMap
                if (viewPager.currentItem >= 0 && viewPager.currentItem < pagerAdapter.routeList.size) {
                    currentRoute = pagerAdapter.routeList[viewPager.currentItem]
                }
                showMapFragment()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onResume() {
        super.onResume()
        if (suggestion != null) {
            appIndexStart(suggestion!!)
        }
    }

    public override fun onPause() {
        super.onPause()
        WorkManager.getInstance().enqueue(
                OneTimeWorkRequest.Builder(UpdateAppShortcutWorker::class.java)
                        .addTag("AppShortcut").build()
        )
    }

    override fun onDestroy() {
        if (suggestion != null) {
            appIndexStop(suggestion!!)
        }
        super.onDestroy()
    }

    private fun showEmptyView() {
        emptyView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        emptyText.setText(R.string.message_fail_to_request)
        fab.hide()
    }

    private fun showLoadingView() {
        emptyView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        emptyText.setText(R.string.message_loading)
        fab.hide()
    }

    protected open fun loadRoute(companyCode: String, routeNo: String) {
        if (companyCode.isEmpty() or routeNo.isEmpty()) {
            showEmptyView()
            return
        }
        showLoadingView()
        // load route from database
        val viewModel = ViewModelProvider(this).get(RouteViewModel::class.java)
        val liveData = viewModel.getAsLiveData(companyCode, routeNo)
        liveData.observe(this, { routes ->
            pagerAdapter.clear()
            var company = ""
            var isScrollToPage = false
            var hasDestination = false
            routes?.forEach { route ->
                company = route.companyCode ?: companyCode
                var navStop = stopFromIntent
                if (navStop == null && !stopIdFromIntent.isNullOrEmpty()) {
                    navStop = RouteStop()
                    navStop.companyCode = route.companyCode
                    navStop.routeNo = route.name
                    navStop.routeSequence = route.sequence
                    navStop.routeServiceType = route.serviceType
                    navStop.stopId = stopIdFromIntent
                }
                val fragment: Fragment = when (company) {
                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> NwstStopListFragment.newInstance(route, navStop)
                    C.PROVIDER.KMB, C.PROVIDER.LWB -> if (PreferenceUtil.isUsingKmbWebApi(applicationContext)) {
                        KmbStopListFragment.newInstance(route, navStop)
                    } else {
                        LwbStopListFragment.newInstance(route, navStop)
                    }
                    C.PROVIDER.LRTFEEDER -> MtrBusStopListFragment.newInstance(route, navStop)
                    C.PROVIDER.MTR -> MtrStationListFragment.newInstance(route, navStop)
                    C.PROVIDER.NLB, C.PROVIDER.GMB901 -> NlbStopListFragment.newInstance(route, navStop)
                    else -> return@forEach
                }
                val pageTitle = if (!route.origin.isNullOrEmpty()) {
                    ((if (route.origin.isNullOrEmpty()) "" else route.origin!! + if (routes.size > 1) "\n" else " ")
                            + (if (!route.destination.isNullOrEmpty()) getString(R.string.destination, route.destination) else "")
                            + if (route.isSpecial!!) "#" else "")
                } else {
                    route.name ?: getString(R.string.route)
                }
                if (pageTitle != route.name && pageTitle != getString(R.string.route)) {
                    hasDestination = true
                }
                pagerAdapter.addFragment(fragment, pageTitle, route)
                val fragmentCount = pagerAdapter.count
                if (navStop != null
                        && route.companyCode == navStop.companyCode
                        && route.sequence == navStop.routeSequence
                        && route.serviceType == navStop.routeServiceType
                        && (navStop.routeId.isNullOrBlank() || route.code == navStop.routeId)
                ) {
                    fragNo = fragmentCount - 1
                    isScrollToPage = true
                }
                activityColor(route.companyColour(this))
            }

            val routeName = Route.companyName(this, company, routeNo) + " " + routeNo
            supportActionBar?.title = routeName
            tabLayout.visibility = if (hasDestination) View.VISIBLE else View.GONE
            if (routes?.isNotEmpty() == true && !company.isEmpty() && company != C.PROVIDER.MTR) {
                suggestion = Suggestion.createInstance()
                suggestion?.companyCode = company
                suggestion?.route = routeNo
                suggestion?.type = Suggestion.TYPE_HISTORY
                if (suggestion != null) {
                    suggestionDatabase.suggestionDao().insert(suggestion!!)
                    appIndexStart(suggestion!!)
                }
            }

            if (isScrollToPage && !isScrolledToPage) {
                viewPager.setCurrentItem(fragNo, false)
                isScrolledToPage = true
                isScrollToPage = false
            }
            if (isShowMap) {
                showMapFragment()
                if (fragNo - 1 < 1) {
                    fragNo = 0
                }
                if (viewPager.currentItem == fragNo && viewPager.currentItem < pagerAdapter.routeList.size) {
                    currentRoute = pagerAdapter.routeList[viewPager.currentItem]
                    mapHandler.removeCallbacksAndMessages(null)
                    mapHandler.postDelayed({
                        loadMapMarkers(currentRoute!!)
                    }, 200)
                }
            }

            if (pagerAdapter.count > 0) {
                emptyView.visibility = View.GONE
                liveData.removeObservers(this)
            }
        })
        var loadStop = true
        when (companyCode) {
            C.PROVIDER.LRTFEEDER, C.PROVIDER.MTR, C.PROVIDER.NLB, C.PROVIDER.GMB901 -> return
            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                if (PreferenceUtil.isUsingNwstDataGovHkApi(applicationContext)) {
                    if (routeDatabase.routeDao().count(arrayListOf(C.PROVIDER.DATAGOVHK_NWST), companyCode, routeNo) > 0) return
                } else {
                    if (routeDatabase.routeDao().count(arrayListOf(""), companyCode, routeNo) > 0) {
                        loadStop = false
                        routeDatabase.routeDao().deleteBySource("", companyCode, routeNo, System.currentTimeMillis() / 1000)
                    }
                }
            }
        }

        val data = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putBoolean(C.EXTRA.LOAD_STOP, loadStop)
                .build()
        val request = when (companyCode) {
            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                OneTimeWorkRequest.Builder(
                        if (PreferenceUtil.isUsingNwstDataGovHkApi(applicationContext)) {
                            RtNwstWorker::class.java
                        } else {
                            NwstRouteWorker::class.java
                        })
                        .setInputData(data)
                        .addTag("RouteList")
                        .build()
            }
            C.PROVIDER.KMB, C.PROVIDER.LWB -> {
                OneTimeWorkRequest.Builder(
                        if (PreferenceUtil.isUsingKmbWebApi(applicationContext)) {
                            KmbRouteWorker::class.java
                        } else {
                            LwbRouteWorker::class.java
                        })
                        .setInputData(data)
                        .addTag("RouteList")
                        .build()
            }
            else -> return
        }
        if (requestId != null) {
            WorkManager.getInstance().cancelWorkById(requestId!!)
            requestId = null
        }
        requestId = request.id
        WorkManager.getInstance().enqueue(request)

        WorkManager.getInstance().getWorkInfoByIdLiveData(request.id)
                .observe(this, Observer { workInfo ->
                    // val workerResult = workInfo?.outputData
                    // val companyCode = workerResult?.getString(C.EXTRA.COMPANY_CODE)
                    // val routeNo = workerResult?.getString(C.EXTRA.ROUTE_NO)
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        // showEmptyView()
                        if (!ConnectivityUtil.isConnected(applicationContext)) {
                            emptyText.setText(R.string.message_no_internet_connection)
                        } else {
                            emptyText.setText(R.string.message_fail_to_request)
                        }
                    }
                })
    }

    private fun showMapFragment() {
        when (companyCode) {
            C.PROVIDER.MTR -> {
                showMapMenuItem?.isVisible = false
                isShowMap = false
            }
        }
        val lp = findViewById<Toolbar>(R.id.toolbar).layoutParams as ViewGroup.MarginLayoutParams
        lp.setMargins(0, 0, 0, 0)
        val guideline = findViewById<Guideline>(R.id.guideline)
        if (!isShowMap) {
            findViewById<FrameLayout>(R.id.map).visibility = View.GONE
            if (mapFragment != null) {
                val ft = supportFragmentManager.beginTransaction()
                ft.remove(mapFragment!!)
                ft.commitAllowingStateLoss()
            }
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                guideline?.setGuidelinePercent(0.0f)
            }
        } else {
            mapFragment = SupportMapFragment.newInstance()
            if (mapFragment != null && !mapFragment!!.isAdded) {
                mapFragment!!.getMapAsync(this)
                supportFragmentManager.beginTransaction().replace(R.id.map, mapFragment!!).commit()
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    guideline?.setGuidelinePercent(0.4f)
                } else {
                    lp.setMargins(0, 0, 0, 440)
                }
            }
        }
        findViewById<Toolbar>(R.id.toolbar).layoutParams = lp
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap == null) return
        map = googleMap
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(22.3964, 114.1095), 10f))
        val options = GoogleMapOptions()
        options.mapToolbarEnabled(false)
        options.compassEnabled(true)
        options.rotateGesturesEnabled(true)
        options.scrollGesturesEnabled(false)
        options.tiltGesturesEnabled(true)
        options.zoomControlsEnabled(false)
        options.zoomGesturesEnabled(true)
        googleMap.isBuildingsEnabled = false
        googleMap.isIndoorEnabled = false
        googleMap.isTrafficEnabled = false
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMyLocationButtonClickListener {
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(googleMap.myLocation.latitude, googleMap.myLocation.longitude)))
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(16f))
            true
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
        if (currentRoute != null) {
            mapHandler.removeCallbacksAndMessages(null)
            mapHandler.postDelayed({
                loadMapMarkers(currentRoute!!)
            }, 200)
        }
        findViewById<FrameLayout>(R.id.map).visibility = View.VISIBLE
        appBarLayout.setExpanded(true, true)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        try {
            if (viewPager.currentItem >= 0 && viewPager.currentItem < pagerAdapter.routeList.size) {
                val fragment = viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem)
                if (fragment is RouteStopListFragmentAbstract) {
                    fragment.onMarkerClick(marker)
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
        }
        if (marker.tag is RouteStop) {
            marker.showInfoWindow()
            val routeStop = marker.tag as RouteStop? ?: return false
            val lat = routeStop.latitude?.toDouble()?:0.0
            val lng = routeStop.longitude?.toDouble()?:0.0
            if (lat != 0.0 && lng != 0.0) {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f))
            }
            try {
                val intent = Intent(this, EtaService::class.java)
                intent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                startService(intent)
            } catch (ignored: IllegalStateException) {
            }
            return true
        }
        return false
    }

    open fun mapCamera(latitude: Double?, longitude: Double?) {
        if (isShowMap && latitude?.isFinite() == true && longitude?.isFinite() == true && latitude != 0.0 && longitude != 0.0) {
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 18f))
        }
    }

    private fun markerIconFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun loadMapMarkers(route: Route) {
        val routeStopLiveData = routeDatabase.routeStopDao().liveData(route.companyCode
                ?: "", route.code ?: "", route.name ?: "", route.sequence ?: "", route.serviceType
                ?: "")
        routeStopLiveData.removeObservers(this)
        routeStopLiveData.observe(this, { list ->
            map?.clear()
            val mapCoordinates: MutableList<LatLong> = route.mapCoordinates
            val hasMapCoordinates = mapCoordinates.size > 0
            if (!hasMapCoordinates) {
                mapCoordinates.clear()
            }
            list?.forEachIndexed { index, routeStop ->
                if (!routeStop.latitude.isNullOrEmpty() && !routeStop.longitude.isNullOrEmpty()) {
                    val lat = routeStop.latitude?.toDouble() ?: 0.0
                    val lng = routeStop.longitude?.toDouble() ?: 0.0
                    if (lat != 0.0 && lng != 0.0) {
                        if (!hasMapCoordinates) {
                            mapCoordinates.add(LatLong(lat, lng))
                        }
                        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_twotone_directions_bus_18dp)
                        val marker = map?.addMarker(MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(routeStop.sequence + ": " + routeStop.name)
                                .icon(markerIconFromDrawable(drawable!!))
                        )
                        marker?.tag = routeStop
                        if (index == 0) {
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14f))
                        }
                    }
                }
            }
            val singleLine = PolylineOptions().width(20f).zIndex(1f)
                    .color(ContextCompat.getColor(this, R.color.grey))
                    .startCap(RoundCap()).endCap(RoundCap())
            for (latlong in mapCoordinates) {
                singleLine.add(LatLng(latlong.latitude, latlong.longitude))
            }
            if (mapCoordinates.size > 0) {
                map?.addPolyline(singleLine)
                routeStopLiveData.removeObservers(this@RouteActivityAbstract)
            }
        })

        val liveData = arrivalTimeDatabase.arrivalTimeDao().getLiveData(route.companyCode
                ?: "", route.name ?: "", route.sequence ?: "")
        liveData.removeObservers(this)
        liveData.observe(this, { list ->
            for (marker in markerMap.values.toTypedArray()) {
                marker.remove()
            }
            markerMap.clear()
            list?.forEach { arrivalTime ->
                if (arrivalTime.latitude != 0.0 && arrivalTime.longitude != 0.0) {
                    if (arrivalTime.plate.isNotEmpty()) {
                        val iconFactory = IconGenerator(applicationContext)
                        val bmp = iconFactory.makeIcon(arrivalTime.plate)
                        if (markerMap.containsKey(arrivalTime.plate)) {
                            markerMap[arrivalTime.plate]?.remove()
                            markerMap.remove(arrivalTime.plate)
                        }
                        if (map != null) {
                            val marker = map!!.addMarker(MarkerOptions()
                                    .position(LatLng(arrivalTime.latitude, arrivalTime.longitude))
                                    .icon(BitmapDescriptorFactory.fromBitmap(bmp)))
                            marker.tag = arrivalTime
                            markerMap[arrivalTime.plate] = marker
                        }
                    }
                }
            }
        })
    }

    private fun getIndexApiAction(suggestion: Suggestion): Action {
        return Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(suggestion.route, Uri.parse(C.URI.ROUTE).buildUpon()
                        .appendPath(suggestion.route).build().toString())
                // Keep action data for personal content on the device
                //.setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build()
    }

    private fun appIndexStart(suggestion: Suggestion) {
        if (suggestion.route.isEmpty()) return
        FirebaseUserActions.getInstance().start(getIndexApiAction(suggestion))
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Timber.d("App Indexing: Recorded start successfully")
                    } else {
                        Timber.d("App Indexing: fail")
                    }
                }
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.METHOD, "search")
        bundle.putString(FirebaseAnalytics.Param.CONTENT, "${suggestion.companyCode} ${suggestion.route}")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "route")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
    }

    private fun appIndexStop(suggestion: Suggestion) {
        if (suggestion.route.isEmpty()) return
        FirebaseUserActions.getInstance().end(getIndexApiAction(suggestion))
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Timber.d("App Indexing: Recorded end successfully")
                    } else {
                        Timber.d("App Indexing: fail")
                    }
                }
    }

    private fun activityColor(color: Int) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            window?.statusBarColor = color
            window?.navigationBarColor = ContextCompat.getColor(this, R.color.transparent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window?.statusBarColor = ColorUtil.darkenColor(color)
            window?.navigationBarColor = ColorUtil.darkenColor(color)
        }
        findViewById<AppBarLayout>(R.id.app_bar_layout)?.setBackgroundColor(color)
        findViewById<FrameLayout>(R.id.map)?.setBackgroundColor(color)
        findViewById<FrameLayout>(R.id.adView_container)?.setBackgroundColor(color)
        findViewById<TabLayout>(R.id.tabs)?.background = ColorDrawable(color)
        findViewById<FloatingActionButton>(R.id.fab)?.backgroundTintList = ColorStateList.valueOf(color)
    }
}
