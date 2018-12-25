package com.alvinhkh.buseta.datagovhk.ui;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.route.model.Route;
import com.alvinhkh.buseta.route.model.RouteStop;
import com.alvinhkh.buseta.mtr.ui.MtrScheduleItemAdapter;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.route.RouteStopFragment;

import java.util.ArrayList;
import java.util.List;


public class MtrLineStationsAdapter
        extends ArrayListRecyclerViewAdapter<MtrLineStationsAdapter.ViewHolder> {

    private OnClickItemListener listener;

    private static ArrivalTimeDatabase arrivalTimeDatabase = null;

    private FragmentManager fragmentManager;

    public MtrLineStationsAdapter(@NonNull FragmentManager fragmentManager,
                                  @NonNull RecyclerView recyclerView, OnClickItemListener listener) {
        super(recyclerView);
        this.fragmentManager = fragmentManager;
        this.listener = listener;
        arrivalTimeDatabase = ArrivalTimeDatabase.Companion.getInstance(recyclerView.getContext());
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ViewHolder.createViewHolder(parent, viewType, listener, fragmentManager, arrivalTimeDatabase);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.bindItem(this, items.get(position), position);
    }

    static abstract class ViewHolder extends ArrayListRecyclerViewAdapter.ViewHolder {

        ViewHolder(final View itemView, final int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
        }

        public static ViewHolder createViewHolder(ViewGroup parent, int viewType,
                                                  OnClickItemListener listener,
                                                  FragmentManager fragmentManager,
                                                  ArrivalTimeDatabase arrivalTimeDatabase) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root;

            switch (viewType) {
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_railway_station, parent, false);
                    return new StationHolder(root, viewType, listener, fragmentManager, arrivalTimeDatabase);
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

        FragmentManager fragmentManager;

        ArrivalTimeDatabase database;

        private String direction = "";

        StationHolder(View itemView, int viewType, OnClickItemListener listener,
                      FragmentManager fragmentManager, ArrivalTimeDatabase arrivalTimeDatabase) {
            super(itemView, viewType, listener);
            this.itemView = itemView;
            this.fragmentManager = fragmentManager;
            nameTv = itemView.findViewById(R.id.name);
            scheduleList = itemView.findViewById(R.id.schedule_list);
            database = arrivalTimeDatabase;
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
                    if (database != null) {
                        List<ArrivalTime> arrivalTimeList = ArrivalTime.Companion.getList(database, object);
                        for (ArrivalTime arrivalTime : arrivalTimeList) {
                            if (arrivalTime == null) continue;
                            arrivalTime = ArrivalTime.Companion.estimate(itemView.getContext(), arrivalTime);
                            if (!TextUtils.isEmpty(direction) && items.size() > 1 && !direction.equals(arrivalTime.getDirection())) {
                                items.add(new Item(Item.TYPE_SECTION, ""));
                            }
                            items.add(new Item(Item.TYPE_DATA, arrivalTime));
                            direction = arrivalTime.getDirection();
                        }
                    }
                    MtrScheduleItemAdapter scheduleItemAdapter = new MtrScheduleItemAdapter(scheduleList, null);
                    scheduleItemAdapter.addAll(items);
                    scheduleList.setAdapter(scheduleItemAdapter);
                    scheduleList.setVisibility(View.VISIBLE);
                }
                itemView.setOnClickListener(l -> listener.onClickItem(item, position));
                itemView.setOnLongClickListener(l -> {
                    Route route = new Route();
                    route.setCompanyCode(object.getCompanyCode());
                    route.setName(object.getRouteNo());
                    route.setCode(object.getRouteId());
                    route.setDestination(object.getRouteDestination());
                    route.setOrigin(object.getRouteOrigin());
                    route.setServiceType(object.getRouteServiceType());
                    route.setSequence(object.getRouteSequence());
                    try {
                        BottomSheetDialogFragment bottomSheetDialogFragment = RouteStopFragment.newInstance(route, object);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    } catch (IllegalStateException ignored) {}
                    return true;
                });
            }
        }
    }

}