package com.alvinhkh.buseta.kmb.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract


class KmbStopListFragment : RouteStopListFragmentAbstract() {

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.findItem(R.id.action_timetable)?.isVisible = true
        menu?.findItem(R.id.action_bbi)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_timetable -> if (route != null && context != null) {
                val intent = Intent(context, KmbScheduleActivity::class.java)
                intent.putExtra(C.EXTRA.ROUTE_OBJECT, route)
                startActivity(intent)
                return true
            }
            R.id.action_bbi -> if (route != null && context != null) {
                val intent = Intent(context, KmbBBIActivity::class.java)
                intent.putExtra(C.EXTRA.ROUTE_OBJECT, route)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route,
                        routeStop: RouteStop?): KmbStopListFragment {
            val fragment = KmbStopListFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }
    }
}
