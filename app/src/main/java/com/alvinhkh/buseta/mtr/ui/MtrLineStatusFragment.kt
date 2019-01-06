package com.alvinhkh.buseta.mtr.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.utils.ConnectivityUtil


class MtrLineStatusFragment: Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var viewModel: MtrLineStatusViewModel
    private lateinit var viewAdapter: MtrLineStatusViewAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var snackbar: Snackbar
    private var fab: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        setHasOptionsMenu(true)
        fab = activity?.findViewById(R.id.fab)
        snackbar = Snackbar.make(rootView?.findViewById(R.id.coordinator_layout)?:rootView, R.string.message_no_internet_connection, Snackbar.LENGTH_INDEFINITE)
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener(this)
        emptyView = rootView.findViewById(R.id.empty_view)
        emptyView.visibility = View.VISIBLE
        val emptyTextView = rootView.findViewById<TextView>(R.id.empty_text)
        emptyTextView.text = getString(R.string.message_no_data)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        viewAdapter = MtrLineStatusViewAdapter()
        swipeRefreshLayout.isRefreshing = true
        recyclerView.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
        }
        viewModel = ViewModelProviders.of(this).get(MtrLineStatusViewModel::class.java)
        onRefresh()
        return rootView
    }

    override fun onResume() {
        super.onResume()
        fab?.hide()
        swipeRefreshLayout.isEnabled = false
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_refresh -> onRefresh()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() {
        if (ConnectivityUtil.isConnected(context)) {
            snackbar.dismiss()
        } else {
            snackbar.show()
        }
        viewModel.getAsLiveData().removeObservers(this)
        viewModel.getAsLiveData().observe(this, Observer { list ->
            if (ConnectivityUtil.isConnected(context)) {
                snackbar.dismiss()
            } else {
                snackbar.show()
            }
            if (!swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = true
            }
            viewAdapter.replace(list?: listOf())
            emptyView.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }
}
