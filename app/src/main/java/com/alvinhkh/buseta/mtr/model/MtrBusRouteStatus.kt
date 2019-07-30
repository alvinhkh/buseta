package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class MtrBusRouteStatus(
        @field:SerializedName("routeNumber")
        var routeNumber: String? = null,
        @field:SerializedName("status")
        var status: String? = null
)