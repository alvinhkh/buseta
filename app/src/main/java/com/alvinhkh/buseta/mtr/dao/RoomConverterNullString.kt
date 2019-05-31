package com.alvinhkh.buseta.mtr.dao

import androidx.room.TypeConverter

object RoomConverterNullString {
    @TypeConverter
    @JvmStatic
    fun fromNullToString(value: String?): String {
        return value ?: ""
    }
}