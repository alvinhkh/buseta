package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NlbDatabase {

    @SerializedName("version")
    public String version;

    @SerializedName("routes")
    public List<NlbRoute> routes;

    @SerializedName("stop_districts")
    public List<NlbStopDistrict> stop_districts;

    @SerializedName("stops")
    public List<NlbStop> stops;

    @SerializedName("route_stops")
    public List<NlbRouteStop> route_stops;

    public String toString() {
        return "NlbDatabase{version=" + this.version + "}";
    }

}
