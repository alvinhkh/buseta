package com.alvinhkh.buseta.kmb.model.network

import com.google.gson.annotations.SerializedName

data class KmbWebEtaRes(
        @SerializedName("data")
        var data: KmbEtaRes? = null,

        @SerializedName("result")
        var result: Boolean? = null
)