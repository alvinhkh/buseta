package com.alvinhkh.buseta.follow.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.utils.PreferenceUtil
import java.util.*



class FollowViewAdapter(
        recyclerView: RecyclerView,
        private var data: MutableList<Data>?
): RecyclerView.Adapter<FollowViewAdapter.Holder>() {

    private val followDatabase = FollowDatabase.getInstance(recyclerView.context)
    private val context = recyclerView.context

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_FOLLOW = 2
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data?.get(position), followDatabase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (viewType == Data.TYPE_FOLLOW) {
            return Holder(LayoutInflater.from(context).inflate(R.layout.item_route_follow, parent, false))
        }
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_section, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return data?.get(position)?.type?:0
    }

    override fun getItemCount(): Int = data?.size?:0

    fun addItem(t: Follow): Int {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_FOLLOW, t))
        val index = data?.size?:0
        notifyItemInserted(index)
        return index - 1
    }

    fun replaceItem(index: Int, t: Follow) {
        if (index < data?.size?:0 && index >= 0) {
            data?.set(index, Data(Data.TYPE_FOLLOW, t))
            notifyItemChanged(index)
        }
    }

    fun clear() {
        data?.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(data: Data?, followDatabase: FollowDatabase?) {
            if (data?.type == Data.TYPE_FOLLOW) {
                val follow = data.obj as Follow
                itemView.findViewById<TextView>(R.id.name).text = follow.stopName
                itemView.findViewById<TextView>(R.id.route_no).text = follow.routeNo
                itemView.findViewById<TextView>(R.id.route_location_end).text = follow.routeDestination

                itemView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(itemView.context, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.STOP_OBJECT, follow.toRouteStop())
                    itemView.context.startActivity(intent)
                }
                itemView.setOnLongClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle(follow.routeNo + "?")
                    builder.setMessage(itemView.context.getString(R.string.message_remove_from_follow_list))
                    builder.setNegativeButton(R.string.action_cancel) { d, _ -> d.cancel() }
                    builder.setPositiveButton(R.string.action_confirm) { _, _ ->
                        val rowDeleted = followDatabase?.followDao()?.delete(follow.type,
                                follow.companyCode, follow.routeNo, follow.routeSeq,
                                follow.routeServiceType, follow.stopId, follow.stopSeq)
                        if (rowDeleted != null && rowDeleted > 0) {
                            Snackbar.make(itemView.rootView.findViewById(R.id.coordinator_layout)?:itemView.rootView,
                                    itemView.context.getString(R.string.removed_from_follow_list,
                                    String.format(Locale.ENGLISH, "%s %s", follow.routeNo, follow.stopName)),
                                    Snackbar.LENGTH_LONG)
                                    .addCallback(object : Snackbar.Callback() {
                                        override fun onDismissed(snackbar: Snackbar?, event: Int) {
                                            when (event) {
                                                Snackbar.Callback.DISMISS_EVENT_ACTION -> {
                                                    followDatabase.followDao().insert(follow)
                                                }
                                            }
                                        }
                                    })
                                    .setAction(R.string.undo) {
                                        // do nothing
                                    }.show()
                        }
                    }
                    builder.show()
                    true
                }

                var direction = ""
                if (follow.etas.isEmpty()) {
                    itemView.findViewById<TextView>(R.id.eta).text = ""
                    itemView.findViewById<TextView>(R.id.eta_next).text = ""
                }
                follow.etas.forEachIndexed { _, obj ->
                    val arrivalTime = ArrivalTime.estimate(itemView.context, obj)
                    if (!TextUtils.isEmpty(arrivalTime.order)) {
                        val etaText = SpannableStringBuilder(arrivalTime.text)
                        val pos = Integer.parseInt(arrivalTime.order)
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
                        if (!TextUtils.isEmpty(arrivalTime.platform)) {
                            etaText.insert(0, "[" + arrivalTime.platform + "] ")
                        }
                        if (!TextUtils.isEmpty(arrivalTime.note)) {
                            etaText.append("#")
                        }
                        if (arrivalTime.isSchedule) {
                            etaText.append("*")
                        }
                        if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                            etaText.append(" (").append(arrivalTime.estimate).append(")")
                        }
                        if (arrivalTime.distanceKM >= 0) {
                            etaText.append(" ").append(itemView.context.getString(R.string.km_short, arrivalTime.distanceKM))
                        }
                        if (!TextUtils.isEmpty(arrivalTime.plate)) {
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
                                if (pos == 0) {
                                    drawable!!.setBounds(0, 0,
                                            itemView.findViewById<TextView>(R.id.eta).lineHeight,
                                            itemView.findViewById<TextView>(R.id.eta).lineHeight)
                                } else {
                                    drawable!!.setBounds(0, 0,
                                            itemView.findViewById<TextView>(R.id.eta_next).lineHeight,
                                            itemView.findViewById<TextView>(R.id.eta_next).lineHeight)
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
                            if (pos == 0) {
                                drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta).lineHeight,
                                        itemView.findViewById<TextView>(R.id.eta).lineHeight)
                            } else {
                                drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta_next).lineHeight,
                                        itemView.findViewById<TextView>(R.id.eta_next).lineHeight)
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
                            if (pos == 0) {
                                drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta).lineHeight,
                                        itemView.findViewById<TextView>(R.id.eta).lineHeight)
                            } else {
                                drawable!!.setBounds(0, 0, itemView.findViewById<TextView>(R.id.eta_next).lineHeight,
                                        itemView.findViewById<TextView>(R.id.eta_next).lineHeight)
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
                        if (arrivalTime.companyCode == C.PROVIDER.MTR) {
                            if (direction != arrivalTime.direction) {
                                if (pos == 0) {
                                    itemView.findViewById<TextView>(R.id.eta).text = etaText
                                } else {
                                    itemView.findViewById<TextView>(R.id.eta_next).text = etaText
                                }
                            }
                        } else {
                            when (pos) {
                                0 -> {
                                    itemView.findViewById<TextView>(R.id.eta).text = etaText
                                    itemView.findViewById<TextView>(R.id.eta_next).text = null
                                }
                                1 -> {
                                    etaText.insert(0, itemView.findViewById<TextView>(R.id.eta_next).text)
                                    itemView.findViewById<TextView>(R.id.eta_next).text = etaText
                                }
                                2 -> {
                                    etaText.insert(0, "  ")
                                    etaText.insert(0, itemView.findViewById<TextView>(R.id.eta_next).text)
                                    itemView.findViewById<TextView>(R.id.eta_next).text = etaText
                                }
                                else -> {
                                    etaText.insert(0, "  ")
                                    etaText.insert(0, itemView.findViewById<TextView>(R.id.eta_next).text)
                                    itemView.findViewById<TextView>(R.id.eta_next).text = etaText
                                }
                            }
                        }
                    }
                    direction = arrivalTime.direction
                }
            }
        }
    }

}