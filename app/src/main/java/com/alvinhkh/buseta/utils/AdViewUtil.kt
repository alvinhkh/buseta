package com.alvinhkh.buseta.utils

import android.support.v7.preference.PreferenceManager
import android.view.View
import android.widget.FrameLayout

import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

object AdViewUtil {

    fun banner(adViewContainer: FrameLayout,
               _adView: AdView?,
               isForce: Boolean?): AdView? {
        var adView = _adView
        val context = adViewContainer.context ?: return adView
        if (adView != null) {
            adView.destroy()
            adView.visibility = View.GONE
        }
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (preferences == null) {
            adViewContainer.visibility = View.VISIBLE
            return adView
        }
        val hideAdView = preferences.getBoolean(C.PREF.AD_HIDE, false)
        if (!hideAdView || isForce!!) {
            adView = AdView(context)
            adView.adUnitId = context.getString(R.string.AD_BANNER_UNIT_ID)
            adView.adSize = AdSize.SMART_BANNER
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adViewContainer.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(errorCode: Int) {
                    adViewContainer.visibility = View.GONE
                }
            }
            adViewContainer.addView(adView)
            val mAdRequest = AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)  // All emulators
                    .build()
            adView.loadAd(mAdRequest)
        }
        return adView
    }
}
