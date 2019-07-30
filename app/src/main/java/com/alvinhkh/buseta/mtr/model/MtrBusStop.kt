package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "busStop")
data class MtrBusStop(
        @PrimaryKey
        @ColumnInfo(name = "stop_ID")
        var stopId: Int,
        @ColumnInfo(name = "routeLine_ID")
        var routeLineId: Int,
        @ColumnInfo(name = "name_en", typeAffinity = ColumnInfo.TEXT)
        var nameEn: String,
        @ColumnInfo(name = "name_ch", typeAffinity = ColumnInfo.TEXT)
        var nameCh: String? = null,
        @ColumnInfo(name = "remark_en", typeAffinity = ColumnInfo.TEXT)
        var remarkEn: String? = null,
        @ColumnInfo(name = "remark_ch", typeAffinity = ColumnInfo.TEXT)
        var remarkCh: String? = null,
        @ColumnInfo(name = "rail_line", typeAffinity = ColumnInfo.TEXT)
        var railLine: String? = null,
        @ColumnInfo(name = "is_edge")
        var isEdge: Int,
        @ColumnInfo(name = "sort_order")
        var sortOrder: Int,
        @ColumnInfo(name = "latitude", typeAffinity = ColumnInfo.TEXT)
        var latitude: String? = null,
        @ColumnInfo(name = "longitude", typeAffinity = ColumnInfo.TEXT)
        var longitude: String? = null,
        @ColumnInfo(name = "name2_en", typeAffinity = ColumnInfo.TEXT)
        var name2En: String? = null,
        @ColumnInfo(name = "name2_ch", typeAffinity = ColumnInfo.TEXT)
        var name2Ch: String? = null,
        @ColumnInfo(name = "ref_ID", typeAffinity = ColumnInfo.TEXT)
        var refId: String? = null
)