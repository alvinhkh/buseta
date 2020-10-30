package com.alvinhkh.buseta.route.ui

import android.Manifest
import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.kmb.KmbStopListWorker
import com.alvinhkh.buseta.lwb.LwbStopListWorker
import com.alvinhkh.buseta.nwst.NwstStopListWorker
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListViewAdapter.Data.Companion.TYPE_RAILWAY_STATION
import com.alvinhkh.buseta.route.ui.RouteStopListViewAdapter.Data.Companion.TYPE_ROUTE_STOP
import com.alvinhkh.buseta.service.EtaService
import com.alvinhkh.buseta.ui.route.RouteAnnounceActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.PreferenceUtil
import com.google.android.gms.maps.model.Marker
import timber.log.Timber
import java.util.UUID


// TODO: better way to find nearest stop
// TODO: keep (nearest) stop on top
// TODO: auto refresh eta for follow stop and nearby stop
abstract class RouteStopListFragmentAbstract : Fragment(),  SwipeRefreshLayout.OnRefreshListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private val SCROLL_POSITION_STATE_KEY = "SCROLL_POSITION_STATE_KEY"

    protected var route: Route? = null

    private var navToStop: RouteStop? = null

    private var scrollToPos: Int? = 0

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var recyclerView: RecyclerView? = null

    private lateinit var emptyView: View

    private lateinit var progressBar: ProgressBar

    private lateinit var emptyText: TextView

    private lateinit var viewModel: RouteStopListViewModel

    private var viewAdapter: RouteStopListViewAdapter? = null

    private var requestId: UUID? = null

    private val initLoadHandler = Handler()

    private val initLoadRunnable = object : Runnable {
        override fun run() {
            Timber.d("initLoadRunnable")
            if (view != null) {
                if (userVisibleHint) {
                    refreshHandler.post(refreshRunnable)
                    adapterUpdateHandler.post(adapterUpdateRunnable)
                    if (activity != null) {
                        val fab = activity!!.findViewById<FloatingActionButton>(R.id.fab)
                        fab?.setOnClickListener { onRefresh() }
                    }
                } else {
                    refreshHandler.removeCallbacksAndMessages(null)
                    adapterUpdateHandler.removeCallbacksAndMessages(null)
                }
                initLoadHandler.removeCallbacksAndMessages(null)
            } else {
                initLoadHandler.postDelayed(this, 5000)  // try every 5 sec
            }
        }
    }

    protected val adapterUpdateHandler = Handler()

    protected val adapterUpdateRunnable: Runnable = object : Runnable {
        override fun run() {
            Timber.d("adapterUpdateRunnable")
            viewAdapter?.notifyDataSetChanged()
            adapterUpdateHandler.postDelayed(this, 30000)  // refresh every 30 sec
        }
    }

    private var refreshInterval = 0

    protected val refreshHandler = Handler()

    protected val refreshRunnable: Runnable = object : Runnable {
        override fun run() {
            Timber.d("refreshRunnable: %s", refreshInterval)
            if (refreshInterval > 0) {
                onRefresh()
                viewAdapter?.notifyDataSetChanged()
                refreshHandler.postDelayed(this, (refreshInterval * 1000).toLong())
            } else {
                refreshHandler.removeCallbacksAndMessages(null)
            }
        }
    }

    private var locationCallback: LocationCallback? = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (locationResult == null) {
                return
            }
            for (location in locationResult.locations) {
                if (location != null) {
                    viewAdapter?.setCurrentLocation(location)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (arguments != null) {
            route = requireArguments().getParcelable(C.EXTRA.ROUTE_OBJECT)
        }
        if (context == null) return
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        emptyView = rootView.findViewById(R.id.empty_view)
        emptyView.visibility = View.VISIBLE
        progressBar = rootView.findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        emptyText = rootView.findViewById(R.id.empty_text)
        emptyText.setText(R.string.message_loading)
        if (fragmentManager == null) return rootView
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(requireContext())
                    .lastLocation
                    .addOnSuccessListener { location -> if (location != null) viewAdapter?.setCurrentLocation(location) }
        }

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.setOnRefreshListener(this)

        if (arguments != null) {
            navToStop = arguments?.getParcelable(C.EXTRA.STOP_OBJECT)
        }
        swipeRefreshLayout.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false

        // load route stops from database
        recyclerView = rootView.findViewById(R.id.recycler_view)
        recyclerView?.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = RouteStopListViewAdapter(requireActivity(), route?:Route())
            adapter = viewAdapter
            viewModel = ViewModelProvider(this@RouteStopListFragmentAbstract).get(RouteStopListViewModel::class.java)
            viewModel.liveData(route?.companyCode?:"", route?.code?:"", route?.name?:"", route?.sequence?:"", route?.serviceType?:"")
                    .observe(viewLifecycleOwner, { stops ->
                        if (stops != null) {
                            val dataType = if (route?.companyCode == C.PROVIDER.MTR) TYPE_RAILWAY_STATION else TYPE_ROUTE_STOP
                            if (!swipeRefreshLayout.isRefreshing) {
                                swipeRefreshLayout.isRefreshing = true
                            }
                            viewAdapter?.replaceAll(stops, dataType)
                            if (route?.description?.isEmpty() == false) {
                                viewAdapter?.addHeader(route?.description?:"")
                            }

                            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            refreshInterval = preferences.getString("load_eta", "0")?.toInt()?:0
                            if (!listOf(C.PROVIDER.LRTFEEDER).contains(route?.companyCode) && refreshInterval <= 0) {
                                activity?.findViewById<FloatingActionButton>(R.id.fab)?.show()
                            }

                            if (emptyView.visibility == View.VISIBLE || visibility == View.GONE) {
                                emptyView.visibility = if (viewAdapter?.itemCount?:0 > 0) View.GONE else View.VISIBLE

                                var isScrollToPosition: Boolean? = false
                                var scrollToPosition: Int? = 0
                                if (viewAdapter?.itemCount?:0 > 0) {
                                    if ((route != null
                                                    && route?.companyCode != null && route?.companyCode == navToStop?.companyCode
                                                    && route?.name != null && route?.name == navToStop?.routeNo
                                                    && route?.sequence != null && route?.sequence == navToStop?.routeSequence
                                                    && route?.serviceType != null && route?.serviceType == navToStop?.routeServiceType

                                                    && navToStop != null
                                                    && navToStop?.companyCode != null && navToStop?.name != null
                                                    && navToStop?.routeServiceType != null
                                                    && (navToStop?.sequence != null || navToStop?.stopId != null))
                                    ) {
                                        for (i in 0 until (viewAdapter?.itemCount?:0)) {
                                            val item = viewAdapter?.get(i)?: continue
                                            if (item.type != TYPE_ROUTE_STOP) continue
                                            val stop = item.obj as RouteStop
                                            if ((stop.name != null
                                                            && stop.name == navToStop?.name
                                                            && stop.sequence != null
                                                            && stop.sequence == navToStop?.sequence)
                                                    || (stop.stopId != null && (stop.stopId == navToStop?.stopId
                                                            || stop.stopId?.replaceFirst(Regex("^0*"), "") == navToStop?.stopId))) {
                                                scrollToPosition = i
                                                isScrollToPosition = true
                                                try {
                                                    val intent = Intent(context, EtaService::class.java)
                                                    intent.putExtra(C.EXTRA.STOP_OBJECT, stop)
                                                    context.startService(intent)
                                                } catch (ignored: Throwable) {}
                                                break
                                            }
                                        }
                                    }
                                    if (scrollToPos!! > 0) {
                                        scrollToPosition = scrollToPos
                                        isScrollToPosition = true
                                        scrollToPos = 0
                                    }
                                    visibility = View.VISIBLE
                                    if (isScrollToPosition!!) {
                                        scrollToPosition(scrollToPosition!!)
                                    }
                                    emptyView.visibility = View.GONE
                                    progressBar.visibility = View.GONE
                                }
                            }
                        }
                        if (swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = false
                        }
                    })
        }
        initLoadHandler.post(initLoadRunnable)

        return rootView
    }

    override fun setUserVisibleHint(isUserVisible: Boolean) {
        super.setUserVisibleHint(isUserVisible)
        initLoadHandler.postDelayed(initLoadRunnable, 10)
    }

    override fun onResume() {
        super.onResume()
        if (context != null) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            refreshInterval = preferences.getString("load_eta", "0")?.toInt()?:0
            if (!listOf(C.PROVIDER.LRTFEEDER).contains(route?.companyCode) && refreshInterval <= 0) {
                activity?.findViewById<FloatingActionButton>(R.id.fab)?.show()
            }
        }
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val lastFirstVisiblePosition = (recyclerView?.layoutManager as LinearLayoutManager)
                .findFirstVisibleItemPosition()
        outState.putInt(SCROLL_POSITION_STATE_KEY, lastFirstVisiblePosition)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            scrollToPos = savedInstanceState.getInt(SCROLL_POSITION_STATE_KEY, 0)
        }
    }

    override fun onDestroy() {
        if (requestId != null) {
            WorkManager.getInstance().cancelWorkById(requestId!!)
            requestId = null
        }
        adapterUpdateHandler.removeCallbacksAndMessages(null)
        initLoadHandler.removeCallbacksAndMessages(null)
        refreshHandler.removeCallbacksAndMessages(null)
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this)
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_notice -> if (route != null) {
                val intent = Intent(context, RouteAnnounceActivity::class.java)
                intent.putExtra(C.EXTRA.ROUTE_OBJECT, route)
                startActivity(intent)
                return true
            }
            R.id.action_refresh -> return loadStopList(route?:Route())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
        if (key == "load_wheelchair_icon" || key == "load_wifi_icon") {
            // to reflect changes when toggle display icon
            if (viewAdapter?.itemCount?:0 > 0) {
                viewAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onRefresh() {
        swipeRefreshLayout.isRefreshing = false
        if (context != null) {
            try {
                val intent = Intent(context, EtaService::class.java)
                intent.putExtra(C.EXTRA.STOP_LIST, true)
                intent.putExtra(C.EXTRA.COMPANY_CODE, route?.companyCode)
                intent.putExtra(C.EXTRA.ROUTE_ID, route?.code)
                intent.putExtra(C.EXTRA.ROUTE_NO, route?.name)
                intent.putExtra(C.EXTRA.ROUTE_SEQUENCE, route?.sequence)
                intent.putExtra(C.EXTRA.ROUTE_SERVICE_TYPE, route?.serviceType)
                requireContext().startService(intent)
            } catch (ignored: Throwable) {
            }
        }
    }

    private fun loadStopList(route: Route): Boolean {
        val companyCode = route.companyCode?:""
        when (companyCode) {
            C.PROVIDER.LRTFEEDER, C.PROVIDER.MTR, C.PROVIDER.NLB, C.PROVIDER.GMB901 -> {
                viewAdapter?.notifyDataSetChanged()
                return true
            }
            "" -> return false
        }

        val data = Data.Builder()
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_ID, route.code?:"")
                .putString(C.EXTRA.ROUTE_NO, route.name?:"")
                .putString(C.EXTRA.ROUTE_SEQUENCE, route.sequence?:"")
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, route.serviceType?:"")
                .build()
        val tag = "StopList_${companyCode}_${route.code}_${route.name}_${route.sequence}_${route.serviceType}"
        WorkManager.getInstance().cancelAllWorkByTag(tag)
        val request = when (companyCode) {
            C.PROVIDER.KMB, C.PROVIDER.LWB -> {
                OneTimeWorkRequest.Builder(
                        if (PreferenceUtil.isUsingKmbWebApi(requireContext())) {
                            KmbStopListWorker::class.java
                        } else {
                            LwbStopListWorker::class.java
                        }
                )
                        .setInputData(data)
                        .addTag(tag)
                        .build()
            }
            C.PROVIDER.NWST, C.PROVIDER.NWFB, C.PROVIDER.CTB -> {
                if (!PreferenceUtil.isUsingNwstDataGovHkApi(requireContext())) {
                    OneTimeWorkRequest.Builder(NwstStopListWorker::class.java)
                            .setInputData(data)
                            .addTag(tag)
                            .build()
                } else {
                    return true
                }
            }
            else -> return false
        }
        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }
        if (requestId != null) {
            WorkManager.getInstance().cancelWorkById(requestId!!)
            requestId = null
        }
        requestId = request.id
        WorkManager.getInstance().enqueue(request)
        WorkManager.getInstance().getWorkInfoByIdLiveData(request.id)
                .observe(this, { workInfo ->
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        if (!ConnectivityUtil.isConnected(context)) {
                            showEmptyMessage(getString(R.string.message_no_internet_connection))
                        } else {
                            showEmptyMessage(getString(R.string.message_fail_to_request))
                        }
                    }
                })
        return true
    }

    fun onMarkerClick(marker: Marker) {
        if (marker.tag is RouteStop) {
            val markerStop = marker.tag as RouteStop?
            for (i in 0 until (viewAdapter?.itemCount?:0)) {
                if (viewAdapter?.get(i)?.type == TYPE_ROUTE_STOP && viewAdapter?.get(i)?.obj is RouteStop) {
                    val routeStop = viewAdapter?.get(i)?.obj as RouteStop
                    if (routeStop.sequence == markerStop?.sequence) {
                        val smoothScroller = object : androidx.recyclerview.widget.LinearSmoothScroller(recyclerView?.context) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START
                            }
                        }
                        smoothScroller.targetPosition = i
                        recyclerView?.layoutManager?.startSmoothScroll(smoothScroller)
                        break
                    }
                }
            }
        }
    }

    private fun showEmptyMessage(s: String?) {
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = false
        }
        if (viewAdapter?.itemCount?:0 > 0) {
            if (!s.isNullOrEmpty()) {
                Snackbar.make(view?.rootView?.findViewById(R.id.coordinator_layout)?:requireView(), s, Snackbar.LENGTH_INDEFINITE).show()
            }
        } else {
            recyclerView?.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            emptyText.text = s
        }
    }

    private fun startLocationUpdates() {
        if (context == null || locationCallback == null) return
        if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 10000
            locationRequest.fastestInterval = (10000 / 2).toLong()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            LocationServices.getFusedLocationProviderClient(requireContext())
                    .requestLocationUpdates(locationRequest, locationCallback!!, null/* Looper */)
        }
    }

    private fun stopLocationUpdates() {
        if (context == null || locationCallback == null) return
        LocationServices.getFusedLocationProviderClient(requireContext()).removeLocationUpdates(locationCallback!!)
    }
}
