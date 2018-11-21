package com.alvinhkh.buseta.kmb.model.network


import com.alvinhkh.buseta.kmb.model.KmbRouteBound
import com.google.gson.annotations.SerializedName

import java.util.ArrayList

data class KmbRouteBoundRes(
    @SerializedName("data")
    var data: ArrayList<KmbRouteBound>? = null,
    @SerializedName("exception")
    var exception: String? = null,
    @SerializedName("result")
    var result: Boolean? = null
)
