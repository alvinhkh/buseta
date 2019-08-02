package com.alvinhkh.buseta.datagovhk.model

import com.google.gson.annotations.SerializedName

data class NwstEta(
        @SerializedName("co")
        var companyCode: String = "",
        @SerializedName("route")
        var routeName: String = "",
        @SerializedName("dir")
        var direction: String = "",
        @SerializedName("seq")
        var sequence: Int = -1,
        @SerializedName("dest_tc")
        var destinationTc: String = "",
        @SerializedName("dest_en")
        var destinationEn: String = "",
        @SerializedName("dest_sc")
        var destinationSc: String = "",
        @SerializedName("eta")
        var eta: String = "",
        @SerializedName("rmk_tc")
        var remarkTc: String = "",
        @SerializedName("rmk_en")
        var remarkEn: String = "",
        @SerializedName("rmk_sc")
        var remarkSc: String = "",
        @SerializedName("eta_seq")
        var etaSequence: Int = -1,
        @SerializedName("data_timestamp")
        var dataTimestamp: String = ""
)