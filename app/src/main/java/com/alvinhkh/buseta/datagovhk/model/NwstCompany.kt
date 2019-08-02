package com.alvinhkh.buseta.datagovhk.model

import com.google.gson.annotations.SerializedName

data class NwstCompany(
        @SerializedName("co")
        var companyCode: String = "",
        @SerializedName("name_tc")
        var nameTc: String = "",
        @SerializedName("name_en")
        var nameEn: String = "",
        @SerializedName("name_sc")
        var nameSc: String = "",
        @SerializedName("url")
        var url: String = "",
        @SerializedName("data_timestamp")
        var dataTimestamp: String = ""
) {
        companion object {
                val CTB = "ctb"

                val NWFB = "nwfb"
        }
}