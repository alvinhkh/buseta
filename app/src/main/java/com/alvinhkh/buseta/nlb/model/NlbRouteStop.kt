package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbRouteStop(
        @SerializedName("route_id")
        var route_id: String = "",

        @SerializedName("stop_sequence")
        var stop_sequence: String = "",

        @SerializedName("stop_id")
        var stop_id: String = "",

        @SerializedName("fare")
        var fare: String = "",

        @SerializedName("fare_holiday")
        var fare_holiday: String = "",

        @SerializedName("some_departure_observe_only")
        var some_departure_observe_only: Int = 0
)