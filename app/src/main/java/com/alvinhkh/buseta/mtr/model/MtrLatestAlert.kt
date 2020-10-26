package com.alvinhkh.buseta.mtr.model

import com.alvinhkh.buseta.mtr.dao.EmptyElementConverter
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.convert.Convert

@Root(name = "item")
data class MtrLatestAlert (
        @field:Element(name = "type")
        var type: Int = -1,
        @field:Element(name = "msg_tc", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var msgTc: String? = null,
        @field:Element(name = "msg_en", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var msgEn: String? = null,
        @field:Element(name = "url_tc", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var urlTc: String = "",
        @field:Element(name = "url_en", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var urlEn: String = "",
        @field:Element(name = "banner_title_tc", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var bannerTitleTc: String = "",
        @field:Element(name = "banner_title_en", required = false)
        @field:Convert(value = EmptyElementConverter::class)
        var bannerTitleEn: String = ""
)