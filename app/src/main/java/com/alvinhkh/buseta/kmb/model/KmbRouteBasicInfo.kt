package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbRouteBasicInfo(
        @SerializedName("Racecourse")
        var racecourse: String = "",

        @SerializedName("DestEName")
        var destinationEn: String = "",

        @SerializedName("OriCName")
        var originTc: String = "",

        @SerializedName("ServiceTypeENG")
        var serviceTypeEn: String = "",

        @SerializedName("DestCName")
        var destinationTc: String = "",

        @SerializedName("BusType")
        var busType: String = "",

        @SerializedName("Airport")
        var airport: String = "",

        @SerializedName("ServiceTypeTC")
        var serviceTypeTc: String = "",

        @SerializedName("Overnight")
        var overnight: String = "",

        @SerializedName("ServiceTypeSC")
        var serviceTypeSc: String = "",

        @SerializedName("OriSCName")
        var originSc: String = "",

        @SerializedName("DestSCName")
        var destinationSc: String = "",

        @SerializedName("Special")
        var special: String = "",

        @SerializedName("OriEName")
        var originEn: String = ""
)