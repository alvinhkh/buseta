package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

public class NightModeHelper {

    public static void update(Context context) {
        if (null == context) return;
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Integer.valueOf(mPrefs.getString("app_theme", "1"))) {
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

}
