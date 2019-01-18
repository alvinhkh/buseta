package com.alvinhkh.buseta.kmb.ui

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.kmb.model.KmbBBI2
import com.alvinhkh.buseta.ui.*
import kotlinx.android.synthetic.main.item_note.view.*
import kotlinx.android.synthetic.main.item_route_bbi.view.*
import kotlinx.android.synthetic.main.item_section.view.*

class KmbBBIViewAdapter(
        private val data: MutableList<Data> = mutableListOf()
): RecyclerView.Adapter<KmbBBIViewAdapter.Holder>(),
        PinnedHeaderItemDecoration.PinnedHeaderAdapter {

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 1

            const val TYPE_NOTE = 2

            const val TYPE_BBI = 3
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(when (viewType) {
            Data.TYPE_BBI -> R.layout.item_route_bbi
            Data.TYPE_NOTE -> R.layout.item_note
            else -> R.layout.item_section
        }, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].type
    }

    override fun getItemCount(): Int = data.size

    override fun isPinnedViewType(viewType: Int): Boolean {
        return viewType == Data.TYPE_SECTION
    }

    fun replace(list: List<Data>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!){

        fun bindItems(data: Data?) {
            if (data?.type == Data.TYPE_BBI) {
                val record = data.obj as KmbBBI2.Record
                itemView.route_no.text = record.secRouteno
                itemView.route_location_end.text = itemView.context.getString(R.string.destination, record.secDest)
                itemView.name.text = record.xchange
                itemView.discount.text = record.discountMax
                itemView.remark.text = record.validity
            } else if (data?.type == Data.TYPE_SECTION) {
                itemView.section_label.text = data.obj as String
            } else if (data?.type == Data.TYPE_NOTE) {
                itemView.note.text = data.obj as String
            }
        }

    }

}