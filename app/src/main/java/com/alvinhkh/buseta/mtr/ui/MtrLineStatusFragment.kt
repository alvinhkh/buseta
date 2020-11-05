package com.alvinhkh.buseta.mtr.ui

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.ui.image.ImageActivity
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
        emptyTextView.text = getString(R.string.updating)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        viewAdapter = MtrLineStatusViewAdapter()
        swipeRefreshLayout.isRefreshing = true
        recyclerView.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = viewAdapter
        }
        val lineStatusViewModel = ViewModelProvider(this).get(MtrLineStatusViewModel::class.java)
        lineStatusViewModel.getAsLiveData().observe(viewLifecycleOwner, { list ->
            if (ConnectivityUtil.isConnected(context)) {
                snackbar.dismiss()
            } else {
                emptyTextView.text = getString(R.string.message_no_data)
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
        val latestAlertViewModel = ViewModelProvider(this).get(MtrLatestAlertViewModel::class.java)
        latestAlertViewModel.getAsLiveData().observe(viewLifecycleOwner, { list ->
            viewAdapter.alert(list?: listOf())
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_route, menu)
        menu.findItem(R.id.action_search)?.isVisible = false
        menu.findItem(R.id.action_search_open)?.isVisible = false
        menu.findItem(R.id.action_show_map)?.isVisible = true
        menu.findItem(R.id.action_notice)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_show_map -> {
                val intent = Intent(context, ImageActivity::class.java)
                intent.putExtra(ImageActivity.IMAGE_TITLE, "港鐵車務狀況 (運輸署)")
                intent.putExtra(ImageActivity.IMAGE_URL, "http://210.3.170.180/mtr_status/MTR.jpg")
                intent.putExtra(ImageActivity.COLOUR, ContextCompat.getColor(requireContext(), R.color.black))
                startActivity(intent)
                return true
            }
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
//        WorkManager.getInstance().enqueue(
//                OneTimeWorkRequest.Builder(MtrTrainSpecialNewsWorker::class.java)
//                        .addTag("MtrTrainSpecialNews").build())
//        WorkManager.getInstance().enqueue(OneTimeWorkRequest.Builder(MtrLineWorker::class.java).addTag("RouteList").build())
    }
}
