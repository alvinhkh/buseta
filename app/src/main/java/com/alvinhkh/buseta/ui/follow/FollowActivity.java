package com.alvinhkh.buseta.ui.follow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.service.CheckUpdateService;
import com.alvinhkh.buseta.service.LocationService;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.google.android.gms.ads.AdView;


// TODO: sort follow stop
public class FollowActivity extends BaseActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private AdView adView;

    private FrameLayout adViewContainer;

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_follow);

        // set action bar
        setToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        adViewContainer = findViewById(R.id.adView_container);
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView);
        }

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (searchMenuItem != null) searchMenuItem.expandActionView();
            // startActivity(new Intent(this, SearchActivity.class));
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, FollowFragment.newInstance());
        fragmentTransaction.addToBackStack("follow_list");
        fragmentTransaction.commit();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        Intent intent = new Intent(this, CheckUpdateService.class);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        final MenuItem itemShare = menu.findItem(R.id.action_share);
        itemShare.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_follow:
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, EditFollowFragment.newInstance());
                fragmentTransaction.addToBackStack("follow_list");
                fragmentTransaction.commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        super.onSharedPreferenceChanged(sp, key);
        if (key.matches(C.PREF.AD_HIDE)) {
            if (adViewContainer != null) {
                adView = AdViewUtil.banner(adViewContainer, adView);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        if (fab != null) {
            fab.show();
        }
    }

    @Override
    protected void onPause() {
        if (fab != null) {
            fab.hide();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.pause();
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        try {
            Intent intent = new Intent(this, LocationService.class);
            intent.setAction(C.ACTION.CANCEL);
            startService(intent);
        } catch (IllegalStateException ignored) {}
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() < 2) {
            finish();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
