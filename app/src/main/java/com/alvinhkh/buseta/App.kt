package com.alvinhkh.buseta

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.alvinhkh.buseta.utils.NightModeUtil
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.fabric.sdk.android.Fabric
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val builder = unsafeOkHttpClient()
        builder.addNetworkInterceptor(UserAgentInterceptor())
        httpClient = builder
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .build()
        val builder2 = OkHttpClient.Builder()
        builder2.addNetworkInterceptor(UserAgentInterceptor())
        httpClient2 = builder2
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
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
        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
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

    private fun unsafeOkHttpClient(): OkHttpClient.Builder {
        return try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        @Throws(CertificateException::class)
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        @Throws(CertificateException::class)
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return arrayOf()
                        }
                    }
            )

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, (trustAllCerts[0] as X509TrustManager))
            builder.hostnameVerifier(HostnameVerifier { hostname: String?, session: SSLSession? -> true })
            builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    companion object {

        lateinit var httpClient: OkHttpClient

        lateinit var httpClient2: OkHttpClient
    }
}