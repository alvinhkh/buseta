package com.alvinhkh.buseta.kmb.ui

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
import com.alvinhkh.buseta.kmb.KmbInfoWorker
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.route.ui.RouteStopListFragmentAbstract
import com.alvinhkh.buseta.ui.webview.WebViewActivity


class KmbStopListFragment : RouteStopListFragmentAbstract() {

    private var infoItem: MenuItem? = null

    private var infoHtml = ""

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_timetable)?.isVisible = true
        menu.findItem(R.id.action_bbi)?.isVisible = true
        infoItem = menu.findItem(R.id.action_info)
        infoItem?.isVisible = infoHtml.isNotEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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
            R.id.action_info -> if (infoHtml.isNotEmpty()) {
                val intent = Intent(context, WebViewActivity::class.java)
                var title = getString(R.string.special_info)
                if (route != null && !route?.name.isNullOrEmpty()) {
                    title = route?.name + " " + title
                    intent.putExtra(WebViewActivity.COLOUR, Route.companyColour(requireContext(), route?.companyCode?: C.PROVIDER.NWST, route?.name?: ""))
                }
                intent.putExtra(WebViewActivity.TITLE, title)
                intent.putExtra(WebViewActivity.HTML, infoHtml)
                startActivity(intent)
                return true
            } else {
                infoItem?.isVisible = false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tag = "Info_${route?.companyCode}_${route?.code}_${route?.name}_${route?.sequence}"
        WorkManager.getInstance().cancelAllWorkByTag(tag)
        val request = OneTimeWorkRequest.Builder(KmbInfoWorker::class.java)
                .setInputData(Data.Builder()
                        .putString(C.EXTRA.COMPANY_CODE, route?.companyCode)
                        .putString(C.EXTRA.ROUTE_ID, route?.code)
                        .putString(C.EXTRA.ROUTE_NO, route?.name)
                        .putString(C.EXTRA.ROUTE_SEQUENCE, route?.sequence)
                        .putString(C.EXTRA.ROUTE_SERVICE_TYPE, route?.serviceType)
                        .build())
                .addTag(tag)
                .build()
        WorkManager.getInstance().enqueue(request)
        WorkManager.getInstance().getWorkInfoByIdLiveData(request.id)
                .observe(this, { workInfo ->
                    if (workInfo?.state == WorkInfo.State.FAILED) {
                        infoItem?.isVisible = false
                    }
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        try {
                            infoHtml = workInfo.outputData.getString(C.EXTRA.HTML)?:""
                            infoItem?.isVisible = true
                        } catch (e: Exception) {
                            infoHtml = ""
                            infoItem?.isVisible = false
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
