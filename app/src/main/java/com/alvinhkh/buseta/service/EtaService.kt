package com.alvinhkh.buseta.service

import android.app.IntentService
import android.content.Intent
import android.text.TextUtils

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.datagovhk.DataGovHkService
import com.alvinhkh.buseta.datagovhk.model.MtrLineStation
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.kmb.KmbService
import com.alvinhkh.buseta.kmb.model.network.KmbEtaRes
import com.alvinhkh.buseta.kmb.util.KmbEtaUtil
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.model.RouteStop
import com.alvinhkh.buseta.mtr.MtrService
import com.alvinhkh.buseta.mtr.model.AESEtaBus
import com.alvinhkh.buseta.mtr.model.AESEtaBusRes
import com.alvinhkh.buseta.mtr.model.AESEtaBusStopsRequest
import com.alvinhkh.buseta.mtr.model.MtrSchedule
import com.alvinhkh.buseta.mtr.model.MtrScheduleRes
import com.alvinhkh.buseta.nlb.NlbService
import com.alvinhkh.buseta.nlb.model.NlbEtaRequest
import com.alvinhkh.buseta.nlb.model.NlbEtaRes
import com.alvinhkh.buseta.nlb.util.NlbEtaUtil
import com.alvinhkh.buseta.nwst.NwstService
import com.alvinhkh.buseta.nwst.model.NwstEta
import com.alvinhkh.buseta.nwst.util.NwstEtaUtil
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.HashUtil
import com.alvinhkh.buseta.utils.RouteStopUtil
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

import org.jsoup.Jsoup

import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.Locale

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import okhttp3.ResponseBody
import timber.log.Timber

import com.alvinhkh.buseta.nwst.NwstService.*


class EtaService : IntentService(EtaService::class.java.simpleName) {

    private val disposables = CompositeDisposable()

    private lateinit var arrivalTimeDatabase: ArrivalTimeDatabase

    private lateinit var followDatabase: FollowDatabase

    private val aesService = MtrService.aes.create(MtrService::class.java)

    private val dataGovHkService = DataGovHkService.resource.create(DataGovHkService::class.java)

    private val kmbEtaApi = KmbService.etav3.create(KmbService::class.java)

    private val mtrService = MtrService.api.create(MtrService::class.java)

    private val nwstApi = NwstService.api.create(NwstService::class.java)

