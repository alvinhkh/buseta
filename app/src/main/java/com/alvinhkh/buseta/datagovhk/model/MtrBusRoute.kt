package com.alvinhkh.buseta.datagovhk.model

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.StringReader

data class MtrBusRoute(
        var routeId: String? = null,
        var routeNameChi: String? = null,
        var routeNameEng: String? = null
) {
    companion object {
        fun fromCSV(text: String): List<MtrBusRoute> {
            if (text.isBlank()) return listOf()
            val l: MutableList<MtrBusRoute> = mutableListOf()
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
                if (line.size <= 2 || line[0].isBlank()) continue
                val route = MtrBusRoute()
                route.routeId = line[0]
                route.routeNameChi = line[1]
                route.routeNameEng = line[2]
                l.add(route)
            }
            return l
        }
    }
}
