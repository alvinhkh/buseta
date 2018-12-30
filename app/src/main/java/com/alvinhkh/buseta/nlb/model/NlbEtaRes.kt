package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbEtaRes(
        @SerializedName("estimatedArrivalTime")
        var estimatedArrivalTime: ETA = ETA()
) {
    data class ETA(
            @SerializedName("html")
            var html: String = ""
    )
}
