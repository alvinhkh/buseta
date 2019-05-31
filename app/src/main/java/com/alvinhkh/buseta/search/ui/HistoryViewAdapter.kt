package com.alvinhkh.buseta.search.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
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
        private val data: MutableList<Suggestion> = mutableListOf()
): RecyclerView.Adapter<HistoryViewAdapter.Holder>() {

    private val suggestionDatabase = SuggestionDatabase.getInstance(recyclerView.context)

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], suggestionDatabase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_history, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun replace(list: List<Suggestion>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(suggestion: Suggestion, suggestionDatabase: SuggestionDatabase?) {
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
                                .setAction(R.string.undo) {
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