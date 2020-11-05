package com.alvinhkh.buseta.appwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Binder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.PreferenceUtil

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
class FollowWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StackRemoteViewsFactory(intent)
    }

    /**
     * This is the factory that will provide data to the collection widget.
     */
    private inner class StackRemoteViewsFactory internal constructor(intent: Intent) : RemoteViewsFactory {

        private var arrivalTimeDatabase: ArrivalTimeDatabase? = ArrivalTimeDatabase.getInstance(applicationContext)

        private var followDatabase: FollowDatabase? = FollowDatabase.getInstance(applicationContext)

        private val widgetId: Int = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)

        private var followGroupId: String = ""

        private var itemNoOfLines: Int = 3

        override fun onCreate() {
            val widgetPreferences = WidgetPreferences(applicationContext)
            itemNoOfLines = widgetPreferences.getItemRowNum(widgetId)
            followGroupId = widgetPreferences.getFollowGroupId(widgetId)?:""
        }

        override fun onDestroy() {}

        override fun getCount(): Int {
            return followDatabase?.followDao()?.count(followGroupId) ?: 0
        }

        override fun getViewAt(position: Int): RemoteViews? {
            val remoteViews = RemoteViews(packageName, R.layout.widget_item_eta)
            // Get the data for this position from the content provider
            if (followDatabase == null || position >= count) return remoteViews
            val follow = followDatabase?.followDao()?.list(followGroupId)?.get(position)?: return remoteViews
            val stop = follow.toRouteStop()
            remoteViews.setTextViewText(R.id.left_line1, null)
            remoteViews.setTextViewText(R.id.left_line2, null)
            remoteViews.setTextViewText(R.id.left_line3, null)
            remoteViews.setTextViewText(R.id.eta, null)
            remoteViews.setTextViewText(R.id.eta2, null)
            remoteViews.setTextViewText(R.id.eta3, null)
            if (itemNoOfLines == 1) {
                remoteViews.setViewVisibility(R.id.left_line1, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.left_line2, View.GONE)
                remoteViews.setViewVisibility(R.id.left_line3, View.GONE)
                remoteViews.setViewVisibility(R.id.eta, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.eta2, View.GONE)
                remoteViews.setViewVisibility(R.id.eta3, View.GONE)
                remoteViews.setTextViewText(R.id.left_line1, stop.routeNo + " " + stop.name)
            } else if (itemNoOfLines == 2) {
                remoteViews.setViewVisibility(R.id.left_line1, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.left_line2, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.left_line3, View.GONE)
                remoteViews.setViewVisibility(R.id.eta, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.eta2, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.eta3, View.GONE)
                if (!TextUtils.isEmpty(stop.routeDestination)) {
                    remoteViews.setTextViewText(R.id.left_line1, stop.routeNo + " " +
                            getString(R.string.destination, stop.routeDestination))
                } else {
                    remoteViews.setTextViewText(R.id.left_line1, stop.routeNo)
                }
                remoteViews.setTextViewText(R.id.left_line2, stop.name)
            } else {
                remoteViews.setViewVisibility(R.id.left_line1, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.left_line2, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.eta, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.eta2, View.VISIBLE)
                remoteViews.setViewVisibility(R.id.eta3, View.VISIBLE)
                remoteViews.setTextViewText(R.id.left_line1, stop.name)
                remoteViews.setTextViewText(R.id.left_line2, stop.routeNo)
                if (!TextUtils.isEmpty(stop.routeDestination)) {
                    remoteViews.setViewVisibility(R.id.left_line3, View.VISIBLE)
                    remoteViews.setTextViewText(R.id.left_line3, getString(R.string.destination, stop.routeDestination))
                } else {
                    remoteViews.setViewVisibility(R.id.left_line3, View.GONE)
                }
            }
            // ETA
            // http://stackoverflow.com/a/20645908/2411672
            val token = Binder.clearCallingIdentity()
            try {
                val etaTexts = SpannableStringBuilder()
                if (arrivalTimeDatabase != null) {
                    val arrivalTimeList = ArrivalTime.getList(arrivalTimeDatabase!!, stop)
                    var direction = ""
                    for (a in arrivalTimeList) {
                        val arrivalTime = ArrivalTime.estimate(applicationContext, a)
                        if (!TextUtils.isEmpty(arrivalTime.order)) {
                            val etaText = SpannableStringBuilder(arrivalTime.text)
                            val pos = Integer.parseInt(arrivalTime.order)
                            var colorInt: Int? = ContextCompat.getColor(applicationContext,
                                    if (arrivalTime.expired)
                                        R.color.widgetTextDiminish
                                    else
                                        if (pos > 0) R.color.widgetTextPrimary else R.color.widgetTextHighlighted)
                            if (arrivalTime.companyCode == C.PROVIDER.MTR) {
                                colorInt = ContextCompat.getColor(applicationContext, if (arrivalTime.expired)
                                    R.color.widgetTextDiminish
                                else
                                    R.color.widgetTextPrimary)
                            }
                            if (!TextUtils.isEmpty(arrivalTime.platform)) {
                                etaText.insert(0, "[" + arrivalTime.platform + "] ")
                            }
                            if (!TextUtils.isEmpty(arrivalTime.note)) {
                                etaText.append("#")
                            }
                            if (arrivalTime.isSchedule) {
                                etaText.append("*")
                            }
                            if (itemNoOfLines > 1 || pos == 0) {
                                if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                                    etaText.append(" (").append(arrivalTime.estimate).append(")")
                                }
                            }
                            if (arrivalTime.distanceKM >= 0) {
                                etaText.append(" ").append(getString(R.string.km_short, arrivalTime.distanceKM))
                            }
                            if (!TextUtils.isEmpty(arrivalTime.plate)) {
                                etaText.append(" ").append(arrivalTime.plate)
                            }
                            if (arrivalTime.capacity >= 0) {
                                var capacity = ""
                                if (arrivalTime.capacity == 0L) {
                                    capacity = getString(R.string.capacity_empty)
                                } else if (arrivalTime.capacity in 1..3) {
                                    capacity = "¼"
                                } else if (arrivalTime.capacity in 4..6) {
                                    capacity = "½"
                                } else if (arrivalTime.capacity in 7..9) {
                                    capacity = "¾"
                                } else if (arrivalTime.capacity >= 10) {
                                    capacity = getString(R.string.capacity_full)
                                }
                                if (!TextUtils.isEmpty(capacity)) {
                                    etaText.append(" [").append(capacity).append("]")
                                }
                            }
                            if (arrivalTime.hasWheelchair && PreferenceUtil.isShowWheelchairIcon(applicationContext)) {
                                etaText.append(" \u267F")
                            }
                            if (arrivalTime.hasWifi && PreferenceUtil.isShowWifiIcon(applicationContext)) {
                                etaText.append(" [W]")
                            }

                            if (etaText.isNotEmpty()) {
                                etaText.setSpan(ForegroundColorSpan(colorInt!!), 0, etaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            if (itemNoOfLines == 1) {
                                when (pos) {
                                    0 -> etaTexts.append(etaText)
                                    else -> {
                                        etaText.insert(0, "  ")
                                        etaTexts.append(etaText)
                                    }
                                }
                                remoteViews.setTextViewText(R.id.eta, etaTexts)
                            } else if (itemNoOfLines == 2) {
                                when (pos) {
                                    0 -> remoteViews.setTextViewText(R.id.eta, etaText)
                                    1 -> {
                                        remoteViews.setTextViewText(R.id.eta2, etaText)
                                        etaTexts.append(etaText)
                                        etaText.insert(0, "  ")
                                        remoteViews.setTextViewText(R.id.eta2, etaTexts)
                                    }
                                    2 -> {
                                        etaText.insert(0, "  ")
                                        remoteViews.setTextViewText(R.id.eta2, etaTexts)
                                    }
                                }
                            } else {
                                when (pos) {
                                    0 -> remoteViews.setTextViewText(R.id.eta, etaText)
                                    1 -> remoteViews.setTextViewText(R.id.eta2, etaText)
                                    2 -> remoteViews.setTextViewText(R.id.eta3, etaText)
                                }
                            }
                        }
                        direction = arrivalTime.direction
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token)
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClass(applicationContext, SearchActivity::class.java)
            intent.putExtra(C.EXTRA.STOP_OBJECT, stop)
            remoteViews.setOnClickFillInIntent(R.id.widget_item_eta, intent)
            if (!ConnectivityUtil.isConnected(applicationContext)) {
                remoteViews.setTextViewText(R.id.eta2, getString(R.string.message_no_internet_connection))
            }
            return remoteViews
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 2
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun onDataSetChanged() {
            arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(applicationContext)
            followDatabase = FollowDatabase.getInstance(applicationContext)
            val widgetPreferences = WidgetPreferences(applicationContext)
            itemNoOfLines = widgetPreferences.getItemRowNum(widgetId)
            followGroupId = widgetPreferences.getFollowGroupId(widgetId)?:""
            // http://stackoverflow.com/a/20645908/2411672
            val token = Binder.clearCallingIdentity()
            try {
            } finally {
                Binder.restoreCallingIdentity(token)
            }
        }
    }
}