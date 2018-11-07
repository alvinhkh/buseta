package com.alvinhkh.buseta.ui.route

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.DataSetObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
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

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.model.Route
import com.alvinhkh.buseta.model.RouteStop
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.ui.BaseActivity
import com.alvinhkh.buseta.utils.AdViewUtil
import com.alvinhkh.buseta.utils.RouteStopUtil
import com.alvinhkh.buseta.utils.RouteUtil
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.gson.Gson

import org.osmdroid.views.MapView

import java.util.ArrayList

import io.reactivex.disposables.CompositeDisposable
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

abstract class RouteActivityAbstract : BaseActivity() {

    val disposables = CompositeDisposable()

    lateinit var followDatabase: FollowDatabase

    lateinit var suggestionDatabase: SuggestionDatabase

    /**
     * The [PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [FragmentStatePagerAdapter].
     */
    var pagerAdapter: RoutePagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    var viewPager: ViewPager? = null

    var fab: FloatingActionButton? = null

    var appBarLayout: AppBarLayout? = null

    var emptyView: View? = null

    var progressBar: ProgressBar? = null

    var emptyText: TextView? = null

    var mapView: MapView? = null

    var stopFromIntent: RouteStop? = null

    var routeNo: String? = null

    private var suggestion: Suggestion? = null

    private var isScrollToPage: Boolean? = false

    private var fragNo: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras
        if (bundle != null) {
            routeNo = bundle.getString(C.EXTRA.ROUTE_NO)
            stopFromIntent = bundle.getParcelable(C.EXTRA.STOP_OBJECT)
        }
        if (TextUtils.isEmpty(routeNo)) {
            if (stopFromIntent != null) {
                routeNo = stopFromIntent!!.routeNo
            }
        }

        followDatabase = FollowDatabase.getInstance(this)!!
        suggestionDatabase = SuggestionDatabase.getInstance(this)!!

        setContentView(R.layout.activity_route)

        // set action bar
        setToolbar()
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name)
            actionBar.subtitle = null
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        appBarLayout = findViewById(R.id.app_bar_layout)
        appBarLayout?.setExpanded(true)

        adViewContainer = findViewById(R.id.adView_container)
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false)
        }
        fab = findViewById(R.id.fab)

        emptyView = findViewById(android.R.id.empty)
        progressBar = findViewById(R.id.progressBar)
        progressBar?.isIndeterminate = true
        emptyText = findViewById(R.id.empty_text)
        showLoadingView()

        // TODO: map show route
        mapView = findViewById(R.id.map)
        mapView?.visibility = View.GONE
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setBuiltInZoomControls(false)
        mapView?.setMultiTouchControls(true)
        mapView?.isTilesScaledToDpi = true
        mapView?.maxZoomLevel = 20.0
        mapView?.minZoomLevel = 14.0
        val mapController = mapView?.controller
        mapController?.setZoom(10.0)
        mapController?.setCenter(GeoPoint(22.396428, 114.109497))

        val rotationOverlay = RotationGestureOverlay(mapView!!)
        rotationOverlay.isEnabled = true
        mapView?.setMultiTouchControls(true)
        mapView?.overlays?.add(rotationOverlay)

        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(applicationContext), mapView!!)
        locationOverlay.enableMyLocation()
        mapView?.overlays?.add(locationOverlay)

        val compassOverlay = CompassOverlay(applicationContext, InternalCompassOrientationProvider(applicationContext), mapView!!)
        compassOverlay.enableCompass()
        mapView?.overlays?.add(compassOverlay)
