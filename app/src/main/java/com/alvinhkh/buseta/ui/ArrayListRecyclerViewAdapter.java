package com.alvinhkh.buseta.ui;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/*
 * ArrayList RecyclerViewAdapter with the ability to load more data,
 * implemented PinnedHeaderItemDecoration to provide types of view in single recyclerview
 */
public abstract class ArrayListRecyclerViewAdapter<VH extends ArrayListRecyclerViewAdapter.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements PinnedHeaderItemDecoration.PinnedHeaderAdapter {

    protected List<Item> items;

    protected int lastVisibleItem, totalItemCount;

    protected boolean loading;

    protected OnLoadMoreListener onLoadMoreListener;

    protected OnClickItemListener onClickItemListener;

    public static class Item {
        public final static int TYPE_SECTION = 0;
        public final static int TYPE_DATA = 1;
        public final static int TYPE_HEADER = 2;
        public final static int TYPE_FOOTER = 3;

        int type;
        Object object;

        public Item(int type, @NonNull Object object) {
            this.type = type;
            this.object = object;
        }

        protected Item(Parcel in) {
            type = in.readInt();
        }

        public String getText() {
            return this.object.toString();
        }

        public int getType() {
            return this.type;
        }

        public Object getObject() {
            return this.object;
        }
    }

    public ArrayListRecyclerViewAdapter(@NonNull RecyclerView recyclerView) {
        items = new ArrayList<>();

        recyclerView.addItemDecoration(new PinnedHeaderItemDecoration());
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (!loading && totalItemCount <= (lastVisibleItem + 1)) {
                        // End has been reached
                        // Do something
                        if (onLoadMoreListener != null) {
                            onLoadMoreListener.onLoadMore();
                        }
                        loading = true;
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getDataItemCount() {
        int count = 0;
        for (Item item : items)
            if (item.getType() == Item.TYPE_DATA)
                count++;
        return count;
    }

    public List<Item> getDataItems() {
        List<Item> list = new ArrayList<>();
        for (Item item : items)
            if (item.getType() == Item.TYPE_DATA)
                list.add(item);
        return list;
    }

    public Item getLastSection() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).getType() == Item.TYPE_SECTION && items.get(i).getText() != null)
                return items.get(i);
        }
        return null;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public boolean isPinnedViewType(int viewType) {
        return viewType == Item.TYPE_SECTION;
    }

    public void add(Item item) {
        items.add(item);
        notifyItemInserted(getItemCount());
    }

    public void addAll(List<Item> list) {
        final int size = getItemCount();
        items.addAll(list);
        notifyItemRangeInserted(size, list.size());
    }

    public void clear() {
        final int size = getItemCount();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    public Item getItem(int i) {
        return items.get(i);
    }

    public List<Item> getItems() {
        return items;
    }

    public void remove(Item item) {
        final int position = items.indexOf(item);
        items.remove(item);
        notifyItemRemoved(position);
    }

    public void remove(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void setLoaded() {
        loading = false;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
        onLoadMoreListener.onLoadMore();
    }

    public void load() {
        this.onLoadMoreListener.onLoadMore();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public void setOnClickItemListener(OnClickItemListener onClickItemListener) {
        this.onClickItemListener = onClickItemListener;
    }

    public interface OnClickItemListener {
        void onClickItem(Item item, int position);
    }

    abstract public static class ViewHolder extends RecyclerView.ViewHolder {

        public final View view;
        public final int viewType;
        public OnClickItemListener listener;

        public ViewHolder(@NonNull View itemView, int viewType) {
            this(itemView, viewType, null);
        }

        public ViewHolder(@NonNull View itemView, int viewType, OnClickItemListener listener) {
            super(itemView);
            this.viewType = viewType;
            this.view = itemView;
            this.listener = listener;
        }
    }
}
