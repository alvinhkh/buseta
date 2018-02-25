package com.alvinhkh.buseta.datagovhk.ui;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.mtr.ui.MtrScheduleItemAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;

import java.util.ArrayList;
import java.util.List;


public class MtrLineStationsAdapter
        extends ArrayListRecyclerViewAdapter<MtrLineStationsAdapter.ViewHolder> {

    private OnClickItemListener listener;

    public MtrLineStationsAdapter(@NonNull RecyclerView recyclerView, OnClickItemListener listener) {
        super(recyclerView);
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ViewHolder.createViewHolder(parent, viewType, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.bindItem(this, items.get(position), position);
    }

    static abstract class ViewHolder extends ArrayListRecyclerViewAdapter.ViewHolder {

        ViewHolder(final View itemView, final int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
        }

        public static ViewHolder createViewHolder(final ViewGroup parent, final int viewType, OnClickItemListener listener) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root;

            switch (viewType) {
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_railway_station, parent, false);
                    return new StationHolder(root, viewType, listener);
                default:
                    return null;
            }
        }

        abstract public void bindItem(MtrLineStationsAdapter adapter, Item item, int position);
    }

    static class StationHolder extends ViewHolder {
        
        View itemView;

        TextView nameTv;

        RecyclerView scheduleList;

        private String direction = "";

        StationHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
            this.itemView = itemView;
            nameTv = itemView.findViewById(R.id.name);
            scheduleList = itemView.findViewById(R.id.schedule_list);
        }

        @Override
        public void bindItem(MtrLineStationsAdapter adapter, Item item, int position) {
            scheduleList.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            scheduleList.setHasFixedSize(true);
            scheduleList.setNestedScrollingEnabled(false);
            scheduleList.setVisibility(View.GONE);
            RouteStop object = (RouteStop) item.getObject();
            if (object != null) {
                nameTv.setText(object.getName());
                if (itemView.getContext() != null) {
                    List<Item> items = new ArrayList<>();
                    // ETA
                    ArrivalTimeUtil.query(itemView.getContext(), object).subscribe(cursor -> {
                        // Cursor has been moved +1 position forward.
                        ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
                        if (arrivalTime == null) return;
                        arrivalTime = ArrivalTimeUtil.estimate(itemView.getContext(), arrivalTime);
                        if (!TextUtils.isEmpty(direction) && items.size() > 1 && !direction.equals(arrivalTime.direction)) {
                            items.add(new Item(Item.TYPE_SECTION, ""));
                        }
                        items.add(new Item(Item.TYPE_DATA, arrivalTime));
                        direction = arrivalTime.direction;
                    });
                    MtrScheduleItemAdapter scheduleItemAdapter = new MtrScheduleItemAdapter(scheduleList, null);
                    scheduleItemAdapter.addAll(items);
                    scheduleList.setAdapter(scheduleItemAdapter);
                    scheduleList.setVisibility(View.VISIBLE);
                }
                itemView.setOnClickListener(l -> {
                    listener.onClickItem(item, position);
                });
            }
        }
    }

}