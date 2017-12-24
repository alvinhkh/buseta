package com.alvinhkh.buseta.nwst.model

data class NwstLatLong(
        var path: List<Pair<Double, Double>>
) {

    companion object {
        fun fromString(text: String): NwstLatLong? {
            if (text.isBlank()) return null
            val data = text.trim().split("||").dropLastWhile { it.isBlank() }.toTypedArray()
            val a = data
                    .map { it.split(",") }
                    .map { Pair(it[0].toDouble(), it[1].toDouble()) }
            return NwstLatLong(a)
        }
    }
}
