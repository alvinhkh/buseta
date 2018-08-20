package com.alvinhkh.buseta.kmb.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.model.Route


class KmbScheduleFragment: Fragment() {

    private lateinit var viewModel: KmbScheduleViewModel
    private lateinit var viewAdapter: KmbScheduleViewAdapter
    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private lateinit var route: Route

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow_edit, container, false)
        route = arguments!!.getParcelable(C.EXTRA.ROUTE_OBJECT)
        val routeNo: String = route.name?:""
        val routeBound: String = route.sequence?:""
        if (routeNo.isEmpty() || routeBound.isEmpty()) {
            return rootView
        }
        setHasOptionsMenu(true)
        emptyView = rootView.findViewById(R.id.empty_view)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        if (recyclerView != null) {
            with(recyclerView!!) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                viewAdapter = KmbScheduleViewAdapter(routeBound, null)
                adapter = viewAdapter
                viewModel = ViewModelProviders.of(this@KmbScheduleFragment).get(KmbScheduleViewModel::class.java)
                viewModel.getAsLiveData(routeNo, routeBound).observe(this@KmbScheduleFragment, Observer { list ->
                    viewAdapter.clear()
                    var lastDayType = ""
                    var lastServiceType = ""
                    list?.forEach {
                        if (lastServiceType != it.serviceTypeTc && !it.serviceTypeTc.isNullOrEmpty()) {
                            viewAdapter.addSection(it.serviceTypeTc?:"")
                            lastServiceType = it.serviceTypeTc?:""
                        }
                        if (lastDayType != it.dayType) {
                            var dayTypeText = ""
                            when ((it.dayType?:"").trim().toUpperCase()) {
                                "D" -> dayTypeText = getString(R.string.everyday)
                                "MF" -> dayTypeText = getString(R.string.monday_to_friday)
                                "S" -> dayTypeText = getString(R.string.saturday)
                                "H" -> dayTypeText = getString(R.string.sunday_and_holiday)
                            }
                            viewAdapter.addSection(dayTypeText)
                            lastDayType = it.dayType?:""
                        }
                        if ((routeBound.replace("0", "") == "1" && (it.boundText1?:"").isNotEmpty())
                            || routeBound.replace("0", "") == "2" && (it.boundText2?:"").isNotEmpty()) {
                            viewAdapter.addItem(it)
                        }
                    }
                    val actionBar = (activity as AppCompatActivity).supportActionBar
                    if (!route.origin.isNullOrEmpty() && !route.destination.isNullOrEmpty()) {
                        actionBar?.subtitle = route.origin + getString(R.string.destination, route.destination)
                    } else {
                        actionBar?.subtitle = null
                    }
                    emptyView?.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
                })
            }
        }
        if (activity != null) {
            val fab = activity!!.findViewById<FloatingActionButton>(R.id.fab)
            fab?.hide()
        }
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
        viewAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.action_refresh) {
            viewAdapter.notifyDataSetChanged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
