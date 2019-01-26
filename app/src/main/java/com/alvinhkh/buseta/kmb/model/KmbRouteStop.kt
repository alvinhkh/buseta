package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbRouteStop(
        @SerializedName("CName")
        var nameTc: String = "",

        @SerializedName("Y")
        var Y: String = "",

        @SerializedName("ELocation")
        var locationEn: String = "",

        @SerializedName("X")
        var X: String = "",

        @SerializedName("AirFare")
        var airFare: String = "",

        @SerializedName("EName")
        var nameEn: String = "",

        @SerializedName("SCName")
        var nameSc: String = "",

        @SerializedName("ServiceType")
        var serviceType: String = "",

        @SerializedName("CLocation")
        var locationTc: String = "",

        @SerializedName("BSICode")
        var bsiCode: String = "",

        @SerializedName("Seq")
        var seq: String = "",

        @SerializedName("SCLocation")
        var locationSc: String = "",

        @SerializedName("Direction")
        var direction: String = "",

        @SerializedName("Bound")
        var bound: String = "",

        @SerializedName("Route")
        var route: String = ""
)