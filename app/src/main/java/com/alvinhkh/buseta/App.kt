package com.alvinhkh.buseta

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.alvinhkh.buseta.nwst.model.NwstQueryData
import com.alvinhkh.buseta.utils.NightModeUtil
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
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

        val builder = OkHttpClient.Builder()
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
        val builder3 = unsafeOkHttpClient()
        builder3.addNetworkInterceptor(UserAgentInterceptor())
        httpClientUnsafe = builder3
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .build()


        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
            Timber.plant(CrashlyticsTree())
        }
        NightModeUtil.update(this)

        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().userAgentValue = System.getProperty("http.agent")

        // Disk Persistence
        Firebase.database.setPersistenceEnabled(true)
        // Sync required settings
        val database = Firebase.database
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val nwstQueries = database.getReference("nwst_queries")
        nwstQueries.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val data = dataSnapshot.getValue<NwstQueryData>()
                Timber.d("$data")
                val editor = preferences.edit()
                editor.putString("nwst_syscode5", data?.syscode5)
                editor.putString("nwst_appId", data?.appId)
                editor.putString("nwst_version", data?.version)
                editor.putString("nwst_version2", data?.version2)
                editor.putLong("nwst_lastUpdated", data?.lastUpdated?:0)
                editor.apply()
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w(error.toException(), "Failed to read value.")
            }
        })

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
            if (!TextUtils.isEmpty(message)) FirebaseCrashlytics.getInstance().log(message)
            if (t != null) FirebaseCrashlytics.getInstance().recordException(t)
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
            val sslContext = SSLContext.getInstance("TLSv1.2")
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

        lateinit var httpClientUnsafe: OkHttpClient
    }
}