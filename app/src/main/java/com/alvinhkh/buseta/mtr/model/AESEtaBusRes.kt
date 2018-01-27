package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class AESEtaBusRes(
        @SerializedName("busStop")
        var busStops: List<AESEtaBusStop>? = null,
        @SerializedName("caseNumber")
        var caseNumber: Number? = null,
        @SerializedName("caseNumberDetail")
        var caseNumberDetail: String? = null,
        @SerializedName("footerRemarks")
        var footerRemarks: String? = null,
        @SerializedName("routeName")
        var routeName: String? = null,
        @SerializedName("routeStatus")
        var routeStatus: String? = null,
        @SerializedName("routeStatusColour")
        var routeStatusColour: String? = null,
        @SerializedName("routeStatusRemarkContent")
        var routeStatusRemarkContent: String? = null,
        @SerializedName("routeStatusRemarkFooterRemark")
        var routeStatusRemarkFooterRemark: String? = null,
        @SerializedName("routeStatusRemarkTitle")
        var routeStatusRemarkTitle: String? = null,
        @SerializedName("routeStatusTime")
        var routeStatusTime: String? = null,
        @SerializedName("status")
        var status: String? = null
)