package com.alvinhkh.buseta.datagovhk.model

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import java.io.StringReader

data class MtrBusFare(
        var routeId: String? = null,
        var fareOctopusAdult: Number? = -1.0,
        var fareOctopusChild: Number? = -1.0,
        var fareOctopusElderly: Number? = -1.0,
        var fareOctopusPersonWithDisabilities: Number? = -1.0,
        var fareOctopusStudent: Number? = -1.0,
        var fareSingleAdult: Number? = -1.0,
        var fareSingleChild: Number? = -1.0,
        var fareSingleElderly: Number? = -1.0
) {
    companion object {
        fun fromCSV(text: String): List<MtrBusFare> {
            if (text.isBlank()) return listOf()
            val l: MutableList<MtrBusFare> = mutableListOf()
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
                if (line.size <= 8 || line[0].isBlank()) continue
                val fare = MtrBusFare()
                fare.routeId = line[0]
                fare.fareOctopusAdult = line[1].toFloat()
                fare.fareOctopusChild = line[2].toFloat()
                fare.fareOctopusElderly = line[3].toFloat()
                fare.fareOctopusPersonWithDisabilities = line[4].toFloat()
                fare.fareOctopusStudent = line[5].toFloat()
                fare.fareSingleAdult = line[6].toFloat()
                fare.fareSingleChild = line[7].toFloat()
                fare.fareSingleElderly = line[8].toFloat()
                l.add(fare)
            }
            return l
        }
    }
}
