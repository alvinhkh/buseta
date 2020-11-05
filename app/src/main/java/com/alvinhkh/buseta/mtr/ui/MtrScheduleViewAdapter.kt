package com.alvinhkh.buseta.mtr.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import java.text.SimpleDateFormat
import java.util.*


class MtrScheduleViewAdapter(
        val data: MutableList<Data> = mutableListOf()
): RecyclerView.Adapter<MtrScheduleViewAdapter.Holder>() {

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 0
            const val TYPE_HEADER = 1
            const val TYPE_FOOTER = 2
            const val TYPE_ARRIVAL_TIME = 3
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(when(viewType) {
            Data.TYPE_SECTION -> R.layout.item_separator
            Data.TYPE_ARRIVAL_TIME -> R.layout.item_railway_schedule
            else -> R.layout.item_note
        }, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].type
    }

    override fun getItemCount(): Int = data.size

    fun add(o: ArrivalTime): Int {
        data.add(Data(Data.TYPE_ARRIVAL_TIME, o))
        val index = data.size
        notifyItemInserted(index)
        return index - 1
    }

    fun addAll(list: List<ArrivalTime>) {
        var direction = ""
        list.forEach {
            if (direction.isNotEmpty() && list.size > 1 && direction != it.direction) {
                data.add(Data(Data.TYPE_SECTION, ""))
            }
            data.add(Data(Data.TYPE_ARRIVAL_TIME, it))
            direction = it.direction
        }
        notifyDataSetChanged()
    }

    fun clear(notify: Boolean = false) {
        data.clear()
        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun get(index: Int): Data? {
        return data[index]
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        var dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

        var timeFormat = SimpleDateFormat("H:mm", Locale.ENGLISH)

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(data: Data) {
            if (data.type == Data.TYPE_ARRIVAL_TIME) {
                val arrivalTime = data.obj as ArrivalTime
                itemView.findViewById<TextView>(R.id.title).text = arrivalTime.platform
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    itemView.findViewById<TextView>(R.id.title).backgroundTintList = ContextCompat.getColorStateList(itemView.context,
                            if (arrivalTime.expired) R.color.textDiminish else R.color.textPrimary)
                }

                val colorInt = ContextCompat.getColor(itemView.context,
                        if (arrivalTime.expired) R.color.textDiminish else R.color.textPrimary)
                var timeText = arrivalTime.text
                try {
                    timeText = timeFormat.format(dateFormat.parse(arrivalTime.text))
                } catch (ignored: Throwable) {
                }
                val etaText = SpannableStringBuilder(timeText)
                if (!arrivalTime.estimate.isEmpty()) {
                    etaText.append(" (").append(arrivalTime.estimate).append(")")
                }
                if (etaText.isNotEmpty()) {
                    etaText.setSpan(ForegroundColorSpan(colorInt), 0, etaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                itemView.findViewById<TextView>(R.id.text).text = etaText
            } else if (data.type == Data.TYPE_HEADER || data.type == Data.TYPE_FOOTER) {
                itemView.findViewById<TextView>(R.id.note).text = data.obj as String
            } else if (data.type == Data.TYPE_SECTION) {
            } else {
                itemView.findViewById<TextView>(R.id.note).visibility = View.GONE
            }
        }
    }

}