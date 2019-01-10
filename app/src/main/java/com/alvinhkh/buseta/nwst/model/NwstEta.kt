package com.alvinhkh.buseta.nwst.model

import org.jsoup.Jsoup


data class NwstEta(
        var serverTime: String = "",
        var companyCode: String = "NWST",
        var routeNo: String = "",
        var placeTo: String = "",
        var adultFare: Double = 0.0,
        var stopId: String = "",
        var rdv: String = "",
        var stopName: String = "",
        var routeId: String = "",
        var boundText: String = "",
        var bound: String = "",
        var etaIsoTime: String = "",
        var etaSecond: Int = 0,
        var etaTime: String = "",
        var title: String = "",
        var subtitle: String = ""
) {
    companion object {
        fun fromString(text: String): NwstEta? {
            if (text.isBlank()) return null
            val data = text.replace("<br>", "").trim().split("\\|\\|".toRegex()).dropLastWhile { it.isBlank() }.toTypedArray()
            val obj = NwstEta()
            if (data.size < 2) return null
            obj.companyCode = data[0]
            if (data.size >= 12) {
                obj.routeNo = data[1]
                obj.placeTo = data[2]
                obj.adultFare = data[3].toDouble()
                obj.stopId = data[5]
                obj.rdv = data[6]
                obj.stopName = data[8]
                obj.routeId = data[9]
                obj.boundText = data[10]
                obj.bound = data[11]
            }
            if (data.size >= 19) {
                obj.etaIsoTime = data[12]
                obj.etaSecond = data[13].toInt()
                if (obj.etaIsoTime == "0000-00-00 00:00:00") {
                    obj.etaIsoTime = ""
                }
                val tmp2 = data[16].split("|")
                obj.etaTime = tmp2[0]
                val tmp3 = data[17].split("|")
                val tmp4 = data[18].split("|")
                obj.title = tmp3[0].replace(Regex.fromLiteral("往: " + obj.placeTo), "")
                        .replace(", ", "").replace(obj.boundText, "")
                if (obj.title.isEmpty()) {
                    obj.title = obj.etaTime
                }
                obj.subtitle = tmp4[0]
            }
            if (!obj.companyCode.isEmpty()) {
                if (obj.companyCode.contains("DISABLED")) {
                    obj.title = data[12]
                } else if (obj.companyCode.contains("HTML")) {
                    obj.title = Jsoup.parse(data[1]).text()
                } else if (obj.companyCode.contains("TEXT") || obj.companyCode.contains("SUSPEND")) {
                    obj.title = data[1]
                }
                obj.companyCode = "NWST"
            }
            return obj
        }
    }
}
