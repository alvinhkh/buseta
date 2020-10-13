package com.alvinhkh.buseta.arrivaltime.model

import androidx.lifecycle.LiveData
import androidx.room.*
import android.content.Context
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime.Companion.COLUMN_COMPANY
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime.Companion.COLUMN_ETA_ID
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime.Companion.COLUMN_ROUTE_NO
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime.Companion.COLUMN_ROUTE_SEQ
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime.Companion.COLUMN_STOP_ID
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime.Companion.COLUMN_STOP_SEQ
import com.alvinhkh.buseta.kmb.util.KmbEtaUtil
import com.alvinhkh.buseta.route.model.RouteStop
import com.alvinhkh.buseta.mtr.model.AESEtaBus
import com.alvinhkh.buseta.mtr.model.MtrSchedule
import com.alvinhkh.buseta.nlb.util.NlbEtaUtil
import com.alvinhkh.buseta.nwst.util.NwstEtaUtil
import java.text.SimpleDateFormat
import java.util.*


@Entity(tableName = ArrivalTime.TABLE_NAME, indices = [(Index(value = arrayOf(COLUMN_COMPANY, COLUMN_ROUTE_NO, COLUMN_ROUTE_SEQ, COLUMN_STOP_ID, COLUMN_STOP_SEQ, COLUMN_ETA_ID), unique = true))])
data class ArrivalTime (
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = ArrivalTime.COLUMN_ID, typeAffinity = ColumnInfo.INTEGER)
        var _id: Long,
        @ColumnInfo(name = ArrivalTime.COLUMN_COMPANY, typeAffinity = ColumnInfo.TEXT)
        var companyCode: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_ROUTE_NO, typeAffinity = ColumnInfo.TEXT)
        var routeNo: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_ROUTE_SEQ, typeAffinity = ColumnInfo.TEXT)
        var routeSeq: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_STOP_ID, typeAffinity = ColumnInfo.TEXT)
        var stopId: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_STOP_SEQ, typeAffinity = ColumnInfo.TEXT)
        var stopSeq: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_ETA_ID, typeAffinity = ColumnInfo.TEXT)
        var order: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_GENERATED_AT, typeAffinity = ColumnInfo.INTEGER)
        var generatedAt: Long,
        @ColumnInfo(name = ArrivalTime.COLUMN_UPDATED_AT, typeAffinity = ColumnInfo.INTEGER)
        var updatedAt: Long,
        @ColumnInfo(name = ArrivalTime.COLUMN_TEXT, typeAffinity = ColumnInfo.TEXT)
        var text: String,
        @Ignore
        var estimate: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_ETA_EXPIRE, typeAffinity = ColumnInfo.TEXT)
        var expire: String,
        @Ignore
        var expired: Boolean,
        @ColumnInfo(name = ArrivalTime.COLUMN_DESTINATION, typeAffinity = ColumnInfo.TEXT)
        var destination: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_DIRECTION, typeAffinity = ColumnInfo.TEXT)
        var direction: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_IS_SCHEDULED)
        var isSchedule: Boolean,
        @ColumnInfo(name = ArrivalTime.COLUMN_HAS_WHEELCHAIR)
        var hasWheelchair: Boolean,
        @ColumnInfo(name = ArrivalTime.COLUMN_HAS_WIFI)
        var hasWifi: Boolean,
        @ColumnInfo(name = ArrivalTime.COLUMN_ISO_TIME, typeAffinity = ColumnInfo.TEXT)
        var isoTime: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_CAPACITY, typeAffinity = ColumnInfo.INTEGER)
        var capacity: Long,
        @ColumnInfo(name = ArrivalTime.COLUMN_DISTANCE, typeAffinity = ColumnInfo.REAL)
        var distanceKM: Double,
        @ColumnInfo(name = ArrivalTime.COLUMN_PLATE, typeAffinity = ColumnInfo.TEXT)
        var plate: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_LATITUDE, typeAffinity = ColumnInfo.REAL)
        var latitude: Double,
        @ColumnInfo(name = ArrivalTime.COLUMN_LONGITUDE, typeAffinity = ColumnInfo.REAL)
        var longitude: Double,
        @ColumnInfo(name = ArrivalTime.COLUMN_PLATFORM, typeAffinity = ColumnInfo.TEXT)
        var platform: String,
        @ColumnInfo(name = ArrivalTime.COLUMN_NOTE, typeAffinity = ColumnInfo.TEXT)
        var note: String
) {
    constructor() : this(0, "", "", "", "", "", "", 0, 0, "", "", "", false, "", "", false, false, false, "", 0, -1.0, "", 0.0, 0.0, "", "")

    companion object {
        const val TABLE_NAME = "eta"
        const val COLUMN_ID = "_id"
        const val COLUMN_CAPACITY = "capacity"
        const val COLUMN_COMPANY = "company"
        const val COLUMN_ROUTE_NO = "route_no"
        const val COLUMN_ROUTE_SEQ = "route_seq"
        const val COLUMN_STOP_SEQ = "stop_seq"
        const val COLUMN_STOP_ID = "stop_id"
        const val COLUMN_ETA_EXPIRE = "eta_expire"
        const val COLUMN_ETA_ID = "eta_id"
        const val COLUMN_IS_SCHEDULED = "is_scheduled"
        const val COLUMN_HAS_WHEELCHAIR = "has_wheelchair"
        const val COLUMN_HAS_WIFI = "has_wifi"
        const val COLUMN_TEXT = "text"
        const val COLUMN_ISO_TIME = "iso_time"
        const val COLUMN_DISTANCE = "distance"
        const val COLUMN_PLATE = "plate"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_PLATFORM = "platform"
        const val COLUMN_DESTINATION = "destination"
        const val COLUMN_DIRECTION = "direction"
        const val COLUMN_GENERATED_AT = "generated_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val COLUMN_NOTE = "note"

        fun emptyInstance(context: Context, routeStop: RouteStop?): ArrivalTime {
            val arrivalTime = ArrivalTime()
            arrivalTime.order = "0"
            arrivalTime.text = context.getString(R.string.message_no_data)
            arrivalTime.capacity = -1
            arrivalTime.updatedAt = System.currentTimeMillis()
            arrivalTime.generatedAt = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 1)
            arrivalTime.expire = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(calendar.time)
            arrivalTime.companyCode = routeStop?.companyCode?:""
            arrivalTime.routeNo = routeStop?.routeNo?:""
            arrivalTime.routeSeq = routeStop?.routeSequence?:""
            arrivalTime.stopId = routeStop?.stopId?:""
            arrivalTime.stopSeq = routeStop?.sequence?:""
            return arrivalTime
        }

        fun estimate(context: Context, arrivalTime: ArrivalTime): ArrivalTime {
            if (!arrivalTime.companyCode.isEmpty()) {
                when (arrivalTime.companyCode) {
                    C.PROVIDER.LRTFEEDER -> return AESEtaBus.estimate(context, arrivalTime)
                    C.PROVIDER.KMB, C.PROVIDER.LWB -> return KmbEtaUtil.estimate(context, arrivalTime)
                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> return NwstEtaUtil.estimate(context, arrivalTime)
                    C.PROVIDER.NLB, C.PROVIDER.GMB901 -> return NlbEtaUtil.estimate(context, arrivalTime)
                    C.PROVIDER.MTR -> return MtrSchedule.estimate(context, arrivalTime)
                }
            }
            return arrivalTime
        }

        @JvmStatic
        fun getList(database: ArrivalTimeDatabase, stop: RouteStop): List<ArrivalTime> {
            return database.arrivalTimeDao().getList(stop.companyCode?:"",
                    stop.routeNo?:"",
                    stop.routeSequence?:"",
                    stop.stopId?:"",
                    stop.sequence?:"",
                    System.currentTimeMillis() - 600000,
                    System.currentTimeMillis().toString())
        }

        fun liveData(database: ArrivalTimeDatabase, stop: RouteStop): LiveData<MutableList<ArrivalTime>> {
            return database.arrivalTimeDao().getLiveData(stop.companyCode?:"",
                    stop.routeNo?:"",
                    stop.routeSequence?:"",
                    stop.stopId?:"",
                    stop.sequence?:"")
        }
    }
}
