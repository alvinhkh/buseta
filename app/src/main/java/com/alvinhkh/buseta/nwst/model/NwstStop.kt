package com.alvinhkh.buseta.nwst.model


data class NwstStop(
        var rdv: String = "",
        var sequence: Int = 0,
        var stopId: String = "",
        var poleId: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var stopName: String = "",
        var placeTo: String = "",
        var ccode: String = "",
        var adultFare: Double = 0.0,
        var isEta: Boolean = false,
        var seniorFare: Double = 0.0,
        var childFare: Double = 0.0
) {
    companion object {
        fun fromString(text: String): NwstStop? {
            if (text.isBlank()) return null
            val data = text.replace("<br>", "").trim().split("\\|\\|".toRegex()).dropLastWhile { it.isBlank() }.toTypedArray()
            if (data.size < 13) return null
            if (!data[0].equals("X1")) return null
            val obj = NwstStop()
            obj.rdv = data[1]
            obj.sequence = data[2].toInt()
            obj.stopId = data[3]
            obj.poleId = data[4]
            obj.latitude = data[5].toDouble()
            obj.longitude = data[6].toDouble()
            obj.stopName = data[7]
            obj.placeTo = data[8]
            obj.ccode = data[9]
            obj.adultFare = data[10].toDouble()
            obj.isEta = data[11].equals("Y")
            obj.seniorFare = data[12].toDouble()
            obj.childFare = data[13].toDouble()
            return obj
        }
    }
}
