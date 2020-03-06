package com.alvinhkh.buseta.nwst.model


data class NwstRoute(
        var companyCode: String = "NWST",
        var routeNo: String = "",
        var locationCode: String = "",
        var routeType: String = "",
        var placeFrom: String = "",
        var placeTo: String = "",
        var rdv: String = "",
        var routeIndex: String = "",
        var bound: String = "",
        var remark: String = "",
        var stopsStartSequence: Int = 0,
        var stopsEndSequence: Int = 0
) {

    companion object {
        fun fromString(text: String): NwstRoute? {
            if (text.isBlank()) return null
            val data = text.replace("<br>", "").trim().split("\\|\\|".toRegex()).dropLastWhile { it.isBlank() }.toTypedArray()
            if (data.size < 10) return null
            val obj = NwstRoute()
            obj.companyCode = data[0]
            obj.routeNo = data[1]
            obj.locationCode = data[2]
            obj.routeType = data[3]
            obj.placeFrom = data[4]
            obj.placeTo = data[5]
            obj.rdv = data[7]
            obj.routeIndex = data[8]
            obj.bound = data[9]
            if (data.size > 10) {
                obj.remark = data[10]
            }
            if (data.size > 13) {
                // data[11] // KMB
                obj.stopsStartSequence = data[12].toIntOrNull()?:0
                obj.stopsEndSequence = data[13].toIntOrNull()?:0
                // data[14] // N
            }
            return obj
        }
    }
}
