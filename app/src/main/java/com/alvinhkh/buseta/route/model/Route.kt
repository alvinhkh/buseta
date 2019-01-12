package com.alvinhkh.buseta.route.model

import android.arch.persistence.room.*
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.ContextCompat
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.route.model.Route.CREATOR.TABLE_NAME

@Entity(tableName = TABLE_NAME, indices = [(Index(value = arrayOf(Route.COLUMN_COMPANY_CODE,
        Route.COLUMN_NAME, Route.COLUMN_CODE, Route.COLUMN_SEQUENCE, Route.COLUMN_SERVICE_TYPE), unique = true))])
data class Route(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
        var id: Long = 0,
        @ColumnInfo(name = COLUMN_CODE, typeAffinity = ColumnInfo.TEXT)
        var code: String? = "",
        @ColumnInfo(name = COLUMN_COLOUR, typeAffinity = ColumnInfo.TEXT)
        var colour: String? = "",
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
            parcel.readString(),
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
            parcel.readInt(),
            parcel.readLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(code)
        parcel.writeString(colour)
        parcel.writeString(companyCode)
        parcel.writeString(origin)
        parcel.writeString(destination)
        parcel.writeString(name)
        parcel.writeString(sequence)
        parcel.writeString(serviceType)
        parcel.writeString(description)
        parcel.writeValue(isSpecial)
        parcel.writeInt(stopsStartSequence?:0)
        parcel.writeLong(lastUpdate?:0)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun companyColour(context: Context): Int {
        return Route.companyColour(context, companyCode?:"", name)
                ?:ContextCompat.getColor(context, R.color.colorPrimary)
    }

    fun companyName(context: Context): String {
        return Route.companyName(context, companyCode?:"", name)
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
        const val COLUMN_COLOUR = "colour"
        const val COLUMN_COMPANY_CODE = "company_code"
        const val COLUMN_ORIGIN = "origin"
        const val COLUMN_DESTINATION = "destination"
        const val COLUMN_NAME = "name"
        const val COLUMN_SEQUENCE = "sequence"
        const val COLUMN_SERVICE_TYPE = "service_type"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_IS_SPECIAL = "is_special"
        const val COLUMN_STOPS_START_SEQUENCE = "stops_start_sequence"
        const val COLUMN_LAST_UPDATE = "last_update"
        const val COLUMN_MAP_COORDINATES = "map_coordinates"

        @JvmStatic
        fun companyName(context: Context, companyCode: String, routeNo: String?): String {
            return when (companyCode) {
                C.PROVIDER.AESBUS -> context.getString(R.string.provider_short_aes_bus)
                C.PROVIDER.CTB -> context.getString(R.string.provider_short_ctb)
                C.PROVIDER.KMB -> {
                    val lwb = listOf("N30", "N30P", "N30S", "N31", "N42", "N42A", "N42P", "N64", "R8", "R33", "R42", "X1", "X33", "X34", "X41")
                    if (!routeNo.isNullOrEmpty() && routeNo.startsWith("NR")) {
                            context.getString(R.string.provider_short_residents)
                    } else if (!routeNo.isNullOrEmpty() && (routeNo.startsWith("A")
                                    || routeNo.startsWith("E")
                                    || routeNo.startsWith("NA")
                                    || routeNo.startsWith("S"))) {
                        context.getString(R.string.provider_short_lwb)
                    } else if (!routeNo.isNullOrEmpty() && (lwb.indexOf(routeNo) >= 0)) {
                        context.getString(R.string.provider_short_lwb)
                    } else {
                        context.getString(R.string.provider_short_kmb)
                    }
                }
                C.PROVIDER.LRTFEEDER -> context.getString(R.string.provider_short_lrtfeeder)
                C.PROVIDER.MTR -> context.getString(R.string.provider_short_mtr)
                C.PROVIDER.NLB -> context.getString(R.string.provider_short_nlb)
                C.PROVIDER.NWFB -> context.getString(R.string.provider_short_nwfb)
                C.PROVIDER.NWST -> context.getString(R.string.provider_short_nwst)
                else -> companyCode
            }
        }

        @JvmStatic
        fun companyColour(context: Context, companyCode: String, routeNo: String?): Int? {
            return when (companyCode) {
                C.PROVIDER.AESBUS -> ContextCompat.getColor(context, R.color.provider_aes_bus)
                C.PROVIDER.CTB -> ContextCompat.getColor(context, R.color.provider_ctb)
                C.PROVIDER.KMB -> {
                    val lwb = listOf("N30", "N30P", "N30S", "N31", "N42", "N42A", "N42P", "N64", "R8", "R33", "R42", "X1", "X33", "X34", "X41")
                    if (!routeNo.isNullOrEmpty() && routeNo.startsWith("NR")) {
                        ContextCompat.getColor(context, R.color.colorPrimary)
                    } else if (!routeNo.isNullOrEmpty() && (routeNo.startsWith("A")
                                    || routeNo.startsWith("E")
                                    || routeNo.startsWith("NA")
                                    || routeNo.startsWith("S"))) {
                        ContextCompat.getColor(context, R.color.provider_lwb)
                    } else if (!routeNo.isNullOrEmpty() && (lwb.indexOf(routeNo) >= 0)) {
                        ContextCompat.getColor(context, R.color.provider_lwb)
                    } else {
                        ContextCompat.getColor(context, R.color.provider_kmb)
                    }
                }
                C.PROVIDER.LRTFEEDER -> ContextCompat.getColor(context, R.color.provider_lrtfeeder)
                C.PROVIDER.MTR -> ContextCompat.getColor(context, R.color.provider_mtr)
                C.PROVIDER.NLB -> ContextCompat.getColor(context, R.color.provider_nlb)
                C.PROVIDER.NWFB -> ContextCompat.getColor(context, R.color.provider_nwfb)
                else -> null
            }
        }
    }
}