    private val nlbApi = NlbService.api.create(NlbService::class.java)

    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    override fun onCreate() {
        super.onCreate()
        arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(this)!!
        followDatabase = FollowDatabase.getInstance(this)!!
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val extras = intent.extras ?: return

        if (!ConnectivityUtil.isConnected(this)) return     // network connection check

        val widgetId = extras.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = extras.getInt(C.EXTRA.NOTIFICATION_ID, -1)
        val row = extras.getInt(C.EXTRA.ROW, -1)

        var routeStopList: MutableList<RouteStop>? = extras.getParcelableArrayList(C.EXTRA.STOP_LIST)
        if (routeStopList == null) {
            disposables.clear()
            routeStopList = ArrayList()
        }
        val stop = extras.getParcelable<RouteStop>(C.EXTRA.STOP_OBJECT)
        if (stop != null) {
            routeStopList.add(stop)
        }
        if (extras.getBoolean(C.EXTRA.FOLLOW)) {
            disposables.clear()
            val followList = followDatabase.followDao().getList()
            for (follow in followList) {
                routeStopList.add(RouteStopUtil.fromFollow(follow))
            }
        }
        for (i in routeStopList.indices) {
            val routeStop = routeStopList[i]
            if (!TextUtils.isEmpty(routeStop.companyCode)) {
                if (!TextUtils.isEmpty(routeStop.routeNo) && !TextUtils.isEmpty(routeStop.stopId)
                        && !TextUtils.isEmpty(routeStop.sequence)) {
                    arrivalTimeDatabase.arrivalTimeDao().clear(routeStop.companyCode!!,
                            routeStop.routeNo!!, routeStop.routeSeq!!, routeStop.stopId!!, routeStop.sequence!!)
                }
                notifyUpdate(routeStop, C.EXTRA.UPDATING, widgetId, notificationId, row)
                when (routeStop.companyCode) {
                    C.PROVIDER.KMB -> disposables.add(kmbEtaApi.getEta(routeStop.routeNo, routeStop.routeSeq, routeStop.stopId, routeStop.sequence, routeStop.routeServiceType, "tc", "")
                            .subscribeWith(kmbEtaObserver(routeStop, widgetId, notificationId, row, i == routeStopList.size - 1)))
                    C.PROVIDER.NLB -> {
                        val request = NlbEtaRequest(routeStop.routeSeq, routeStop.stopId, "zh")
                        disposables.add(nlbApi.eta(request)
                                .subscribeWith(nlbEtaObserver(routeStop, widgetId, notificationId, row, i == routeStopList.size - 1)))
                    }
                    C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> disposables.add(nwstApi.eta((routeStop.stopId?:"0").toInt().toString(),
                            routeStop.routeNo, "Y", "60", LANGUAGE_TC, routeStop.routeSeq,
                            routeStop.sequence, routeStop.routeId, "Y", "Y",
                            NwstRequestUtil.syscode(), PLATFORM, APP_VERSION, APP_VERSION2, NwstRequestUtil.syscode2(), firebaseRemoteConfig.getString("nwst_tk"))
                            .subscribeWith(nwstEtaObserver(routeStop, widgetId, notificationId, row, i == routeStopList.size - 1)))
                    C.PROVIDER.LRTFEEDER -> {
                        val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                        arrivalTime.text = getString(R.string.provider_no_eta)
                        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                        notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row)
                    }
                    C.PROVIDER.AESBUS -> {
                        val key = HashUtil.md5("mtrMobile_" + SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).format(Date()))
                        if (!TextUtils.isEmpty(key)) {
                            disposables.add(aesService.getBusStopsDetail(AESEtaBusStopsRequest(routeStop.routeNo!!, "2", "zh", key!!))
                                    .subscribeWith(aesBusEtaObserver(routeStop, widgetId, notificationId, row, i == routeStopList.size - 1)))
                        } else {
                            notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row)
                        }
                    }
                    C.PROVIDER.MTR -> {
                        if (TextUtils.isEmpty(routeStop.routeId) || TextUtils.isEmpty(routeStop.stopId)) {
                            notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row)
                            return
                        }
                        disposables.add(dataGovHkService.mtrLinesAndStations()
                                .subscribeWith(mtrLinesAndStationsObserver(routeStop, widgetId, notificationId, row, i == routeStopList.size - 1)))
                    }
                    else -> notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row)
                }
            } else {
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row)
            }
        }
    }

    private fun notifyUpdate(stop: RouteStop, status: String,
                             widgetId: Int, notificationId: Int, row: Int) {
        val intent = Intent(C.ACTION.ETA_UPDATE)
        intent.putExtra(status, true)
        if (widgetId >= 0) {
            intent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId)
        }
        if (notificationId >= 0) {
            intent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId)
        }
        if (row >= 0) {
            intent.putExtra(C.EXTRA.ROW, row)
        }
        intent.putExtra(C.EXTRA.STOP_OBJECT, stop)
        sendBroadcast(intent)
    }

    private fun kmbEtaObserver(routeStop: RouteStop,
                                widgetId: Int,
                                notificationId: Int,
                                rowNo: Int,
                                isLast: Boolean): DisposableObserver<KmbEtaRes> {
        // put kmb eta data to local eta database, EtaEntry
        return object : DisposableObserver<KmbEtaRes>() {
            override fun onNext(res: KmbEtaRes) {
                if (res.etas != null && res.etas.size > 0) {
                    for (i in res.etas.indices) {
                        val arrivalTime = KmbEtaUtil.toArrivalTime(applicationContext, res.etas[i], res.generated)
                        arrivalTime.routeNo = routeStop.routeNo?:""
                        arrivalTime.routeSeq = routeStop.routeSeq?:""
                        arrivalTime.stopId = routeStop.stopId?:""
                        arrivalTime.stopSeq = routeStop.sequence?:""
                        arrivalTime.order = Integer.toString(i)
                        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                    }
                    notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                    return
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onError(e: Throwable) {
                Timber.d(e)
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onComplete() {
                if (isLast) notifyUpdate(routeStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo)
            }
        }
    }

    private fun nlbEtaObserver(routeStop: RouteStop,
                                widgetId: Int,
                                notificationId: Int,
                                rowNo: Int,
                                isLast: Boolean): DisposableObserver<NlbEtaRes> {
        return object : DisposableObserver<NlbEtaRes>() {
            override fun onNext(res: NlbEtaRes) {
                if (res.estimatedArrivalTime != null && !TextUtils.isEmpty(res.estimatedArrivalTime.html)) {
                    val doc = Jsoup.parse(res.estimatedArrivalTime.html)
                    val divs = doc.body().getElementsByTag("div")
                    if (divs != null && divs.size > 0) {
                        var s = divs.size
                        if (s > 1) {
                            s -= 1
                        }
                        for (i in 0 until s) {
                            val arrivalTime = NlbEtaUtil.toArrivalTime(applicationContext, divs[i])
                            arrivalTime.order = Integer.toString(i)
                            arrivalTime.routeNo = routeStop.routeNo?:""
                            arrivalTime.routeSeq = routeStop.routeSeq?:""
                            arrivalTime.stopId = routeStop.stopId?:""
                            arrivalTime.stopSeq = routeStop.sequence?:""
                            arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                        }
                        notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                        return
                    }
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.generatedAt = System.currentTimeMillis()
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onError(e: Throwable) {
                Timber.d(e)
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onComplete() {
                if (isLast) notifyUpdate(routeStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo)
            }
        }
    }

    private fun nwstEtaObserver(routeStop: RouteStop,
                                 widgetId: Int,
                                 notificationId: Int,
                                 rowNo: Int,
                                 isLast: Boolean): DisposableObserver<ResponseBody> {
        return object : DisposableObserver<ResponseBody>() {
            override fun onNext(body: ResponseBody) {
                try {
                    val text = body.string()
                    val serverTime = text.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' }
                    val data = text.trim { it <= ' ' }.replaceFirst("^[^|]*\\|##\\|".toRegex(), "").split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (i in data.indices) {
                        val nwstEta = NwstEta.fromString(data[i]) ?: continue
                        nwstEta.serverTime = serverTime.replace("[^0-9:]".toRegex(), "")
                        val arrivalTime = NwstEtaUtil.toArrivalTime(applicationContext, routeStop, nwstEta)
                        arrivalTime.companyCode = routeStop.companyCode?:""
                        arrivalTime.routeNo = routeStop.routeNo?:""
                        arrivalTime.routeSeq = routeStop.routeSeq?:""
                        arrivalTime.stopId = routeStop.stopId?:""
                        arrivalTime.stopSeq = routeStop.sequence?:""
                        arrivalTime.order = Integer.toString(i)
                        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                    }
                    notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                    return
                } catch (e: IOException) {
                    Timber.d(e)
                }

                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.generatedAt = System.currentTimeMillis()
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onError(e: Throwable) {
                Timber.d(e)
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onComplete() {
                if (isLast) notifyUpdate(routeStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo)
            }
        }
    }

    private fun aesBusEtaObserver(routeStop: RouteStop,
                                   widgetId: Int,
                                   notificationId: Int,
                                   rowNo: Int,
                                   isLast: Boolean): DisposableObserver<AESEtaBusRes> {
        return object : DisposableObserver<AESEtaBusRes>() {

            var isError = false

            override fun onNext(res: AESEtaBusRes) {
                if (res.routeName != null && res.routeName.equals(routeStop.routeNo)) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
                    var statusTime = Date()
                    if (res.routeStatusTime != null) {
                        try {
                            statusTime = sdf.parse(res.routeStatusTime)
                        } catch (ignored: ParseException) {
                        }

                    }
                    var isAvailable = false
                    val etas = res.busStops
                    if (etas != null && etas.size > 0) {
                        // TODO: better way to store and show aes eta
                        for (i in etas.indices) {
                            val (buses, _, busStopId) = etas[i]
                            if (busStopId == null) continue
                            if (busStopId != routeStop.stopId && busStopId != "999") continue
                            if (buses != null && buses.isNotEmpty()) {
                                for (j in 0 until buses.size) {
                                    isAvailable = true
                                    val bus = buses[j]
                                    val arrivalTime = AESEtaBus.toArrivalTime(applicationContext, bus, statusTime, routeStop)
                                    arrivalTime.routeNo = routeStop.routeNo?:""
                                    arrivalTime.routeSeq = routeStop.routeSeq?:""
                                    arrivalTime.stopId = routeStop.stopId?:""
                                    arrivalTime.stopSeq = routeStop.sequence?:""
                                    arrivalTime.order = Integer.toString(j)
                                    arrivalTime.generatedAt = statusTime.time
                                    arrivalTime.updatedAt = System.currentTimeMillis()
                                    arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                                }
                            }
                            if (isAvailable) {
                                notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                            }
                        }
                    }
                    if (!isAvailable) {
                        val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                        if (!TextUtils.isEmpty(res.routeStatusRemarkTitle)) {
                            arrivalTime.text = res.routeStatusRemarkTitle?:""
                        }
                        arrivalTime.generatedAt = System.currentTimeMillis()
                        arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                        notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                    }
                } else {
                    isError = true
                }
            }

            override fun onError(e: Throwable) {
                isError = true
                Timber.d(e)
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onComplete() {
                if (!isError) {
                    notifyUpdate(routeStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo)
                }
            }
        }
    }
    
    private fun mtrLinesAndStationsObserver(routeStop: RouteStop,
                                             widgetId: Int,
                                             notificationId: Int,
                                             rowNo: Int,
                                             isLast: Boolean): DisposableObserver<ResponseBody> {
        return object : DisposableObserver<ResponseBody>() {

            var codeMap = HashMap<String, String>()

            override fun onNext(body: ResponseBody) {
                try {
                    val stations = MtrLineStation.fromCSV(body.string(), routeStop.routeId!!)
                    for ((_, _, stationCode, _, chineseName) in stations) {
                        if (!codeMap.containsKey(stationCode?:"")) {
                            codeMap[stationCode?:""] = chineseName?:""
                        }
                    }
                } catch (e: IOException) {
                    Timber.d(e)
                }

                val lang = "en"
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                val secret = firebaseRemoteConfig.getString("mtr_schedule_secret")
                val key = HashUtil.sha1(routeStop.routeId + "|" + routeStop.stopId + "|" + lang + "|" + today + "|" + secret)
                if (TextUtils.isEmpty(key) || TextUtils.isEmpty(routeStop.routeId) || TextUtils.isEmpty(routeStop.stopId)) {
                    notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
                    return
                }
                disposables.add(mtrService.getSchedule(key!!, routeStop.routeId!!, routeStop.stopId!!, lang)
                        .subscribeWith(mtrScheduleObserver(routeStop, widgetId, notificationId, rowNo, isLast, codeMap)))
            }

            override fun onError(e: Throwable) {
                Timber.d(e)
            }

            override fun onComplete() {}
        }
    }

    private fun mtrScheduleObserver(routeStop: RouteStop,
                                     widgetId: Int,
                                     notificationId: Int,
                                     rowNo: Int,
                                     isLast: Boolean,
                                     codeMap: HashMap<String, String>): DisposableObserver<MtrScheduleRes> {
        return object : DisposableObserver<MtrScheduleRes>() {
            var isError: Boolean = false

            override fun onNext(res: MtrScheduleRes) {
                if (res.status == 0) {
                    val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                    arrivalTime.text = getString(R.string.provider_no_eta)
                    arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                    notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                    return
                }
                if (res.data != null && res.data!!.isNotEmpty()) {
                    var hasData = false
                    for ((_, value) in res.data!!) {
                        val (currentTime, _, up, down) = value
                        var i = 0
                        if (up != null && up.isNotEmpty()) {
                            hasData = true
                            for (schedule in up) {
                                val arrivalTime = MtrSchedule.toArrivalTime(applicationContext, "UT", schedule, currentTime, codeMap)
                                arrivalTime.routeNo = routeStop.routeNo?:""
                                arrivalTime.routeSeq = routeStop.routeSeq?:""
                                arrivalTime.stopId = routeStop.stopId?:""
                                arrivalTime.stopSeq = routeStop.sequence?:""
                                arrivalTime.order = i.toString()
                                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                                i++
                            }
                        }
                        if (down != null && down.isNotEmpty()) {
                            hasData = true
                            for (schedule in down) {
                                val arrivalTime = MtrSchedule.toArrivalTime(applicationContext, "DT", schedule, currentTime, codeMap)
                                arrivalTime.routeNo = routeStop.routeNo?:""
                                arrivalTime.routeSeq = routeStop.routeSeq?:""
                                arrivalTime.stopId = routeStop.stopId?:""
                                arrivalTime.stopSeq = routeStop.sequence?:""
                                arrivalTime.order = i.toString()
                                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                                i++
                            }
                        }
                    }
                    if (hasData) {
                        notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
                        return
                    }
                }
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.routeNo = routeStop.routeNo?:""
                arrivalTime.routeSeq = routeStop.routeSeq?:""
                arrivalTime.stopId = routeStop.stopId?:""
                arrivalTime.stopSeq = routeStop.sequence?:""
                arrivalTime.order = "0"
                arrivalTime.text = getString(R.string.message_no_data)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo)
            }

            override fun onError(e: Throwable) {
                isError = true
                Timber.d(e)
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo)
            }

            override fun onComplete() {
                if (!isError) {
                    notifyUpdate(routeStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo)
                }
            }
        }
    }
}
