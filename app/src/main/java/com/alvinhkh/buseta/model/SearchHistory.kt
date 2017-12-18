package com.alvinhkh.buseta.model;

data class SearchHistory(
        var companyCode: String = "",
        var route: String = "",
        var timestamp: Long = 0L,
        var type: String = ""
)