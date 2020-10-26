package com.alvinhkh.buseta.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.alvinhkh.buseta.Api
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.RtNwstWorker
import com.alvinhkh.buseta.datagovhk.TdWorker
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.kmb.KmbRouteWorker
import com.alvinhkh.buseta.lwb.LwbRouteWorker
import com.alvinhkh.buseta.mtr.MtrBusWorker
import com.alvinhkh.buseta.mtr.MtrLineWorker
import com.alvinhkh.buseta.mtr.MtrResourceWorker
import com.alvinhkh.buseta.nlb.NlbWorker
import com.alvinhkh.buseta.nwst.NwstRouteWorker
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.ConnectivityUtil
import com.alvinhkh.buseta.utils.PreferenceUtil
import timber.log.Timber

class ProviderUpdateService: Service() {

    private lateinit var followDatabase: FollowDatabase

    private lateinit var routeDatabase: RouteDatabase

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var workManager: WorkManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        followDatabase = FollowDatabase.getInstance(this)!!
        routeDatabase = RouteDatabase.getInstance(this)!!
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        workManager = WorkManager.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.deleteNotificationChannel("CHANNEL_ID_UPDATE")
            if (notificationManager?.getNotificationChannel(C.NOTIFICATION.CHANNEL_UPDATE) == null) {
                val notificationChannel = NotificationChannel(C.NOTIFICATION.CHANNEL_UPDATE,
                        getString(R.string.channel_name_update), NotificationManager.IMPORTANCE_MIN)
                notificationChannel.description = getString(R.string.channel_description_update)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                notificationChannel.setSound(null, null)
                notificationChannel.setShowBadge(false)
                notificationManager?.createNotificationChannel(notificationChannel)
            }
        }
    }

    private fun startForegroundNotification(max: Int, progress: Int) {
        val notificationId = 2000
        val builder = NotificationCompat.Builder(this, C.NOTIFICATION.CHANNEL_UPDATE)
        builder.setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.ic_outline_directions_bus_24dp)
                .setShowWhen(false)
                .setSound(null)
                .setProgress(max, progress, progress <= 0)
                .setContentTitle(getString(if (max == 100 && progress == 0) {
                    R.string.channel_name_update
                } else {
                    R.string.channel_description_update
                }))
                .setContentText(if (progress <= 0) {
                    null
                } else {
                    "$progress/$max"
                })
        startForeground(notificationId, builder.build())
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val manualUpdate = intent.getBooleanExtra(C.EXTRA.MANUAL, false)
        if (!ConnectivityUtil.isConnected(this)) {
            // Check internet connection
            val i = Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE)
            i.putExtra(C.EXTRA.UPDATED, true)
            i.putExtra(C.EXTRA.MANUAL, manualUpdate)
            i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_no_internet_connection)
            sendBroadcast(i)
            stopSelf()
            return START_NOT_STICKY
        }
        workManager.cancelAllWorkByTag(TAG)

        val routeCount = routeDatabase.routeDao().count()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val timeNow = System.currentTimeMillis() / 1000
        val savedTime = sharedPreferences.getLong("last_update_check", 0)
        if (!manualUpdate && routeCount > 0 && timeNow < savedTime + 259200) {
            Timber.d("recently updated and not manual update")
            stopSelf()
            return START_NOT_STICKY
        }
        sharedPreferences.edit().putLong("last_update_check", timeNow).apply()

        startForegroundNotification(100, 0)

        workManager.cancelAllWorkByTag("RouteList")
        var workCount = 0
        workManager.getWorkInfosByTagLiveData(TAG).observeForever { workInfos ->
            if (workCount == 0) {
                workCount = workInfos.size
            }
            if (workInfos.size == workCount || workInfos.size == 0) return@observeForever
            var finishedCount = 0
            for (workInfo in workInfos) {
                if (workInfo.state.isFinished) {
                    finishedCount += 1
                }
            }
            startForegroundNotification(workInfos.size - workCount, finishedCount - workCount)
            if (finishedCount >= workInfos.size) {
                val i = Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE)
                i.putExtra(C.EXTRA.UPDATED, true)
                i.putExtra(C.EXTRA.MANUAL, manualUpdate)
                i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_database_updated)
                applicationContext.sendBroadcast(i)
                workManager.cancelAllWorkByTag(TAG)
                updateFollowRoute(manualUpdate)
            }
        }

        val dataTd = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .build()
        val tdRequest = OneTimeWorkRequest.Builder(TdWorker::class.java)
                .addTag(TAG).setInputData(dataTd).build()
        workManager.enqueue(tdRequest)

        try {
            val apiService = Api.retrofit.create(Api::class.java)
            val response = apiService.appUpdate().execute()
            if (response.isSuccessful) {
                val res = response.body()
                if (res != null && res.isNotEmpty()) {
                    val appUpdate = res[0]
                    val i = Intent(C.ACTION.APP_UPDATE)
                    i.putExtra(C.EXTRA.UPDATED, true)
                    i.putExtra(C.EXTRA.MANUAL, manualUpdate)
                    i.putExtra(C.EXTRA.APP_UPDATE_OBJECT, appUpdate)
                    sendBroadcast(i)
                }
            }
        } catch (ignored: Throwable) {
        }

        val dataMtrBus = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.LRTFEEDER)
                .build()
        val busMtrResourceRequest = OneTimeWorkRequest.Builder(MtrResourceWorker::class.java)
                .addTag(TAG).setInputData(dataMtrBus).build()
        val mtrBusRequest = OneTimeWorkRequest.Builder(MtrBusWorker::class.java)
                .addTag(TAG).setInputData(dataMtrBus).build()
        workManager.beginWith(busMtrResourceRequest)
                .then(mtrBusRequest)
                .enqueue()

        val dataMtrLine = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.MTR)
                .build()
        val mtrLineRequest = OneTimeWorkRequest.Builder(MtrLineWorker::class.java)
                .addTag(TAG).setInputData(dataMtrLine).build()
        workManager.enqueue(mtrLineRequest)

        val dataNlb = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.NLB)
                .build()
        val nlbRequest = OneTimeWorkRequest.Builder(NlbWorker::class.java)
                .addTag(TAG).setInputData(dataNlb).build()
        workManager.enqueue(nlbRequest)

        return START_STICKY
    }

    private fun updateFollowRoute(manualUpdate: Boolean) {
        // TODO: check any route in follow list updated or removed
        val followTag = "FollowRoute"
        workManager.cancelAllWorkByTag(followTag)
        val map = hashMapOf<String, Pair<String, String>>()
        val list = followDatabase.followDao().list()
        list.forEach { follow ->
            val id = when(follow.companyCode) {
                C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST,
                C.PROVIDER.KMB, C.PROVIDER.LWB -> follow.companyCode + follow.routeNo
                else -> follow.companyCode
            }
            map[id] = Pair(follow.companyCode, follow.routeNo)
        }
        val requests = mutableListOf<OneTimeWorkRequest>()
        map.values.forEach { item ->
            val data = Data.Builder()
                    .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                    .putString(C.EXTRA.TAG, followTag)
                    .putString(C.EXTRA.COMPANY_CODE, item.first)
                    .putString(C.EXTRA.ROUTE_NO, item.second)
                    .putBoolean(C.EXTRA.LOAD_STOP, true)
                    .build()
            when (item.first) {
                C.PROVIDER.CTB, C.PROVIDER.NWFB, C.PROVIDER.NWST -> {
                    requests.add(OneTimeWorkRequest.Builder(
                            if (PreferenceUtil.isUsingNwstDataGovHkApi(applicationContext)) {
                                RtNwstWorker::class.java
                            } else {
                                NwstRouteWorker::class.java
                            }
                    ).addTag(followTag).setInputData(data).build())
                }
                C.PROVIDER.KMB, C.PROVIDER.LWB -> {
                    requests.add(OneTimeWorkRequest.Builder(
                            if (PreferenceUtil.isUsingKmbWebApi(applicationContext)) {
                                KmbRouteWorker::class.java
                            } else {
                                LwbRouteWorker::class.java
                            }
                    ).addTag(followTag).setInputData(data).build())
                }
            }
        }
        if (requests.size > 0) {
            var workCount = 0
            workManager.getWorkInfosByTagLiveData(followTag).observeForever { workInfos ->
                if (workCount == 0) {
                    workCount = workInfos.size
                }
                if (workInfos.size == workCount || workInfos.size == 0) return@observeForever
                var finishedCount = 0
                for (workInfo in workInfos) {
                    if (workInfo.state.isFinished) {
                        finishedCount += 1
                    }
                }
                startForegroundNotification(workInfos.size - workCount, finishedCount - workCount)
                if (finishedCount >= workInfos.size) {
                    workManager.cancelAllWorkByTag(followTag)
                    stopSelf()
                }
            }
            workManager.enqueue(requests)
        } else {
            stopSelf()
        }
    }

    companion object {
        private const val TAG = "ProviderUpdateService"
    }

}