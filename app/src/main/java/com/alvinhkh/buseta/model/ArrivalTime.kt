package com.alvinhkh.buseta.model


data class ArrivalTime (
    var capacity: Int = -1,
    var companyCode: String = "",
    var estimate: String = "",
    var expire: String = "",
    var expired: Boolean = false,
    var id: String = "0",
    var isSchedule: Boolean = false,
    var hasWheelchair: Boolean = false,
    var hasWifi: Boolean = false,
    var text: String = "",
    var isoTime: String = "",
    var distanceKM: Float = -1.0f,
    var plate: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var platform: String = "",
    var destination: String = "",
    var direction: String = "",
    var generatedAt: Long = 0L,
    var updatedAt: Long = 0L,
    var note: String = ""
)
