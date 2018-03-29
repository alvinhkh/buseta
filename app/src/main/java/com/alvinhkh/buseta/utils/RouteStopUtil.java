package com.alvinhkh.buseta.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.datagovhk.model.MtrBusFare;
import com.alvinhkh.buseta.datagovhk.model.MtrBusStop;
import com.alvinhkh.buseta.datagovhk.model.MtrLineStation;
import com.alvinhkh.buseta.kmb.model.KmbRouteStop;
import com.alvinhkh.buseta.lwb.model.LwbRouteStop;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.mtr.model.AESBusStop;
import com.alvinhkh.buseta.nlb.model.NlbRouteStop;
import com.alvinhkh.buseta.nlb.model.NlbStop;
import com.alvinhkh.buseta.nwst.model.NwstStop;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;

import org.jsoup.parser.Parser;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import timber.log.Timber;

public class RouteStopUtil {

    public static RouteStop fromFollowStop(FollowStop followStop) {
        if (followStop == null) return null;
        RouteStop object = new RouteStop();
        object.setCompanyCode(followStop.companyCode);
        object.setRoute(followStop.route);
        object.setRouteId(followStop.routeId);
        object.setDirection(followStop.direction);
        object.setCode(followStop.code);
        object.setSequence(followStop.sequence);
        object.setDestination(followStop.locationEnd);
        object.setOrigin(followStop.locationStart);
        object.setName(followStop.name);
        object.setEtaGet(followStop.etaGet);
        object.setLatitude(followStop.latitude);
        object.setLongitude(followStop.longitude);
        return object;
    }

    public static FollowStop toFollowStop(@NonNull RouteStop routeStop) {
        FollowStop object = new FollowStop();
        object.companyCode = routeStop.getCompanyCode();
        object.route = routeStop.getRoute();
        object.routeId = routeStop.getRouteId();
        object.direction = routeStop.getDirection();
        object.code = routeStop.getCode();
        object.sequence = routeStop.getSequence();
        object.locationEnd = routeStop.getDestination();
        object.locationStart = routeStop.getOrigin();
        object.name = routeStop.getName();
        object.etaGet = routeStop.getEtaGet();
        object.latitude = routeStop.getLatitude();
        object.longitude = routeStop.getLongitude();
        return object;
    }