//        mapView?.overlays?.add(new CopyrightOverlay(getContext()));

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
        pagerAdapter = RoutePagerAdapter(supportFragmentManager, this, stopFromIntent)

        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.viewPager)
        viewPager?.adapter = pagerAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.addOnTabSelectedListener(object : TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val routes = ArrayList(pagerAdapter!!.routes)
                val fragment = RouteSelectDialogFragment.newInstance(routes, viewPager!!)
                fragment.show(supportFragmentManager, "route_select_dialog_fragment")
            }
        })

        pagerAdapter?.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                if (pagerAdapter?.count?:0 > 0) {
                    emptyView?.visibility = View.GONE
                    viewPager?.offscreenPageLimit = Math.min(pagerAdapter?.count?:0, 10)
                } else {
                    showEmptyView()
                }
            }
        })

        if (routeNo?.isNotBlank() == true) {
            loadRouteNo(routeNo!!)
        } else {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_refresh -> if (routeNo?.isNotBlank() == true) {
                loadRouteNo(routeNo!!)
            }
            R.id.action_show_map -> if (appBarLayout != null) {
                appBarLayout!!.setExpanded(true)
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
        updateAppShortcuts()
    }

    override fun onDestroy() {
        disposables.clear()
        if (suggestion != null) {
            appIndexStop(suggestion!!)
        }
        super.onDestroy()
    }

    protected fun showEmptyView() {
        emptyView?.visibility = View.VISIBLE
        progressBar?.visibility = View.GONE
        emptyText?.setText(R.string.message_fail_to_request)
        fab?.hide()
    }

    protected fun showLoadingView() {
        emptyView?.visibility = View.VISIBLE
        progressBar?.visibility = View.VISIBLE
        emptyText?.setText(R.string.message_loading)
        fab?.hide()
    }

    protected open fun loadRouteNo(no: String) {
        pagerAdapter?.clearSequence()
        if (no.isEmpty()) {
            showEmptyView()
            return
        }
        showLoadingView()
    }

    @Synchronized
    protected fun onCompleteRoute(routes: List<Route>, code: String?) {
        var companyCode = code
        if (pagerAdapter == null || companyCode.isNullOrEmpty()) return
        for (route in routes) {
            if (route == null) continue
            if (TextUtils.isEmpty(route.name) || route.name != routeNo) continue
            companyCode = route.companyCode
            pagerAdapter!!.addSequence(route)
            val fragmentCount = pagerAdapter!!.count
            if (stopFromIntent != null && route.companyCode != null
                    && route.sequence != null && route.serviceType != null
                    && route.companyCode == stopFromIntent!!.companyCode
                    && route.sequence == stopFromIntent!!.routeSeq
                    && route.serviceType == stopFromIntent!!.routeServiceType) {
                fragNo = fragmentCount
                isScrollToPage = true
            }
        }
        val routeName = RouteUtil.getCompanyName(this, companyCode?:"", routeNo) + " " + routeNo
        supportActionBar?.title = routeName
        if (routes.isNotEmpty() && !TextUtils.isEmpty(companyCode)) {
            suggestion = Suggestion.createInstance()
            suggestion?.companyCode = companyCode?:""
            suggestion?.route = routeNo?:""
            suggestion?.type = Suggestion.TYPE_HISTORY
            if (suggestion != null) {
                suggestionDatabase.suggestionDao().insert(suggestion!!)
                appIndexStart(suggestion!!)
            }
        }

        if (isScrollToPage!!) {
            if (viewPager != null && fragNo!! - 1 >= 0) {
                viewPager!!.setCurrentItem(fragNo!! - 1, false)
            }
            isScrollToPage = false
        }
    }

    fun getIndexApiAction(suggestion: Suggestion): Action {
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

    @Synchronized
    private fun updateAppShortcuts() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        if (applicationContext == null) return
        // Dynamic App Shortcut
        try {
            val shortcutManager = applicationContext.getSystemService(ShortcutManager::class.java)
                    ?: return
            val shortcuts = ArrayList<ShortcutInfo>()
            val followList = followDatabase.followDao().getList()
            val maxShortcutCount = shortcutManager.maxShortcutCountPerActivity
            run {
                var i = 0
                while (i < maxShortcutCount - 1 && i < followList.size) {
                    val follow = followList[i]
                    val routeStop = RouteStopUtil.fromFollow(follow)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(applicationContext, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.STOP_OBJECT_STRING, Gson().toJson(routeStop))
                    shortcuts.add(ShortcutInfo.Builder(applicationContext, "buseta-" + routeStop.companyCode + routeStop.routeNo + routeStop.routeSeq + routeStop.stopId)
                            .setShortLabel(routeStop.routeNo + " " + routeStop.name)
                            .setLongLabel(routeStop.routeNo + " " + routeStop.name + " " + getString(R.string.destination, routeStop.routeDestination))
                            .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_shortcut_directions_bus))
                            .setIntent(intent)
                            .build())
                    i++

                }
            }
            if (followList.size < maxShortcutCount - 1) {
                val historyList = suggestionDatabase.suggestionDao().historyList(maxShortcutCount)
                var i = 0
                while (i < maxShortcutCount - followList.size - 1 && i < historyList.size) {
                    val (_, companyCode, route) = historyList[i]
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(applicationContext, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.ROUTE_NO, route)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, companyCode)
                    shortcuts.add(ShortcutInfo.Builder(applicationContext, "buseta-q-$companyCode$route")
                            .setShortLabel(route)
                            .setLongLabel(route)
                            .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_shortcut_search))
                            .setIntent(intent)
                            .build())
                    i++
                }
            }
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (ignored: NoClassDefFoundError) {
        } catch (ignored: NoSuchMethodError) {
        }

    }
}
