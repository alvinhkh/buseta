package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Bus_new")
data class AESBusRoute(
        @PrimaryKey
        @ColumnInfo(name = "BusNumber", typeAffinity = ColumnInfo.TEXT)
        var busNumber: String,
        @ColumnInfo(name = "Route", typeAffinity = ColumnInfo.TEXT)
        var route: String? = null,
        @ColumnInfo(name = "ServiceHours", typeAffinity = ColumnInfo.TEXT)
        var serviceHours: String? = null,
        @ColumnInfo(name = "Frequency", typeAffinity = ColumnInfo.TEXT)
        var frequency: String? = null,
        @ColumnInfo(name = "NearestAEMTRStation")
        var nearestAEMTRStation: Int? = 0,
        @ColumnInfo(name = "RouteSection")
        var routeSection: Int? = 0,
        @ColumnInfo(name = "DistrictID")
        var districtID: Int? = 0
)