package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbNewsRes(
        @SerializedName("news")
        var news: NlbNews? = null
)