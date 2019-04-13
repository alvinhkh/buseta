package com.alvinhkh.buseta.datagovhk.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "dataroot", strict = false)
data class TdRouteStop (
        @field:ElementList(entry = "RSTOP", inline = true)
        var stopList: List<RouteStop> = arrayListOf()
) {

        @Root(name = "RSTOP", strict = false)
        data class RouteStop (
                @field:Element(name = "ROUTE_ID")
                var routeId: String = "",

                @field:Element(name = "ROUTE_SEQ")
                var routeSequence: String = "",

                @field:Element(name = "STOP_SEQ")
                var stopSequence: String = "",

                @field:Element(name = "STOP_ID")
                var stopId: String = "",

                @field:Element(name = "STOP_NAMEC")
                var stopNameTc: String = "",

                @field:Element(name = "STOP_NAMES")
                var stopNameSc: String = "",

                @field:Element(name = "STOP_NAMEE")
                var stopNameEn: String = "",

                @field:Element(name = "LAST_UPDATE_DATE")
                var lastUpdateDate: String = ""
        )
}
