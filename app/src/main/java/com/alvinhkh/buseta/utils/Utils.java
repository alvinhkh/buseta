package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.content.pm.PackageManager;

public class Utils {

    public static boolean isPackageInstalled(String name, Context context) {
        if (null == context) return false;
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

}
