package com.alvinhkh.buseta.datagovhk.model

import com.google.gson.annotations.SerializedName

data class NwstStop(
        @SerializedName("stop")
        var stopId: String = "",
        @SerializedName("name_tc")
        var nameTc: String = "",
        @SerializedName("name_en")
        var nameEn: String = "",
        @SerializedName("name_sc")
        var nameSc: String = "",
        @SerializedName("lat")
        var latitude: Double = 0.0,
        @SerializedName("long")
        var longitude: Double = 0.0,
        @SerializedName("data_timestamp")
        var dataTimestamp: String = ""
)