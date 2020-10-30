package com.alvinhkh.buseta.kmb.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*


class KmbBBIFragment: Fragment() {

    private var viewAdapter: KmbBBIViewAdapter? = null
    private lateinit var route: Route

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        route = requireArguments().getParcelable(C.EXTRA.ROUTE_OBJECT)?:Route()
        val routeNo: String = route.name?:""
        val routeBound: String = route.sequence?:""
        if (routeNo.isEmpty() || routeBound.isEmpty()) {
            return rootView
        }
        setHasOptionsMenu(true)
        val snackbar = Snackbar.make(rootView.findViewById(R.id.coordinator_layout), R.string.no_bbi, Snackbar.LENGTH_INDEFINITE)
        val swipeRefreshLayout: SwipeRefreshLayout? = rootView.findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout?.isEnabled = false
        swipeRefreshLayout?.isRefreshing = true
        val emptyView: View? = rootView.findViewById(R.id.empty_view)
        emptyView?.visibility = View.GONE
        val recyclerView: RecyclerView? = rootView.findViewById(R.id.recycler_view)
        recyclerView?.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            viewAdapter = KmbBBIViewAdapter()
            adapter = viewAdapter
        }
        val viewModel = ViewModelProvider(this@KmbBBIFragment).get(KmbBBIViewModel::class.java)
        rootView.findViewById<TextInputLayout>(R.id.search_edittext_layout)?.visibility = View.VISIBLE
        with(rootView.findViewById<TextInputEditText>(R.id.search_edittext)) {
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    val t = p0?.replace("[^a-zA-Z0-9]*".toRegex(), "")?.toUpperCase(Locale.ENGLISH)
                    val liveData = viewModel.liveData(routeNo, routeBound, t ?: "")
                    liveData.observe(viewLifecycleOwner, { items ->
                        swipeRefreshLayout?.isRefreshing = true
                        if (items.isNullOrEmpty()) {
                            viewAdapter?.clear()
                            snackbar.show()
                        } else {
                            snackbar.dismiss()
                            viewAdapter?.replace(items)
                        }
                        swipeRefreshLayout?.isRefreshing = false
                        liveData.removeObservers(this@KmbBBIFragment)
                    })
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }
        val liveData = viewModel.liveData(routeNo, routeBound, "")
        liveData.observe(viewLifecycleOwner, { items ->
            swipeRefreshLayout?.isRefreshing = true
            if (items.isNullOrEmpty()) {
                viewAdapter?.clear()
                snackbar.show()
            } else {
                snackbar.dismiss()
                viewAdapter?.replace(items)
            }
            swipeRefreshLayout?.isRefreshing = false
            liveData.removeObservers(this)
        })
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.hide()
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (activity != null) {
            (activity as AppCompatActivity).supportActionBar?.title = route.name + " " + getString(R.string.bus_to_bus_interchange_scheme)
        }
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
