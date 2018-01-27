package com.alvinhkh.buseta.mtr.model


data class AESEtaBusStopsRequest(
        var routeName: String,
        var ver: String,
        var language: String,
        var key: String
)
