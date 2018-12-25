package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbRoutesInStop(
    @field:SerializedName("data") var data: List<String>? = null,
    @field:SerializedName("result") var result: Boolean? = null
)