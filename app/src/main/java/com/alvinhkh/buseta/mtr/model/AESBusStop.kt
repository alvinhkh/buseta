package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Stop_new")
data class AESBusStop(
        @PrimaryKey
        @ColumnInfo(name = "StopID", typeAffinity = ColumnInfo.TEXT)
        var stopId: String,
        @ColumnInfo(name = "BusNumber", typeAffinity = ColumnInfo.TEXT)
        var busNumber: String,
        @ColumnInfo(name = "StopNameCN", typeAffinity = ColumnInfo.TEXT)
        var stopNameCn: String? = null,
        @ColumnInfo(name = "StopNameEN", typeAffinity = ColumnInfo.TEXT)
        var stopNameEn: String? = null,
        @ColumnInfo(name = "StopLatitude", typeAffinity = ColumnInfo.TEXT)
        var stopLatitude: String? = null,
        @ColumnInfo(name = "StopLongitude", typeAffinity = ColumnInfo.TEXT)
        var stopLongitude: String? = null,
        @ColumnInfo(name = "RouteSection", typeAffinity = ColumnInfo.TEXT)
        var routeSection: String? = null,
        @ColumnInfo(name = "DisplayStopNo", typeAffinity = ColumnInfo.TEXT)
        var displayStopNo: String? = null,
        @ColumnInfo(name = "NearestStopID", typeAffinity = ColumnInfo.TEXT)
        var nearestStopID: String? = null
)