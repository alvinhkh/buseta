package com.alvinhkh.buseta.route.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.viewpager.widget.ViewPager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
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
        val viewModel = ViewModelProvider(this).get(RouteViewModel::class.java)
        viewModel.getAsLiveData(companyCode, routeNo)
                .observe(this, { routes ->
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