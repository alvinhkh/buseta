package com.alvinhkh.buseta.follow.model

import androidx.room.*
import android.os.Parcel
import android.os.Parcelable
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_GROUP_ID
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_COMPANY_CODE
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_ROUTE_NO
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_ROUTE_SEQ
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_ROUTE_SERVICE_TYPE
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_STOP_ID
import com.alvinhkh.buseta.follow.model.Follow.CREATOR.COLUMN_STOP_SEQ
import com.alvinhkh.buseta.route.model.Route
import com.alvinhkh.buseta.route.model.RouteStop

@Entity(tableName = Follow.TABLE_NAME, indices = [(Index(value = arrayOf(COLUMN_GROUP_ID, COLUMN_COMPANY_CODE,
        COLUMN_ROUTE_NO, COLUMN_ROUTE_SEQ, COLUMN_ROUTE_SERVICE_TYPE, COLUMN_STOP_ID, COLUMN_STOP_SEQ), unique = true))])
data class Follow(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
        var _id: Long,
        @ColumnInfo(name = COLUMN_GROUP_ID, typeAffinity = ColumnInfo.TEXT)
        var groupId: String,
        @ColumnInfo(name = COLUMN_TYPE, typeAffinity = ColumnInfo.TEXT)
        var type: String,
        @ColumnInfo(name = COLUMN_COMPANY_CODE, typeAffinity = ColumnInfo.TEXT)
        var companyCode: String,
        @ColumnInfo(name = COLUMN_ROUTE_NO, typeAffinity = ColumnInfo.TEXT)
        var routeNo: String,
        @ColumnInfo(name = COLUMN_ROUTE_ID, typeAffinity = ColumnInfo.TEXT)
        var routeId: String,
        @ColumnInfo(name = COLUMN_ROUTE_SEQ, typeAffinity = ColumnInfo.TEXT)
        var routeSeq: String,
        @ColumnInfo(name = COLUMN_ROUTE_SERVICE_TYPE, typeAffinity = ColumnInfo.TEXT)
        var routeServiceType: String,
        @ColumnInfo(name = COLUMN_ROUTE_DESTINATION, typeAffinity = ColumnInfo.TEXT)
        var routeDestination: String,
        @ColumnInfo(name = COLUMN_ROUTE_ORIGIN, typeAffinity = ColumnInfo.TEXT)
        var routeOrigin: String,
        @Ignore
        var routeColour: String = "",
        @ColumnInfo(name = COLUMN_STOP_ID, typeAffinity = ColumnInfo.TEXT)
        var stopId: String,
        @ColumnInfo(name = COLUMN_STOP_NAME, typeAffinity = ColumnInfo.TEXT)
        var stopName: String,
        @ColumnInfo(name = COLUMN_STOP_SEQ, typeAffinity = ColumnInfo.TEXT)
        var stopSeq: String,
        @ColumnInfo(name = COLUMN_STOP_LATITUDE, typeAffinity = ColumnInfo.TEXT)
        var stopLatitude: String,
        @ColumnInfo(name = COLUMN_STOP_LONGITUDE, typeAffinity = ColumnInfo.TEXT)
        var stopLongitude: String,
        @ColumnInfo(name = COLUMN_ETA_GET, typeAffinity = ColumnInfo.TEXT)
        var etaGet: String,
        @ColumnInfo(name = COLUMN_DISPLAY_ORDER, typeAffinity = ColumnInfo.INTEGER)
        var order: Int,
        @ColumnInfo(name = COLUMN_UPDATED_AT, typeAffinity = ColumnInfo.INTEGER)
        var updatedAt: Long,
        @Ignore
        var etas: List<ArrivalTime> = listOf()
) : Parcelable {

    constructor() : this(0, "", "", "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", 0, 0, listOf<ArrivalTime>())

    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readString()?:"",
            parcel.readInt(),
            parcel.readLong(),
            listOf<ArrivalTime>())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(_id)
        parcel.writeString(groupId)
        parcel.writeString(type)
        parcel.writeString(companyCode)
        parcel.writeString(routeNo)
        parcel.writeString(routeId)
        parcel.writeString(routeSeq)
        parcel.writeString(routeServiceType)
        parcel.writeString(routeDestination)
        parcel.writeString(routeOrigin)
        parcel.writeString(routeColour)
        parcel.writeString(stopId)
        parcel.writeString(stopName)
        parcel.writeString(stopSeq)
        parcel.writeString(stopLatitude)
        parcel.writeString(stopLongitude)
        parcel.writeString(etaGet)
        parcel.writeInt(order)
        parcel.writeLong(updatedAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toRouteStop(): RouteStop {
        val routeStop = RouteStop()
        routeStop.companyCode = companyCode
        routeStop.routeNo = routeNo
        routeStop.routeId = routeId
        routeStop.routeServiceType = routeServiceType
        routeStop.routeSequence = routeSeq
        routeStop.stopId = stopId
        routeStop.sequence = stopSeq
        routeStop.routeDestination = routeDestination
        routeStop.routeOrigin = routeOrigin
        routeStop.name = stopName
        routeStop.latitude = stopLatitude
        routeStop.longitude = stopLongitude
        routeStop.etaGet = etaGet
        return routeStop
    }

    companion object CREATOR : Parcelable.Creator<Follow> {

        const val TABLE_NAME = "follow"
        const val COLUMN_ID = "_id"
        const val COLUMN_GROUP_ID = "category_id"
        const val COLUMN_COMPANY_CODE = "company"
        const val COLUMN_DISPLAY_ORDER = "display_order"
        const val COLUMN_ROUTE_NO = "no"
        const val COLUMN_ROUTE_ID = "route_id"
        const val COLUMN_ROUTE_SEQ = "bound"
        const val COLUMN_ROUTE_ORIGIN = "origin"
        const val COLUMN_ROUTE_DESTINATION = "destination"
        const val COLUMN_ROUTE_SERVICE_TYPE = "route_service_type"
        const val COLUMN_STOP_SEQ = "stop_seq"
        const val COLUMN_STOP_ID = "stop_code"
        const val COLUMN_STOP_NAME = "stop_name"
        const val COLUMN_STOP_LATITUDE = "stop_latitude"
        const val COLUMN_STOP_LONGITUDE = "stop_longitude"
        const val COLUMN_ETA_GET = "eta_get"
        const val COLUMN_TYPE = "type"
        const val COLUMN_UPDATED_AT = "date"
        const val TYPE_ROUTE_STOP = "route_stop"
        const val TYPE_RAILWAY_STOP = "railway_stop"

        fun createInstance(route: Route?, routeStop: RouteStop?): Follow {
            val follow = Follow()
            follow.groupId = FollowGroup.UNCATEGORISED
            follow.type = TYPE_ROUTE_STOP
            if (route?.companyCode == C.PROVIDER.MTR) {
                follow.type = TYPE_RAILWAY_STOP
            }
            follow.companyCode = routeStop?.companyCode?:""
            follow.routeNo = route?.name?:""
            follow.routeId = route?.code?:""
            follow.routeSeq = route?.sequence?:""
            follow.routeServiceType = route?.serviceType?:""
            follow.routeOrigin = route?.origin?:""
            follow.routeDestination = route?.destination?:""
            follow.routeColour = route?.colour?:""
            follow.stopId = routeStop?.stopId?:""
            follow.stopLatitude = routeStop?.latitude?:""
            follow.stopLongitude = routeStop?.longitude?:""
            follow.stopName = routeStop?.name?:""
            follow.stopSeq = routeStop?.sequence?:""
            follow.etaGet = routeStop?.etaGet?:""
            follow.order = 0
            follow.updatedAt = System.currentTimeMillis()
            return follow
        }

        override fun createFromParcel(parcel: Parcel): Follow {
            return Follow(parcel)
        }

        override fun newArray(size: Int): Array<Follow?> {
            return arrayOfNulls(size)
        }
    }
}
