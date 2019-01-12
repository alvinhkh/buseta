package com.alvinhkh.buseta.route.model

import android.arch.persistence.room.*
import android.os.Parcel
import android.os.Parcelable
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.model.Follow
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.TABLE_NAME
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_COMPANY_CODE
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_ROUTE_ID
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_ROUTE_NO
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_ROUTE_SEQUENCE
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_ROUTE_SERVICE_TYPE
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_SEQUENCE
import com.alvinhkh.buseta.route.model.RouteStop.CREATOR.COLUMN_STOP_ID

@Entity(tableName = TABLE_NAME, indices = [(Index(value = arrayOf(COLUMN_COMPANY_CODE, COLUMN_ROUTE_NO,
        COLUMN_ROUTE_ID, COLUMN_ROUTE_SEQUENCE, COLUMN_ROUTE_SERVICE_TYPE, COLUMN_STOP_ID, COLUMN_SEQUENCE), unique = true))])
data class RouteStop(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
        var id: Long = 0,
        @ColumnInfo(name = COLUMN_COMPANY_CODE, typeAffinity = ColumnInfo.TEXT)
        var companyCode: String? = null,
        @ColumnInfo(name = COLUMN_DESCRIPTION, typeAffinity = ColumnInfo.TEXT)
        var description: String? = null,
        @ColumnInfo(name = COLUMN_ETA_GET, typeAffinity = ColumnInfo.TEXT)
        var etaGet: String? = null,
        @ColumnInfo(name = COLUMN_FARE_CHILD, typeAffinity = ColumnInfo.TEXT)
        var fareChild: String? = null,
        @ColumnInfo(name = COLUMN_FARE_FULL, typeAffinity = ColumnInfo.TEXT)
        var fareFull: String? = null,
        @ColumnInfo(name = COLUMN_FARE_HOLIDAY, typeAffinity = ColumnInfo.TEXT)
        var fareHoliday: String? = null,
        @ColumnInfo(name = COLUMN_FARE_SENIOR, typeAffinity = ColumnInfo.TEXT)
        var fareSenior: String? = null,
        @ColumnInfo(name = COLUMN_IMAGE_URL, typeAffinity = ColumnInfo.TEXT)
        var imageUrl: String? = null,
        @ColumnInfo(name = COLUMN_LAST_UPDATE, typeAffinity = ColumnInfo.INTEGER)
        var lastUpdate: Long? = 0,
        @ColumnInfo(name = COLUMN_LATITUDE, typeAffinity = ColumnInfo.TEXT)
        var latitude: String? = null,
        @ColumnInfo(name = COLUMN_LOCATION, typeAffinity = ColumnInfo.TEXT)
        var location: String? = null,
        @ColumnInfo(name = COLUMN_LONGITUDE, typeAffinity = ColumnInfo.TEXT)
        var longitude: String? = null,
        @ColumnInfo(name = COLUMN_NAME, typeAffinity = ColumnInfo.TEXT)
        var name: String? = null,
        @ColumnInfo(name = COLUMN_ROUTE_DESTINATION, typeAffinity = ColumnInfo.TEXT)
        var routeDestination: String? = null,
        @ColumnInfo(name = COLUMN_ROUTE_ID, typeAffinity = ColumnInfo.TEXT)
        var routeId: String? = null,
        @ColumnInfo(name = COLUMN_ROUTE_NO, typeAffinity = ColumnInfo.TEXT)
        var routeNo: String? = null,
        @ColumnInfo(name = COLUMN_ROUTE_ORIGIN, typeAffinity = ColumnInfo.TEXT)
        var routeOrigin: String? = null,
        @ColumnInfo(name = COLUMN_ROUTE_SEQUENCE, typeAffinity = ColumnInfo.TEXT)
        var routeSequence: String? = null,
        @ColumnInfo(name = COLUMN_ROUTE_SERVICE_TYPE, typeAffinity = ColumnInfo.TEXT)
        var routeServiceType: String? = null,
        @ColumnInfo(name = COLUMN_SEQUENCE, typeAffinity = ColumnInfo.TEXT)
        var sequence: String? = null,
        @ColumnInfo(name = COLUMN_STOP_ID, typeAffinity = ColumnInfo.TEXT)
        var stopId: String? = null,
        @Ignore
        var etas: List<ArrivalTime> = listOf()
) : Parcelable {

    constructor() : this(0, "", "", "", "",
            "", "", "", "", 0,
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", listOf<ArrivalTime>())

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
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(companyCode)
        parcel.writeString(description)
        parcel.writeString(etaGet)
        parcel.writeString(fareChild)
        parcel.writeString(fareFull)
        parcel.writeString(fareHoliday)
        parcel.writeString(fareSenior)
        parcel.writeString(imageUrl)
        parcel.writeLong(lastUpdate?:0)
        parcel.writeString(latitude)
        parcel.writeString(location)
        parcel.writeString(longitude)
        parcel.writeString(name)
        parcel.writeString(routeDestination)
        parcel.writeString(routeId)
        parcel.writeString(routeNo)
        parcel.writeString(routeOrigin)
        parcel.writeString(routeSequence)
        parcel.writeString(routeServiceType)
        parcel.writeString(sequence)
        parcel.writeString(stopId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RouteStop> {
        override fun createFromParcel(parcel: Parcel): RouteStop {
            return RouteStop(parcel)
        }

        override fun newArray(size: Int): Array<RouteStop?> {
            return arrayOfNulls(size)
        }

        const val TABLE_NAME = "route_stops"
        const val COLUMN_ID = "_id"
        const val COLUMN_COMPANY_CODE = "company_code"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_ETA_GET = "eta_get"
        const val COLUMN_FARE_FULL = "fare_full"
        const val COLUMN_FARE_HOLIDAY = "fare_holiday"
        const val COLUMN_FARE_CHILD = "fare_child"
        const val COLUMN_FARE_SENIOR = "fare_senior"
        const val COLUMN_IMAGE_URL = "image_url"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_NAME = "name"
        const val COLUMN_ROUTE_DESTINATION = "route_destination"
        const val COLUMN_ROUTE_ID = "route_id"
        const val COLUMN_ROUTE_NO = "route_no"
        const val COLUMN_ROUTE_ORIGIN = "route_origin"
        const val COLUMN_ROUTE_SEQUENCE = "route_sequence"
        const val COLUMN_ROUTE_SERVICE_TYPE = "route_service_type"
        const val COLUMN_SEQUENCE = "sequence"
        const val COLUMN_STOP_ID = "stop_id"
        const val COLUMN_LAST_UPDATE = "last_update"
    }
}
