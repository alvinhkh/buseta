package com.alvinhkh.buseta.kmb.model.network


import com.alvinhkh.buseta.kmb.model.KmbEta
import com.google.gson.annotations.SerializedName

data class KmbEtaRes(
        @SerializedName("routeNo")
        var routeNo: String? = null,

        @SerializedName("bound")
        var bound: Int? = null,

        @SerializedName("service_type")
        var serviceType: Int? = null,

        @SerializedName("seq")
        var seq: Int? = null,

        @SerializedName("responsecode")
        var responseCode: Int? = null,

        @SerializedName("generated")
        var generated: Long? = null,

        @SerializedName("updated")
        var updated: Long? = null,

        @SerializedName("eta")
        var etas: ArrayList<KmbEta>? = null
)