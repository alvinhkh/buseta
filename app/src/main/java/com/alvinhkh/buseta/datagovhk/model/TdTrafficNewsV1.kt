package com.alvinhkh.buseta.datagovhk.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "list", strict = false)
data class TdTrafficNewsV1 (
        @field:ElementList(entry = "message", inline = true)
        var messages: List<Message> = arrayListOf()
) {

        @Root(strict = false)
        data class Message (
                @field:Element(name = "msgID", required = false)
                var msgID: String = "",

                @field:Element(name = "CurrentStatus", required = false)
                var CurrentStatus: String = "",

                @field:Element(name = "ChinText", required = false)
                var ChinText: String = "",

                @field:Element(name = "ChinShort", required = false)
                var ChinShort: String = "",

                @field:Element(name = "ReferenceDate", required = false)
                var ReferenceDate: String = "",

                @field:Element(name = "IncidentRefNo", required = false)
                var IncidentRefNo: String = "",

                @field:Element(name = "CountofDistricts", required = false)
                var CountofDistricts: String = "",

                @field:Element(name = "ListOfDistrict", required = false)
                var ListOfDistrict: String = ""
        )
}
