package com.alvinhkh.buseta.mtr.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R


import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract


class AESBusStopListFragment : RouteStopListFragmentAbstract() {

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.findItem(R.id.action_notice)?.isVisible = false
        menu?.findItem(R.id.action_timetable)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_timetable -> {
                val link = Uri.parse(WEB_PAGE_URL)
                if (link != null && context != null) {
                    try {
                        val builder = CustomTabsIntent.Builder()
                        builder.setToolbarColor(ContextCompat.getColor(context!!, R.color.colorPrimary))
                        val customTabsIntent = builder.build()
                        customTabsIntent.launchUrl(context!!, link)
                    } catch (ignored: Exception) {
                        val intent = Intent(Intent.ACTION_VIEW, link)
                        if (intent.resolveActivity(context!!.packageManager) != null) {
                            startActivity(intent)
                        }
                    }

                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        private const val WEB_PAGE_URL = "https://www.mtr.com.hk/ch/customer/services/complom_free_bus.html"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route,
                        routeStop: RouteStop?): AESBusStopListFragment {
            val fragment = AESBusStopListFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }
    }
}
