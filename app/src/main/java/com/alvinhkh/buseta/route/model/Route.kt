package com.alvinhkh.buseta.route.model

import android.arch.persistence.room.*
import android.os.Parcel
import android.os.Parcelable
import com.alvinhkh.buseta.route.model.Route.CREATOR.TABLE_NAME

@Entity(tableName = TABLE_NAME, indices = [(Index(value = arrayOf(Route.COLUMN_COMPANY_CODE,
        Route.COLUMN_NAME, Route.COLUMN_CODE, Route.COLUMN_SEQUENCE, Route.COLUMN_SERVICE_TYPE), unique = true))])
data class Route(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
        var id: Long = 0,
        @ColumnInfo(name = COLUMN_CODE, typeAffinity = ColumnInfo.TEXT)
        var code: String? = "",
        @ColumnInfo(name = COLUMN_COMPANY_CODE, typeAffinity = ColumnInfo.TEXT)
        var companyCode: String? = "",
        @ColumnInfo(name = COLUMN_ORIGIN, typeAffinity = ColumnInfo.TEXT)
        var origin: String? = "",
        @ColumnInfo(name = COLUMN_DESTINATION, typeAffinity = ColumnInfo.TEXT)
        var destination: String? = "",
        @ColumnInfo(name = COLUMN_NAME, typeAffinity = ColumnInfo.TEXT)
        var name: String? = "",
        @ColumnInfo(name = COLUMN_SEQUENCE, typeAffinity = ColumnInfo.TEXT)
        var sequence: String? = "",
        @ColumnInfo(name = COLUMN_SERVICE_TYPE, typeAffinity = ColumnInfo.TEXT)
        var serviceType: String? = "",
        @ColumnInfo(name = COLUMN_DESCRIPTION, typeAffinity = ColumnInfo.TEXT)
        var description: String? = "",
        @ColumnInfo(name = COLUMN_IS_SPECIAL, typeAffinity = ColumnInfo.INTEGER)
        var isSpecial: Boolean? = false,
        @ColumnInfo(name = COLUMN_STOPS_START_SEQUENCE, typeAffinity = ColumnInfo.INTEGER)
        var stopsStartSequence: Int? = 0,
        @ColumnInfo(name = COLUMN_INFO_KEY, typeAffinity = ColumnInfo.TEXT)
        var infoKey: String? = "",
        @ColumnInfo(name = COLUMN_RDV, typeAffinity = ColumnInfo.TEXT)
        var rdv: String? = "",
        @ColumnInfo(name = COLUMN_LAST_UPDATE, typeAffinity = ColumnInfo.INTEGER)
        var lastUpdate: Long? = 0,
        @ColumnInfo(name = COLUMN_MAP_COORDINATES)
        var mapCoordinates: MutableList<LatLong> = arrayListOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(code)
        parcel.writeString(companyCode)
        parcel.writeString(origin)
        parcel.writeString(destination)
        parcel.writeString(name)
        parcel.writeString(sequence)
        parcel.writeString(serviceType)
        parcel.writeString(description)
        parcel.writeValue(isSpecial)
        parcel.writeInt(stopsStartSequence?:0)
        parcel.writeString(infoKey)
        parcel.writeString(rdv)
        parcel.writeLong(lastUpdate?:0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Route> {
        override fun createFromParcel(parcel: Parcel): Route {
            return Route(parcel)
        }

        override fun newArray(size: Int): Array<Route?> {
            return arrayOfNulls(size)
        }

        const val TABLE_NAME = "routes"
        const val COLUMN_ID = "_id"
        const val COLUMN_CODE = "code"
        const val COLUMN_COMPANY_CODE = "company_code"
        const val COLUMN_ORIGIN = "origin"
        const val COLUMN_DESTINATION = "destination"
        const val COLUMN_NAME = "name"
        const val COLUMN_SEQUENCE = "sequence"
        const val COLUMN_SERVICE_TYPE = "service_type"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_IS_SPECIAL = "is_special"
        const val COLUMN_STOPS_START_SEQUENCE = "stops_start_sequence"
        const val COLUMN_INFO_KEY = "info_key"
        const val COLUMN_RDV = "rdv"
        const val COLUMN_LAST_UPDATE = "last_update"
        const val COLUMN_MAP_COORDINATES = "map_coordinates"

    }
}
