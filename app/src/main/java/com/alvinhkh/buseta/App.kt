package com.alvinhkh.buseta

import android.app.Application

import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager

import android.content.Context
import android.text.TextUtils
import android.util.Log

import com.alvinhkh.buseta.utils.NightModeUtil
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

import io.fabric.sdk.android.Fabric
import org.osmdroid.config.Configuration

import java.util.concurrent.TimeUnit

import okhttp3.OkHttpClient
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val builder = OkHttpClient.Builder()
        builder.addNetworkInterceptor(UserAgentInterceptor())
        httpClient = builder
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        val builder2 = OkHttpClient.Builder()
        builder2.addNetworkInterceptor(UserAgentInterceptor("CitybusNWFB/5 CFNetwork/975.0.3 Darwin/18.2.0"))
        httpClient2 = builder2
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

        val crashlytics = Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build()
        val fabric = Fabric.Builder(this)
                .kits(crashlytics, Answers())
                .debuggable(BuildConfig.DEBUG)
                .build()
        Fabric.with(fabric)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
        }
        NightModeUtil.update(this)

        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        var cacheExpiration = 3600L
        if (BuildConfig.DEBUG) {
            cacheExpiration = 0
        }
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(cacheExpiration)
                .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        firebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults)
        firebaseRemoteConfig.fetch(0)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // After config data is successfully fetched, it must be activated before newly fetched
                        // values are returned.
                        firebaseRemoteConfig.activate()
                    }
                }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) return
            if (!TextUtils.isEmpty(message)) Crashlytics.log(priority, tag, message)
            if (t != null) Crashlytics.logException(t)
        }
    }

    companion object {

        lateinit var httpClient: OkHttpClient

        lateinit var httpClient2: OkHttpClient
    }
}