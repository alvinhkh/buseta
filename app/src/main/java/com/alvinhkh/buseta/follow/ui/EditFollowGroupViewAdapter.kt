package com.alvinhkh.buseta.follow.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.fragment.app.FragmentManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.model.FollowGroup
import com.alvinhkh.buseta.ui.*
import java.lang.ref.WeakReference

class EditFollowGroupViewAdapter(
        private val fragmentManagerRef: WeakReference<FragmentManager>,
        private val listener: OnItemDragListener?,
        private val data: MutableList<FollowGroup> = mutableListOf()
): RecyclerView.Adapter<EditFollowGroupViewAdapter.Holder>(), ItemTouchHelperAdapter {

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bindItems(data[position], fragmentManagerRef)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_follow_group_edit, parent, false), listener)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onItemDismiss(position: Int) {
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun addItem(f: FollowGroup) {
        data.add(f)
        notifyItemInserted(data.size)
    }

    fun replaceItems(l: MutableList<FollowGroup>) {
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
        fun bindItems(followGroup: FollowGroup, fragmentManagerRef: WeakReference<FragmentManager>) {
            itemView.findViewById<ImageView>(R.id.drag_handle).setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onItemDragListener?.onItemStartDrag(this)
                }
                true
            }
            val name = when {
                followGroup.name.isNotEmpty() -> followGroup.name
                followGroup.id == FollowGroup.UNCATEGORISED -> itemView.context.getString(R.string.uncategorised)
                else -> followGroup.id
            }
            itemView.findViewById<TextView>(R.id.name).text = name
            val color = if (!followGroup.colour.isEmpty()) Color.parseColor(followGroup.colour) else ContextCompat.getColor(itemView.context, R.color.colorPrimary)
            itemView.findViewById<ImageView>(R.id.colour).colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
            itemView.findViewById<ImageView>(R.id.edit).setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val fragmentManager = fragmentManagerRef.get()
                    if (fragmentManager != null) {
                        val fragmentTransaction = fragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.fragment_container, EditFollowFragment.newInstance(followGroup.id))
                        fragmentTransaction.addToBackStack("edit_follow_list")
                        fragmentTransaction.commit()
                    }
                }
                true
            }
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