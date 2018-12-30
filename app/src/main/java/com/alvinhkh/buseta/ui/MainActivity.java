package com.alvinhkh.buseta.ui;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.follow.dao.FollowDatabase;
import com.alvinhkh.buseta.follow.ui.FollowFragment;
import com.alvinhkh.buseta.mtr.ui.MtrLineStatusFragment;
import com.alvinhkh.buseta.search.ui.HistoryFragment;
import com.alvinhkh.buseta.search.ui.SearchActivity;
import com.alvinhkh.buseta.follow.ui.EditFollowFragment;
import com.alvinhkh.buseta.service.ProviderUpdateService;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.ColorUtil;


public class MainActivity extends BaseActivity {

    private FloatingActionButton fab;

    private BottomNavigationView bottomNavigationView;

    private Fragment followFragment;

    private Fragment historyFragment;

    private Fragment mtrLineStatusFragment;

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

        followFragment = new FollowFragment();
        historyFragment = new HistoryFragment();
        mtrLineStatusFragment = MtrLineStatusFragment.newInstance();

        adViewContainer = findViewById(R.id.adView_container);
        if (adViewContainer != null) {
            adView = AdViewUtil.INSTANCE.banner(adViewContainer, adView, false);
        }

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            // if (searchMenuItem != null) searchMenuItem.expandActionView();
            startActivity(new Intent(this, SearchActivity.class));
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                item -> {
                    Integer colorInt = 0;
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm == null) return false;
                    fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    switch (item.getItemId()) {
                        case R.id.action_follow:
                        {
                            if (fm.findFragmentByTag("follow_list") == null) {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(R.string.app_name);
                                    getSupportActionBar().setSubtitle(null);
                                }
                                colorInt = ContextCompat.getColor(this, R.color.colorPrimary);
                                FragmentTransaction ft = fm.beginTransaction();
                                ft.replace(R.id.fragment_container, followFragment);
                                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                                ft.addToBackStack("follow_list");
                                ft.commit();
                            }
                            break;
                        }
                        case R.id.action_search_history:
                        {
                            if (fm.findFragmentByTag("search_history") == null) {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(R.string.app_name);
                                    getSupportActionBar().setSubtitle(null);
                                }
                                colorInt = ContextCompat.getColor(this, R.color.colorPrimary);
                                FragmentTransaction ft = fm.beginTransaction();
                                ft.replace(R.id.fragment_container, historyFragment);
                                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                                ft.addToBackStack("search_history");
                                ft.commit();
                            }
                            break;
                        }
                        case R.id.action_railway:
                        {
                            if (fm.findFragmentByTag("railway") == null) {
                                if (getSupportActionBar() != null) {
                                    getSupportActionBar().setTitle(R.string.provider_mtr);
                                    getSupportActionBar().setSubtitle(null);
                                }
                                colorInt = ContextCompat.getColor(this, R.color.provider_mtr);
                                FragmentTransaction ft = fm.beginTransaction();
                                ft.replace(R.id.fragment_container, mtrLineStatusFragment);
                                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                                ft.addToBackStack("railway");
                                ft.commit();
                            }
                            break;
                        }
                        default:
                            finish();
                    }
                    if (colorInt != 0 && getSupportActionBar() != null) {
                        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(colorInt));
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                && getWindow() != null) {
                            getWindow().setStatusBarColor(ColorUtil.Companion.darkenColor(colorInt));
                            getWindow().setNavigationBarColor(ColorUtil.Companion.darkenColor(colorInt));
                        }
                    }
                    return true;
                });
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (f != null && bottomNavigationView != null){
                switch (f.getClass().getName()) {
                    case "FollowFragment":
                        bottomNavigationView.setSelectedItemId(R.id.action_follow);
                        break;
                    case "HistoryFragment":
                        bottomNavigationView.setSelectedItemId(R.id.action_search_history);
                        break;
                    case "MtrLineStatusFragment":
                        bottomNavigationView.setSelectedItemId(R.id.action_railway);
                        break;
                }
            }

        });
        if (bottomNavigationView != null) {
            FollowDatabase followDatabase = FollowDatabase.Companion.getInstance(getApplicationContext());
            if (followDatabase != null && followDatabase.followDao().count() > 0) {
                bottomNavigationView.setSelectedItemId(R.id.action_follow);
            } else {
                bottomNavigationView.setSelectedItemId(R.id.action_search_history);
            }
        }

        try {
            Intent intent = new Intent(this, ProviderUpdateService.class);
            startService(intent);
        } catch (IllegalStateException ignored) {}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_follow:
                bottomNavigationView.setSelectedItemId(0);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, new EditFollowFragment());
                fragmentTransaction.addToBackStack("follow_list");
                fragmentTransaction.commit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
