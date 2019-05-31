package com.alvinhkh.buseta.kmb.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.kmb.KmbService
import com.alvinhkh.buseta.kmb.model.KmbAnnounce
import com.alvinhkh.buseta.ui.image.ImageActivity
import kotlinx.android.synthetic.main.item_route_announce.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import timber.log.Timber
import java.net.URLEncoder

class KmbAnnounceViewAdapter(
        private val data: MutableList<KmbAnnounce> = mutableListOf()
): RecyclerView.Adapter<KmbAnnounceViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_announce, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun replace(list: List<KmbAnnounce>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!){

        fun bindItems(kmbAnnounce: KmbAnnounce) {
            if (!kmbAnnounce.url.isEmpty() && kmbAnnounce.url.contains(".jpg")) {
                itemView.icon.setImageResource(R.drawable.ic_outline_event_note_24dp)
            } else if (!kmbAnnounce.url.isEmpty() && kmbAnnounce.url.contains(".pdf")) {
                itemView.icon.setImageResource(R.drawable.ic_outline_picture_as_pdf_24dp)
            } else {
                itemView.icon.setImageResource(R.drawable.ic_outline_event_note_24dp)
            }
            itemView.title.text = kmbAnnounce.titleTc

            itemView.setOnClickListener { v ->
                when {
                    kmbAnnounce.url.contains(".jpg") -> {
                        val intent = Intent(v.context, ImageActivity::class.java)
                        intent.putExtra(ImageActivity.IMAGE_TITLE, kmbAnnounce.titleTc)
                        intent.putExtra(ImageActivity.IMAGE_URL,
                                KmbService.ANNOUNCEMENT_PICTURE + kmbAnnounce.url)
                        v.context.startActivity(intent)
                    }
                    kmbAnnounce.url.contains(".pdf") -> openPdf(KmbService.ANNOUNCEMENT_PICTURE + kmbAnnounce.url)
                    else -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            val kmbService = KmbService.webSearchHtmlCoroutine.create(KmbService::class.java)
                            val response = kmbService.announcementPicture(kmbAnnounce.url).await()
                            val body = response.body()
                            if (response.isSuccessful && body != null) {
                                val contentType = body.contentType()?.toString()?:""
                                when {
                                    contentType.contains("image") -> {
                                        val intent = Intent(v.context, ImageActivity::class.java)
                                        intent.putExtra(ImageActivity.IMAGE_TITLE, kmbAnnounce.titleTc)
                                        intent.putExtra(ImageActivity.IMAGE_URL,
                                                KmbService.ANNOUNCEMENT_PICTURE + kmbAnnounce.url)
                                        v.context.startActivity(intent)
                                    }
                                    contentType.contains("html") -> try {
                                        val doc = Jsoup.parse(body.string())
                                        val htmlBody = doc.select("body").first()
                                        if (htmlBody != null) {
                                            val p = htmlBody.select("p")
                                            val sb = StringBuilder()
                                            for (i in p.indices) {
                                                sb.append(p[i].text())
                                                if (i < p.size - 1)
                                                    sb.append("\n\n")
                                            }
                                            if (!TextUtils.isEmpty(sb)) {
                                                // TODO: maybe another format instead of dialog
                                                AlertDialog.Builder(v.context)
                                                        .setTitle(kmbAnnounce.titleTc)
                                                        .setMessage(sb)
                                                        .setPositiveButton(R.string.action_confirm) { dialoginterface, _ -> dialoginterface.cancel() }.show()
                                            }
                                        }
                                    } catch (e: Throwable) {
                                        Timber.e(e)
                                    }
                                    else -> Timber.d(kmbAnnounce.toString())
                                }
                            }
                        }
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