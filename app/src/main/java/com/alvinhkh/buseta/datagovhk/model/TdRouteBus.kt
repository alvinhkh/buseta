package com.alvinhkh.buseta.datagovhk.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "dataroot", strict = false)
data class TdRouteBus (
        @field:ElementList(entry = "ROUTE", inline = true)
        var routeList: List<Route> = arrayListOf()
) {

        @Root(name = "ROUTE", strict = false)
        data class Route (
                @field:Element(name = "ROUTE_ID")
                var routeId: String = "",

                @field:Element(name = "COMPANY_CODE")
                var companyCode: String = "",

                @field:Element(name = "ROUTE_NAMEC")
                var routeNameTc: String = "",

                @field:Element(name = "ROUTE_NAMES")
                var routeNameSc: String = "",

                @field:Element(name = "ROUTE_NAMEE")
                var routeNameEn: String = "",

                @field:Element(name = "ROUTE_TYPE")
                var routeType: String = "",

                @field:Element(name = "SERVICE_MODE")
                var serviceMode: String = "",

                @field:Element(name = "SPECIAL_TYPE")
                var specialType: String = "",

                @field:Element(name = "LOC_START_NAMEC")
                var locationStartNameTc: String = "",

                @field:Element(name = "LOC_START_NAMES")
                var locationStartNameSc: String = "",

                @field:Element(name = "LOC_START_NAMEE")
                var locationStartNameEn: String = "",

                @field:Element(name = "LOC_END_NAMEC")
                var locationEndNameTc: String = "",

                @field:Element(name = "LOC_END_NAMES")
                var locationEndNameSc: String = "",

                @field:Element(name = "LOC_END_NAMEE")
                var locationEndNameEn: String = "",

                @field:Element(name = "HYPERLINK_C")
                var hyperlinkTc: String = "",

                @field:Element(name = "HYPERLINK_S")
                var hyperlinkSc: String = "",

                @field:Element(name = "HYPERLINK_E")
                var hyperlinkEn: String = "",

                @field:Element(name = "FULL_FARE")
                var fullFare: String = "",

                @field:Element(name = "LAST_UPDATE_DATE")
                var lastUpdateDate: String = ""
        )
}
