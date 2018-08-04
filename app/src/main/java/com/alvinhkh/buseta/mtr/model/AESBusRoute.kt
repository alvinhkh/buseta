package com.alvinhkh.buseta.mtr.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "Bus_new")
data class AESBusRoute(
        @PrimaryKey
        @ColumnInfo(name = "BusNumber", typeAffinity = ColumnInfo.TEXT)
        var busNumber: String,
        @ColumnInfo(name = "Route", typeAffinity = ColumnInfo.TEXT)
        var route: String,
        @ColumnInfo(name = "ServiceHours", typeAffinity = ColumnInfo.TEXT)
        var serviceHours: String? = null,
        @ColumnInfo(name = "Frequency", typeAffinity = ColumnInfo.TEXT)
        var frequency: String? = null,
        @ColumnInfo(name = "NearestAEMTRStation")
        var nearestAEMTRStation: Int,
        @ColumnInfo(name = "RouteSection")
        var routeSection: Int,
        @ColumnInfo(name = "DistrictID")
        var districtID: Int
)