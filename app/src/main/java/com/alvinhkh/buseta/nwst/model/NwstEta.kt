package com.alvinhkh.buseta.nwst.model

import org.jsoup.Jsoup
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


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
        var subtitle: String = "",
        var distanceKM: String = "",
        var isSchedule: Boolean = true
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
                obj.placeTo = data[2] // empty
                obj.adultFare = data[3].toDoubleOrNull()?:0.0 // empty
                obj.stopId = data[5]
                obj.rdv = data[6]
                obj.stopName = data[8] // empty
                obj.routeId = data[9] // empty
                obj.boundText = data[10] // empty
                obj.bound = data[11]
            }
            if (data.size >= 26) {
                obj.etaIsoTime = data[12]
                obj.isSchedule = data[22] != "Y"
                obj.etaSecond = data[13].toInt()
                val tmp19 = data[19].split("|*|")
                if (tmp19.size >= 2 && tmp19[1].startsWith("202")) {
                    obj.etaIsoTime = tmp19[1]
                    try {
                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                        val etaDate = df.parse(obj.etaIsoTime)?: Date()
                        obj.etaTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(etaDate)
                        obj.title = obj.etaTime
                    } catch (e: Exception) {
                    }
                }
                if (obj.etaIsoTime == "0000-00-00 00:00:00") {
                    obj.etaIsoTime = ""
                }
                if (obj.etaTime.isEmpty() && obj.etaSecond > 0) {
                    try {
                        obj.serverTime = data[21]
                        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                        val nowCalendar = Calendar.getInstance()
                        nowCalendar.time = df.parse(obj.serverTime) ?: Date()
                        nowCalendar.add(Calendar.SECOND, obj.etaSecond)
                        obj.etaTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(nowCalendar.time)
                        obj.title = obj.etaTime
                    } catch (e: Exception) {
                    }
                }
                val tmp25 = data[25].split("|*|")
                if (tmp25.size >= 2) {
                    if (obj.title.isNotEmpty()) {
                        obj.title += " "
                    }
                    obj.title += tmp25[0]
                    if (tmp25[1].contains("距離") || tmp25[1].contains("距离") || tmp25[1].contains("Distance")) {
                        obj.distanceKM = tmp25[1]
                    } else {
                        obj.subtitle = tmp25[1]
                    }
                } else {
                    obj.subtitle = data[25].replace("|*", "")
                }
                val tmp26 = data[26].replace("|**|", "").split("|*|")
                if (obj.boundText.isEmpty() && tmp26.size > 4) {
                    obj.boundText = tmp26[4]
                }
            }
            if (obj.companyCode.isNotEmpty()) {
                if (obj.companyCode.contains("DISABLED")) {
                    obj.title = data[12]
                } else if (obj.companyCode.contains("HTML")) {
                    obj.title = Jsoup.parse(data[1]).text()
                } else if (obj.companyCode.contains("TEXT") || obj.companyCode.contains("SUSPEND")) {
                    obj.title = data[1]
                }
                obj.companyCode = "NWST"
            } else {
                obj.companyCode = "NWST"
            }
            return obj
        }
    }
}
