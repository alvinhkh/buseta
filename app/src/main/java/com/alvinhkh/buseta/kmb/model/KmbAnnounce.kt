package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbAnnounce (
        @SerializedName("krbpiid")
        var krbpiid: String = "",

        @SerializedName("krbpiid_boundno")
        var krbpiid_boundno: String = "",

        @SerializedName("krbpiid_routeno")
        var krbpiid_routeno: String = "",

        @SerializedName("kpi_title")
        var titleEn: String = "",

        @SerializedName("kpi_title_chi_s")
        var titleSc: String = "",

        @SerializedName("kpi_title_chi")
        var titleTc: String = "",

        @SerializedName("kpi_noticeimageurl")
        var url: String = "",

        @SerializedName("kpi_referenceno")
        var referenceNo: String = ""
)