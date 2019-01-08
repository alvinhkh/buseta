package com.alvinhkh.buseta.kmb.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.kmb.model.KmbSchedule
import com.alvinhkh.buseta.ui.*
import kotlinx.android.synthetic.main.item_route_schedule.view.*
import kotlinx.android.synthetic.main.item_section.view.*
import timber.log.Timber
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class KmbScheduleViewAdapter(
        private val routeBound: String,
        private var data: MutableList<Data>?
): RecyclerView.Adapter<KmbScheduleViewAdapter.Holder>(),
        PinnedHeaderItemDecoration.PinnedHeaderAdapter {

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 1

            const val TYPE_SCHEDULE = 2
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(routeBound, data?.get(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (viewType == Data.TYPE_SCHEDULE) {
            return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_schedule, parent, false))
        }
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_section, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return data?.get(position)?.type?:0
    }

    override fun getItemCount(): Int = data?.size?:0

    override fun isPinnedViewType(viewType: Int): Boolean {
        return viewType == Data.TYPE_SECTION
    }

    fun addSection(s: String) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_SECTION, s))
        notifyItemInserted(data?.size?:0)
    }

    fun addItem(t: KmbSchedule) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_SCHEDULE, t))
        notifyItemInserted(data?.size?:0)
    }

    fun clear() {
        data?.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!){

        fun bindItems(routeBound: String, data: Data?) {
            if (data?.type == Data.TYPE_SCHEDULE) {
                val kmbSchedule = data.obj as KmbSchedule
                var timeRangeText: String? = null
                if (routeBound.replace("0", "") == "2") {
                    if (kmbSchedule.boundTime2.isNullOrEmpty()) {
                        itemView.minute.visibility = View.INVISIBLE
                        itemView.text.text = kmbSchedule.boundText2
                    } else {
                        itemView.minute.visibility = View.VISIBLE
                        itemView.text.text = kmbSchedule.boundText2
                        itemView.time.text = kmbSchedule.boundTime2
                        timeRangeText = kmbSchedule.boundText2
                    }
                } else {
                    if (kmbSchedule.boundTime1.isNullOrEmpty()) {
                        itemView.minute.visibility = View.INVISIBLE
                        itemView.text.text = kmbSchedule.boundText1
                    } else {
                        itemView.minute.visibility = View.VISIBLE
                        itemView.text.text = kmbSchedule.boundText1
                        itemView.time.text = kmbSchedule.boundTime1
                        timeRangeText = kmbSchedule.boundText1
                    }
                }
                if (!timeRangeText.isNullOrEmpty()) {
                    try {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val timeStr = timeRangeText.split("-")
                        if (timeStr.size == 2) {
                            val timeStart = Calendar.getInstance()
                            timeStart.time = sdf.parse(timeStr[0])
                            val timeEnd = Calendar.getInstance()
                            timeEnd.time = sdf.parse(timeStr[1])
                            val calendarNow = Calendar.getInstance()
                            val calendarStart = Calendar.getInstance()
                            calendarStart.set(Calendar.HOUR_OF_DAY, timeStart.get(Calendar.HOUR_OF_DAY))
                            calendarStart.set(Calendar.MINUTE, timeStart.get(Calendar.MINUTE))
                            val calendarEnd = Calendar.getInstance()
                            calendarEnd.set(Calendar.HOUR_OF_DAY, timeEnd.get(Calendar.HOUR_OF_DAY))
                            calendarEnd.set(Calendar.MINUTE, timeEnd.get(Calendar.MINUTE))
                            if (calendarEnd < calendarStart) {
                                calendarEnd.add(Calendar.DAY_OF_MONTH, 1)
                            }

                            val t1 = calendarStart.time.time % 86400000L
                            val t2 = calendarEnd.time.time % 86400000L
                            val timeNow = calendarNow.time.time % 86400000L
                            if (timeNow in t1..t2) {
                                itemView.text.text = ">>> $timeRangeText"
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            } else if (data?.type == Data.TYPE_SECTION) {
                itemView.section_label.text = data.obj as String
            }
        }

    }

}