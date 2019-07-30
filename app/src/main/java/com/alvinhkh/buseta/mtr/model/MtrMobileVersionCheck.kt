package com.alvinhkh.buseta.mtr.model

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "mtrmobile", strict = false)
data class MtrMobileVersionCheck(
        @field:Element(name = "resources_v12")
        var resources: ResourcesV12? = null
) {
    @Root(strict = false)
    data class ResourcesV12(
            @field:Element(name = "mtr_aes_v12")
            var aes: ResourcesV12Attr? = null,

            @field:Element(name = "MTRBus_v12")
            var mtrBus: ResourcesV12Attr? = null
    ) {
        @Root(strict = false)
        data class ResourcesV12Attr(
                @field:Attribute(name = "url")
                var url: String? = null,

                @field:Attribute(name = "last_update_datetime")
                var lastUpdateDatetime: String? = null,

                @field:Attribute(name = "minDate")
                var minDate: String? = null,

                @field:Attribute(name = "fileSize")
                var fileSize: String? = null,

                @field:Attribute(name = "supportVersion")
                var supportVersion: String? = null
        )
    }
}
