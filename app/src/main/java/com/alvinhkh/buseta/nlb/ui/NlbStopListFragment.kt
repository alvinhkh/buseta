package com.alvinhkh.buseta.nlb.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.nlb.NlbService.Companion.GMB901
import com.alvinhkh.buseta.nlb.NlbService.Companion.NLB
import com.alvinhkh.buseta.nlb.NlbService.Companion.TIMETABLE_URL
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract


class NlbStopListFragment : RouteStopListFragmentAbstract() {

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val timetableItem = menu.findItem(R.id.action_timetable)
        timetableItem.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_timetable -> if (route != null && route?.code != null) {
                val prefix = if (route?.companyCode == C.PROVIDER.GMB901) {
                    GMB901
                } else {
                    NLB
                }
                openLink(requireContext(), prefix + TIMETABLE_URL + route?.code,
                        route?.companyColour(requireContext())?: ContextCompat.getColor(requireContext(), R.color.provider_nlb))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openLink(context: Context, url: String, color: Int) {
        val link = Uri.parse(url)
        try {
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(color)
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, link)
        } catch (ignored: Throwable) {
            val intent = Intent(Intent.ACTION_VIEW, link)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route,
                        routeStop: RouteStop?): NlbStopListFragment {
            val fragment = NlbStopListFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }
    }
}
