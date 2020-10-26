package com.alvinhkh.buseta.datagovhk.ui

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.model.TdTrafficNewsV1
import kotlinx.android.synthetic.main.item_traffic_news.view.*


class TdTrafficNewsViewAdapter(
        private val data: MutableList<TdTrafficNewsV1.Message> = mutableListOf()
): RecyclerView.Adapter<TdTrafficNewsViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_traffic_news, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun replace(list: List<TdTrafficNewsV1.Message>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!){

        @SuppressLint("SetTextI18n")
        fun bindItems(message: TdTrafficNewsV1.Message) {
            val source = itemView.context.getString(R.string.provider_td)
            itemView.text.text = message.ChinText
            itemView.date.text = "$source ${message.ReferenceDate}"
        }
    }

}