package com.alvinhkh.buseta.utils

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

object NightModeUtil {

    @JvmStatic
    fun update(context: Context): Boolean {
        var isRecreate = false
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val nightMode = (preferences.getString("app_theme", "1")?:"1").toInt()
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            isRecreate = true
            when (nightMode) {
                0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE)
            if (uiModeManager is UiModeManager) {
                if (uiModeManager.nightMode != AppCompatDelegate.getDefaultNightMode() && uiModeManager.nightMode != nightMode) {
                    when (nightMode) {
                        0 -> uiModeManager.nightMode = UiModeManager.MODE_NIGHT_AUTO
                        1 -> uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
                        2 -> uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
                        else -> uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
                    }
                }
            }
        }
        return isRecreate
    }

}
