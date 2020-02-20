package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "busRouteLine")
data class MtrBusRouteLine(
        @PrimaryKey
        @ColumnInfo(name = "routeLine_ID")
        var routeLineId: Int? = 0,
        @ColumnInfo(name = "route_ID")
        var routeId: Int? = 0,
        @ColumnInfo(name = "from_stop", typeAffinity = ColumnInfo.TEXT)
        var fromStop: String? = null,
        @ColumnInfo(name = "shape", typeAffinity = ColumnInfo.TEXT)
        var shape: String? = null,
        @ColumnInfo(name = "station_remark_en", typeAffinity = ColumnInfo.TEXT)
        var stationRemarkEn: String? = null,
        @ColumnInfo(name = "station_remark_zh", typeAffinity = ColumnInfo.TEXT)
        var stationRemarkZh: String? = null
)