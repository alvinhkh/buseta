package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbStop(
        @SerializedName("stop_id")
        var stop_id: String = "",

        @SerializedName("stop_district_id")
        var stop_district_id: String = "",

        @SerializedName("stop_name_c")
        var stop_name_c: String = "",

        @SerializedName("stop_name_s")
        var stop_name_s: String = "",

        @SerializedName("stop_name_e")
        var stop_name_e: String = "",

        @SerializedName("stop_location_c")
        var stop_location_c: String = "",

        @SerializedName("stop_location_s")
        var stop_location_s: String = "",

        @SerializedName("stop_location_e")
        var stop_location_e: String = "",

        @SerializedName("latitude")
        var latitude: String = "",

        @SerializedName("longitude")
        var longitude: String = ""
)
