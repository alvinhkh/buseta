package com.alvinhkh.buseta.mtr.model

import android.content.Context
import android.text.TextUtils
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.google.gson.annotations.SerializedName
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class MtrSchedule(
        @field:SerializedName("ttnt")
        var ttnt: String? = null,
        @field:SerializedName("valid")
        var valid: String? = null,
        @field:SerializedName("plat")
        var platform: String? = null,
        @field:SerializedName("time")
        var time: String? = null,
        @field:SerializedName("source")
        var source: String? = null,
        @field:SerializedName("dest")
        var destination: String? = null,
        @field:SerializedName("seq")
        var sequence: String? = null
) {
    companion object {

        fun estimate(context: Context,
                     arrivalTime: ArrivalTime) : ArrivalTime {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

            val generatedDate = Date(arrivalTime.generatedAt)
            // given timeText
            if (!TextUtils.isEmpty(arrivalTime.text) &&
                    arrivalTime.text.matches(".*\\d.*".toRegex()) &&
                    !arrivalTime.text.contains("unexpected")) {
                // if text has digit
                var estimateMinutes = ""
                val differences = Date().time - generatedDate.time // compare device and server time
                try
                {
                    val etaDate = dateFormat.parse(arrivalTime.text)
                    var minutes = ((etaDate.time / 60000) - ((generatedDate.time + differences) / 60000)).toInt()
                    if (minutes < -12 * 60)
                    {
                        // plus one day to get correct eta date
                        // note: because time value from server does not show correct day
                        val calendar: Calendar = Calendar.getInstance()
                        calendar.time = etaDate
                        calendar.add(Calendar.HOUR, 24)
                        minutes = ((calendar.time.time / 60000) - ((generatedDate.time + differences) / 60000)).toInt()
                    }
                    if (minutes >= 0 && minutes < 24 * 60)
                    {
                        // minutes should be 0 to within a day
                        estimateMinutes = (minutes).toString()
                    }
                    arrivalTime.expired = minutes <= -2  // time past
                    arrivalTime.expired = arrivalTime.expired or (TimeUnit.MILLISECONDS.toMinutes(Date().time - arrivalTime.updatedAt) >= 2) // maybe outdated
                    arrivalTime.expired = arrivalTime.expired or (TimeUnit.MILLISECONDS.toMinutes(Date().time - generatedDate.time) >= 2)  // maybe outdated
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
                          direction: String,
                          schedule: MtrSchedule, serverTime: String?,
                          codeMap: Map<String, String>?): ArrivalTime {
            var arrivalTime = ArrivalTime.emptyInstance(context, routeStop = null)
            arrivalTime.companyCode = C.PROVIDER.MTR
            arrivalTime.text = schedule.time!!
            arrivalTime.destination = codeMap?.get(schedule.destination.orEmpty())!!
            arrivalTime.platform = schedule.platform!!
            arrivalTime.estimate = schedule.ttnt!!
            arrivalTime.direction = direction
            if (!serverTime.isNullOrEmpty()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                try {
                    arrivalTime.generatedAt = dateFormat.parse(serverTime).time
                } catch (ignored: ParseException) { }
            }
            arrivalTime.updatedAt = System.currentTimeMillis()
            arrivalTime = ArrivalTime.estimate(context, arrivalTime)
            return arrivalTime
        }

    }
}