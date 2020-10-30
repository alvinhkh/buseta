package com.alvinhkh.buseta.nwst.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route


class NwstNoticeFragment: Fragment() {

    private var viewAdapter: NwstNoticeViewAdapter? = null
    private lateinit var route: Route

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        route = requireArguments().getParcelable(C.EXTRA.ROUTE_OBJECT)?:Route()
        val routeNo: String = route.name?:""
        val routeBound: String = route.sequence?:""
        val routeServiceType: String = route.serviceType?:""
        if (routeNo.isEmpty()) {
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
            viewAdapter = NwstNoticeViewAdapter()
            adapter = viewAdapter
        }
        val snackbar = Snackbar.make(rootView.findViewById(R.id.coordinator_layout), R.string.message_no_notice, Snackbar.LENGTH_INDEFINITE)
        val viewModel = ViewModelProvider(this@NwstNoticeFragment).get(NwstNoticeViewModel::class.java)
        viewModel.getAsLiveData(requireContext(), routeNo, routeBound, routeServiceType).observe(viewLifecycleOwner, { items ->
            swipeRefreshLayout?.isRefreshing = true
            if (items.isNullOrEmpty()) {
                snackbar.show()
                viewAdapter?.clear()
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
