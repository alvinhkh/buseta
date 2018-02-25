package com.alvinhkh.buseta.mtr.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ryg_status")
data class MtrLineStatusRes (
        @field:Element
        var lastBuildDate: String? = null,
        @field:Element
        var refreshInterval: String? = null,
        @field:ElementList(entry = "line", inline = true)
        var lines: List<MtrLineStatus>? = null
)
