package com.alvinhkh.buseta.utils

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager

import com.alvinhkh.buseta.R

object PreferenceUtil {

    fun isShowWheelchairIcon(context: Context): Boolean {
        val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        return null != mPrefs && mPrefs.getBoolean("load_wheelchair_icon", true)
    }

    fun isShowWifiIcon(context: Context): Boolean {
        val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        return null != mPrefs && mPrefs.getBoolean("load_wifi_icon", true)
    }

    fun isUsingNewKmbApi(context: Context): Boolean {
        val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        return null != mPrefs && mPrefs.getString("kmb_api", "kmb_web") == "kmb_web"
    }

    fun shareAppIntent(context: Context): Intent {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.message_share_text))
        return intent
    }

}