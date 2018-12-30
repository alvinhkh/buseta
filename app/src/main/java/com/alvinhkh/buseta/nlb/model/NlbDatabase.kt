package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbDatabase(
        @SerializedName("version")
        var version: String = "",

        @SerializedName("routes")
        var routes: List<NlbRoute> = arrayListOf(),

        @SerializedName("stop_districts")
        var stop_districts: List<NlbStopDistrict> = arrayListOf(),

        @SerializedName("stops")
        var stops: List<NlbStop> = arrayListOf(),

        @SerializedName("route_stops")
        var route_stops: List<NlbRouteStop> = arrayListOf()
)
