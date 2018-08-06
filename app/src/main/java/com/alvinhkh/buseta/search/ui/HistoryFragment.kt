package com.alvinhkh.buseta.search.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion


class HistoryFragment: Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase
    private lateinit var suggestionDatabase: SuggestionDatabase
    private lateinit var viewModel: HistoryViewModel
    private lateinit var viewAdapter: HistoryViewAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_history, container, false)
        setHasOptionsMenu(true)

        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(rootView.context)!!
        suggestionDatabase = SuggestionDatabase.getInstance(rootView.context)!!
        emptyView = rootView.findViewById(R.id.empty_view)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener(this)
        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = false
        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 2)
            viewAdapter = HistoryViewAdapter(this, null)
            adapter = viewAdapter
            viewModel = ViewModelProviders.of(this@HistoryFragment).get(HistoryViewModel::class.java)
            viewModel.getAsLiveData().observe(this@HistoryFragment, Observer<MutableList<Suggestion>> { it ->
                viewAdapter.clear()
                it?.forEach {
                    viewAdapter.addItem(it)
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
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_follow, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.action_refresh) {
            onRefresh()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRefresh() {
        viewAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
    }
}
