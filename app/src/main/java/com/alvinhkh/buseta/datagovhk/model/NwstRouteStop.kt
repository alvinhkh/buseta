package com.alvinhkh.buseta.datagovhk.model

import com.google.gson.annotations.SerializedName

data class NwstRouteStop(
        @SerializedName("co")
        var companyCode: String = "",
        @SerializedName("route")
        var routeName: String = "",
        @SerializedName("dir")
        var direction: String = "",
        @SerializedName("seq")
        var sequence: Int = -1,
        @SerializedName("stop")
        var stopId: String = "",
        @SerializedName("data_timestamp")
        var dataTimestamp: String = ""
)