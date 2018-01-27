package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class AESEtaBusLocation(
        @field:SerializedName("latitude")
        var latitude: Double? = null,
        @field:SerializedName("longitude")
        var longitude: Double? = null
)