package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class MtrBusRouteStatusRes(
        @SerializedName("routeStatus")
        var routeStatus: List<MtrBusRouteStatus>? = null
)