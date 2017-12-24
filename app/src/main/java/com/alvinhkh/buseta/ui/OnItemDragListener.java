package com.alvinhkh.buseta.ui;


import android.support.v7.widget.RecyclerView;

/**
 * Listener for manual initiation of item touch.
 */
public interface OnItemDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onItemStartDrag(RecyclerView.ViewHolder viewHolder);

    /**
     * Called when a view is requesting a stop of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onItemStopDrag(RecyclerView.ViewHolder viewHolder);

}