package com.alvinhkh.buseta.mtr.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R


import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract


class MtrStationListFragment : RouteStopListFragmentAbstract() {

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.findItem(R.id.action_notice)?.isVisible = false
        menu?.findItem(R.id.action_timetable)?.isVisible = false
    }

    companion object {

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route,
                        routeStop: RouteStop?): MtrStationListFragment {
            val fragment = MtrStationListFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }
    }
}
