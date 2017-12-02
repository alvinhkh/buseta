package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbStopDistrict {

    @SerializedName("stop_district_id")
    public String stop_district_id;

    @SerializedName("district_name_c")
    public String district_name_c;

    @SerializedName("district_name_s")
    public String district_name_s;

    @SerializedName("district_name_e")
    public String district_name_e;

    public String toString() {
        return "NlbStopDistrict{stop_district_id=" + stop_district_id + ", district_name_c=" + district_name_c +
                ", district_name_s=" + district_name_s + ", district_name_e=" + district_name_e + "}";
    }

}
