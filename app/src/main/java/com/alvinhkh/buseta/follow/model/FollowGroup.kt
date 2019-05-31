package com.alvinhkh.buseta.follow.model

import androidx.room.*
import com.alvinhkh.buseta.follow.model.FollowGroup.Companion.COLUMN_ID

@Entity(tableName = FollowGroup.TABLE_NAME, indices = [(Index(value = arrayOf(COLUMN_ID), unique = true))])
data class FollowGroup(
        @PrimaryKey
        @ColumnInfo(name = FollowGroup.COLUMN_ID, typeAffinity = ColumnInfo.TEXT)
        var id: String = "",
        @ColumnInfo(name = FollowGroup.COLUMN_NAME, typeAffinity = ColumnInfo.TEXT)
        var name: String = "",
        @ColumnInfo(name = FollowGroup.COLUMN_COLOUR, typeAffinity = ColumnInfo.TEXT)
        var colour: String = "",
        @ColumnInfo(name = FollowGroup.COLUMN_DISPLAY_ORDER, typeAffinity = ColumnInfo.INTEGER)
        var displayOrder: Int = 0,
        @ColumnInfo(name = FollowGroup.COLUMN_UPDATED_AT, typeAffinity = ColumnInfo.INTEGER)
        var updatedAt: Long = 0
) {

    companion object {

        const val TABLE_NAME = "follow_category"
        const val COLUMN_ID = "id"
        const val COLUMN_COLOUR = "colour"
        const val COLUMN_NAME = "name"
        const val COLUMN_DISPLAY_ORDER = "display_order"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val UNCATEGORISED = "uncategorised"
    }
}
