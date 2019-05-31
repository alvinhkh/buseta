package com.alvinhkh.buseta.ui


import androidx.recyclerview.widget.RecyclerView

/**
 * Listener for manual initiation of item touch.
 */
interface OnItemDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onItemStartDrag(viewHolder: RecyclerView.ViewHolder)

    /**
     * Called when a view is requesting a stop of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onItemStopDrag(viewHolder: RecyclerView.ViewHolder)

}