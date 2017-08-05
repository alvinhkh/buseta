package com.alvinhkh.buseta.lwb.model;

import com.google.gson.annotations.SerializedName;

public class LwbRouteStop {

    @SerializedName("lat")
    public String lat;

    @SerializedName("lng")
    public String lng;

    @SerializedName("area")
    public String area;

    @SerializedName("subarea")
    public String subarea;

    @SerializedName("title_eng")
    public String name_en;

    @SerializedName("title_chi")
    public String name_tc;

    @SerializedName("address_eng")
    public String address_en;

    @SerializedName("address_chi")
    public String address_tc;

    @SerializedName("normal_fare")
    public String normal_fare;

    @SerializedName("air_cond_fare")
    public String air_cond_fare;

    @SerializedName("rel_bus")
    public String rel_bus;

}
