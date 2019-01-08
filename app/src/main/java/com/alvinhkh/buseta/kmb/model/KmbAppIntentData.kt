package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbAppIntentData(
        @field:SerializedName("action") var action: String = "",
        @field:SerializedName("r") var route: String = "",
        @field:SerializedName("b") var bound: String = "",
        @field:SerializedName("c") var stopCode: String = "",
        @field:SerializedName("s") var serviceType: String = "",
        @field:SerializedName("t") var t: Int = 0
)