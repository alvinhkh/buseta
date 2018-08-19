package com.alvinhkh.buseta.kmb.model.network

import com.alvinhkh.buseta.kmb.model.KmbSchedule
import com.google.gson.annotations.SerializedName

data class KmbScheduleRes(
    @field:SerializedName("data") var data: Map<String, List<KmbSchedule>>? = null,
    @field:SerializedName("result") var result: Boolean? = null
)