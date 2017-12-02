package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbRoute {

    @SerializedName("route_id")
    public String route_id;

    @SerializedName("route_no")
    public String route_no;

    @SerializedName("route_name_c")
    public String route_name_c;

    @SerializedName("route_name_s")
    public String route_name_s;

    @SerializedName("route_name_e")
    public String route_name_e;

    @SerializedName("time_table_c")
    public String time_table_c;

    @SerializedName("trip_time")
    public String trip_time;

    @SerializedName("trip_distance")
    public String trip_distance;

    @SerializedName("overnight_route")
    public Integer overnight_route;

    @SerializedName("special_route")
    public Integer special_route;

    public String toString() {
        return "NlbRoute{route_id=" + route_id + ", route_no=" + route_no + ", route_name_c=" + route_name_c +
                ", route_name_s=" + route_name_s + ", route_name_e=" + route_name_e + ", time_table_c=" + time_table_c +
                ", trip_time=" + trip_time + ", trip_distance=" + trip_distance +
                ", overnight_route=" + overnight_route + ", special_route=" + special_route + "}";
    }

}
