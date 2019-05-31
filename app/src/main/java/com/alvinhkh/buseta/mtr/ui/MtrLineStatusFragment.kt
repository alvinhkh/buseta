package com.alvinhkh.buseta.mtr.ui

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.MtrLineWorker
import com.alvinhkh.buseta.utils.ConnectivityUtil


class MtrLineStatusFragment: Fragment(), SwipeRefreshLayout.OnRefreshListener {

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
        val viewModel = ViewModelProviders.of(this).get(MtrLineStatusViewModel::class.java)
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
        WorkManager.getInstance().enqueue(OneTimeWorkRequest.Builder(MtrLineWorker::class.java).build())
    }
}
