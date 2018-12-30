package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbStopDistrict(
        @SerializedName("stop_district_id")
        var stop_district_id: String = "",

        @SerializedName("district_name_c")
        var district_name_c: String = "",

        @SerializedName("district_name_s")
        var district_name_s: String = "",

        @SerializedName("district_name_e")
        var district_name_e: String = ""
)
