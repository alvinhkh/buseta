package com.alvinhkh.buseta.nlb.model

import com.google.gson.annotations.SerializedName

data class NlbNews(
        @SerializedName("newsId")
        var newsId: String = "",

        @SerializedName("title")
        var title: String = "",

        @SerializedName("content")
        var content: String = "",

        @SerializedName("publishDate")
        var publishDate: String = ""
)