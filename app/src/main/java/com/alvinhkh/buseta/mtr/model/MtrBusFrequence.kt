package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "busFrequence")
data class MtrBusFrequence(
        @PrimaryKey
        @ColumnInfo(name = "frequence_ID")
        var frequenceId: Int? = 0,
        @ColumnInfo(name = "routeLine_ID")
        var routeLineId: Int? = 0,
        @ColumnInfo(name = "description_en", typeAffinity = ColumnInfo.TEXT)
        var descriptionEn: String? = null,
        @ColumnInfo(name = "description_zh", typeAffinity = ColumnInfo.TEXT)
        var descriptionZh: String? = null,
        @ColumnInfo(name = "period", typeAffinity = ColumnInfo.TEXT)
        var period: String? = null,
        @ColumnInfo(name = "frequency", typeAffinity = ColumnInfo.TEXT)
        var frequency: String? = null,
        @ColumnInfo(name = "sort_order")
        var sortOrder: Int
)