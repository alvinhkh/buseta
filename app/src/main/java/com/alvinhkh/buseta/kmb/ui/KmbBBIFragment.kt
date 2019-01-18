package com.alvinhkh.buseta.kmb.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route


class KmbBBIFragment: Fragment() {

    private var viewAdapter: KmbBBIViewAdapter? = null
    private lateinit var route: Route

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        route = arguments!!.getParcelable(C.EXTRA.ROUTE_OBJECT)?:Route()
        val routeNo: String = route.name?:""
        val routeBound: String = route.sequence?:""
        if (routeNo.isEmpty() || routeBound.isEmpty()) {
            return rootView
        }
        setHasOptionsMenu(true)
        val snackbar = Snackbar.make(rootView.findViewById(R.id.coordinator_layout), R.string.no_bbi, Snackbar.LENGTH_INDEFINITE)
        val swipeRefreshLayout: SwipeRefreshLayout? = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout?.isEnabled = false
        swipeRefreshLayout?.isRefreshing = true
        val emptyView: View? = rootView.findViewById(R.id.empty_view)
        emptyView?.visibility = View.GONE
        val recyclerView: RecyclerView? = rootView.findViewById(R.id.recycler_view)
        recyclerView?.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = KmbBBIViewAdapter()
            adapter = viewAdapter
        }
        val viewModel = ViewModelProviders.of(this@KmbBBIFragment).get(KmbBBIViewModel::class.java)
        viewModel.getAsLiveData(routeNo, routeBound).observe(this@KmbBBIFragment, Observer { items ->
            swipeRefreshLayout?.isRefreshing = true
            if (items.isNullOrEmpty()) {
                viewAdapter?.clear()
                snackbar.show()
            } else {
                snackbar.dismiss()
                viewAdapter?.replace(items)
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
            (activity as AppCompatActivity).supportActionBar?.title = route.name + " " + getString(R.string.bus_to_bus_interchange_scheme)
        }
        viewAdapter?.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.findItem(R.id.action_search_open)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.action_refresh) {
            viewAdapter?.notifyDataSetChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
