package com.alvinhkh.buseta.ui.route;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.ui.KmbStopListFragment;
import com.alvinhkh.buseta.lwb.ui.LwbStopListFragment;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.nlb.ui.NlbStopListFragment;
import com.alvinhkh.buseta.nwst.ui.NwstStopListFragment;
import com.alvinhkh.buseta.utils.PreferenceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RoutePagerAdapter extends FragmentStatePagerAdapter {

    private int pagerAdapterPosChanged = POSITION_UNCHANGED;

    public static final int MIN_PAGE = 0;

    private Context context;

    private String routeNo;

    private List<BusRoute> routes = new ArrayList<>();

    private SparseArray<Fragment> fragments = new SparseArray<>();

    private BusRouteStop busRouteStop;

    public RoutePagerAdapter(FragmentManager fm, Context context, BusRouteStop busRouteStop) {
        super(fm);
        this.context = context;
        this.busRouteStop = busRouteStop;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        fragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        fragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getFragment(int position) {
        return fragments.get(position);
    }

    public void setRoute(@NonNull String routeNo) {
        this.routeNo = routeNo;
        pagerAdapterPosChanged = POSITION_NONE;
        notifyDataSetChanged();
    }

    public void addSequence(@NonNull BusRoute busRoute) {
        if (!TextUtils.isEmpty(busRoute.getName()) && !busRoute.getName().equals(routeNo)) return;
        if (routes.contains(busRoute)) return;
        routes.add(busRoute);
        pagerAdapterPosChanged = POSITION_NONE;
        notifyDataSetChanged();
    }

    public void clearSequence() {
        routes.clear();
        fragments.clear();
        pagerAdapterPosChanged = POSITION_NONE;
        notifyDataSetChanged();
    }

    public List<BusRoute> getRoutes() {
        return this.routes;
    }

    @Override
    public Fragment getItem(int position) {
        if (position > getCount()) {
            throw new IllegalArgumentException();
        }
        if (position >= MIN_PAGE) {
            BusRoute busRoute = routes.get(position - MIN_PAGE);
            if (busRoute.getCompanyCode().equals(BusRoute.COMPANY_NLB)) {
                return NlbStopListFragment.newInstance(busRoute, busRouteStop);
            } else if (Arrays.asList(new String[]{BusRoute.COMPANY_CTB, BusRoute.COMPANY_NWFB, BusRoute.COMPANY_NWST}).contains(busRoute.getCompanyCode())) {
                return NwstStopListFragment.newInstance(busRoute, busRouteStop);
            } else {
                if (PreferenceUtil.isUsingNewKmbApi(context)) {
                    return KmbStopListFragment.newInstance(busRoute, busRouteStop);
                } else {
                    return LwbStopListFragment.newInstance(busRoute, busRouteStop);
                }
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return MIN_PAGE + routes.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position > getCount()) {
            throw new IllegalArgumentException();
        }
        switch (position) {
            case MIN_PAGE:
            default:
                BusRoute busRoute = routes.get(position - MIN_PAGE);
                if (busRoute != null && !TextUtils.isEmpty(busRoute.getLocationEndName())) {
                    return busRoute.getLocationStartName()
                            + (getCount() > 1 ? "\n" : " ")
                            + context.getString(R.string.destination, busRoute.getLocationEndName())
                            + (busRoute.getSpecial() ? "#" : "");
                }
                return context.getString(R.string.route) + " " + position;
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return pagerAdapterPosChanged;
    }
}