package com.alvinhkh.buseta.mtr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.mtr.model.MtrLineStatus
import com.alvinhkh.buseta.search.ui.SearchActivity

class MtrLineStatusViewAdapter(
        private var data: MutableList<MtrLineStatus> = mutableListOf()
): RecyclerView.Adapter<MtrLineStatusViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_railway_status, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun replace(l: List<MtrLineStatus>) {
        data.clear()
        data.addAll(l)
        notifyDataSetChanged()
    }

    class Holder(itemView: View): RecyclerView.ViewHolder(itemView){

        fun bindItems(status: MtrLineStatus) {
            itemView.findViewById<TextView>(R.id.name).text = status.lineName
            itemView.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_outline_directions_railway_24dp)
            if (!status.lineColour.isEmpty()) {
                itemView.findViewById<ImageView>(R.id.icon).setColorFilter(Color.parseColor(status.lineColour))
            }
            if (!status.status.isEmpty()) {
                when (status.status.toLowerCase()) {
                    "green", "yellow", "pink", "red", "grey", "typhoon" -> {
                        val packageName = itemView.context.packageName
                        val resId = itemView.context.resources.getIdentifier("mtr_status_" + status.status.toLowerCase(), "color", packageName)
                        val color = if (resId == 0) {
                            R.color.grey
                        } else {
                            ContextCompat.getColor(itemView.context, resId)
                        }
                        itemView.findViewById<ImageView>(R.id.circle).setColorFilter(color)
                    }
                }
            }
            if (!status.urlTc.isEmpty()) {
                itemView.findViewById<ImageView>(R.id.open_url).visibility = View.VISIBLE
                itemView.findViewById<ImageView>(R.id.open_url).setOnClickListener { v ->
                    val link = Uri.parse(status.urlTc)
                    if (link != null) {
                        try {
                            val builder = CustomTabsIntent.Builder()
                            builder.setToolbarColor(ContextCompat.getColor(v.context, R.color.colorPrimary))
                            val customTabsIntent = builder.build()
                            customTabsIntent.launchUrl(v.context, link)
                        } catch (ignored: Throwable) {
                            val intent = Intent(Intent.ACTION_VIEW, link)
                            if (intent.resolveActivity(v.context.packageManager) != null) {
                                itemView.context.startActivity(intent)
                            }
                        }

                    }
                }
            } else {
                itemView.findViewById<ImageView>(R.id.open_url).visibility = View.GONE
            }
            itemView.setOnClickListener { v ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setClass(v.context, SearchActivity::class.java)
                intent.putExtra(C.EXTRA.TYPE, C.TYPE.RAILWAY)
                intent.putExtra(C.EXTRA.LINE_CODE, status.lineCode)
                intent.putExtra(C.EXTRA.LINE_COLOUR, status.lineColour)
                intent.putExtra(C.EXTRA.LINE_NAME, status.lineName)
                itemView.context.startActivity(intent)
            }
        }
    }

}