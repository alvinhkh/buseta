package com.alvinhkh.buseta.nwst.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.nwst.model.NwstNotice
import kotlinx.android.synthetic.main.item_route_announce.view.*
import java.net.URLEncoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class NwstNoticeViewAdapter(
        private val data: MutableList<NwstNotice> = mutableListOf()
): RecyclerView.Adapter<NwstNoticeViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_announce, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun replace(list: List<NwstNotice>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!){

        fun bindItems(nwstNotice: NwstNotice) {
            itemView.icon.setImageResource(R.drawable.ic_outline_event_note_24dp)
            itemView.title.text = nwstNotice.title
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                val d = SimpleDateFormat("dd/MM", Locale.ENGLISH)
                val t = SimpleDateFormat("HH:mm", Locale.ENGLISH)
                val date = sdf.parse(nwstNotice.releaseDate)
                var iconText = d.format(date)
                if (t.format(date) != "00:00") {
                    iconText += "\n" + t.format(date)
                }
                itemView.icon.visibility = View.GONE
                itemView.iconText.visibility = View.VISIBLE
                itemView.iconText.text = iconText
            } catch (ignored: ParseException) {
                itemView.icon.visibility = View.VISIBLE
                itemView.iconText.visibility = View.GONE
            }

            itemView.setOnClickListener { v ->
                if (Patterns.WEB_URL.matcher(nwstNotice.link).matches()) {
                    if (nwstNotice.link.contains(".pdf")) {
                        openPdf(nwstNotice.link)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(nwstNotice.link)
                        v.context.startActivity(intent)
                    }
                }
            }
        }

        private fun openPdf(url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setPackage("com.google.android.apps.docs")
                intent.setDataAndType(Uri.parse(url), "application/pdf")
                itemView.context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                            Uri.parse("https://docs.google.com/viewer?embedded=true&url=" + URLEncoder.encode(url, "utf-8")),
                            "text/html")
                    itemView.context.startActivity(intent)
                } catch (ignored: Throwable) {
                }
            }
        }
    }

}