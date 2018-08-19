package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PreferenceUtil {

    public static boolean isShowWheelchairIcon(@NonNull Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return null != mPrefs && mPrefs.getBoolean("load_wheelchair_icon", true);
    }

    public static boolean isShowWifiIcon(@NonNull Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return null != mPrefs && mPrefs.getBoolean("load_wifi_icon", true);
    }

    public static boolean isUsingNewKmbApi(@NonNull Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return null != mPrefs && mPrefs.getString("kmb_api", "kmb_web").equals("kmb_web");
    }

    public static Intent shareAppIntent(@NonNull Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.message_share_text));
        return intent;
    }

}