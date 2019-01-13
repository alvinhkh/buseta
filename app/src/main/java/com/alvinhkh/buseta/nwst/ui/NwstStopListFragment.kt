package com.alvinhkh.buseta.nwst.ui

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.nwst.NwstStopTimetableWorker
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract
import com.alvinhkh.buseta.ui.webview.WebViewActivity
import java.io.File


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = OneTimeWorkRequest.Builder(NwstStopTimetableWorker::class.java)
                .setInputData(Data.Builder()
                        .putString(C.EXTRA.COMPANY_CODE, route?.companyCode)
                        .putString(C.EXTRA.ROUTE_ID, route?.code)
                        .putString(C.EXTRA.ROUTE_NO, route?.name)
                        .putString(C.EXTRA.ROUTE_SEQUENCE, route?.sequence)
                        .putString(C.EXTRA.ROUTE_SERVICE_TYPE, route?.serviceType)
                        .build())
                .build()
        WorkManager.getInstance().enqueue(request)
        WorkManager.getInstance().getWorkInfoByIdLiveData(request.id)
                .observe(this, Observer { workInfo ->
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        timetableItem?.isVisible = false
                    }
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        try {
                            val filePath = workInfo.outputData.getString(C.EXTRA.ROUTE_TIMETABLE_FILE)?:""
                            timetableHtml = File(filePath).inputStream().readBytes().toString(Charsets.UTF_8)
                            timetableItem?.isVisible = true
                        } catch (e: Exception) {
                            timetableHtml = ""
                            timetableItem?.isVisible = false
                        }
                    }
                })
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
