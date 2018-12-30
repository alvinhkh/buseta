package com.alvinhkh.buseta.route.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import java.lang.ref.WeakReference

class RouteSelectDialogFragment : BottomSheetDialogFragment() {

    private var viewPagerRef: WeakReference<ViewPager?>? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val companyCode = arguments?.getString(C.EXTRA.COMPANY_CODE)?:""
        val routeNo = arguments?.getString(C.EXTRA.ROUTE_NO)?:""
        val itemView = inflater.inflate(R.layout.bottom_sheet_route_select, container, false)
        val recyclerView = itemView.findViewById<RecyclerView>(R.id.recycler_view)
        val viewAdapter = RouteSelectViewAdapter(viewPagerRef!!, WeakReference(this))
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = viewAdapter
        }
        val viewModel = ViewModelProviders.of(this).get(RouteViewModel::class.java)
        viewModel.getAsLiveData(companyCode, routeNo)
                .observe(this, Observer<MutableList<Route>> { routes ->
                    viewAdapter.replaceItems(routes?: mutableListOf())
                })
        return itemView

    }

    companion object {

        fun newInstance(companyCode: String, routeNo: String, viewPagerRef: WeakReference<ViewPager?>): RouteSelectDialogFragment {
            val fragment = RouteSelectDialogFragment()
            val args = Bundle()
            args.putString(C.EXTRA.COMPANY_CODE, companyCode)
            args.putString(C.EXTRA.ROUTE_NO, routeNo)
            fragment.arguments = args
            fragment.viewPagerRef = viewPagerRef
            return fragment
        }
    }
}