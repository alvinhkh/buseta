package com.alvinhkh.buseta.nlb.model;

import com.google.gson.annotations.SerializedName;

public class NlbStop {

    @SerializedName("stop_id")
    public String stop_id;

    @SerializedName("stop_district_id")
    public String stop_district_id;

    @SerializedName("stop_name_c")
    public String stop_name_c;

    @SerializedName("stop_name_s")
    public String stop_name_s;

    @SerializedName("stop_name_e")
    public String stop_name_e;

    @SerializedName("stop_location_c")
    public String stop_location_c;

    @SerializedName("stop_location_s")
    public String stop_location_s;

    @SerializedName("stop_location_e")
    public String stop_location_e;

    @SerializedName("latitude")
    public String latitude;

    @SerializedName("longitude")
    public String longitude;

    public String toString() {
        return "NlbStop{stop_id=" + stop_id + ", stop_district_id=" + stop_district_id +
                ", stop_name_c=" + stop_name_c + ", stop_name_s=" + stop_name_s +
                ", stop_name_e=" + stop_name_e + ", stop_location_c=" + stop_location_c +
                ", stop_location_s=" + stop_location_s + ", stop_location_e=" + stop_location_e +
                ", latitude=" + latitude + ", longitude=" + longitude + "}";
    }

}
