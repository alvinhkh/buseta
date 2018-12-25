package com.alvinhkh.buseta.ui.route;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.route.model.Route;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter.Item;

import java.util.ArrayList;

public class RouteSelectDialogFragment extends BottomSheetDialogFragment {

    private ViewPager viewPager;

    public static RouteSelectDialogFragment newInstance(@NonNull ArrayList<Route> routes, @NonNull ViewPager viewPager) {
        RouteSelectDialogFragment fragment = new RouteSelectDialogFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(C.EXTRA.ROUTE_LIST, routes);
        fragment.setArguments(args);
        fragment.viewPager = viewPager;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        ArrayList<Route> routes = new ArrayList<>();
        if (bundle != null) {
            routes = bundle.getParcelableArrayList(C.EXTRA.ROUTE_LIST);
        }
        View itemView = inflater.inflate(R.layout.bottom_sheet_route_select, container, false);
        RecyclerView recyclerView = itemView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        RouteSelectAdapter adapter = new RouteSelectAdapter(recyclerView);
        recyclerView.setAdapter(adapter);
        if (routes != null) {
            for (Route route : routes) {
                adapter.add(new Item(Item.TYPE_DATA, route));
            }
        }
        adapter.setOnClickItemListener((item, position) -> {
            viewPager.setCurrentItem(position);
            dismiss();
        });
        return itemView;

    }
}