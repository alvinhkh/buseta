package com.alvinhkh.buseta.mtr.model

import android.content.Context
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.google.gson.annotations.SerializedName
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// https://opendata.mtr.com.hk/doc/Next_Train_DataDictionary_v1.1.pdf
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

        val isoDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)

        fun estimate(context: Context,
                     arrivalTime: ArrivalTime) : ArrivalTime {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

            val generatedDate = Date(arrivalTime.generatedAt)

            // given iso time
            if (arrivalTime.isoTime.isNotBlank()) {
                val differences = Date().time - generatedDate.time // get device timeText and compare to server timeText
                try {
                    var estimateMinutes = ""
                    val etaDate = isoDf.parse(arrivalTime.isoTime)
                    val minutes = (etaDate.time / 60000 - (generatedDate.time + differences) / 60000).toInt()
                    if (minutes in 0..1439) {
                        // minutes should be 0 to within a day
                        estimateMinutes = minutes.toString()
                    }
                    if (minutes > 120) {
                        // likely calculation error
                        estimateMinutes = ""
                    }
                    if (!estimateMinutes.isEmpty()) {
                        if (estimateMinutes == "0") {
                            arrivalTime.estimate = context.getString(R.string.now)
                        } else {
                            arrivalTime.estimate = context.getString(R.string.minutes, estimateMinutes)
                        }
                    }
                    var expired = minutes <= -3  // time past
                    expired = expired or (TimeUnit.MILLISECONDS.toMinutes(Date().time - arrivalTime.updatedAt) >= 2) // maybe outdated
                    arrivalTime.expired = expired
                    return arrivalTime
                } catch (e: Exception) {
                    Timber.d(e)
                }
            }

            // given timeText
            if (!arrivalTime.text.isEmpty() &&
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

                if (!estimateMinutes.isEmpty())
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
                          schedule: MtrSchedule, serverTime: String?,
                          codeMap: Map<String, String>?): ArrivalTime {
            var arrivalTime = ArrivalTime.emptyInstance(context, routeStop = null)
            arrivalTime.companyCode = C.PROVIDER.MTR
            arrivalTime.text = ""
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")
            arrivalTime.isoTime = isoDf.format(sdf.parse(schedule.time!!))
            val destination = schedule.destination.orEmpty()
            arrivalTime.destination = codeMap?.get(destination)?:destination
            arrivalTime.platform = schedule.platform!!
            arrivalTime.estimate = schedule.ttnt!!
            arrivalTime.order = schedule.sequence?:""
            if (!serverTime.isNullOrEmpty()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
                try {
                    arrivalTime.generatedAt = dateFormat.parse(serverTime).time
                } catch (ignored: ParseException) { }
            }
            arrivalTime.updatedAt = System.currentTimeMillis()
            arrivalTime = ArrivalTime.estimate(context, arrivalTime)
            if (arrivalTime.text.isEmpty()) {
                val etaDate = isoDf.parse(arrivalTime.isoTime)
                arrivalTime.text = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(etaDate)
                arrivalTime.text += " " + arrivalTime.destination
            }
            return arrivalTime
        }

    }
}