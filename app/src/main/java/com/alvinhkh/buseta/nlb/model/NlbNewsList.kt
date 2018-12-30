package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbNewsList(
        @SerializedName("newses")
        var newses: List<NlbNews> = arrayListOf()
)