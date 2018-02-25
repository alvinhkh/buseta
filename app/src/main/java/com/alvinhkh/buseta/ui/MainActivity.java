package com.alvinhkh.buseta.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.mtr.ui.MtrLineStatusFragment;
import com.alvinhkh.buseta.service.CheckUpdateService;
import com.alvinhkh.buseta.service.LocationService;
import com.alvinhkh.buseta.ui.follow.EditFollowFragment;
import com.alvinhkh.buseta.ui.follow.FollowFragment;
import com.alvinhkh.buseta.utils.AdViewUtil;


public class MainActivity extends BaseActivity {

    private FloatingActionButton fab;

    private BottomNavigationView bottomNavigationView;

    private FollowFragment followFragment;

    private MtrLineStatusFragment mtrLineStatusFragment;

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

        followFragment = FollowFragment.newInstance();
        mtrLineStatusFragment = MtrLineStatusFragment.newInstance();

        adViewContainer = findViewById(R.id.adView_container);
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false);
        }

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (searchMenuItem != null) searchMenuItem.expandActionView();
            // startActivity(new Intent(this, SearchActivity.class));
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                item -> {
                    FragmentManager fm = getSupportFragmentManager();
                    fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    switch (item.getItemId()) {
                        case R.id.action_bus:
                        {
                            if (fm.findFragmentByTag("follow_list") == null) {
                                FragmentTransaction ft = fm.beginTransaction();
                                ft.replace(R.id.fragment_container, followFragment);
                                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                                ft.addToBackStack("follow_list");
                                ft.commit();
                            }
                            break;
                        }
                        case R.id.action_railway:
                        {
                            if (fm.findFragmentByTag("railway") == null) {
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
                    return true;
                });
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (f != null){
                switch (f.getClass().getName()) {
                    case "FollowFragment":
                        bottomNavigationView.setSelectedItemId(R.id.action_bus);
                        break;
                    case "MtrLineStatusFragment":
                        bottomNavigationView.setSelectedItemId(R.id.action_railway);
                        break;
                }
            }

        });
        bottomNavigationView.setSelectedItemId(R.id.action_bus);

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
                bottomNavigationView.setSelectedItemId(0);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, EditFollowFragment.newInstance());
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
