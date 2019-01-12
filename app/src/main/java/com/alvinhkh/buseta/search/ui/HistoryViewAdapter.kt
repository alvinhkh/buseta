package com.alvinhkh.buseta.search.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.model.Suggestion

class HistoryViewAdapter(
        recyclerView: RecyclerView,
        private val data: MutableList<Data> = mutableListOf()
): RecyclerView.Adapter<HistoryViewAdapter.Holder>() {

    private val suggestionDatabase = SuggestionDatabase.getInstance(recyclerView.context)
    private val context = recyclerView.context

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_HISTORY = 1
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], suggestionDatabase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (viewType == Data.TYPE_HISTORY) {
            return Holder(LayoutInflater.from(context).inflate(R.layout.item_route_history, parent, false))
        }
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_section, parent, false))
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].type
    }

    override fun getItemCount(): Int = data.size

    fun addItem(t: Suggestion): Int {
        data.add(Data(Data.TYPE_HISTORY, t))
        val index = data.size
        notifyItemInserted(index)
        return index - 1
    }

    fun replaceItem(index: Int, t: Suggestion) {
        if (index < data.size && index >= 0) {
            data[index] = Data(Data.TYPE_HISTORY, t)
            notifyItemChanged(index)
        }
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(data: Data?, suggestionDatabase: SuggestionDatabase?) {
            if (data?.type == Data.TYPE_HISTORY) {
                val suggestion = data.obj as Suggestion
                itemView.findViewById<TextView>(android.R.id.text1).text = suggestion.route
                if (suggestion.companyCode == C.PROVIDER.MTR) {
                    itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_directions_railway_24dp)
                } else {
                    itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_directions_bus_24dp)
                }
                Route.companyColour(itemView.context, suggestion.companyCode, suggestion.route)?.run {
                    itemView.findViewById<ImageView>(R.id.icon).colorFilter = PorterDuffColorFilter(this, PorterDuff.Mode.SRC_IN)
                }

                itemView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(itemView.context, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, suggestion.companyCode)
                    intent.putExtra(C.EXTRA.ROUTE_NO, suggestion.route)
                    itemView.context.startActivity(intent)
                }
                itemView.setOnLongClickListener {
                    val builder = AlertDialog.Builder(itemView.context)
                    val companyName = Route.companyName(itemView.context, suggestion.companyCode, suggestion.route)
                    builder.setTitle(companyName + " " + suggestion.route + "?")
                    builder.setMessage(itemView.context.getString(R.string.message_remove_from_search_history))
                    builder.setNegativeButton(R.string.action_cancel) { d, _ -> d.cancel() }
                    builder.setPositiveButton(R.string.action_confirm) { _, _ ->
                        val rowDeleted = suggestionDatabase?.suggestionDao()?.delete(suggestion.type, suggestion.companyCode, suggestion.route)
                        if (rowDeleted != null && rowDeleted > 0) {
                            Snackbar.make(itemView.rootView.findViewById(R.id.coordinator_layout)?:itemView.rootView,
                                    itemView.context.getString(R.string.removed_from_search_history, companyName + " " + suggestion.route),
                                    Snackbar.LENGTH_LONG)
                                    .addCallback(object : Snackbar.Callback() {
                                        override fun onDismissed(snackbar: Snackbar?, event: Int) {
                                            when (event) {
                                                Snackbar.Callback.DISMISS_EVENT_ACTION -> {
                                                    suggestionDatabase.suggestionDao().insert(suggestion)
                                                }
                                            }
                                        }
                                    })
                                    .setAction(R.string.undo) { _ ->
                                        // do nothing
                                    }.show()
                        }
                    }
                    builder.show()
                    true
                }
            }
        }
    }

}