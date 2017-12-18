package com.alvinhkh.buseta.nwst.model


data class NwstNotice(
        var companyCode: String = "NWST",
        var routeNo: String = "",
        var releaseDate: String = "",
        var link: String = "",
        var title: String = "",
        var source: String = ""
) {

    companion object {
        fun fromString(text: String): NwstNotice? {
            if (text.isBlank()) return null
            val data = text.replace("<br>", "").trim().split("\\|\\|".toRegex()).dropLastWhile { it.isBlank() }.toTypedArray()
            if (data.size < 6) return null
            val obj = NwstNotice()
            obj.companyCode = data[0]
            obj.routeNo = data[1]
            obj.releaseDate = data[2]
            obj.link = data[3]
            obj.title = data[4]
            obj.source = data[5]
            return obj
        }
    }
}
