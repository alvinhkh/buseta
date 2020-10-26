package com.alvinhkh.buseta.nwst.model

data class NwstQueryData(
        var appId: String = "",
        var syscode5: String = "",
        var version: String = "",
        var version2: String = "",
        var lastUpdated: Long = 0
)