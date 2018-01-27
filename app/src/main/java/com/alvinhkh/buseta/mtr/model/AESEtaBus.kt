package com.alvinhkh.buseta.mtr.model

import android.content.Context
import android.location.Location
import android.text.TextUtils
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.model.ArrivalTime
import com.alvinhkh.buseta.model.BusRoute
import com.alvinhkh.buseta.model.BusRouteStop
import com.alvinhkh.buseta.utils.ArrivalTimeUtil
import com.google.gson.annotations.SerializedName
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AESEtaBus(
        @SerializedName("arrivalTimeInSecond")
        var arrivalTimeInSecond: String? = null,
        @SerializedName("arrivalTimeText")
        var arrivalTimeText: String? = null,
        @SerializedName("busId")
        var busId: String? = null,
        @SerializedName("busLocation")
        var busLocation: AESEtaBusLocation? = null,
        @SerializedName("busRemark")
        var busRemark: String? = null,
        @SerializedName("isDelayed")
        var isDelayed: String? = null,
        @SerializedName("isScheduled")
        var isScheduled: String? = null
) {
    companion object {

        fun estimate(context: Context,
                     arrivalTime: ArrivalTime) : ArrivalTime {
            val etaDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            val generatedDate = Date(arrivalTime.generatedAt)
            // given timeText
            if (!TextUtils.isEmpty(arrivalTime.text) && arrivalTime.text.matches(".*\\d.*".toRegex()) && !arrivalTime.text.contains("unexpected")) {
                // if text has digit
                var estimateMinutes = ""
                val differences = Date().time - generatedDate.time // get device timeText and compare to server timeText
                try
                {
                    var etaCompareDate = generatedDate
                    // first assume eta timeText and server timeText is on the same date
                    var etaDate = etaDateFormat.parse(
                            SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                    SimpleDateFormat("MM", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                    SimpleDateFormat("dd", Locale.ENGLISH).format(etaCompareDate) + " " + arrivalTime.text)
                    // if not minutes will get negative integer
                    var minutes = ((etaDate.time / 60000) - ((generatedDate.time + differences) / 60000)).toInt()
                    if (minutes < -12 * 60)
                    {
                        // plus one day to get correct eta date
                        etaCompareDate = Date(generatedDate.time + 24 * 60 * 60 * 1000)
                        etaDate = etaDateFormat.parse(
                                (SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                        SimpleDateFormat("MM", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                        SimpleDateFormat("dd", Locale.ENGLISH).format(etaCompareDate) + " " + arrivalTime.text))
                        minutes = ((etaDate.time / 60000) - ((generatedDate.time + differences) / 60000)).toInt()
                    }
                    if (minutes >= 0 && minutes < 24 * 60)
                    {
                        // minutes should be 0 to within a day
                        estimateMinutes = (minutes).toString()
                    }
                    arrivalTime.expired = minutes <= -3  // time past
                    arrivalTime.expired = arrivalTime.expired or (TimeUnit.MILLISECONDS.toMinutes(Date().time - arrivalTime.updatedAt) >= 5) // maybe outdated
                    arrivalTime.expired = arrivalTime.expired or (TimeUnit.MILLISECONDS.toMinutes(Date().time - generatedDate.time) >= 5)  // maybe outdated
                }
                catch (ep: ParseException) {
                    Timber.d(ep)
                }
                catch (ep:ArrayIndexOutOfBoundsException) {
                    Timber.d(ep)
                }

                if (!TextUtils.isEmpty(estimateMinutes))
                {
                    if (estimateMinutes == "0")
                    {
                        arrivalTime.estimate = ""
                    }
                    else
                    {
                        arrivalTime.estimate = context.getString(R.string.minutes, estimateMinutes)
                    }
                }
            }
            return arrivalTime
        }
        
        fun toArrivalTime(context: Context,
                          aesEtaBus: AESEtaBus,
                          statusTime: Date,
                          busRouteStop: BusRouteStop?): ArrivalTime {
            val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            var arrivalTime = ArrivalTimeUtil.emptyInstance(context)
            arrivalTime.companyCode = BusRoute.COMPANY_AESBUS
            arrivalTime.estimate = aesEtaBus.arrivalTimeText
            val calendar = Calendar.getInstance()
            calendar.time = statusTime
            calendar.add(Calendar.SECOND, aesEtaBus.arrivalTimeInSecond.orEmpty().toInt())
            arrivalTime.text = sdf.format(calendar.time)
            if (!aesEtaBus.busRemark.isNullOrEmpty()) {
                arrivalTime.text += " " + aesEtaBus.busRemark
            }
            arrivalTime.plate = aesEtaBus.busId
            arrivalTime.isSchedule = aesEtaBus.isScheduled == "1"
            arrivalTime.latitude = 0.0
            arrivalTime.longitude = 0.0
            arrivalTime.distanceKM = -1.0f
            if (!arrivalTime.isSchedule) {
                arrivalTime.latitude = aesEtaBus.busLocation?.latitude
                arrivalTime.longitude = aesEtaBus.busLocation?.longitude
                if (!busRouteStop?.latitude.isNullOrEmpty() &&
                        !busRouteStop?.longitude.isNullOrEmpty() &&
                        arrivalTime.latitude.isFinite() &&
                        arrivalTime.longitude.isFinite()) {
                    val stopLocation = Location("")
                    stopLocation.latitude = busRouteStop?.latitude!!.toDouble()
                    stopLocation.longitude = busRouteStop.longitude!!.toDouble()
                    val busLocation = Location("")
                    busLocation.latitude = arrivalTime.latitude
                    busLocation.longitude = arrivalTime.longitude
                    if (busLocation.distanceTo(stopLocation).toDouble() != 0.0) {
                        arrivalTime.distanceKM = busLocation.distanceTo(stopLocation) / 1000.0f
                    } else {
                        arrivalTime.distanceKM = 0.0f
                    }
                }
            }
            arrivalTime.updatedAt = System.currentTimeMillis()
            arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime)
            return arrivalTime
        }

    }
}