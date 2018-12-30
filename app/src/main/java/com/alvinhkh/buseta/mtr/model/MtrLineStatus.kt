package com.alvinhkh.buseta.mtr.model

import com.alvinhkh.buseta.mtr.dao.EmptyElementConverter

import org.simpleframework.xml.Element
import org.simpleframework.xml.convert.Convert

data class MtrLineStatus(
        @field:Element(name = "line_code")
        var lineCode: String? = null,
        @field:Element(name = "url_tc", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var urlTc: String? = null,
        @field:Element(name = "url_en", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var urlEn: String? = null,
        @field:Element(name = "status")
        var status: String? = null,
        var lineName: String? = null,
        var lineColour: String? = null
)