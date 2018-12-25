package com.alvinhkh.buseta.lwb.ui

import android.os.Bundle

import com.alvinhkh.buseta.C


import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract


class LwbStopListFragment : RouteStopListFragmentAbstract() {

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route,
                        routeStop: RouteStop?): LwbStopListFragment {
            val fragment = LwbStopListFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }
    }
}
