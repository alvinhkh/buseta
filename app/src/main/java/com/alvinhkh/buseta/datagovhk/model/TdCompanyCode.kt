package com.alvinhkh.buseta.datagovhk.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "dataroot", strict = false)
data class TdCompanyCode (
        @field:ElementList(entry = "COMPANY_CODE", inline = true)
        var list: List<CompanyCode> = arrayListOf()
) {

        @Root(name = "COMPANY_CODE", strict = false)
        data class CompanyCode (
                @field:Element(name = "COMPANY_CODE")
                var companyCode: String = "",

                @field:Element(name = "COMPANY_NAMEC")
                var companyNameTc: String = "",

                @field:Element(name = "COMPANY_NAMES")
                var companyNameSc: String = "",

                @field:Element(name = "COMPANY_NAMEE")
                var companyNameEn: String = "",

                @field:Element(name = "DESCRIPTION")
                var description: String = ""
        )
}