    public static Pair<Double, Double> fromHK80toWGS84(@NonNull Pair<Double, Double> pair) {
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
            return new Pair<>(p2.y, p2.x);
        } catch (IllegalStateException e) {
            Timber.e(e);
        }
        return null;
    }

    public static RouteStop fromKmbRouteStop(@NonNull KmbRouteStop kmbRouteStop,
                                             @NonNull Route route,
                                             Integer position,
                                             Boolean isLastStop) {
        RouteStop object = new RouteStop();
        object.setCompanyCode(C.PROVIDER.KMB);
        object.setRoute(kmbRouteStop.route);
        object.setRouteId(kmbRouteStop.route);
        object.setDirection(kmbRouteStop.bound);
        object.setCode(kmbRouteStop.bsiCode);
        object.setSequence(Integer.toString(position));
        object.setName(kmbRouteStop.nameTc);
        object.setFare(kmbRouteStop.airFare);
        object.setLocation(kmbRouteStop.locationTc);
        Pair<Double, Double> latlong = fromHK80toWGS84(
                new Pair<>(Double.parseDouble(kmbRouteStop.X), Double.parseDouble(kmbRouteStop.Y)));
        if (latlong != null) {
            object.setLatitude(String.valueOf(latlong.first));
            object.setLongitude(String.valueOf(latlong.second));
        }
        object.setDestination(route.getOrigin());
        object.setOrigin(route.getDestination());
        if (!TextUtils.isEmpty(object.getCode())) {
            object.setImageUrl("http://www.kmb.hk/chi/img.php?file=" + object.getCode());
        }
        object.setEtaGet(String.format("/?action=geteta&lang=tc&route=%s&bound=%s&stop=%s&stop_seq=%s&serviceType=%s",
                object.getRoute(), object.getDirection(), object.getCode(), isLastStop ? 999 : object.getSequence(), route.getServiceType()));
        return object;
    }

    public static RouteStop fromLwb(@NonNull LwbRouteStop lwbRouteStop,
                                    @NonNull Route route,
                                    Integer position,
                                    Boolean isLastStop) {
        RouteStop object = new RouteStop();
        object.setCompanyCode(C.PROVIDER.KMB);
        object.setRoute(route.getName());
        object.setRouteId(route.getName());
        object.setDirection(route.getSequence());
        object.setCode(lwbRouteStop.subarea);
        object.setSequence(Integer.toString(position));
        object.setName(lwbRouteStop.name_tc);
        object.setFare(lwbRouteStop.air_cond_fare);
        object.setLatitude(lwbRouteStop.lat);
        object.setLongitude(lwbRouteStop.lng);
        object.setLocation(lwbRouteStop.address_tc);

        object.setDestination(route.getOrigin());
        object.setOrigin(route.getDestination());

        if (!TextUtils.isEmpty(object.getCode())) {
            object.setImageUrl("http://www.kmb.hk/chi/img.php?file=" + object.getCode());
        }
        object.setEtaGet(String.format("/?action=geteta&lang=tc&route=%s&bound=%s&stop=%s&stop_seq=%s&serviceType=%s",
                object.getRoute(), object.getDirection(), object.getCode(), isLastStop ? 999 : object.getSequence(), route.getServiceType()));
        return object;
    }

    public static RouteStop fromNlb(@NonNull NlbRouteStop nlbRouteStop,
                                    @NonNull NlbStop nlbStop,
                                    @NonNull Route route) {
        RouteStop object = new RouteStop();
        object.setCompanyCode(C.PROVIDER.NLB);
        object.setRoute(route.getName());
        object.setRouteId(nlbRouteStop.route_id);
        object.setDirection(route.getSequence());
        object.setCode(nlbRouteStop.stop_id);
        object.setName(nlbStop.stop_name_c);
        object.setFare(nlbRouteStop.fare);
        object.setFareHoliday(nlbRouteStop.fare_holiday);
        object.setLatitude(nlbStop.latitude);
        object.setLongitude(nlbStop.longitude);
        object.setLocation(nlbStop.stop_location_c);
        object.setDestination(route.getOrigin());
        object.setOrigin(route.getDestination());
        return object;
    }

    public static RouteStop fromNwst(@NonNull NwstStop nwstStop,
                                     @NonNull Route route) {
        RouteStop object = new RouteStop();
        object.setCode(nwstStop.getStopId());
        object.setCompanyCode(route.getCompanyCode());
        object.setDestination(route.getOrigin());
        object.setDirection(route.getSequence());
        object.setFare(Double.toString(nwstStop.getAdultFare()));
        object.setFareChild(Double.toString(nwstStop.getChildFare()));
        object.setFareSenior(Double.toString(nwstStop.getSeniorFare()));
        object.setLatitude(Double.toString(nwstStop.getLatitude()));
        object.setLongitude(Double.toString(nwstStop.getLongitude()));
        object.setName(TextUtils.isEmpty(nwstStop.getStopName()) ? nwstStop.getStopName() : nwstStop.getStopName().split(",")[0]);
        object.setOrigin(route.getDestination());
        object.setRoute(route.getName());
        object.setRouteId(nwstStop.getRdv());
        object.setSequence(Integer.toString(nwstStop.getSequence()));
        object.setEtaGet(Boolean.toString(nwstStop.isEta()));
        if (!TextUtils.isEmpty(nwstStop.getPoleId())) {
            object.setImageUrl("http://mobile.nwstbus.com.hk/api6/getstopphoto.php?filename=w" + nwstStop.getPoleId() + "001.jpg&syscode=" + NwstRequestUtil.syscode());
        }
        return object;
    }

    public static RouteStop fromMtrBus(@NonNull MtrBusStop mtrBusStop,
                                       @Nullable MtrBusFare mtrBusFare,
                                       @NonNull Route route) {
        RouteStop object = new RouteStop();
        object.setCode(String.valueOf(mtrBusStop.getStationSequenceNo()));
        object.setCompanyCode(route.getCompanyCode());
        object.setDestination(route.getOrigin());
        object.setDirection(route.getSequence());
        if (mtrBusFare != null) {
            object.setFare(String.valueOf(mtrBusFare.getFareSingleAdult()));
            object.setFareChild(String.valueOf(mtrBusFare.getFareSingleChild()));
            object.setFareSenior(String.valueOf(mtrBusFare.getFareSingleElderly()));
        }
        // object.latitude = Double.toString(mtrBusStop.getLatitude());
        // object.longitude = Double.toString(mtrBusStop.getLongitude());
        if (!TextUtils.isEmpty(mtrBusStop.getStationNameChi())) {
            object.setName(Parser.unescapeEntities(mtrBusStop.getStationNameChi(), false));
        }
        object.setOrigin(route.getDestination());
        object.setRoute(route.getName());
        object.setRouteId(mtrBusStop.getRouteId());
        object.setSequence(String.valueOf(mtrBusStop.getStationSequenceNo()));
        object.setEtaGet("");
        object.setImageUrl("");
        return object;
    }

    public static RouteStop fromAESBus(@NonNull AESBusStop aesBusStop,
                                       @NonNull Route route) {
        RouteStop object = new RouteStop();
        object.setCode(String.valueOf(aesBusStop.getStopId()));
        object.setCompanyCode(route.getCompanyCode());
        object.setDestination(route.getOrigin());
        object.setDirection(route.getSequence());
        object.setFare("0");
        object.setLatitude(aesBusStop.getStopLatitude());
        object.setLongitude(aesBusStop.getStopLongitude());
        object.setName(aesBusStop.getStopNameCn());
        object.setOrigin(route.getDestination());
        object.setRoute(route.getName());
        object.setRouteId(aesBusStop.getBusNumber());
        object.setSequence(String.valueOf(aesBusStop.getNearestStopID()));
        object.setEtaGet("");
        object.setImageUrl("");
        return object;
    }

    public static RouteStop fromMtrLineStation(@NonNull MtrLineStation mtrLineStation) {
        RouteStop object = new RouteStop();
        object.setCode(mtrLineStation.getStationCode());
        object.setCompanyCode(C.PROVIDER.MTR);
        // object.destination = "";
        object.setDirection(mtrLineStation.getDirection());
        // object.fare = "0";
        // object.latitude = "";
        // object.longitude = "";
        object.setName(mtrLineStation.getChineseName());
        // object.origin = "";
        object.setRoute(mtrLineStation.getLineCode());
        object.setRouteId(mtrLineStation.getLineCode());
        object.setSequence(mtrLineStation.getStationID());
        object.setEtaGet("");
        object.setImageUrl("");
        return object;
    }
}
