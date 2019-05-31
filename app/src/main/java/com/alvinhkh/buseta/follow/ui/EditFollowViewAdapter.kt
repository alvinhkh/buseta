package com.alvinhkh.buseta.follow.ui

import android.annotation.SuppressLint
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.ui.*
import java.util.Locale

class EditFollowViewAdapter(
        private val recyclerView: RecyclerView,
        private val listener: OnItemDragListener?,
        private val data: MutableList<Follow> = mutableListOf()
): RecyclerView.Adapter<EditFollowViewAdapter.Holder>(), ItemTouchHelperAdapter {

    private val followDatabase = FollowDatabase.getInstance(recyclerView.context)

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_route_follow_edit, parent, false), listener)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onItemDismiss(position: Int) {
        if (position > data.size) return
        val follow = data[position]
        val rowDeleted = followDatabase?.followDao()?.delete(follow.groupId, follow.companyCode, follow.routeNo, follow.routeSeq, follow.routeServiceType, follow.stopId, follow.stopSeq)
        if (rowDeleted != null && rowDeleted > 0) {
            data.removeAt(position)
            notifyItemRemoved(position)
            Snackbar.make(recyclerView.rootView.findViewById(R.id.coordinator_layout)?:recyclerView,
                    recyclerView.context.getString(R.string.removed_from_follow_list,
                            String.format(Locale.ENGLISH, "%s %s", follow.routeNo, follow.stopName)),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        followDatabase?.followDao()?.insert(follow)
                        data.add(position, follow)
                        notifyItemInserted(position)
                    }.show()
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun addItem(f: Follow) {
        data.add(f)
        notifyItemInserted(data.size)
    }

    fun replaceItems(l: MutableList<Follow>) {
        data.clear()
        data.addAll(l)
        notifyDataSetChanged()
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?, private val onItemDragListener: OnItemDragListener?):
            RecyclerView.ViewHolder(itemView!!), ItemTouchHelperViewHolder {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(follow: Follow) {
            itemView.findViewById<ImageView>(R.id.drag_handle).setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onItemDragListener?.onItemStartDrag(this)
                }
                true
            }
            itemView.findViewById<TextView>(R.id.name).text = follow.stopName
            itemView.findViewById<TextView>(R.id.route_no).text = follow.routeNo
            itemView.findViewById<TextView>(R.id.route_location_end).text = follow.routeDestination
            itemView.setOnLongClickListener{
                onItemDragListener?.onItemStartDrag(this)
                true
            }
        }

        override fun onItemSelected() {}

        override fun onItemClear() {
            onItemDragListener?.onItemStopDrag(this)
        }

    }

}