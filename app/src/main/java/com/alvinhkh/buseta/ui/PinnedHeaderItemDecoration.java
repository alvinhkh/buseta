package com.alvinhkh.buseta.ui;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;

/**
 * ItemDecoration for Pinned Header.
 *
 * https://github.com/takahr/pinned-section-item-decoration
 * porting from https://github.com/beworker/pinned-section-listview
 */
public class PinnedHeaderItemDecoration extends RecyclerView.ItemDecoration {

    public interface PinnedHeaderAdapter {
        boolean isPinnedViewType(int viewType);
    }

    private RecyclerView.Adapter mAdapter;

    // cached data
    // pinned header view
    private View mPinnedHeaderView;
    private int mHeaderPosition = -1;

    private SparseBooleanArray mPinnedViewTypes = new SparseBooleanArray();

    private int mPinnedHeaderTop;
    private Rect mClipBounds;

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        createPinnedHeader(parent);

        if (mPinnedHeaderView != null) {
            // check overlap section view.
            // TODO: support only vertical header currently.
            final int headerEndAt = mPinnedHeaderView.getTop() + mPinnedHeaderView.getHeight();
            final View v = parent.findChildViewUnder(c.getWidth() / 2, headerEndAt - 1);

            if (isHeaderView(parent, v)) {
                mPinnedHeaderTop = v.getTop() - mPinnedHeaderView.getHeight();
            } else {
                mPinnedHeaderTop = 0;
            }

            mClipBounds = c.getClipBounds();
            mClipBounds.top = mPinnedHeaderTop + mPinnedHeaderView.getHeight();
            c.clipRect(mClipBounds);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mPinnedHeaderView != null) {
            c.save();

            mClipBounds.top = 0;
            c.clipRect(mClipBounds, Region.Op.UNION);
            c.translate(0, mPinnedHeaderTop);
            mPinnedHeaderView.draw(c);

            c.restore();
        }
    }

    private void createPinnedHeader(RecyclerView parent) {
        checkCache(parent);

        // get LinearLayoutManager.
        final LinearLayoutManager linearLayoutManager;
        final RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            linearLayoutManager = (LinearLayoutManager) layoutManager;
        } else {
            return;
        }

        final int firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition();
        final int headerPosition = findPinnedHeaderPosition(firstVisiblePosition);

        if (headerPosition >= 0 && mHeaderPosition != headerPosition) {
            mHeaderPosition = headerPosition;
            final int viewType = mAdapter.getItemViewType(headerPosition);

            final RecyclerView.ViewHolder pinnedViewHolder = mAdapter.createViewHolder(parent, viewType);
            mAdapter.bindViewHolder(pinnedViewHolder, headerPosition);
            mPinnedHeaderView = pinnedViewHolder.itemView;

            // read layout parameters
            ViewGroup.LayoutParams layoutParams = mPinnedHeaderView.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mPinnedHeaderView.setLayoutParams(layoutParams);
            }

            int heightMode = View.MeasureSpec.getMode(layoutParams.height);
            int heightSize = View.MeasureSpec.getSize(layoutParams.height);

            if (heightMode == View.MeasureSpec.UNSPECIFIED) {
                heightMode = View.MeasureSpec.EXACTLY;
            }

            final int maxHeight = parent.getHeight() - parent.getPaddingTop() - parent.getPaddingBottom();
            if (heightSize > maxHeight) {
                heightSize = maxHeight;
            }

            // measure & layout
            final int ws = View.MeasureSpec.makeMeasureSpec(parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight(), View.MeasureSpec.EXACTLY);
            final int hs = View.MeasureSpec.makeMeasureSpec(heightSize, heightMode);
            mPinnedHeaderView.measure(ws, hs);
            mPinnedHeaderView.layout(0, 0, mPinnedHeaderView.getMeasuredWidth(), mPinnedHeaderView.getMeasuredHeight());
        }
    }

    private int findPinnedHeaderPosition(int fromPosition) {
        if (fromPosition > mAdapter.getItemCount()) {
            return -1;
        }

        for (int position = fromPosition; position >= 0; position--) {
            final int viewType = mAdapter.getItemViewType(position);
            if (isPinnedViewType(viewType)) {
                return position;
            }
        }

        return -1;
    }

    private boolean isPinnedViewType(int viewType) {
        if (mPinnedViewTypes.indexOfKey(viewType) <= -1) {
            mPinnedViewTypes.put(viewType, ((PinnedHeaderAdapter) mAdapter).isPinnedViewType(viewType));
        }

        return mPinnedViewTypes.get(viewType);
    }

    private boolean isHeaderView(RecyclerView parent, View v) {
        final int position = parent.getChildLayoutPosition(v);
        if (position == RecyclerView.NO_POSITION) {
            return false;
        }
        final int viewType = mAdapter.getItemViewType(position);

        return isPinnedViewType(viewType);
    }

    private void checkCache(RecyclerView parent) {
        RecyclerView.Adapter adapter = parent.getAdapter();
        if (mAdapter != adapter) {
            disableCache();
            if (adapter instanceof PinnedHeaderAdapter) {
                mAdapter = adapter;
            } else {
                mAdapter = null;
            }
        }
    }

    private void disableCache() {
        mPinnedHeaderView = null;
        mHeaderPosition = -1;
        mPinnedViewTypes.clear();
    }

}