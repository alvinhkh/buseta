package com.alvinhkh.buseta.mtr.model

import com.google.gson.annotations.SerializedName

data class MtrScheduleRes(
        @field:SerializedName("status")
        var status: Int = -1,
        @field:SerializedName("message")
        var message: String? = null,
        @field:SerializedName("curr_time")
        var currentTime: String? = null,
        @field:SerializedName("sys_time")
        var systemTime: String? = null,
        @field:SerializedName("isdelay")
        var isDelay: String? = null,
        @field:SerializedName("data")
        var data: Map<String, Data>? = null
) {
    data class Data(
            @field:SerializedName("curr_time")
            var currentTime: String? = null,
            @field:SerializedName("sys_time")
            var systemTime: String? = null,
            @field:SerializedName("UP")
            var up: List<MtrSchedule>? = null,
            @field:SerializedName("DOWN")
            var down: List<MtrSchedule>? = null
    )
}