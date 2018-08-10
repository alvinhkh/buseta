package com.alvinhkh.buseta.ui.route;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.datagovhk.ui.MtrBusStopListFragment;
import com.alvinhkh.buseta.kmb.ui.KmbStopListFragment;
import com.alvinhkh.buseta.lwb.ui.LwbStopListFragment;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.mtr.ui.AESBusStopListFragment;
import com.alvinhkh.buseta.nlb.ui.NlbStopListFragment;
import com.alvinhkh.buseta.nwst.ui.NwstStopListFragment;
import com.alvinhkh.buseta.utils.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;


public class RoutePagerAdapter extends FragmentStatePagerAdapter {

    private Context context;

    private static List<Route> routes = new ArrayList<>();

    private RouteStop routeStop;

    public RoutePagerAdapter(FragmentManager fm, Context context, RouteStop routeStop) {
        super(fm);
        this.context = context;
        this.routeStop = routeStop;
    }

    public void addSequence(@NonNull Route route) {
        if (routes.contains(route)) return;
        routes.add(route);
        notifyDataSetChanged();
    }

    public void clearSequence() {
        if (routes.size() > 0) {
            routes.clear();
            notifyDataSetChanged();
        }
    }

    public List<Route> getRoutes() {
        return routes;
    }

    @Override
    public Fragment getItem(int position) {
        if (routes.size() > position) {
            Route route = routes.get(position);
            if (route != null && route.getCompanyCode() != null) {
                switch (route.getCompanyCode()) {
                    case C.PROVIDER.AESBUS:
                        return AESBusStopListFragment.newInstance(route, routeStop);
                    case C.PROVIDER.CTB:
                    case C.PROVIDER.NWFB:
                    case C.PROVIDER.NWST:
                        return NwstStopListFragment.newInstance(route, routeStop);
                    case C.PROVIDER.LRTFEEDER:
                        return MtrBusStopListFragment.newInstance(route, routeStop);
                    case C.PROVIDER.NLB:
                        return NlbStopListFragment.newInstance(route, routeStop);
                    case C.PROVIDER.KMB:
                    default:
                        if (PreferenceUtil.isUsingNewKmbApi(context)) {
                            return KmbStopListFragment.newInstance(route, routeStop);
                        } else {
                            return LwbStopListFragment.newInstance(route, routeStop);
                        }
                }
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return routes.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (context == null) return "" + position;
        Route route = routes.get(position);
        if (route != null) {
            if (!TextUtils.isEmpty(route.getOrigin())) {
                return (TextUtils.isEmpty(route.getOrigin()) ? "" : (route.getOrigin() + (getCount() > 1 ? "\n" : " ")))
                        + (!TextUtils.isEmpty(route.getDestination()) ? context.getString(R.string.destination, route.getDestination()) : "")
                        + (route.isSpecial() != null && route.isSpecial() ? "#" : "");
            }
            if (!TextUtils.isEmpty(route.getName())) {
                return route.getName();
            }
        }
        return context.getString(R.string.route) + " " + position;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // Causes adapter to reload all Fragments when
        // notifyDataSetChanged is called
        return POSITION_NONE;
    }
}