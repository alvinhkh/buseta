package com.alvinhkh.buseta.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import io.reactivex.annotations.NonNull;

public class NightModeUtil {

    public static boolean update(@NonNull Context context) {
        if (context == null) return false;
        Boolean isRecreate = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Integer nightMode = Integer.valueOf(preferences.getString("app_theme", "1"));
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            isRecreate = true;
            switch (nightMode) {
                case 0:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
                    break;
                case 1:
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case 2:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
            }
        }
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null) {
            if (uiModeManager.getNightMode() != AppCompatDelegate.getDefaultNightMode() &&
                    uiModeManager.getNightMode() != nightMode) {
                switch (nightMode) {
                    case 0:
                        uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
                        break;
                    case 1:
                    default:
                        uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO);
                        break;
                    case 2:
                        uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
                        break;
                }
            }
        }
        return isRecreate;
    }

}
