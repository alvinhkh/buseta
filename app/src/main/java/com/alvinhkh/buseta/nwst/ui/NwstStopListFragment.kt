package com.alvinhkh.buseta.nwst.ui

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
import com.alvinhkh.buseta.nwst.NwstTokenWorker
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract
import com.alvinhkh.buseta.ui.webview.WebViewActivity
import java.io.File


class NwstStopListFragment : RouteStopListFragmentAbstract() {

    private var bbiItem: MenuItem? = null

    private var timetableItem: MenuItem? = null

    private var remarkHtml = ""

    private var timetableHtml = ""

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        bbiItem = menu.findItem(R.id.action_bbi)
        bbiItem?.isVisible = remarkHtml.isNotEmpty()
        timetableItem = menu.findItem(R.id.action_timetable)
        timetableItem?.isVisible = timetableHtml.isNotEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_bbi -> if (remarkHtml.isNotEmpty()) {
                val intent = Intent(context, WebViewActivity::class.java)
                var title = getString(R.string.bus_to_bus_interchange_scheme)
                if (route != null && !route?.name.isNullOrEmpty()) {
                    title = route?.name + " " + title
                    intent.putExtra(WebViewActivity.COLOUR, Route.companyColour(requireContext(), route?.companyCode?: C.PROVIDER.NWST, route?.name?: ""))
                }
                intent.putExtra(WebViewActivity.TITLE, title)
                intent.putExtra(WebViewActivity.HTML, remarkHtml)
                startActivity(intent)
                return true
            }
            R.id.action_timetable -> if (timetableHtml.isNotEmpty()) {
                val intent = Intent(context, WebViewActivity::class.java)
                var title = getString(R.string.timetable)
                if (route != null && !route?.name.isNullOrEmpty()) {
                    title = route?.name + " " + title
                    intent.putExtra(WebViewActivity.COLOUR, Route.companyColour(requireContext(), route?.companyCode?: C.PROVIDER.NWST, route?.name?: ""))
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
        val tag = "TimeTable_${route?.companyCode}_${route?.code}_${route?.name}_${route?.sequence}"
        WorkManager.getInstance().cancelAllWorkByTag(tag)
        val request = OneTimeWorkRequest.Builder(NwstStopTimetableWorker::class.java)
                .setInputData(Data.Builder()
                        .putString(C.EXTRA.COMPANY_CODE, route?.companyCode)
                        .putString(C.EXTRA.ROUTE_ID, route?.code)
                        .putString(C.EXTRA.ROUTE_NO, route?.name)
                        .putString(C.EXTRA.ROUTE_SEQUENCE, route?.sequence)
                        .putString(C.EXTRA.ROUTE_SERVICE_TYPE, route?.serviceType)
                        .build())
                .addTag(tag)
                .build()
        WorkManager.getInstance()
                .beginWith(OneTimeWorkRequest.Builder(NwstTokenWorker::class.java).addTag(tag).build())
                .then(request)
                .enqueue()
        WorkManager.getInstance().getWorkInfoByIdLiveData(request.id)
                .observe(this, { workInfo ->
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        timetableItem?.isVisible = false
                        bbiItem?.isVisible = false
                    }
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        try {
                            val timetableFilePath = workInfo.outputData.getString(C.EXTRA.ROUTE_TIMETABLE_FILE)?:""
                            timetableHtml = File(timetableFilePath).inputStream().readBytes().toString(Charsets.UTF_8)
                            timetableItem?.isVisible = true
                            val remarkFilePath = workInfo.outputData.getString(C.EXTRA.ROUTE_REMARK_FILE)?:""
                            remarkHtml = File(remarkFilePath).inputStream().readBytes().toString(Charsets.UTF_8)
                            bbiItem?.isVisible = true
                        } catch (e: Exception) {
                            timetableHtml = ""
                            timetableItem?.isVisible = false
                            remarkHtml = ""
                            bbiItem?.isVisible = false
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
