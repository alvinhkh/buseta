package com.alvinhkh.buseta.ui.setting;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.NightModeUtil;
import com.google.android.gms.ads.AdView;

abstract public class BasePreferenceActivity extends AppCompatPreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private AdView adView;

    private FrameLayout adViewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTaskDescription(getString(R.string.app_name));
        setContentView(R.layout.activity_setting);
        setSupportActionBar(findViewById(R.id.toolbar));

        adViewContainer = findViewById(R.id.adView_container);
        adView = AdViewUtil.banner(adViewContainer, adView, true);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adView = AdViewUtil.banner(adViewContainer, adView, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.matches("app_theme")) {
            NightModeUtil.update(this);
            recreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NightModeUtil.update(this)) {
            recreate();
            return;
        }
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
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

    protected void setTaskDescription(String title) {
        // overview task
        if (Build.VERSION.SDK_INT >= 28) {
            setTaskDescription(new ActivityManager.TaskDescription(title, R.mipmap.ic_launcher,
                    ContextCompat.getColor(this, R.color.colorPrimary600)));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(title, bm,
                    ContextCompat.getColor(this, R.color.colorPrimary600)));
        }
    }

}