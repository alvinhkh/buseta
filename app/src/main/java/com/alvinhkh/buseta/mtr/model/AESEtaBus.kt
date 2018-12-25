package com.alvinhkh.buseta.mtr.model

import android.content.Context
import android.location.Location
import android.text.TextUtils
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.route.model.RouteStop
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
                          routeStop: RouteStop?): ArrivalTime {
            val sdf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            var arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
            arrivalTime.estimate = aesEtaBus.arrivalTimeText.orEmpty()
            val calendar = Calendar.getInstance()
            calendar.time = statusTime
            calendar.add(Calendar.SECOND, aesEtaBus.arrivalTimeInSecond.orEmpty().toInt())
            arrivalTime.text = sdf.format(calendar.time)
            if (!aesEtaBus.busRemark.isNullOrEmpty()) {
                arrivalTime.text += " " + aesEtaBus.busRemark
            }
            arrivalTime.plate = aesEtaBus.busId.orEmpty()
            arrivalTime.isSchedule = aesEtaBus.isScheduled == "1"
            arrivalTime.latitude = 0.0
            arrivalTime.longitude = 0.0
            arrivalTime.distanceKM = -1.0
            if (!arrivalTime.isSchedule) {
                aesEtaBus.busLocation?.let {
                    arrivalTime.latitude = it.latitude
                    arrivalTime.longitude = it.longitude
                }
                if (!routeStop?.latitude.isNullOrEmpty() &&
                        !routeStop?.longitude.isNullOrEmpty() &&
                        arrivalTime.latitude != 0.0 &&
                        arrivalTime.longitude != 0.0) {
                    val stopLocation = Location("")
                    stopLocation.latitude = routeStop?.latitude!!.toDouble()
                    stopLocation.longitude = routeStop.longitude!!.toDouble()
                    val busLocation = Location("")
                    busLocation.latitude = arrivalTime.latitude
                    busLocation.longitude = arrivalTime.longitude
                    if (busLocation.distanceTo(stopLocation).toDouble() != 0.0) {
                        arrivalTime.distanceKM = busLocation.distanceTo(stopLocation) / 1000.0
                    } else {
                        arrivalTime.distanceKM = 0.0
                    }
                }
            }
            arrivalTime.updatedAt = System.currentTimeMillis()
            arrivalTime = ArrivalTime.estimate(context, arrivalTime)
            return arrivalTime
        }

    }
}