package com.alvinhkh.buseta.mtr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.mtr.model.MtrLatestAlert
import com.alvinhkh.buseta.mtr.model.MtrLineStatus
import com.alvinhkh.buseta.search.ui.SearchActivity
import java.util.*

class MtrLineStatusViewAdapter(
        private var data: MutableList<MtrLineStatus> = mutableListOf(),
        private var alert: MutableList<MtrLatestAlert> = mutableListOf()
): RecyclerView.Adapter<MtrLineStatusViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (position >= 0 && position < data.size) {
            holder.bindItems(data[position], 0)
        } else if (position - data.size < alert.size) {
            holder.bindItems(alert[position - data.size], 1)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(if (viewType == 0) {
            R.layout.item_railway_status
        } else {
            R.layout.item_note
        }, parent, false))
    }

    override fun getItemCount(): Int = data.size + alert.size

    override fun getItemViewType(position: Int): Int {
        return if (position >= 0 && position < data.size) {
            0
        } else if (position >= data.size && position < alert.size) {
            1
        } else {
            -1
        }
    }

    fun replace(l: List<MtrLineStatus>) {
        data.clear()
        data.addAll(l)
        notifyDataSetChanged()
    }

    fun alert(l: List<MtrLatestAlert>) {
        alert.clear()
        alert.addAll(l)
        notifyDataSetChanged()
    }

    class Holder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bindItems(item: Any, viewType: Int) {
            if (viewType == 0) {
                val status = item as MtrLineStatus

                itemView.findViewById<TextView>(R.id.name).text = status.lineName
                itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_directions_railway_24dp)
                if (status.lineColour.isNotEmpty()) {
                    itemView.findViewById<ImageView>(R.id.icon).setColorFilter(Color.parseColor(status.lineColour))
                }
                if (status.status.isNotEmpty()) {
                    when (status.status.toLowerCase(Locale.ROOT)) {
                        "green", "yellow", "pink", "red", "grey", "typhoon" -> {
                            val packageName = itemView.context.packageName
                            val resId = itemView.context.resources.getIdentifier("mtr_status_" + status.status.toLowerCase(Locale.ROOT), "color", packageName)
                            val color = if (resId == 0) {
                                R.color.grey
                            } else {
                                ContextCompat.getColor(itemView.context, resId)
                            }
                            itemView.findViewById<ImageView>(R.id.circle).setColorFilter(color)
                        }
                    }
                }
                if (status.urlTc.isNotEmpty()) {
                    itemView.findViewById<ImageView>(R.id.open_url).visibility = View.VISIBLE
                    itemView.findViewById<ImageView>(R.id.open_url).setOnClickListener { v ->
                        openLink(v.context, status.urlTc, R.color.provider_mtr)
                    }
                } else {
                    itemView.findViewById<ImageView>(R.id.open_url).visibility = View.GONE
                }
                itemView.setOnClickListener { v ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(v.context, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, C.PROVIDER.MTR)
                    intent.putExtra(C.EXTRA.ROUTE_NO, status.lineName)
                    itemView.context.startActivity(intent)
                }
            } else if (viewType == 1) {
                val alert = item as MtrLatestAlert
                itemView.findViewById<TextView>(R.id.note).text = if (!alert.msgTc.isNullOrEmpty()) {
                    alert.msgTc + " >>"
                } else {
                    alert.urlTc
                }
                itemView.setOnClickListener { v ->
                    openLink(v.context, alert.urlTc, R.color.provider_mtr)
                }
            }
        }

        private fun openLink(context: Context, url: String, colorInt: Int) {
            val link = Uri.parse(url)
            try {
                val builder = CustomTabsIntent.Builder()
                builder.setToolbarColor(ContextCompat.getColor(context, colorInt))
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(context, link)
            } catch (ignored: Throwable) {
                val intent = Intent(Intent.ACTION_VIEW, link)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        }
    }

}