package com.alvinhkh.buseta.view;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.utils.Utils;
import com.alvinhkh.buseta.view.common.AppCompatPreferenceActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class BasePreferenceActivity extends AppCompatPreferenceActivity {

    private static final String TAG = BasePreferenceActivity.class.getSimpleName();
    static final Class<?>[] INNER_CLASSES = BasePreferenceActivity.class.getDeclaredClasses();

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTaskDescription(getString(R.string.app_name));
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        createAdView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        createAdView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != mAdView)
            mAdView.resume();
    }

    @Override
    protected void onPause() {
        if (null != mAdView)
            mAdView.pause();
        super.onPause();
    }

    /**
     *  Google found a 'security vulnerability' and imposed this hack.
     *  Have to check this fragment was actually conceived by this activity.
     */
    @Override
    protected boolean isValidFragment(String fragmentName) {
        Boolean knownFrag = false;
        for (Class<?> cls : INNER_CLASSES) {
            if ( cls.getName().equals(fragmentName) ){
                knownFrag = true;
                break;
            }
        }
        return knownFrag;
    }

    protected void createAdView() {
        // Admob
        final FrameLayout adViewContainer = (FrameLayout) findViewById(R.id.adView_container);
        if (null == adViewContainer) return;
        adViewContainer.setVisibility(View.GONE);
        if (null != mAdView) {
            mAdView.destroy();
            mAdView.setVisibility(View.GONE);
        }
        boolean hideAdView = false;
        if (!hideAdView) {
            mAdView = new AdView(this);
            mAdView.setAdUnitId(getString(R.string.ad_banner_unit_id));
            mAdView.setAdSize(AdSize.SMART_BANNER);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    adViewContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    adViewContainer.setVisibility(View.GONE);
                }
            });
            adViewContainer.addView(mAdView);
            AdRequest mAdRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)  // All emulators
                    .addTestDevice(getString(R.string.ad_test_device))
                    .build();
            mAdView.loadAd(mAdRequest);
            // get device md5 id
            String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = Utils.md5(android_id).toUpperCase();
            boolean isTestDevice = mAdRequest.isTestDevice(this);
            Log.v(TAG, "is Admob Test Device ? " + deviceId + " " + isTestDevice);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected void setTaskDescription(String title) {
        // overview task
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(title, bm,
                            ContextCompat.getColor(this, R.color.primary_600));
            setTaskDescription(taskDesc);
        }
    }

}