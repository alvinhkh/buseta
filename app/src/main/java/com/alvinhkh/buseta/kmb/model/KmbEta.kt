package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbEta(
        @SerializedName("w")
        var wheelchair: String? = null,

        @SerializedName("ex")
        var expire: String? = null,

        @SerializedName("eot")
        var eot: String? = null,

        @SerializedName("t")
        var time: String? = null,

        @SerializedName("ei")
        var ei: String? = null,

        @SerializedName("bus_service_type")
        var serviceType: String? = null,

        @SerializedName("ol")
        var ol: String? = null,

        @SerializedName("wifi")
        var wifi: String? = null,

        @SerializedName("dis")
        var distanceM: String? = null
)