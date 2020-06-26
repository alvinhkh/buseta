package com.alvinhkh.buseta.kmb.model.network

import com.alvinhkh.buseta.kmb.model.KmbEta
import com.google.gson.annotations.SerializedName

data class KmbWebEtaRes(
        @SerializedName("data")
        var data: KmbEtaRes? = null,

        @SerializedName("result")
        var result: Boolean? = null
) {
        data class KmbEtaRes(
                @SerializedName("responsecode")
                var responsecode: Int? = null,

                @SerializedName("response")
                var etas: ArrayList<KmbEta>? = null,

                @SerializedName("generated")
                var generated: Long? = null,

                @SerializedName("updated")
                var updated: Long? = null
        )
}