package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbDatabaseVersion(
        @SerializedName("version")
        var version: String = ""
)