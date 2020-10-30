package com.alvinhkh.buseta.datagovhk.ui

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


class TdTrafficNewsFragment: Fragment() {

    private var viewAdapter: TdTrafficNewsViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
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
            viewAdapter = TdTrafficNewsViewAdapter()
            adapter = viewAdapter
        }
        val snackbar = Snackbar.make(rootView.findViewById(R.id.coordinator_layout), R.string.updating, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()
        val viewModel = ViewModelProvider(this@TdTrafficNewsFragment).get(TdTrafficNewsViewModel::class.java)
        viewModel.getAsLiveData().observe(viewLifecycleOwner, { items ->
            swipeRefreshLayout?.isRefreshing = true
            if (items.isNullOrEmpty()) {
                snackbar.setText(R.string.message_no_data)
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
