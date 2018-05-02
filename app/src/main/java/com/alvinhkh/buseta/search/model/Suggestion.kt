package com.alvinhkh.buseta.search.model;

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.alvinhkh.buseta.search.model.Suggestion.Companion.COLUMN_COMPANY
import com.alvinhkh.buseta.search.model.Suggestion.Companion.COLUMN_TEXT
import com.alvinhkh.buseta.search.model.Suggestion.Companion.COLUMN_TYPE
import com.alvinhkh.buseta.search.model.Suggestion.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME, indices = [(Index(value = arrayOf(COLUMN_COMPANY, COLUMN_TEXT, COLUMN_TYPE), unique = true))])
data class Suggestion(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
    var id: Long,
    @ColumnInfo(name = COLUMN_COMPANY, typeAffinity = ColumnInfo.TEXT)
    var companyCode: String,
    @ColumnInfo(name = COLUMN_TEXT, typeAffinity = ColumnInfo.TEXT)
    var route: String,
    @ColumnInfo(name = COLUMN_TIMESTAMP, typeAffinity = ColumnInfo.INTEGER)
    var timestamp: Long,
    @ColumnInfo(name = COLUMN_TYPE, typeAffinity = ColumnInfo.TEXT)
    var type: String
) {
    companion object {
        const val TABLE_NAME = "suggestions"
        const val COLUMN_ID = "_id"
        const val COLUMN_COMPANY = "company"
        const val COLUMN_TEXT = "text"
        const val COLUMN_TIMESTAMP = "date"
        const val COLUMN_TYPE = "type"
        const val TYPE_DEFAULT = "default"
        const val TYPE_HISTORY = "history"

        fun createInstance() : Suggestion {
            return Suggestion(0, "", "", System.currentTimeMillis() / 1000, TYPE_HISTORY)
        }
    }
}