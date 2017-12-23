package com.alvinhkh.buseta;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.alvinhkh.buseta.utils.NightModeUtil;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.fabric.sdk.android.Fabric;
import org.osmdroid.config.Configuration;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class App extends Application {

    public static OkHttpClient httpClient;

    @Override
    public void onCreate() {
        super.onCreate();

        httpClient = new OkHttpClient.Builder().build();

        Crashlytics crashlytics = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        final Fabric fabric = new Fabric.Builder(this)
                .kits(crashlytics, new Answers())
                .debuggable(BuildConfig.DEBUG)
                .build();
        Fabric.with(fabric);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashlyticsTree());
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
        }
        NightModeUtil.update(this);

        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private static class CrashlyticsTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) return;
            if (!TextUtils.isEmpty(message)) Crashlytics.log(priority, tag, message);
            if (t != null) Crashlytics.logException(t);
        }
    }
}