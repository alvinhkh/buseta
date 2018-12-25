package com.alvinhkh.buseta.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.datagovhk.model.MtrLineStation;
import com.alvinhkh.buseta.follow.model.Follow;
import com.alvinhkh.buseta.route.model.RouteStop;

public class RouteStopUtil {

    public static RouteStop fromFollow(Follow follow) {
        if (follow == null) return null;
        RouteStop object = new RouteStop();
        object.setCompanyCode(follow.getCompanyCode());
        object.setRouteNo(follow.getRouteNo());
        object.setRouteId(follow.getRouteId());
        object.setRouteServiceType(follow.getRouteServiceType());
        object.setRouteSequence(follow.getRouteSeq());
        object.setStopId(follow.getStopId());
        object.setSequence(follow.getStopSeq());
        object.setRouteDestination(follow.getRouteDestination());
        object.setRouteOrigin(follow.getRouteOrigin());
        object.setName(follow.getStopName());
        object.setLatitude(follow.getStopLatitude());
        object.setLongitude(follow.getStopLongitude());
        object.setEtaGet(follow.getEtaGet());
        if (!TextUtils.isEmpty(object.getCompanyCode()) && object.getCompanyCode().equals(C.PROVIDER.KMB)) {
            if (!TextUtils.isEmpty(object.getStopId())) {
                object.setImageUrl("http://www.kmb.hk/chi/img.php?file=" + object.getStopId());
            }
        }
        return object;
    }

    public static RouteStop fromMtrLineStation(@NonNull MtrLineStation mtrLineStation) {
        RouteStop object = new RouteStop();
        object.setStopId(mtrLineStation.getStationCode());
        object.setCompanyCode(C.PROVIDER.MTR);
        // object.routeDestination = "";
        object.setRouteSequence(mtrLineStation.getDirection());
        // object.fareFull = "0";
        // object.latitude = "";
        // object.longitude = "";
        object.setName(mtrLineStation.getChineseName());
        // object.routeOrigin = "";
        object.setRouteNo(mtrLineStation.getLineCode());
        object.setRouteId(mtrLineStation.getLineCode());
        object.setSequence(mtrLineStation.getStationID());
        object.setEtaGet("");
        object.setImageUrl("");
        return object;
    }
}
