package com.alvinhkh.buseta.route.dao

import android.arch.persistence.room.TypeConverter
import com.alvinhkh.buseta.route.model.LatLong
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LatLngListConverter {

    @TypeConverter
    fun listToJson(value: MutableList<LatLong>?): String {
        val listType = object : TypeToken<List<LatLong>>() {}.type
        return Gson().toJson(value, listType)
    }

    @TypeConverter
    fun jsonToList(value: String): MutableList<LatLong>? {
        val objects = Gson().fromJson(value, Array<LatLong>::class.java) as Array<LatLong>
        val list = objects.toMutableList()
        return list
    }
}