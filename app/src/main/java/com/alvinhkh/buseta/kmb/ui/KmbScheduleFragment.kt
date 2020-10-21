package com.alvinhkh.buseta.kmb.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route


class KmbScheduleFragment: Fragment() {

    private var viewAdapter: KmbScheduleViewAdapter? = null
    private lateinit var route: Route

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        route = arguments!!.getParcelable(C.EXTRA.ROUTE_OBJECT)?:Route()
        val routeNo: String = route.name?:""
        val routeBound: String = route.sequence?:""
        val routeServiceType: String = route.serviceType?:""
        if (routeNo.isEmpty() || routeBound.isEmpty()) {
            return rootView
        }
        setHasOptionsMenu(true)
        val swipeRefreshLayout: SwipeRefreshLayout? = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout?.isEnabled = false
        swipeRefreshLayout?.isRefreshing = true
        val emptyView: View? = rootView.findViewById(R.id.empty_view)
        emptyView?.visibility = View.GONE
        val recyclerView: RecyclerView? = rootView.findViewById(R.id.recycler_view)
        recyclerView?.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = KmbScheduleViewAdapter(routeBound)
            adapter = viewAdapter
        }
        val viewModel = ViewModelProvider(this@KmbScheduleFragment).get(KmbScheduleViewModel::class.java)
        viewModel.getAsLiveData(context!!, routeNo, routeBound, routeServiceType).observe(this@KmbScheduleFragment, Observer { items ->
            swipeRefreshLayout?.isRefreshing = true
            if (items.isNullOrEmpty()) {
                viewAdapter?.clear()
            } else {
                viewAdapter?.replace(items)
            }
            val actionBar = (activity as AppCompatActivity).supportActionBar
            if (!route.origin.isNullOrEmpty() && !route.destination.isNullOrEmpty()) {
                actionBar?.subtitle = route.origin + getString(R.string.destination, route.destination)
            } else {
                actionBar?.subtitle = null
            }
            swipeRefreshLayout?.isRefreshing = false
        })
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.hide()
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.title = route.name + " " + getString(R.string.timetable)
            if (!route.origin.isNullOrEmpty() && !route.destination.isNullOrEmpty()) {
                actionBar?.subtitle = route.origin + getString(R.string.destination, route.destination)
            } else {
                actionBar?.subtitle = null
            }
        }
        viewAdapter?.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_search_open)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_refresh) {
            viewAdapter?.notifyDataSetChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
