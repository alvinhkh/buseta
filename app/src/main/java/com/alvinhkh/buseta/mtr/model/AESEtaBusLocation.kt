package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class AESEtaBusLocation(
        @field:SerializedName("latitude")
        var latitude: Double = 0.0,
        @field:SerializedName("longitude")
        var longitude: Double = 0.0
)