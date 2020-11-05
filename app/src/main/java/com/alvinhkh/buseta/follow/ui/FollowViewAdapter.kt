package com.alvinhkh.buseta.follow.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.ui.RouteStopFragment
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.utils.PreferenceUtil
import java.lang.ref.WeakReference


class FollowViewAdapter(
        private var activityRef: WeakReference<FragmentActivity>,
        private var data: MutableList<Follow> = mutableListOf()
): RecyclerView.Adapter<FollowViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], activityRef)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_follow, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun addItem(f: Follow): Int {
        data.add(f)
        val index = data.size
        notifyItemInserted(index)
        return index - 1
    }

    fun replaceItems(l: MutableList<Follow>) {
        data.clear()
        data.addAll(l)
        notifyDataSetChanged()
    }

    fun replaceItem(index: Int, f: Follow) {
        if (index < data.size && index >= 0) {
            data[index] = f
            notifyItemChanged(index)
        }
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(follow: Follow, activityRef: WeakReference<FragmentActivity>) {
            val companyColor = if (follow.routeColour.isNotEmpty()) {
                Color.parseColor(follow.routeColour)
            } else {
                Route.companyColour(itemView.context, follow.companyCode, follow.routeNo)
            }
            if (companyColor != null) {
                itemView.findViewById<View>(R.id.color).setBackgroundColor(companyColor)
            }
            itemView.findViewById<TextView>(R.id.name).text = follow.stopName
            itemView.findViewById<TextView>(R.id.route_no).text = follow.routeNo
            itemView.findViewById<TextView>(R.id.destination).text = follow.routeDestination

            itemView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setClass(itemView.context, SearchActivity::class.java)
                intent.putExtra(C.EXTRA.STOP_OBJECT, follow.toRouteStop())
                itemView.context.startActivity(intent)
            }
            itemView.setOnLongClickListener {
                try {
                    val bottomSheetDialogFragment = RouteStopFragment.newInstance(follow)
                    bottomSheetDialogFragment.show(activityRef.get()?.supportFragmentManager!!, bottomSheetDialogFragment.tag)
                } catch (ignored: Throwable) {
                }
                true
            }

            if (follow.etas.isEmpty()) {
                itemView.findViewById<TextView>(R.id.eta).text = ""
                itemView.findViewById<TextView>(R.id.eta2).text = ""
            }
            follow.etas.forEachIndexed { _, obj ->
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
                        etaText.insert(0, "[${arrivalTime.platform}] ")
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
                            when (pos) {
                                0 -> drawable!!.setBounds(0, 0,
                                        itemView.findViewById<TextView>(R.id.eta).lineHeight,
                                        itemView.findViewById<TextView>(R.id.eta).lineHeight)
                                else -> drawable!!.setBounds(0, 0,
                                        itemView.findViewById<TextView>(R.id.eta2).lineHeight,
                                        itemView.findViewById<TextView>(R.id.eta2).lineHeight)
                            }
                            DrawableCompat.setTint(drawable.mutate(), colorInt!!)
                            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
                            etaText.append(" ")
                            if (etaText.isNotEmpty()) {
                                etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                            }
                        }
                    }
                    if (arrivalTime.hasWheelchair && PreferenceUtil.isShowWheelchairIcon(itemView.context)) {
                        var drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_accessible_18dp)
                        drawable = DrawableCompat.wrap(drawable!!)
                        when (pos) {
                            0 -> drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta).lineHeight,
                                    itemView.findViewById<TextView>(R.id.eta).lineHeight)
                            else -> drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta2).lineHeight,
                                    itemView.findViewById<TextView>(R.id.eta2).lineHeight)
                        }
                        DrawableCompat.setTint(drawable.mutate(), colorInt!!)
                        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
                        etaText.append(" ")
                        if (etaText.isNotEmpty()) {
                            etaText.setSpan(imageSpan, etaText.length - 1, etaText.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        }
                    }
                    if (arrivalTime.hasWifi && PreferenceUtil.isShowWifiIcon(itemView.context)) {
                        var drawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_wifi_18dp)
                        drawable = DrawableCompat.wrap(drawable!!)
                        when (pos) {
                            0 -> drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta).lineHeight,
                                    itemView.findViewById<TextView>(R.id.eta).lineHeight)
                            else -> drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta2).lineHeight,
                                    itemView.findViewById<TextView>(R.id.eta2).lineHeight)
                        }
                        DrawableCompat.setTint(drawable.mutate(), colorInt!!)
                        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
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
                            itemView.findViewById<TextView>(R.id.eta2).text = null
                        }
                        1 -> {
                            etaText.insert(0, itemView.findViewById<TextView>(R.id.eta2).text)
                            itemView.findViewById<TextView>(R.id.eta2).text = etaText
                        }
                        else -> {
                            etaText.insert(0, "  ")
                            etaText.insert(0, itemView.findViewById<TextView>(R.id.eta2).text)
                            itemView.findViewById<TextView>(R.id.eta2).text = etaText
                        }
                    }
                }
            }
        }
    }

}