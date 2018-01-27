package com.alvinhkh.buseta.mtr.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "District")
data class AESBusDistrict(
        @PrimaryKey
        @ColumnInfo(name = "DistrictID", typeAffinity = ColumnInfo.TEXT)
        var districtID: String = "",
        @ColumnInfo(name = "DistrictCN", typeAffinity = ColumnInfo.TEXT)
        var districtCn: String?,
        @ColumnInfo(name = "DistrictEN", typeAffinity = ColumnInfo.TEXT)
        var districtEn: String?
) {
        constructor() : this("", "", "")
}