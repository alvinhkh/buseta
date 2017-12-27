package com.alvinhkh.buseta.nwst.model

data class NwstVariant(
        var rank: String = "",
        var ball: String = "",
        var rdv: String = "",
        var remark: String = "",
        var routeInfo: String = "",
        var r : String = "",
        var d : String = "",
        var v : String = "",
        var companyCode : String = "",
        var startSequence : Int = 0,
        var endSequence : Int = 0,
        var routeIndex : String = "",
        var bound : String = ""
) {

    companion object {
        fun fromString(text: String): NwstVariant? {
            if (text.isBlank()) return null
            val data = text.replace("<br>", "").trim().split("||").dropLastWhile { it.isBlank() }.toTypedArray()
            if (data.size < 5) return null
            val obj = NwstVariant()
            obj.rank = data[0]
            obj.ball = data[1]
            obj.rdv = data[2]
            obj.remark = data[3]
            obj.routeInfo = data[4]
            if (obj.rdv.isNotEmpty()) {
                val tmp = obj.rdv.split("-")
                obj.r = tmp[0]
                obj.d = tmp[1]
                obj.v = tmp[2]
            }
            if (obj.routeInfo.isNotEmpty()) {
                val tmp2 = obj.routeInfo.split("***")
                if (tmp2.size == 6) {
                    obj.companyCode = tmp2[0]
                    obj.startSequence = tmp2[2].toInt()
                    obj.endSequence = tmp2[3].toInt()
                    obj.routeIndex = tmp2[4]
                    obj.bound = tmp2[5]
                }
            }
            return obj
        }

        fun parseInfo(text: String): NwstVariant? {
            val obj = NwstVariant()
            if (text.isNotEmpty()) {
                val tmp2 = text.split("***")
                if (tmp2.size == 6) {
                    obj.companyCode = tmp2[0]
                    obj.rdv = tmp2[1]
                    obj.startSequence = tmp2[2].toInt()
                    obj.endSequence = tmp2[3].toInt()
                    obj.routeIndex = tmp2[4]
                    obj.bound = tmp2[5]
                }
            }
            return obj
        }
    }
}
