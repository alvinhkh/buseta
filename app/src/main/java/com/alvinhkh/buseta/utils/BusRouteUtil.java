package com.alvinhkh.buseta.utils;

import android.text.TextUtils;

import com.alvinhkh.buseta.kmb.model.KmbRoute;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;

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
        }
        return object;
    }
}
