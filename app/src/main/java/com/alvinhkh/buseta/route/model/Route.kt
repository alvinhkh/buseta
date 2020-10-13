package com.alvinhkh.buseta.route.model

import androidx.room.*
import android.content.Context
import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R

@Entity(tableName = "routes", indices = [(Index(value = arrayOf("company_code", "name", "code", "sequence", "service_type", "data_source"), unique = true))])
data class Route(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id", typeAffinity = ColumnInfo.INTEGER)
        var id: Long = 0,
        @ColumnInfo(name = "code", typeAffinity = ColumnInfo.TEXT)
        var code: String? = "",
        @ColumnInfo(name = "colour", typeAffinity = ColumnInfo.TEXT)
        var colour: String? = "",
        @ColumnInfo(name = "company_code", typeAffinity = ColumnInfo.TEXT)
        var companyCode: String? = "",
        @ColumnInfo(name = "origin", typeAffinity = ColumnInfo.TEXT)
        var origin: String? = "",
        @ColumnInfo(name = "destination", typeAffinity = ColumnInfo.TEXT)
        var destination: String? = "",
        @ColumnInfo(name = "name", typeAffinity = ColumnInfo.TEXT)
        var name: String? = "",
        @ColumnInfo(name = "sequence", typeAffinity = ColumnInfo.TEXT)
        var sequence: String? = "",
        @ColumnInfo(name = "service_type", typeAffinity = ColumnInfo.TEXT)
        var serviceType: String? = "",
        @ColumnInfo(name = "description", typeAffinity = ColumnInfo.TEXT)
        var description: String? = "",
        @ColumnInfo(name = "is_special", typeAffinity = ColumnInfo.INTEGER)
        var isSpecial: Boolean? = false,
        @ColumnInfo(name = "stops_start_sequence", typeAffinity = ColumnInfo.INTEGER)
        var stopsStartSequence: Int? = 0,
        @ColumnInfo(name = "data_source", typeAffinity = ColumnInfo.TEXT)
        var dataSource: String? = "",
        @ColumnInfo(name = "hyperlink", typeAffinity = ColumnInfo.TEXT)
        var hyperlink: String? = "",
        @ColumnInfo(name = "last_update", typeAffinity = ColumnInfo.INTEGER)
        var lastUpdate: Long? = 0,
        @Ignore
        var isActive: Boolean? = false,
        @ColumnInfo(name = "map_coordinates")
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
            parcel.readString(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readValue(Boolean::class.java.classLoader) as? Boolean)

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
        parcel.writeString(hyperlink)
        parcel.writeString(dataSource)
        parcel.writeLong(lastUpdate?:0)
        parcel.writeValue(isActive)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun companyColour(context: Context): Int {
        return if (!colour.isNullOrEmpty()) {
            Color.parseColor(colour)
        } else {
            companyColour(context, companyCode?:"", name)
                    ?:ContextCompat.getColor(context, R.color.colorPrimary)
        }
    }

    fun companyName(context: Context): String {
        return companyName(context, companyCode?:"", name)
    }

    companion object CREATOR : Parcelable.Creator<Route> {
        override fun createFromParcel(parcel: Parcel): Route {
            return Route(parcel)
        }

        override fun newArray(size: Int): Array<Route?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        fun companyName(context: Context, companyCode: String, routeNo: String?): String {
            return when (companyCode) {
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
                C.PROVIDER.LWB -> context.getString(R.string.provider_short_lwb)
                C.PROVIDER.MTR -> context.getString(R.string.provider_short_mtr)
                C.PROVIDER.NLB -> context.getString(R.string.provider_short_nlb)
                C.PROVIDER.NR -> context.getString(R.string.provider_short_residents)
                C.PROVIDER.NWFB -> context.getString(R.string.provider_short_nwfb)
                C.PROVIDER.NWST -> context.getString(R.string.provider_short_nwst)
                C.PROVIDER.GMB901 -> context.getString(R.string.provider_short_gmb)
                else -> companyCode
            }
        }

        @JvmStatic
        fun companyColour(context: Context, companyCode: String, routeNo: String?): Int? {
            return when (companyCode) {
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
                C.PROVIDER.LWB -> ContextCompat.getColor(context, R.color.provider_lwb)
                C.PROVIDER.MTR -> ContextCompat.getColor(context, R.color.provider_mtr)
                C.PROVIDER.NLB -> ContextCompat.getColor(context, R.color.provider_nlb)
                C.PROVIDER.NR -> ContextCompat.getColor(context, R.color.colorPrimary)
                C.PROVIDER.NWFB -> ContextCompat.getColor(context, R.color.provider_nwfb)
                C.PROVIDER.NWST -> ContextCompat.getColor(context, R.color.colorPrimary)
                C.PROVIDER.GMB901 -> ContextCompat.getColor(context, R.color.provider_gmb)
                else -> null
            }
        }
    }
}
