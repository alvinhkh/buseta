package com.alvinhkh.buseta.appwidget

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

class WidgetPreferences(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("widget_preferences", Context.MODE_PRIVATE)

    //using commit instead of apply as value is needed straight away
    @SuppressLint("ApplySharedPref")
    fun setWidgetValues(widgetId: Int, withHeader: Boolean, followGroupId: String, noOfRows: Int) {
        val editor = preferences.edit()
        editor.putBoolean("widget_header_$widgetId", withHeader)
        editor.putString("widget_follow_group_id_$widgetId", followGroupId)
        editor.putInt("widget_item_rows_$widgetId", noOfRows)
        editor.commit()
    }

    fun getWithHeader(widgetId: Int): Boolean {
        return preferences.getBoolean("widget_header_$widgetId", true)
    }

    fun getFollowGroupId(widgetId: Int): String? {
        return preferences.getString("widget_follow_group_id_$widgetId", null)
    }

    fun getItemRowNum(widgetId: Int): Int {
        return preferences.getInt("widget_item_rows_$widgetId", 3)
    }

    fun removeWidget(widgetId: Int) {
        val editor = preferences.edit()
        editor.remove("widget_header_$widgetId")
        editor.remove("widget_follow_group_id_$widgetId")
        editor.remove("widget_item_rows_$widgetId")
        editor.apply()
    }
}