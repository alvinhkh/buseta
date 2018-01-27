package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class AESEtaBusStop(
        @field:SerializedName("bus")
        var buses: List<AESEtaBus>? = null,
        @field:SerializedName("busIcon")
        var busIcon: String? = null,
        @field:SerializedName("busStopId")
        var busStopId: String? = null,
        @field:SerializedName("busStopRemark")
        var busStopRemark: String? = null,
        @field:SerializedName("busStopStatus")
        var busStopStatus: String? = null,
        @field:SerializedName("busStopStatusRemarkContent")
        var busStopStatusRemarkContent: String? = null,
        @field:SerializedName("busStopStatusRemarkTitle")
        var busStopStatusRemarkTitle: String? = null,
        @field:SerializedName("busStopStatusTime")
        var busStopStatusTime: String? = null,
        @field:SerializedName("isSuspended")
        var isSuspended: String? = null
)