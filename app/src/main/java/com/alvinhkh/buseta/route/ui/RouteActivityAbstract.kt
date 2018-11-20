package com.alvinhkh.buseta.route.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.ui.MtrBusStopListFragment
import com.alvinhkh.buseta.kmb.KmbRouteWorker
import com.alvinhkh.buseta.kmb.ui.KmbStopListFragment
import com.alvinhkh.buseta.lwb.LwbRouteWorker
import com.alvinhkh.buseta.lwb.ui.LwbStopListFragment
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.model.RouteStop
import com.alvinhkh.buseta.mtr.AESBusRouteWorker
import com.alvinhkh.buseta.mtr.LrtFeederRouteWorker
import com.alvinhkh.buseta.mtr.ui.AESBusStopListFragment
import com.alvinhkh.buseta.nlb.NlbRouteWorker
import com.alvinhkh.buseta.nlb.ui.NlbStopListFragment
import com.alvinhkh.buseta.nwst.NwstRouteWorker
import com.alvinhkh.buseta.nwst.ui.NwstStopListFragment
import com.alvinhkh.buseta.route.UpdateAppShortcutWorker
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.ui.route.RouteSelectDialogFragment
import com.alvinhkh.buseta.utils.AdViewUtil
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.PreferenceUtil
import com.alvinhkh.buseta.utils.RouteUtil
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions

import org.osmdroid.views.MapView

import io.reactivex.disposables.CompositeDisposable
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber
import java.util.*

abstract class RouteActivityAbstract : BaseActivity() {

    private val disposables = CompositeDisposable()

    /**
     * The [PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [FragmentStatePagerAdapter].
     */
    lateinit var pagerAdapter: RoutePagerAdapter

    /**
     * The [ViewPager] that will host the section contents.
     */
    lateinit var viewPager: ViewPager

    lateinit var fab: FloatingActionButton

    lateinit var appBarLayout: AppBarLayout

    lateinit var emptyView: View

    lateinit var progressBar: ProgressBar

    lateinit var emptyText: TextView

    lateinit var mapView: MapView

    private var suggestion: Suggestion? = null

    private var isScrollToPage: Boolean? = false

    private var fragNo: Int? = 0

    private var companyCode: String? = null

    private var routeNo: String? = null

    private var stopFromIntent: RouteStop? = null

    private var requestId: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras
        if (bundle != null) {
            companyCode = bundle.getString(C.EXTRA.COMPANY_CODE)
            routeNo = bundle.getString(C.EXTRA.ROUTE_NO)
            stopFromIntent = bundle.getParcelable(C.EXTRA.STOP_OBJECT)
        }
        if (routeNo.isNullOrEmpty() && stopFromIntent != null) {
            companyCode = stopFromIntent?.companyCode
            routeNo = stopFromIntent?.routeNo
        }

        val suggestionDatabase = SuggestionDatabase.getInstance(this)!!

        setContentView(R.layout.activity_route)

        // set action bar
        setToolbar()
        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.app_name)
        actionBar?.subtitle = null
        actionBar?.setDisplayHomeAsUpEnabled(true)
        appBarLayout = findViewById(R.id.app_bar_layout)
        appBarLayout.setExpanded(true)

        adViewContainer = findViewById(R.id.adView_container)
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false)
        }
        fab = findViewById(R.id.fab)

        emptyView = findViewById(android.R.id.empty)
        progressBar = findViewById(R.id.progressBar)
        progressBar.isIndeterminate = true
        emptyText = findViewById(R.id.empty_text)
        showLoadingView()

        // TODO: map show route
        mapView = findViewById(R.id.map)
        mapView.visibility = View.GONE
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true
        mapView.maxZoomLevel = 20.0
        mapView.minZoomLevel = 14.0
        val mapController = mapView.controller
        mapController?.setZoom(10.0)
        mapController?.setCenter(GeoPoint(22.396428, 114.109497))

        val rotationOverlay = RotationGestureOverlay(mapView)
        rotationOverlay.isEnabled = true
        mapView.setMultiTouchControls(true)
        mapView.overlays?.add(rotationOverlay)

        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(applicationContext), mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays?.add(locationOverlay)

        val compassOverlay = CompassOverlay(applicationContext, InternalCompassOrientationProvider(applicationContext), mapView)
        compassOverlay.enableCompass()
        mapView.overlays?.add(compassOverlay)
