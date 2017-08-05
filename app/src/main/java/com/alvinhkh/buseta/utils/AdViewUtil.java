package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.FrameLayout;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class AdViewUtil {

    public static AdView banner(@NonNull final FrameLayout adViewContainer, @Nullable AdView adView) {
        Context context = adViewContainer.getContext();
        if (context == null) return adView;
        if (adView != null) {
            adView.destroy();
            adView.setVisibility(View.GONE);
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences == null) {
            adViewContainer.setVisibility(View.VISIBLE);
            return adView;
        }
        boolean hideAdView = preferences.getBoolean(C.PREF.AD_HIDE, false);
        if (!hideAdView) {
            adView = new AdView(context);
            adView.setAdUnitId(context.getString(R.string.AD_BANNER_UNIT_ID));
            adView.setAdSize(AdSize.SMART_BANNER);
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    adViewContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    adViewContainer.setVisibility(View.GONE);
                }
            });
            adViewContainer.addView(adView);
            AdRequest mAdRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)  // All emulators
                    .build();
            adView.loadAd(mAdRequest);
        }
        return adView;
    }
}
