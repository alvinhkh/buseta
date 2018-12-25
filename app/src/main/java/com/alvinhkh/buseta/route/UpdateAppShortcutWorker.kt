package com.alvinhkh.buseta.route

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.search.ui.SearchActivity
import com.alvinhkh.buseta.utils.RouteStopUtil
import com.google.gson.Gson
import java.util.ArrayList

class UpdateAppShortcutWorker(context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val followDatabase = FollowDatabase.getInstance(context)
    private val suggestionDatabase = SuggestionDatabase.getInstance(context)

    override fun doWork(): Result {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return Result.failure()

        // Dynamic App Shortcut
        try {
            val shortcutManager = applicationContext.getSystemService(ShortcutManager::class.java)
                    ?: return Result.failure()
            val shortcuts = ArrayList<ShortcutInfo>()
            val followList = followDatabase?.followDao()?.getList()?: emptyList()
            val maxShortcutCount = shortcutManager.maxShortcutCountPerActivity
            run {
                var i = 0
                while (i < maxShortcutCount - 1 && i < followList.size) {
                    val follow = followList[i]
                    val routeStop = RouteStopUtil.fromFollow(follow)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(applicationContext, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.STOP_OBJECT_STRING, Gson().toJson(routeStop))
                    shortcuts.add(ShortcutInfo.Builder(applicationContext, "buseta-" + routeStop.companyCode + routeStop.routeNo + routeStop.routeSequence + routeStop.stopId)
                            .setShortLabel(routeStop.routeNo + " " + routeStop.name)
                            .setLongLabel(routeStop.routeNo + " " + routeStop.name + " " + applicationContext.getString(R.string.destination, routeStop.routeDestination))
                            .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_shortcut_directions_bus))
                            .setIntent(intent)
                            .build())
                    i++

                }
            }
            if (followList.size < maxShortcutCount - 1) {
                val historyList = suggestionDatabase?.suggestionDao()?.historyList(maxShortcutCount)?: emptyList()
                var i = 0
                while (i < maxShortcutCount - followList.size - 1 && i < historyList.size) {
                    val (_, companyCode, route) = historyList[i]
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setClass(applicationContext, SearchActivity::class.java)
                    intent.putExtra(C.EXTRA.ROUTE_NO, route)
                    intent.putExtra(C.EXTRA.COMPANY_CODE, companyCode)
                    shortcuts.add(ShortcutInfo.Builder(applicationContext, "buseta-q-$companyCode$route")
                            .setShortLabel(route)
                            .setLongLabel(route)
                            .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_shortcut_search))
                            .setIntent(intent)
                            .build())
                    i++
                }
            }
            shortcutManager.dynamicShortcuts = shortcuts
        } catch (ignored: NoClassDefFoundError) {
            return Result.failure()
        } catch (ignored: NoSuchMethodError) {
            return Result.failure()
        }

        return Result.success()
    }
}