package com.alvinhkh.buseta.mtr.model

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ItemsListRSS")
data class MtrLatestAlertRes (
        @field:Attribute(name = "version")
        var version: String? = null,

        @field:ElementList(entry = "items")
        var items: List<MtrLatestAlert>? = null
)