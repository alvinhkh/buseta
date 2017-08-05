package com.alvinhkh.buseta.utils;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.alvinhkh.buseta.kmb.model.KmbRouteStop;
import com.alvinhkh.buseta.lwb.model.LwbRouteStop;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import java.io.UnsupportedEncodingException;

import timber.log.Timber;

public class BusRouteStopUtil {

    public static BusRouteStop fromFollowStop(FollowStop followStop) {
        if (followStop == null) return null;
        BusRouteStop object = new BusRouteStop();
        object.company = followStop.company;
        object.route = followStop.route;
        object.direction = followStop.direction;
        object.code = followStop.code;
        object.sequence = followStop.sequence;
        object.destination = followStop.locationEnd;
        object.origin = followStop.locationStart;
        object.name = followStop.name;
        object.etaGet = followStop.etaGet;
        object.latitude = followStop.latitude;
        object.longitude = followStop.longitude;
        return object;
    }

    public static FollowStop toFollowStop(@NonNull BusRouteStop busRouteStop) {
        FollowStop object = new FollowStop();
        object.company = busRouteStop.company;
        object.route = busRouteStop.route;
        object.direction = busRouteStop.direction;
        object.code = busRouteStop.code;
        object.sequence = busRouteStop.sequence;
        object.locationEnd = busRouteStop.destination;
        object.locationStart = busRouteStop.origin;
        object.name = busRouteStop.name;
        object.etaGet = busRouteStop.etaGet;
        object.latitude = busRouteStop.latitude;
        object.longitude = busRouteStop.longitude;
        return object;
    }

    private static Pair<Double, Double> fromHK80toWGS84(Pair<Double, Double> pair) {
        try {
            // reference: blog.tiger-workshop.com/hk1980-grid-to-wgs84/
            CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
            CRSFactory csFactory = new CRSFactory();
            CoordinateReferenceSystem HK80 = csFactory.createFromParameters("EPSG:2326", "+proj=tmerc +lat_0=22.31213333333334 +lon_0=114.1785555555556 +k=1 +x_0=836694.05 +y_0=819069.8 +ellps=intl +towgs84=-162.619,-276.959,-161.764,0.067753,-2.24365,-1.15883,-1.09425 +units=m +no_defs");
            CoordinateReferenceSystem WGS84 = csFactory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
            CoordinateTransform trans = ctFactory.createTransform(HK80, WGS84);
            ProjCoordinate p = new ProjCoordinate();
            ProjCoordinate p2 = new ProjCoordinate();
            p.x = pair.first;
            p.y = pair.second;
            trans.transform(p, p2);
            return new Pair<>(p2.x, p2.y);
        } catch (IllegalStateException e) {
            Timber.e(e);
        }
        return null;
    }

    public static BusRouteStop fromKmbRouteStop(@NonNull KmbRouteStop kmbRouteStop,
                                                @NonNull BusRoute busRoute,
                                                Integer position) {
        BusRouteStop object = new BusRouteStop();
        object.company = BusRoute.COMPANY_KMB;
        object.route = kmbRouteStop.route;
        object.direction = kmbRouteStop.bound;
        object.code = kmbRouteStop.bsiCode;
        object.sequence = Integer.toString(position);
        object.name = kmbRouteStop.nameTc;
        object.fare = kmbRouteStop.airFare;
        Pair<Double, Double> longlat = fromHK80toWGS84(
                new Pair<>(Double.parseDouble(kmbRouteStop.X), Double.parseDouble(kmbRouteStop.Y)));
        if (longlat != null) {
            object.latitude = String.valueOf(longlat.second);
            object.longitude = String.valueOf(longlat.first);
        }
        object.destination = busRoute.getLocationEndName();
        object.origin = busRoute.getLocationStartName();

        if (object.company != null && object.company.equals(BusRoute.COMPANY_KMB)) {
            if (!TextUtils.isEmpty(object.code)) {
                object.imageUrl = "http://www.kmb.hk/chi/img.php?file=" + object.code;
            }
            object.etaGet = String.format("/?action=geteta&lang=tc&route=%s&bound=%s&stop=%s&stop_seq=%s",
                    object.route, object.direction, object.code, object.sequence);
        }
        return object;
    }

    public static BusRouteStop fromLwb(@NonNull LwbRouteStop lwbRouteStop,
                                       @NonNull BusRoute busRoute,
                                       Integer position) {
        BusRouteStop object = new BusRouteStop();
        object.company = BusRoute.COMPANY_KMB;
        object.route = busRoute.getName();
        object.direction = busRoute.getSequence();
        object.code = lwbRouteStop.subarea;
        object.sequence = Integer.toString(position);
        object.name = lwbRouteStop.name_tc;
        object.fare = lwbRouteStop.air_cond_fare;
        object.latitude = lwbRouteStop.lat;
        object.longitude = lwbRouteStop.lng;

        object.destination = busRoute.getLocationEndName();
        object.origin = busRoute.getLocationStartName();

        if (object.company != null && object.company.equals(BusRoute.COMPANY_KMB)) {
            if (!TextUtils.isEmpty(object.code)) {
                object.imageUrl = "http://www.kmb.hk/chi/img.php?file=" + object.code;
            }
            object.etaGet = String.format("/?action=geteta&lang=tc&route=%s&bound=%s&stop=%s&stop_seq=%s",
                    object.route, object.direction, object.code, object.sequence);
        }
        return object;
    }
}
