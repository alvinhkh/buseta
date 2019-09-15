package com.alvinhkh.buseta.route.ui

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.viewpager.widget.ViewPager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route
import java.lang.ref.WeakReference

class RouteSelectViewAdapter(
        private val viewPagerRef: WeakReference<ViewPager?>,
        private val fragmentRef: WeakReference<BottomSheetDialogFragment?>,
        private val data: MutableList<Route> = mutableListOf()
): RecyclerView.Adapter<RouteSelectViewAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], viewPagerRef, fragmentRef)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_select, parent, false))
    }

    override fun getItemCount(): Int = data.size

    fun addItem(r: Route) {
        data.add(r)
        notifyItemInserted(data.size)
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    fun replaceItems(l: MutableList<Route>) {
        data.clear()
        data.addAll(l)
        notifyDataSetChanged()
    }

    class Holder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(route: Route, viewPagerRef: WeakReference<ViewPager?>, fragmentRef: WeakReference<BottomSheetDialogFragment?>) {
            val routeName = Route.companyName(itemView.context, route.companyCode!!, route.name) + " " + route.name
            itemView.findViewById<TextView>(R.id.route).text = routeName
            if (!route.destination.isNullOrEmpty() && !route.origin.isNullOrEmpty()) {
                itemView.findViewById<TextView>(R.id.location).visibility = View.VISIBLE
                itemView.findViewById<TextView>(R.id.location).text =
                        itemView.context.getString(R.string.route_path, route.origin, route.destination)
            } else if (!route.destination.isNullOrEmpty()) {
                itemView.findViewById<TextView>(R.id.location).visibility = View.VISIBLE
                itemView.findViewById<TextView>(R.id.location).text =
                        itemView.context.getString(R.string.destination, route.destination)
            } else if (!route.origin.isNullOrEmpty()) {
                itemView.findViewById<TextView>(R.id.location).visibility = View.VISIBLE
                itemView.findViewById<TextView>(R.id.location).text = route.origin
            } else {
                itemView.findViewById<TextView>(R.id.location).visibility = View.GONE
            }
            itemView.findViewById<TextView>(R.id.description).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(route.description, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(route.description)
            }
            itemView.findViewById<TextView>(R.id.description).visibility = if (route.description.isNullOrEmpty()) View.GONE else View.VISIBLE
            itemView.setOnClickListener {
                viewPagerRef.get()?.currentItem = adapterPosition
                fragmentRef.get()?.dismiss()
            }
        }
    }

}