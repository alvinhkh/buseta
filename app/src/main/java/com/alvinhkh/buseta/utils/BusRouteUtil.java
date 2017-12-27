package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.text.TextUtils;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.model.KmbRoute;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

public class BusRouteUtil {

    public static BusRoute fromKmb(KmbRoute route) {
        if (route == null) return null;
        BusRoute object = new BusRoute();
        object.setCompanyCode(BusRoute.COMPANY_KMB);
        object.setLocationEndName(HKSCSUtil.convert(route.destinationTc));
        object.setLocationStartName(HKSCSUtil.convert(route.originTc));
        object.setName(route.route);
        object.setSequence(route.bound);
        object.setServiceType(TextUtils.isEmpty(route.serviceType) ? route.serviceType : route.serviceType.trim());
        String desc = HKSCSUtil.convert(route.descTc.trim());
        object.setDescription(desc);
        object.setSpecial(!TextUtils.isEmpty(desc));
        return object;
    }

    public static BusRoute fromNwst(NwstRoute route, @Nullable NwstVariant variant) {
        if (route == null) return null;
        BusRoute object = new BusRoute();
        object.setCompanyCode(route.getCompanyCode());
        object.setDescription(route.getRemark());
        object.setLocationEndName(route.getPlaceTo());
        object.setLocationStartName(route.getPlaceFrom());
        object.setName(route.getRouteNo());
        object.setServiceType(route.getRouteType());
        object.setKey(route.getRdv());
        object.setSequence(route.getBound());
        if (variant != null) {
            object.setStopsStartSequence(variant.getStartSequence());
            object.setChildKey(variant.getRouteInfo());
            object.setDescription(variant.getRemark());
            object.setSpecial(!TextUtils.isEmpty(variant.getRemark()) && !variant.getRemark().equals("正常路線"));
        }
        return object;
    }

    public static String getCompanyName(@NonNull Context context,
                                        @NonNull String companyCode,
                                        @Nullable String routeNo) {
        String companyName = companyCode;
        switch (companyCode) {
            case BusRoute.COMPANY_CTB:
                companyName = context.getString(R.string.provider_short_ctb);
                break;
            case BusRoute.COMPANY_KMB:
                companyName = context.getString(R.string.provider_short_kmb);
                if (!TextUtils.isEmpty(routeNo)) {
                    if (routeNo.startsWith("NR")) {
                        companyName = context.getString(R.string.provider_short_residents);
                    } else if (routeNo.startsWith("A") || routeNo.startsWith("E") || routeNo.startsWith("NA")) {
                        companyName = context.getString(R.string.provider_short_lwb);
                    }
                }
                break;
            case BusRoute.COMPANY_NLB:
                companyName = context.getString(R.string.provider_short_nlb);
                break;
            case BusRoute.COMPANY_NWFB:
                companyName = context.getString(R.string.provider_short_nwfb);
                break;
            case BusRoute.COMPANY_NWST:
                companyName = context.getString(R.string.provider_short_nwst);
                break;
        }
        return companyName;
    }
}
