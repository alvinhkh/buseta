package com.alvinhkh.buseta;

import android.app.Application;

import com.alvinhkh.buseta.utils.NightModeHelper;
import com.google.firebase.crash.FirebaseCrash;

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            FirebaseCrash.setCrashCollectionEnabled(false);
        } else {
            FirebaseCrash.setCrashCollectionEnabled(true);
        }
        NightModeHelper.update(this);
    }

}