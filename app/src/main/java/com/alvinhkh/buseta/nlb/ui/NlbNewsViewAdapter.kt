package com.alvinhkh.buseta.nlb.ui

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.nlb.NlbService
import com.alvinhkh.buseta.nlb.model.NlbNews
import com.alvinhkh.buseta.nlb.model.NlbNewsRequest
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.ui.webview.WebViewActivity
import kotlinx.android.synthetic.main.item_route_announce.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class NlbNewsViewAdapter(
        val companyCode: String,
        private val data: MutableList<NlbNews> = mutableListOf()
): RecyclerView.Adapter<NlbNewsViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(companyCode, LayoutInflater.from(parent.context).inflate(R.layout.item_route_announce, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun replace(list: List<NlbNews>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(val companyCode: String, itemView: View?): RecyclerView.ViewHolder(itemView!!){

        fun bindItems(nlbNews: NlbNews) {
            itemView.icon.setImageResource(R.drawable.ic_outline_event_note_24dp)
            itemView.title.text = nlbNews.title
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val displaySdf = SimpleDateFormat("dd/MM", Locale.ENGLISH)
                val date = sdf.parse(nlbNews.publishDate)
                itemView.icon.visibility = View.GONE
                itemView.iconText.visibility = View.VISIBLE
                itemView.iconText.text = displaySdf.format(date)
            } catch (ignored: ParseException) {
                itemView.icon.visibility = View.VISIBLE
                itemView.iconText.visibility = View.GONE
            }

            itemView.setOnClickListener { v ->
                CoroutineScope(Dispatchers.Main).launch {
                    val request = NlbNewsRequest(nlbNews.newsId, "zh")
                    val response = if (companyCode == C.PROVIDER.GMB901) {
                        val gmb901Service = NlbService.gmb901ApiCoroutine.create(NlbService::class.java)
                        gmb901Service.news(request).await()
                    } else {
                        val nlbService = NlbService.nlbApiCoroutine.create(NlbService::class.java)
                        nlbService.news(request).await()
                    }
                    val body = response.body()
                    if (response.isSuccessful && body?.news?.content?.isEmpty() == false) {
                        val intent = Intent(v.context, WebViewActivity::class.java)
                        intent.putExtra(WebViewActivity.TITLE, body.news?.title)
                        intent.putExtra(WebViewActivity.HTML, body.news?.content)
                        intent.putExtra(WebViewActivity.COLOUR, Route.companyColour(itemView.context, companyCode, ""))
                        v.context.startActivity(intent)
                    }
                }
            }
        }
    }

}