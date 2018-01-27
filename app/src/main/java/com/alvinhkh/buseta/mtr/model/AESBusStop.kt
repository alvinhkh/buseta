package com.alvinhkh.buseta.mtr.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "Stop_new")
data class AESBusStop(
        @PrimaryKey
        @ColumnInfo(name = "StopID", typeAffinity = ColumnInfo.TEXT)
        var stopId: String = "",
        @ColumnInfo(name = "BusNumber", typeAffinity = ColumnInfo.TEXT)
        var busNumber: String?,
        @ColumnInfo(name = "StopNameCN", typeAffinity = ColumnInfo.TEXT)
        var stopNameCn: String?,
        @ColumnInfo(name = "StopNameEN", typeAffinity = ColumnInfo.TEXT)
        var stopNameEn: String?,
        @ColumnInfo(name = "StopLatitude", typeAffinity = ColumnInfo.TEXT)
        var stopLatitude: String?,
        @ColumnInfo(name = "StopLongitude", typeAffinity = ColumnInfo.TEXT)
        var stopLongitude: String?,
        @ColumnInfo(name = "RouteSection", typeAffinity = ColumnInfo.TEXT)
        var routeSection: String?,
        @ColumnInfo(name = "DisplayStopNo", typeAffinity = ColumnInfo.TEXT)
        var displayStopNo: String?,
        @ColumnInfo(name = "NearestStopID", typeAffinity = ColumnInfo.TEXT)
        var nearestStopID: String?
) {
        constructor() : this("", "", "", "", "", "", "" ,"", "")
}