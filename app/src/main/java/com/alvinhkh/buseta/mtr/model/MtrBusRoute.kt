package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "busRoute")
data class MtrBusRoute(
        @PrimaryKey
        @ColumnInfo(name = "route_ID")
        var routeId: Int,
        @ColumnInfo(name = "route_number", typeAffinity = ColumnInfo.TEXT)
        var routeNumber: String,
        @ColumnInfo(name = "description_en", typeAffinity = ColumnInfo.TEXT)
        var descriptionEn: String? = null,
        @ColumnInfo(name = "description_zh", typeAffinity = ColumnInfo.TEXT)
        var descriptionZh: String? = null,
        @ColumnInfo(name = "extra_description_en", typeAffinity = ColumnInfo.TEXT)
        var extraDescriptionEn: String? = null,
        @ColumnInfo(name = "extra_description_zh", typeAffinity = ColumnInfo.TEXT)
        var extraDescriptionZh: String? = null,
        @ColumnInfo(name = "effectiveDate", typeAffinity = ColumnInfo.TEXT)
        var effectiveDate: String? = null,
        @ColumnInfo(name = "frequency_remark_en", typeAffinity = ColumnInfo.TEXT)
        var frequencyRemarkEn: String? = null,
        @ColumnInfo(name = "frequency_remark_zh", typeAffinity = ColumnInfo.TEXT)
        var frequencyRemarkZh: String? = null,
        @ColumnInfo(name = "route_remark_en", typeAffinity = ColumnInfo.TEXT)
        var routeRemarkEn: String? = null,
        @ColumnInfo(name = "route_remark_zh", typeAffinity = ColumnInfo.TEXT)
        var routeRemarkZh: String? = null,
        @ColumnInfo(name = "hotline", typeAffinity = ColumnInfo.TEXT)
        var hotline: String? = null
)