package com.alvinhkh.buseta.kmb.model.network


import com.alvinhkh.buseta.kmb.model.KmbAnnounce
import com.google.gson.annotations.SerializedName

import java.util.ArrayList

data class KmbAnnounceRes(
        @SerializedName("data")
        var data: ArrayList<KmbAnnounce> = arrayListOf(),

        @SerializedName("result")
        var result: Boolean = false
)