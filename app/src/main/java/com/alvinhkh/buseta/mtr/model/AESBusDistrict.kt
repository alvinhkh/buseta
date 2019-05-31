package com.alvinhkh.buseta.mtr.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "District")
data class AESBusDistrict(
        @PrimaryKey
        @ColumnInfo(name = "DistrictID", typeAffinity = ColumnInfo.TEXT)
        var districtID: String,
        @ColumnInfo(name = "DistrictCN", typeAffinity = ColumnInfo.TEXT)
        var districtCn: String,
        @ColumnInfo(name = "DistrictEN", typeAffinity = ColumnInfo.TEXT)
        var districtEn: String
)