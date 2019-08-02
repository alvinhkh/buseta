package com.alvinhkh.buseta.datagovhk.model

import com.google.gson.annotations.SerializedName

data class NwstRoute(
        @SerializedName("co")
        var companyCode: String = "",
        @SerializedName("route")
        var routeName: String = "",
        @SerializedName("orig_tc")
        var originTc: String = "",
        @SerializedName("orig_en")
        var originEn: String = "",
        @SerializedName("orig_sc")
        var originSc: String = "",
        @SerializedName("dest_tc")
        var destinationTc: String = "",
        @SerializedName("dest_en")
        var destinationEn: String = "",
        @SerializedName("dest_sc")
        var destinationSc: String = "",
        @SerializedName("data_timestamp")
        var dataTimestamp: String = ""
)