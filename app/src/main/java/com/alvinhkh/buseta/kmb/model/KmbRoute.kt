package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbRoute(
        @SerializedName("Destination_ENG")
        var destinationEn: String = "",

        @SerializedName("Origin_ENG")
        var originEn: String = "",

        @SerializedName("Origin_CHI")
        var originTc: String = "",

        @SerializedName("To_saturday")
        var toSaturday: String = "",

        @SerializedName("From_saturday")
        var fromSaturday: String = "",

        @SerializedName("Desc_CHI")
        var descTc: String = "",

        @SerializedName("Desc_ENG")
        var descEn: String = "",

        @SerializedName("ServiceType")
        var serviceType: String = "",

        @SerializedName("Route")
        var route: String = "",

        @SerializedName("Destination_CHI")
        var destinationTc: String = "",

        @SerializedName("Bound")
        var bound: String = "",

        @SerializedName("From_weekday")
        var fromWeekday: String = "",

        @SerializedName("From_holiday")
        var fromHoliday: String = "",

        @SerializedName("To_weekday")
        var toWeekday: String = "",

        @SerializedName("To_holiday")
        var toHoliday: String = ""
)
