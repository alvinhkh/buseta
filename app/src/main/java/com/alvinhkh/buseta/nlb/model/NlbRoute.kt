package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbRoute(
        @SerializedName("route_id")
        var route_id: String = "",

        @SerializedName("route_no")
        var route_no: String = "",

        @SerializedName("route_name_c")
        var route_name_c: String = "",

        @SerializedName("route_name_s")
        var route_name_s: String = "",

        @SerializedName("route_name_e")
        var route_name_e: String = "",

        @SerializedName("time_table_c")
        var time_table_c: String = "",

        @SerializedName("trip_time")
        var trip_time: String = "",

        @SerializedName("trip_distance")
        var trip_distance: String = "",

        @SerializedName("overnight_route")
        var overnight_route: Int = 0,

        @SerializedName("special_route")
        var special_route: Int = 0
)