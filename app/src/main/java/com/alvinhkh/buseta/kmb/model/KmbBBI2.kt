package com.alvinhkh.buseta.kmb.model

import com.google.gson.annotations.SerializedName

data class KmbBBI2(
        @field:SerializedName("Result") var result: String = "",
        @field:SerializedName("Message") var message: String = "",
        @field:SerializedName("bus_arr") var busArr: List<Map<String, String>> = listOf(),
        @field:SerializedName("Records") var records: List<Record> = listOf()
) {

    data class Record(
            @field:SerializedName("sec_routeno") var secRouteno: String = "",
            @field:SerializedName("sec_dest") var secDest: String = "",
            @field:SerializedName("xchange") var xchange: String = "",
            @field:SerializedName("validity") var validity: String = "",
            @field:SerializedName("detail") var detail: String = "",
            @field:SerializedName("discount_max") var discountMax: String = "",
            @field:SerializedName("spec_remark_eng") var specRemarkEn: String = "",
            @field:SerializedName("spec_remark_chi") var specRemarkTc: String = ""
    )
}