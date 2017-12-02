package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbRouteStop {

    @SerializedName("route_id")
    public String route_id;

    @SerializedName("stop_sequence")
    public String stop_sequence;

    @SerializedName("stop_id")
    public String stop_id;

    @SerializedName("fare")
    public String fare;

    @SerializedName("fare_holiday")
    public String fare_holiday;

    @SerializedName("some_departure_observe_only")
    public Integer some_departure_observe_only;

    public String toString() {
        return "NlbRouteStop{route_id=" + route_id + ", stop_sequence=" + stop_sequence +
                ", stop_id=" + stop_id + ", fare=" + fare + ", fare_holiday=" + fare_holiday +
                ", some_departure_observe_only=" + some_departure_observe_only + "}";
    }

}
