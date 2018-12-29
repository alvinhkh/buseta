package com.alvinhkh.buseta.follow.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.service.EtaService
import com.alvinhkh.buseta.utils.ConnectivityUtil


class FollowFragment: Fragment() {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase
    private lateinit var followDatabase: FollowDatabase
    private lateinit var viewModel: FollowViewModel
    private lateinit var viewAdapter: FollowViewAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View

    private val fetchEtaHandler = Handler()
    private val fetchEtaRunnable = object : Runnable {
        override fun run() {
            // Check internet connection
            if (ConnectivityUtil.isConnected(context)) {
                val intent = Intent(context, EtaService::class.java)
                intent.putExtra(C.EXTRA.FOLLOW, true)
                context?.startService(intent)
            } else {
                if (activity != null) {
                    Snackbar.make(activity!!.findViewById(R.id.coordinator_layout),
                            R.string.message_no_internet_connection, Snackbar.LENGTH_LONG).show()
                }
            }
            fetchEtaHandler.postDelayed(this, 30000)
            refreshHandler.removeCallbacks(null)
            refreshHandler.postDelayed(refreshRunnable, 10000)
        }
    }

    private val refreshHandler = Handler()
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewAdapter.notifyDataSetChanged()
            refreshHandler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(activity?.applicationContext!!)!!
        followDatabase = FollowDatabase.getInstance(activity?.applicationContext!!)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_follow_edit, container, false)
        setHasOptionsMenu(true)

        emptyView = rootView.findViewById(R.id.empty_view)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = FollowViewAdapter(this, null)
            adapter = viewAdapter
            viewModel = ViewModelProviders.of(this@FollowFragment).get(FollowViewModel::class.java)
            viewModel.getAsLiveData().observe(this@FollowFragment, Observer<MutableList<Follow>> { it ->
                viewAdapter.clear()
                it?.forEach { follow ->
                    val index = viewAdapter.addItem(follow)
                    val id = follow.companyCode + follow.routeNo + follow.routeSeq + follow.routeServiceType + follow.stopId + follow.stopSeq
                    val arrivalTimeLiveData = arrivalTimeDatabase.arrivalTimeDao().getLiveData(follow.companyCode, follow.routeNo, follow.routeSeq, follow.stopId, follow.stopSeq)
                    arrivalTimeLiveData.observe(this@FollowFragment, Observer { etas ->
                        if (etas != null && id == (follow.companyCode + follow.routeNo + follow.routeSeq + follow.routeServiceType + follow.stopId + follow.stopSeq)) {
                            follow.etas = listOf()
                            etas.forEach { eta ->
                                if (eta.updatedAt > System.currentTimeMillis() - 600000) {
                                    follow.etas += eta
                                }
                            }
                            viewAdapter.replaceItem(index, follow)
                        }
                    })
                }
                emptyView.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
            })
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.setTitle(R.string.app_name)
            val fab = activity!!.findViewById<FloatingActionButton>(R.id.fab)
            fab?.show()
        }
        fetchEtaHandler.postDelayed(fetchEtaRunnable, 500)
        refreshHandler.postDelayed(refreshRunnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        fetchEtaHandler.removeCallbacksAndMessages(null)
        refreshHandler.removeCallbacksAndMessages(null)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_follow, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.action_refresh) {
            fetchEtaHandler.postDelayed(fetchEtaRunnable, 100)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
