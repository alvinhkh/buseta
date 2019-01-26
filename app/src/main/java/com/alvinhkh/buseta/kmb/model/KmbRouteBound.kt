package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbRouteBound(
        @SerializedName("SERVICE_TYPE")
        var serviceType: Int = 0,

        @SerializedName("BOUND")
        var bound: Int = 0,

        @SerializedName("ROUTE")
        var route: String = ""
)