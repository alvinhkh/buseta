package com.alvinhkh.buseta.search.ui

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.search.model.Suggestion
import com.alvinhkh.buseta.ui.PinnedHeaderItemDecoration
import kotlinx.android.synthetic.main.item_section.view.*
import kotlinx.android.synthetic.main.row_route.view.*

class SearchViewAdapter(
        private val listener: OnItemClickListener?,
        private val data: MutableList<Data> = mutableListOf()
): RecyclerView.Adapter<SearchViewAdapter.Holder>(), PinnedHeaderItemDecoration.PinnedHeaderAdapter {

    interface OnItemClickListener{
        fun onClick(data: Data)
        fun onLongClick(data: Data)
    }

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 1

            const val TYPE_SUGGESTION = 2

            const val TYPE_ROUTE = 3

            const val TYPE_BUTTON = 4
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data.get(position), listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(when (viewType) {
            Data.TYPE_SUGGESTION -> R.layout.item_route_history
            Data.TYPE_ROUTE -> R.layout.item_search_route
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

    fun addSection(s: String) {
        data.add(Data(Data.TYPE_SECTION, s))
        notifyItemInserted(data.size)
    }

    fun add(t: Suggestion) {
        data.add(Data(Data.TYPE_SUGGESTION, t))
        notifyItemInserted(data.size)
    }

    fun add(route: Route) {
        data.add(Data(Data.TYPE_ROUTE, route))
        notifyItemInserted(data.size)
    }

    fun addButton(s: Suggestion) {
        data.add(Data(Data.TYPE_BUTTON, s))
        notifyItemInserted(data.size)
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        fun bindItems(data: Data, listener: OnItemClickListener?) {
            when {
                data.type == Data.TYPE_SUGGESTION -> {
                    val suggestion = data.obj as Suggestion
                    if (suggestion.type == Suggestion.TYPE_HISTORY) {
                        itemView.icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_history_24dp))
                    } else {
                        itemView.icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.ic_outline_directions_bus_24dp))
                    }
                    itemView.findViewById<TextView>(android.R.id.text1).text = suggestion.route
                    itemView.setOnClickListener{ listener?.onClick(data) }
                    itemView.setOnLongClickListener{
                        listener?.onLongClick(data)
                        true
                    }
                }
                data.type == Data.TYPE_ROUTE -> {
                    val route = data.obj as Route
                    val routeName = route.name
                    if (route.companyCode == C.PROVIDER.MTR) {
                        itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_directions_railway_24dp)
                    } else {
                        itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_directions_bus_24dp)
                    }
                    Route.companyColour(itemView.context, route.companyCode!!, route.name)?.run {
                        itemView.findViewById<ImageView>(R.id.icon).colorFilter = PorterDuffColorFilter(this, PorterDuff.Mode.SRC_IN)
                    }
                    itemView.findViewById<TextView>(R.id.route).text = routeName
                    if (!route.destination.isNullOrEmpty() && !route.origin.isNullOrEmpty()) {
                        itemView.findViewById<TextView>(R.id.location).visibility = View.VISIBLE
                        itemView.findViewById<TextView>(R.id.location).text =
                                itemView.context.getString(R.string.direction_both_ways, route.origin, route.destination)
                    } else if (!route.destination.isNullOrEmpty()) {
                        itemView.findViewById<TextView>(R.id.location).visibility = View.VISIBLE
                        itemView.findViewById<TextView>(R.id.location).text = route.destination
                    } else if (!route.origin.isNullOrEmpty()) {
                        itemView.findViewById<TextView>(R.id.location).visibility = View.VISIBLE
                        itemView.findViewById<TextView>(R.id.location).text = route.origin
                    } else {
                        itemView.findViewById<TextView>(R.id.location).visibility = View.GONE
                    }
                    itemView.findViewById<TextView>(R.id.description).text = route.description
                    itemView.findViewById<TextView>(R.id.description).visibility = if (route.description.isNullOrEmpty()) View.GONE else View.VISIBLE
                    itemView.setOnClickListener{ listener?.onClick(data) }
                    itemView.setOnLongClickListener{
                        listener?.onLongClick(data)
                        true
                    }
                }
                data.type == Data.TYPE_SECTION -> itemView.section_label.text = data.obj as String
                data.type == Data.TYPE_BUTTON -> {
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

}