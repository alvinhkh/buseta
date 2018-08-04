package com.alvinhkh.buseta.mtr.dao

import android.arch.persistence.room.TypeConverter

object RoomConverterNullString {
    @TypeConverter
    @JvmStatic
    fun fromNullToString(value: String?): String {
        return value ?: ""
    }
}