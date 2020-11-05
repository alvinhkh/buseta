package com.alvinhkh.buseta.route.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Build
import android.text.Html
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabsIntent
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.mtr.ui.MtrScheduleViewAdapter
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.service.EtaService
import com.alvinhkh.buseta.utils.PreferenceUtil
import java.text.DecimalFormat
import java.util.*


class RouteStopListViewAdapter(
        val activity: FragmentActivity,
        val route: Route,
        val data: MutableList<Data> = mutableListOf()
): RecyclerView.Adapter<RouteStopListViewAdapter.Holder>() {

    private var currentLocation: Location? = null

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 0
            const val TYPE_HEADER = 1
            const val TYPE_FOOTER = 2
            const val TYPE_ROUTE_STOP = 3
            const val TYPE_RAILWAY_STATION = 4
        }
    }

    fun setCurrentLocation(location: Location) {
        currentLocation = location
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], activity, route, currentLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(when(viewType) {
            Data.TYPE_HEADER, Data.TYPE_FOOTER -> R.layout.item_note
            Data.TYPE_ROUTE_STOP -> R.layout.item_route_stop
            Data.TYPE_RAILWAY_STATION -> R.layout.item_railway_station
            else -> R.layout.item_note
        }, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].type
    }

    override fun getItemCount(): Int = data.size

    fun addHeader(s: String) {
        data.add(0, Data(Data.TYPE_HEADER, s))
        notifyItemInserted(0)
    }

    fun replaceAll(list: List<RouteStop>, dataType: Int = Data.TYPE_ROUTE_STOP) {
        data.clear()
        list.forEach {
            data.add(Data(dataType, it))
        }
        notifyDataSetChanged()
    }

    fun get(index: Int): Data? {
        return data[index]
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(data: Data, activity: FragmentActivity?, route: Route, currentLocation: Location?) {
            if (data.type == Data.TYPE_ROUTE_STOP) {
                val routeStop = (data.obj as RouteStop?)?: return
                itemView.findViewById<TextView>(R.id.name).text = routeStop.name
                itemView.findViewById<TextView>(R.id.distance).text = ""
                if (!routeStop.fareFull.isNullOrEmpty() && routeStop.fareFull!!.toFloat() > 0) {
                    itemView.findViewById<TextView>(R.id.fare).visibility = View.VISIBLE
                    itemView.findViewById<TextView>(R.id.fare).text = String.format(Locale.ENGLISH, "$%1$,.1f", routeStop.fareFull!!.toFloat())
                } else {
                    itemView.findViewById<TextView>(R.id.fare).visibility = View.INVISIBLE
                }
                itemView.findViewById<ImageView>(R.id.follow).visibility = View.GONE
                itemView.findViewById<ImageView>(R.id.nearby).visibility = View.GONE
                itemView.findViewById<TextView>(R.id.eta).text = null
                itemView.findViewById<TextView>(R.id.eta2).text = null
                itemView.findViewById<TextView>(R.id.eta3).text = null

                itemView.setOnClickListener {
                    itemView.findViewById<TextView>(R.id.eta).text = null
                    itemView.findViewById<TextView>(R.id.eta2).text = null
                    itemView.findViewById<TextView>(R.id.eta3).text = null
                    val intent = Intent(itemView.context, EtaService::class.java)
                    intent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                    itemView.context.startService(intent)
                    if (activity is RouteActivityAbstract && !routeStop.latitude.isNullOrEmpty() && !routeStop.longitude.isNullOrEmpty()) {
                        activity.mapCamera(routeStop.latitude?.toDouble()?:0.0, routeStop.longitude?.toDouble()?:0.0)
                    }
                }
                itemView.setOnLongClickListener {
                    try {
                        val bottomSheetDialogFragment = RouteStopFragment.newInstance(Follow.createInstance(route, routeStop))
                        bottomSheetDialogFragment.show(activity?.supportFragmentManager!!, bottomSheetDialogFragment.tag)
                        return@setOnLongClickListener true
                    } catch (ignored: IllegalStateException) {
                    }
                    return@setOnLongClickListener false
                }

                if (!routeStop.latitude.isNullOrEmpty() && !routeStop.longitude.isNullOrEmpty()) {
                    val location = Location("")
                    location.latitude = routeStop.latitude?.toDouble()?:0.0
                    location.longitude = routeStop.longitude?.toDouble()?:0.0
                    if (currentLocation != null) {
                        val distance = currentLocation.distanceTo(location)
                        // TODO: a better way, to show nearest stop
                        if (distance < 200) {
                            itemView.findViewById<TextView>(R.id.distance).text = DecimalFormat("~#.##km").format((distance / 1000).toDouble())
                            itemView.findViewById<ImageView>(R.id.nearby).visibility = View.VISIBLE
                            val drawable = itemView.findViewById<ImageView>(R.id.nearby).drawable
                            drawable.setBounds(0, 0, itemView.findViewById<TextView>(R.id.distance).lineHeight, itemView.findViewById<TextView>(R.id.distance).lineHeight)
                            itemView.findViewById<ImageView>(R.id.nearby).setImageDrawable(drawable)
                        }
                    }
                }

                // Follow
                itemView.findViewById<ImageView>(R.id.follow).visibility = if (routeStop.isFollow) View.VISIBLE else View.GONE

                // ETA
                routeStop.etas.forEach { obj ->
                    val arrivalTime = ArrivalTime.estimate(itemView.context, obj)
                    if (arrivalTime.order.isNotEmpty()) {
                        val etaText = SpannableStringBuilder(arrivalTime.text)
                        val pos = arrivalTime.order.toInt()
                        var colorInt: Int? = ContextCompat.getColor(itemView.context,
                                when {
                                    arrivalTime.expired -> R.color.textDiminish
                                    pos > 0 -> R.color.textPrimary
                                    else -> R.color.textHighlighted
                                })
                        if (arrivalTime.companyCode == C.PROVIDER.MTR) {
                            colorInt = ContextCompat.getColor(itemView.context, if (arrivalTime.expired)
                                R.color.textDiminish
                            else
                                R.color.textPrimary)
                        }
                        if (arrivalTime.platform.isNotEmpty()) {
                            etaText.insert(0, "[" + arrivalTime.platform + "] ")
                        }
                        if (arrivalTime.note.isNotEmpty()) {
                            etaText.append("#")
                        }
                        if (arrivalTime.isSchedule) {
                            etaText.append("*")
                        }
                        if (arrivalTime.estimate.isNotEmpty()) {
                            etaText.append(" (").append(arrivalTime.estimate).append(")")
                        }
                        if (arrivalTime.distanceKM >= 0) {
                            etaText.append(" ").append(itemView.context.getString(R.string.km_short, arrivalTime.distanceKM))
                        }
                        if (arrivalTime.plate.isNotEmpty()) {
                            etaText.append(" ").append(arrivalTime.plate)
                        }
                        if (arrivalTime.capacity >= 0) {
                            var drawable: Drawable? = null
                            when {
                                arrivalTime.capacity == 0L -> drawable =
                                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_capacity_0_black)
                                arrivalTime.capacity in 1..3 -> drawable =
                                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_capacity_20_black)
                                arrivalTime.capacity in 4..6 -> drawable =
                                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_capacity_50_black)
                                arrivalTime.capacity in 7..9 -> drawable =
                                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_capacity_80_black)
                                arrivalTime.capacity >= 10 -> drawable =
                                        ContextCompat.getDrawable(itemView.context, R.drawable.ic_capacity_100_black)
                            }
                            if (drawable != null) {
                                drawable = DrawableCompat.wrap(drawable)
                                drawable.setBounds(0, 0,
                                        itemView.findViewById<TextView>(
                                                when (pos) {
                                                    0 -> R.id.eta
                                                    1 -> R.id.eta2
                                                    else -> R.id.eta3
                                                }).lineHeight,
                                        itemView.findViewById<TextView>(
                                                when (pos) {
                                                    0 -> R.id.eta
                                                    1 -> R.id.eta2
                                                    else -> R.id.eta3
                                                }).lineHeight)
                                DrawableCompat.setTint(drawable.mutate(), colorInt!!)
                                val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
                                etaText.append(" ")
                                if (etaText.isNotEmpty()) {
                                    etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                                }
                            }
                        }
                        if (arrivalTime.hasWheelchair && PreferenceUtil.isShowWheelchairIcon(itemView.context)) {
                            var drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_accessible_18dp)
                            drawable = DrawableCompat.wrap(drawable!!)
                            drawable.setBounds(0, 0,
                                    itemView.findViewById<TextView>(
                                            when (pos) {
                                                0 -> R.id.eta
                                                1 -> R.id.eta2
                                                else -> R.id.eta3
                                            }).lineHeight,
                                    itemView.findViewById<TextView>(
                                            when (pos) {
                                                0 -> R.id.eta
                                                1 -> R.id.eta2
                                                else -> R.id.eta3
                                            }).lineHeight)
                            DrawableCompat.setTint(drawable.mutate(), colorInt!!)
                            val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
                            etaText.append(" ")
                            if (etaText.isNotEmpty()) {
                                etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                            }
                        }
                        if (arrivalTime.hasWifi && PreferenceUtil.isShowWifiIcon(itemView.context)) {
                            var drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_wifi_18dp)
                            drawable = DrawableCompat.wrap(drawable!!)
                            drawable.setBounds(0, 0,
                                    itemView.findViewById<TextView>(
                                            when (pos) {
                                                0 -> R.id.eta
                                                1 -> R.id.eta2
                                                else -> R.id.eta3
                                            }).lineHeight,
                                    itemView.findViewById<TextView>(
                                            when (pos) {
                                                0 -> R.id.eta
                                                1 -> R.id.eta2
                                                else -> R.id.eta3
                                            }).lineHeight)
                            DrawableCompat.setTint(drawable.mutate(), colorInt!!)
                            val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BOTTOM)
                            etaText.append(" ")
                            if (etaText.isNotEmpty()) {
                                etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                            }
                        }
                        if (etaText.isNotEmpty()) {
                            etaText.setSpan(ForegroundColorSpan(colorInt!!), 0, etaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        when (pos) {
                            0 -> {
                                itemView.findViewById<TextView>(R.id.eta).text = etaText
                            }
                            1 -> {
                                itemView.findViewById<TextView>(R.id.eta2).text = etaText
                            }
                            2 -> {
                                itemView.findViewById<TextView>(R.id.eta3).text = etaText
                            }
//                            else -> {
//                                etaText.insert(0, "  ")
//                                etaText.insert(0, itemView.findViewById<TextView>(R.id.eta3).text)
//                                itemView.findViewById<TextView>(R.id.eta3).text = etaText
//                            }
                        }
                    }
                }
            } else if (data.type == Data.TYPE_RAILWAY_STATION) {
                val routeStop = data.obj as RouteStop
                itemView.findViewById<TextView>(R.id.name).text = routeStop.name
                if (!routeStop.stopId.isNullOrEmpty()) {
                    itemView.findViewById<ImageView>(R.id.map_button).setOnClickListener {
                        openLink(it.context,
                                "https://www.mtr.com.hk/archive/en/services/maps/${routeStop.stopId?.toLowerCase(Locale.ROOT)}.pdf",
                                ContextCompat.getColor(it.context, R.color.provider_mtr))
                    }
                    itemView.findViewById<ImageView>(R.id.map_button).visibility = View.VISIBLE
                }
                val scheduleList = itemView.findViewById<RecyclerView>(R.id.schedule_list)
                val viewAdapter = MtrScheduleViewAdapter()
                scheduleList.run {
                    layoutManager = LinearLayoutManager(itemView.context)
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = false
                    adapter = viewAdapter
                    visibility = View.VISIBLE
                }
                viewAdapter.addAll(routeStop.etas)
                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, EtaService::class.java)
                    intent.putExtra(C.EXTRA.STOP_OBJECT, routeStop)
                    itemView.context.startService(intent)
                }
                itemView.setOnLongClickListener {
                    try {
                        val bottomSheetDialogFragment = RouteStopFragment.newInstance(Follow.createInstance(route, routeStop))
                        bottomSheetDialogFragment.show(activity?.supportFragmentManager!!, bottomSheetDialogFragment.tag)
                        return@setOnLongClickListener true
                    } catch (ignored: IllegalStateException) {
                    }
                    return@setOnLongClickListener false
                }
            } else if (data.type == Data.TYPE_HEADER || data.type == Data.TYPE_FOOTER) {
                itemView.findViewById<TextView>(R.id.note).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(data.obj as String, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(data.obj as String)
                }
            } else {
                itemView.findViewById<TextView>(R.id.note).visibility = View.GONE
            }
        }

        private fun openLink(context: Context, url: String, @ColorInt colorInt: Int) {
            val link = Uri.parse(url)
            try {
                val builder = CustomTabsIntent.Builder()
                builder.setToolbarColor(colorInt)
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(context, link)
            } catch (ignored: Throwable) {
                val intent = Intent(Intent.ACTION_VIEW, link)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        }
    }

}