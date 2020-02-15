package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class MtrLineName(
        @SerializedName("code")
        var lineCode: String? = null,
        @SerializedName("name")
        var nameEn: String? = null,
        @SerializedName("name_tc")
        var nameTc: String? = null,
        @SerializedName("colour")
        var colour: String? = null
)