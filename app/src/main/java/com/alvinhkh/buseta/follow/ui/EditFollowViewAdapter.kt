package com.alvinhkh.buseta.follow.ui

import android.annotation.SuppressLint
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
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
import kotlinx.android.synthetic.main.item_section.view.*
import java.util.Locale

class EditFollowViewAdapter(
        private val recyclerView: RecyclerView,
        private var data: MutableList<Data>?,
        private val listener: OnItemDragListener?
): RecyclerView.Adapter<EditFollowViewAdapter.Holder>(),
        PinnedHeaderItemDecoration.PinnedHeaderAdapter,
        ItemTouchHelperAdapter {

    private val context = recyclerView.context

    private val followDatabase = FollowDatabase.getInstance(context)

    data class Data(
            var type: Int,
            var obj: Any
    ) {
        companion object {
            const val TYPE_SECTION = 1

            const val TYPE_FOLLOW = 2
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data?.get(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (viewType == Data.TYPE_FOLLOW) {
            return Holder(LayoutInflater.from(context).inflate(R.layout.item_route_follow_edit, parent, false), listener)
        }
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_section, parent, false), listener)
    }

    override fun getItemViewType(position: Int): Int {
        return data?.get(position)?.type?:0
    }

    override fun getItemCount(): Int = data?.size?:0

    override fun isPinnedViewType(viewType: Int): Boolean {
        return viewType == Data.TYPE_SECTION
    }

    override fun onItemDismiss(position: Int) {
        if (position > data?.size!!) return
        if (data?.get(position)?.type != Data.TYPE_FOLLOW) return
        val follow = data?.get(position)?.obj as Follow
        val rowDeleted = followDatabase?.followDao()?.delete(follow.type, follow.companyCode, follow.routeNo, follow.routeSeq, follow.routeServiceType, follow.stopId, follow.stopSeq)
        if (rowDeleted != null && rowDeleted > 0) {
            notifyItemRemoved(position)
            Snackbar.make(recyclerView.rootView.findViewById(R.id.constraint_layout)?:recyclerView,
                    context.getString(R.string.removed_from_follow_list,
                    String.format(Locale.ENGLISH, "%s %s", follow.routeNo, follow.stopName)),
                    Snackbar.LENGTH_LONG)
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(snackbar: Snackbar?, event: Int) {
                            when (event) {
                                Snackbar.Callback.DISMISS_EVENT_ACTION -> {
                                    followDatabase?.followDao()?.insert(follow)
                                    notifyItemInserted(position)
                                }
                            }
                        }
                    })
                    .setAction(R.string.undo) { _ ->
                        // do nothing
                    }.show()
        }
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun addSection(s: String) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_SECTION, s))
        notifyItemInserted(data?.size?:0)
    }

    fun addItem(t: Follow) {
        if (data == null) {
            data = mutableListOf()
        }
        data?.add(Data(Data.TYPE_FOLLOW, t))
        notifyItemInserted(data?.size?:0)
    }

    fun clear() {
        data?.clear()
        notifyDataSetChanged()
    }

    class Holder(itemView: View?, private val onItemDragListener: OnItemDragListener?):
            RecyclerView.ViewHolder(itemView!!), ItemTouchHelperViewHolder {

        @SuppressLint("ClickableViewAccessibility")
        fun bindItems(data: Data?) {
            if (data?.type == Data.TYPE_FOLLOW) {
                val follow = data.obj as Follow
                itemView.findViewById<ImageView>(R.id.drag_handle).setOnTouchListener({ _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        onItemDragListener?.onItemStartDrag(this)
                    }
                    true
                })
                itemView.findViewById<TextView>(R.id.name).text = follow.stopName
                itemView.findViewById<TextView>(R.id.route_no).text = follow.routeNo
                itemView.findViewById<TextView>(R.id.route_location_end).text = follow.routeDestination
                itemView.setOnLongClickListener{
                    onItemDragListener?.onItemStartDrag(this)
                    true
                }
            } else if (data?.type == Data.TYPE_SECTION) {
                itemView.section_label.text = data.obj as String
            }
        }

        override fun onItemSelected() {}

        override fun onItemClear() {
            onItemDragListener?.onItemStopDrag(this)
        }

    }

}