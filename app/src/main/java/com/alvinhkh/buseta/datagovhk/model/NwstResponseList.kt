package com.alvinhkh.buseta.datagovhk.model

import com.google.gson.annotations.SerializedName

data class NwstResponseList<T>(
        @SerializedName("type")
        var type: String? = null,
        @SerializedName("version")
        var version: String? = null,
        @SerializedName("generated_timestamp")
        var generatedTimestamp: String? = null,
        @SerializedName("data")
        var data: List<T>? = null,
        @SerializedName("code")
        var code: String? = null,
        @SerializedName("message")
        var message: String? = null
)