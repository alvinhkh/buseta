package com.alvinhkh.buseta.search.ui

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.ui.PinnedHeaderItemDecoration
import kotlinx.android.synthetic.main.item_section.view.*
import kotlinx.android.synthetic.main.row_route.view.*

class SuggestionViewAdapter(
        private val context: Context,
        private var data: MutableList<Data>?,
        private val listener: OnItemClickListener?
): RecyclerView.Adapter<SuggestionViewAdapter.Holder>(), PinnedHeaderItemDecoration.PinnedHeaderAdapter {

    interface OnItemClickListener{
        fun onClick(suggestion: Suggestion?)
        fun onLongClick(suggestion: Suggestion?)
    }

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 1

            const val TYPE_SUGGESTION = 2

            const val TYPE_BUTTON = 3
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data?.get(position), listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (viewType == Data.TYPE_SUGGESTION) {
            return Holder(LayoutInflater.from(context).inflate(R.layout.item_route_history, parent, false))
        }
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_section, parent, false))
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

    fun addItem(t: Suggestion) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_SUGGESTION, t))
        notifyItemInserted(data?.size?:0)
    }

    fun addButton(s: Suggestion) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_BUTTON, s))
        notifyItemInserted(data?.size?:0)
    }

    fun clear() {
        data?.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView) {

        fun bindItems(data: Data?, listener: OnItemClickListener?) {
            if (data?.type == Data.TYPE_SUGGESTION) {
                val suggestion = data.obj as Suggestion
                if (suggestion.type == Suggestion.TYPE_HISTORY) {
                    itemView.icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_history_24dp))
                } else {
                    itemView.icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_directions_bus_24dp))
                }
                itemView.findViewById<TextView>(android.R.id.text1).text = suggestion.route
                itemView.setOnClickListener{ listener?.onClick(suggestion) }
                itemView.setOnLongClickListener{
                    listener?.onLongClick(suggestion)
                    true
                }
            } else if (data?.type == Data.TYPE_SECTION) {
                itemView.section_label.text = data.obj as String
            } else if (data?.type == Data.TYPE_BUTTON) {
                val suggestion = data.obj as Suggestion
                itemView.section_label.text = ">>"
                itemView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(it.context, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.ROUTE_NO, suggestion.route)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, suggestion.companyCode)
                    it.context.startActivity(intent)
                }
            }
        }

    }

}