package com.alvinhkh.buseta.search.ui

import androidx.lifecycle.ViewModelProvider
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.search.dao.SuggestionDatabase


class HistoryFragment: Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase
    private lateinit var suggestionDatabase: SuggestionDatabase
    private lateinit var viewModel: HistoryViewModel
    private lateinit var viewAdapter: HistoryViewAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_history, container, false)

        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(rootView.context)!!
        suggestionDatabase = SuggestionDatabase.getInstance(rootView.context)!!
        emptyView = rootView.findViewById(R.id.empty_view)
        recyclerView = rootView.findViewById(R.id.recycler_view)
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener(this)
        swipeRefreshLayout.isRefreshing = false
        swipeRefreshLayout.isEnabled = false
        fab = rootView.findViewById(R.id.fab)
        fab.setOnClickListener {
            startActivity(Intent(context, SearchActivity::class.java))
        }
        recyclerView.run {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(context, 2)
            viewAdapter = HistoryViewAdapter(this)
            adapter = viewAdapter
        }
        viewModel = ViewModelProvider(this@HistoryFragment).get(HistoryViewModel::class.java)
        viewModel.getAsLiveData().observe(viewLifecycleOwner, { list ->
            if (list.isNullOrEmpty()) {
                viewAdapter.clear()
            } else {
                viewAdapter.replace(list)
            }
            emptyView.visibility = if (viewAdapter.itemCount > 0) View.GONE else View.VISIBLE
        })

        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.title = getString(R.string.app_name)
            actionBar?.subtitle = null
        }
        fab.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
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
