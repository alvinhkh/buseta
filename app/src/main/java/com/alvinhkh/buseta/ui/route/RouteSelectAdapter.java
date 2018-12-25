package com.alvinhkh.buseta.ui.route;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;

import com.alvinhkh.buseta.route.model.Route;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.utils.RouteUtil;

public class RouteSelectAdapter extends ArrayListRecyclerViewAdapter<RouteSelectAdapter.ViewHolder> {

    public RouteSelectAdapter(@NonNull RecyclerView recyclerView) {
        super(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ViewHolder.createViewHolder(parent, viewType, onClickItemListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.bindItem(this, items.get(position), position);
    }

    static abstract class ViewHolder extends ArrayListRecyclerViewAdapter.ViewHolder {

        ViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
        }

        public static ViewHolder createViewHolder(ViewGroup parent, int viewType, OnClickItemListener listener) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root;

            switch (viewType) {
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_route_select, parent, false);
                    return new DataViewHolder(root, viewType, listener);
                default:
                    return null;
            }
        }

        abstract public void bindItem(RouteSelectAdapter adapter, Item item, int position);
    }

    static class DataViewHolder extends ViewHolder {

        Context context;

        TextView routeTv;

        TextView locationTv;

        TextView descriptionTv;

        public DataViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
            routeTv = itemView.findViewById(R.id.route);
            locationTv = itemView.findViewById(R.id.location);
            descriptionTv = itemView.findViewById(R.id.description);
            context = itemView.getContext();
        }

        @Override
        public void bindItem(RouteSelectAdapter adapter, Item item, int position) {
            final Route route = (Route) item.getObject();
            assert route != null;
            String routeName = RouteUtil.getCompanyName(context, route.getCompanyCode(), route.getName()) +
                    " " + route.getName();
            routeTv.setText(routeName);
            if (!TextUtils.isEmpty(route.getDestination()) && !TextUtils.isEmpty(route.getOrigin())) {
                locationTv.setVisibility(View.VISIBLE);
                locationTv.setText(context.getString(R.string.route_path,
                        route.getOrigin(), route.getDestination()));
            } else if (!TextUtils.isEmpty(route.getDestination())) {
                locationTv.setVisibility(View.VISIBLE);
                locationTv.setText(context.getString(R.string.destination, route.getDestination()));
            } else if (!TextUtils.isEmpty(route.getOrigin())) {
                locationTv.setVisibility(View.VISIBLE);
                locationTv.setText(route.getOrigin());
            } else {
                locationTv.setVisibility(View.GONE);
            }
            descriptionTv.setText(route.getDescription());
            descriptionTv.setVisibility(TextUtils.isEmpty(route.getDescription()) ? View.GONE: View.VISIBLE);
            itemView.setOnClickListener(v -> {
                if (this.listener != null) {
                    this.listener.onClickItem(item, position);
                }
            });
        }
    }

}