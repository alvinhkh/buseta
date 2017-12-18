package com.alvinhkh.buseta.utils;

import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;

import io.reactivex.annotations.Nullable;

public class BusRouteUtil {

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
