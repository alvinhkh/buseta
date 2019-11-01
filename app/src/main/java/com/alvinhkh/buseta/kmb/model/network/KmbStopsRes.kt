package com.alvinhkh.buseta.kmb.model.network


import com.alvinhkh.buseta.kmb.model.KmbRouteBasicInfo
import com.alvinhkh.buseta.kmb.model.KmbRouteStop
import com.google.gson.annotations.SerializedName

import java.util.ArrayList

data class KmbStopsRes(
        @SerializedName("data")
        var data: Data? = null,

        @SerializedName("result")
        var result: Boolean? = null
) {

    data class Data(
            @SerializedName("basicInfo")
            var basicInfo: KmbRouteBasicInfo? = null,

            @SerializedName("routeStops")
            var routeStops: ArrayList<KmbRouteStop> = arrayListOf(),

            @SerializedName("additionalInfo")
            var additionalInfo: AdditionalInfo? = null,

            @SerializedName("route")
            var route: Route? = null
    )

    data class Route(
            @SerializedName("lineGeometry")
            var lineGeometry: String? = null,

            @SerializedName("bound")
            var bound: String? = null,

            @SerializedName("serviceType")
            var serviceType: String? = null,

            @SerializedName("route")
            var route: String? = null
    )

    data class LineGeometry (
            @SerializedName("paths")
            var paths: List<List<List<Double>>> = emptyList()
    )

    data class AdditionalInfo(
            @SerializedName("ENG")
            var en: String? = null,

            @SerializedName("TC")
            var tc: String? = null,

            @SerializedName("SC")
            var sc: String? = null
    )
}
