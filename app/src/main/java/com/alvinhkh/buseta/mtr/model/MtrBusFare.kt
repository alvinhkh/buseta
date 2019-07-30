package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "busFare")
data class MtrBusFare(
        @PrimaryKey
        @ColumnInfo(name = "fare_ID")
        var fareId: Int,
        @ColumnInfo(name = "route_ID")
        var routeId: Int,
        @ColumnInfo(name = "cash_adult")
        var cashAdult: Float,
        @ColumnInfo(name = "octopus_adult")
        var octopusAdult: Float,
        @ColumnInfo(name = "cash_child_senior")
        var cashChildSenior: Float,
        @ColumnInfo(name = "octopus_child_senior")
        var octopusChildSenior: Float,
        @ColumnInfo(name = "cash_disabilities")
        var cashDisabilities: Float,
        @ColumnInfo(name = "octopus_disabilities")
        var octopusDisabilities: Float,
        @ColumnInfo(name = "cash_student")
        var cashStudent: Float,
        @ColumnInfo(name = "octopus_student")
        var octopusStudent: Float,
        @ColumnInfo(name = "cash_child", typeAffinity = ColumnInfo.TEXT)
        var cashChild: String? = null,
        @ColumnInfo(name = "octopus_child", typeAffinity = ColumnInfo.TEXT)
        var octopusChild: String? = null,
        @ColumnInfo(name = "cash_senior", typeAffinity = ColumnInfo.TEXT)
        var cashSenior: String? = null,
        @ColumnInfo(name = "octopus_senior", typeAffinity = ColumnInfo.TEXT)
        var octopusSenior: String? = null
)