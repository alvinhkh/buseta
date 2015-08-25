package com.alvinhkh.buseta.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    public ViewHolderClicks mClicks;

    public RecyclerViewHolder(View v, ViewHolderClicks clicks) {
        super(v);
        mClicks = clicks;
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mClicks.onClickView(v);
    }

    @Override
    public boolean onLongClick(View v) {
        return mClicks.onLongClickView(v);
    }

    public interface ViewHolderClicks {
        void onClickView(View caller);
        boolean onLongClickView(View caller);
    }

}