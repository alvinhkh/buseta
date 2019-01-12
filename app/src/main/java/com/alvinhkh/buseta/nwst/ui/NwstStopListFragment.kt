package com.alvinhkh.buseta.nwst.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.work.Data

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract
import com.alvinhkh.buseta.ui.webview.WebViewActivity


class NwstStopListFragment : RouteStopListFragmentAbstract() {

    private var timetableItem: MenuItem? = null

    private var timetableHtml = ""

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        timetableItem = menu!!.findItem(R.id.action_timetable)
        timetableItem?.isVisible = !timetableHtml.isEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        when (id) {
            R.id.action_timetable -> if (!timetableHtml.isEmpty()) {
                val intent = Intent(context, WebViewActivity::class.java)
                var title = getString(R.string.timetable)
                if (route != null && !route?.name.isNullOrEmpty()) {
                    title = route?.name + " " + title
                }
                intent.putExtra(WebViewActivity.TITLE, title)
                intent.putExtra(WebViewActivity.HTML, timetableHtml)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onWorkerSucceeded(workerOutputData: Data) {
        timetableItem?.isVisible = true
        timetableHtml = workerOutputData.getString(C.EXTRA.ROUTE_TIMETABLE_HTML)?:""
    }

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(route: Route,
                        routeStop: RouteStop?): NwstStopListFragment {
            val fragment = NwstStopListFragment()
            val args = Bundle()
            args.putParcelable(C.EXTRA.ROUTE_OBJECT, route)
            args.putParcelable(C.EXTRA.STOP_OBJECT, routeStop)
            fragment.arguments = args
            return fragment
        }
    }
}