//        mapView.overlays?.add(new CopyrightOverlay(getContext()));

        // Disable vertical scroll
        val appBarLayout: AppBarLayout = findViewById(R.id.app_bar_layout)
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

        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.addOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val routes = ArrayList(pagerAdapter.routeList)
                val fragment = RouteSelectDialogFragment.newInstance(routes, viewPager)
                fragment.show(supportFragmentManager, "route_select_dialog_fragment")
            }
        })

        pagerAdapter.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                if (pagerAdapter.count > 0) {
                    emptyView.visibility = View.GONE
                } else {
                    showEmptyView()
                }
            }
        })

        if (routeNo?.isNotBlank() == true) {
            loadRouteNo(companyCode?:"", routeNo?:"")
        } else {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // load route from database
        val viewModel = ViewModelProviders.of(this).get(RouteViewModel::class.java)
        viewModel.getAsLiveData(companyCode?:"", routeNo?:"")
                .observe(this, Observer<MutableList<Route>> { routes ->
                    pagerAdapter.clear()
                    var company = ""
                    routes?.forEach { route ->
                        company = route.companyCode?:companyCode?:""
                        val routeStop = stopFromIntent?:RouteStop()
                        val fragment = when (company) {
                            C.PROVIDER.AESBUS -> AESBusStopListFragment.newInstance(route, routeStop)
                            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> NwstStopListFragment.newInstance(route, routeStop)
                            C.PROVIDER.LRTFEEDER -> MtrBusStopListFragment.newInstance(route, routeStop)
                            C.PROVIDER.NLB -> NlbStopListFragment.newInstance(route, routeStop)
                            C.PROVIDER.KMB -> if (PreferenceUtil.isUsingNewKmbApi(applicationContext)) {
                                KmbStopListFragment.newInstance(route, routeStop)
                            } else {
                                LwbStopListFragment.newInstance(route, routeStop)
                            }
                            else -> return@forEach
                        }
                        val pageTitle = if (!route.origin.isNullOrEmpty()) {
                             ((if (route.origin.isNullOrEmpty()) "" else route.origin!! + if (routes.size > 1) "\n" else " ")
                                     + (if (!route.destination.isNullOrEmpty()) getString(R.string.destination, route.destination) else "")
                                     + if (route.isSpecial!!) "#" else "")
                        } else {
                            route.name?:getString(R.string.route)
                        }
                        pagerAdapter.addFragment(fragment, pageTitle, route)
                        val fragmentCount = pagerAdapter.count
                        if (stopFromIntent != null && route.companyCode != null
                                && route.sequence != null && route.serviceType != null
                                && route.companyCode == stopFromIntent?.companyCode
                                && route.sequence == stopFromIntent?.routeSeq
                                && route.serviceType == stopFromIntent?.routeServiceType) {
                            fragNo = fragmentCount
                            isScrollToPage = true
                        }
                    }

                    val routeName = RouteUtil.getCompanyName(this, company, routeNo) + " " + routeNo
                    supportActionBar?.title = routeName
                    if (routes?.isNotEmpty() == true && !TextUtils.isEmpty(company)) {
                        suggestion = Suggestion.createInstance()
                        suggestion?.companyCode = company
                        suggestion?.route = routeNo?:""
                        suggestion?.type = Suggestion.TYPE_HISTORY
                        if (suggestion != null) {
                            suggestionDatabase.suggestionDao().insert(suggestion!!)
                            appIndexStart(suggestion!!)
                        }
                    }

                    if (isScrollToPage!!) {
                        if (fragNo!! - 1 >= 0) {
                            viewPager.setCurrentItem(fragNo!! - 1, false)
                        }
                        isScrollToPage = false
                    }
                })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> loadRouteNo(companyCode?:"", routeNo?:"")
            R.id.action_show_map -> appBarLayout.setExpanded(true)
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
        WorkManager.getInstance()
                .enqueue(OneTimeWorkRequest.Builder(UpdateAppShortcutWorker::class.java).build())
    }

    override fun onDestroy() {
        disposables.clear()
        if (suggestion != null) {
            appIndexStop(suggestion!!)
        }
        super.onDestroy()
    }

    protected fun showEmptyView() {
        emptyView.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        emptyText.setText(R.string.message_fail_to_request)
        fab.hide()
    }

    protected fun showLoadingView() {
        emptyView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        emptyText.setText(R.string.message_loading)
        fab.hide()
    }

    protected open fun loadRouteNo(companyCode: String, no: String) {
        if (companyCode.isEmpty() or no.isEmpty()) {
            showEmptyView()
            return
        }
        showLoadingView()

        val data = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_NO, no)
                .build()
        val request = when (companyCode) {
            C.PROVIDER.KMB -> {
                if (PreferenceUtil.isUsingNewKmbApi(applicationContext)) {
                    OneTimeWorkRequest.Builder(KmbRouteWorker::class.java)
                            .setInputData(data)
                            .build()
                } else {
                    OneTimeWorkRequest.Builder(LwbRouteWorker::class.java)
                            .setInputData(data)
                            .build()
                }
            }
            C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST ->
                OneTimeWorkRequest.Builder(NwstRouteWorker::class.java)
                        .setInputData(data)
                        .build()
            C.PROVIDER.NLB ->
                OneTimeWorkRequest.Builder(NlbRouteWorker::class.java)
                        .setInputData(data)
                        .build()
            C.PROVIDER.AESBUS ->
                OneTimeWorkRequest.Builder(AESBusRouteWorker::class.java)
                        .setInputData(data)
                        .build()
            C.PROVIDER.LRTFEEDER ->
                OneTimeWorkRequest.Builder(LrtFeederRouteWorker::class.java)
                        .setInputData(data)
                        .build()
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
                        showEmptyView()
                        if (!ConnectivityUtil.isConnected(applicationContext)) {
                            emptyText.setText(R.string.message_no_internet_connection)
                        } else {
                            emptyText.setText(R.string.message_fail_to_request)
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
        if (TextUtils.isEmpty(suggestion.route)) return
        FirebaseUserActions.getInstance().start(getIndexApiAction(suggestion))
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Timber.d("App Indexing: Recorded start successfully")
                    } else {
                        Timber.d("App Indexing: fail")
                    }
                }
        Answers.getInstance().logContentView(ContentViewEvent()
                .putContentName("search")
                .putContentType("route")
                .putCustomAttribute("route no", suggestion.route)
                .putCustomAttribute("company", suggestion.companyCode)
        )
    }

    private fun appIndexStop(suggestion: Suggestion) {
        if (TextUtils.isEmpty(suggestion.route)) return
        FirebaseUserActions.getInstance().end(getIndexApiAction(suggestion))
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Timber.d("App Indexing: Recorded end successfully")
                    } else {
                        Timber.d("App Indexing: fail")
                    }
                }
    }
}
