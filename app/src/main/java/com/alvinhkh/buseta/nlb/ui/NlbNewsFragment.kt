package com.alvinhkh.buseta.nlb.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.R
import androidx.appcompat.app.AppCompatActivity
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.route.model.Route


class NlbNewsFragment: Fragment() {

    private var viewAdapter: NlbNewsViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val route = requireArguments().getParcelable(C.EXTRA.ROUTE_OBJECT)?: Route()
        val companyCode = route.companyCode?: C.PROVIDER.NLB
        (activity as AppCompatActivity).supportActionBar?.setTitle(R.string.latest_news)
        (activity as AppCompatActivity).supportActionBar?.subtitle = if (companyCode == C.PROVIDER.GMB901) {
            getString(R.string.provider_gmb) + " 901"
        } else {
            getString(R.string.provider_nlb)
        }
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
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
            viewAdapter = NlbNewsViewAdapter(companyCode)
            adapter = viewAdapter
        }
        val snackbar = Snackbar.make(rootView.findViewById(R.id.coordinator_layout), R.string.message_no_notice, Snackbar.LENGTH_INDEFINITE)
        val viewModel = ViewModelProvider(this@NlbNewsFragment).get(NlbNewsViewModel::class.java)
        viewModel.liveData(companyCode).observe(viewLifecycleOwner, { items ->
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
