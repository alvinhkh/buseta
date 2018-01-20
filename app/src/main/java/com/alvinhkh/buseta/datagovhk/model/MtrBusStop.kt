package com.alvinhkh.buseta.datagovhk.model

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.StringReader

data class MtrBusStop(
        var routeId: String? = null,
        var stationSequenceNo: Number? = -1.0,
        var stationNextSeqNo: Number? = -1.0,
        var stationNameChi: String? = null,
        var stationNameEng: String? = null
) {
    companion object {
        fun fromCSV(text: String): List<MtrBusStop> {
            if (text.isBlank()) return listOf()
            val l: MutableList<MtrBusStop> = mutableListOf()
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
                if (line.size <= 4 || line[0].isBlank()) continue
                val stop = MtrBusStop()
                stop.routeId = line[0]
                stop.stationSequenceNo = line[1].toFloat()
                stop.stationNextSeqNo = line[2].toFloat()
                stop.stationNameChi = line[3]
                stop.stationNameEng = line[4]
                l.add(stop)
            }
            return l
        }
    }
}
