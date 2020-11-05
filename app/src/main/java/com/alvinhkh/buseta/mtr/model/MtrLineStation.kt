package com.alvinhkh.buseta.mtr.model

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.StringReader

data class MtrLineStation(
        var lineCode: String? = null,
        var direction: String? = null,
        var stationCode: String? = null,
        var stationID: String? = null,
        var chineseName: String? = null,
        var englishName: String? = null,
        var sequence: String? = null
) {
    companion object {
        fun fromCSV(text: String, lineCode: String): List<MtrLineStation> {
            if (text.isBlank()) return listOf()
            val l: MutableList<MtrLineStation> = mutableListOf()
            fromCSV(text).forEach {
                if (it.lineCode == lineCode) {
                    l.add(it)
                }
            }
            return l
        }

        fun fromCSV(text: String): List<MtrLineStation> {
            if (text.isBlank()) return listOf()
            val l: MutableList<MtrLineStation> = mutableListOf()
            val csvParser = CSVParserBuilder().withSeparator(',')
                    .withQuoteChar('"')
                    .withStrictQuotes(false)
                    .build()
            val reader = CSVReaderBuilder(StringReader(text))
                    .withCSVParser(csvParser)
                    .withVerifyReader(true)
                    .withKeepCarriageReturn(false)
                    .build()

            val lines = reader.readAll()
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.size <= 6 || line[0].isBlank()) continue
                val station = MtrLineStation()
                station.lineCode = line[0]
                station.direction = if (line[1]?.contains("UT") == true) "UT" else "DT"
                station.stationCode = line[2]
                station.stationID = line[3]
                station.chineseName = line[4]
                station.englishName = line[5]
                station.sequence = line[6]
                l.add(station)
            }
            return l
        }
    }
}