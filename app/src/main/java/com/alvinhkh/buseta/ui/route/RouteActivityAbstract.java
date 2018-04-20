package com.alvinhkh.buseta.ui.route;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.model.SearchHistory;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.RouteUtil;
import com.alvinhkh.buseta.utils.SearchHistoryUtil;
import com.google.android.gms.maps.MapView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public abstract class RouteActivityAbstract extends BaseActivity {

    protected final CompositeDisposable disposables = new CompositeDisposable();

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    public RoutePagerAdapter pagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public ViewPager viewPager;

    protected FloatingActionButton fab;

    protected View emptyView;

    protected ProgressBar progressBar;

    protected TextView emptyText;

    protected RouteStop stopFromIntent;

    protected String routeNo;

    private Boolean isScrollToPage = false;

    private Integer fragNo = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            routeNo = bundle.getString(C.EXTRA.ROUTE_NO);
            stopFromIntent = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
        }
        if (TextUtils.isEmpty(routeNo)) {
            if (stopFromIntent != null) {
                routeNo = stopFromIntent.getRoute();
            }
        }

        setContentView(R.layout.activity_route);

        // set action bar
        setToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        adViewContainer = findViewById(R.id.adView_container);
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false);
        }
        fab = findViewById(R.id.fab);

        emptyView = findViewById(android.R.id.empty);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
        emptyText = findViewById(R.id.empty_text);
        showLoadingView();

        // Create the adapter that will return a fragment
        pagerAdapter = new RoutePagerAdapter(getSupportFragmentManager(), this, stopFromIntent);

        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                ArrayList<Route> routes = new ArrayList<>(pagerAdapter.getRoutes());
                RouteSelectDialogFragment fragment = RouteSelectDialogFragment.newInstance(routes, viewPager);
                fragment.show(getSupportFragmentManager(), "route_select_dialog_fragment");
            }
        });

        pagerAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (isScrollToPage) {
                    if (viewPager != null) {
                        viewPager.setCurrentItem(fragNo, false);
                    }
                    isScrollToPage = false;
                }
                if (pagerAdapter.getCount() > 0) {
                    if (emptyView != null) {
                        emptyView.setVisibility(View.GONE);
                    }
                    if (viewPager != null) {
                        viewPager.setOffscreenPageLimit(Math.min(pagerAdapter.getCount(), 10));
                    }
                } else {
                    showEmptyView();
                }
            }
        });


        if (!TextUtils.isEmpty(routeNo)) {
            loadRouteNo(routeNo);
        } else {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Fixing Later Map loading Delay
        new Thread(() -> {
            try {
                MapView mv = new MapView(getApplicationContext());
                mv.onCreate(null);
                mv.onPause();
                mv.onDestroy();
            } catch (Exception | NoSuchMethodError ignored){}
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                if (!TextUtils.isEmpty(routeNo)) {
                    loadRouteNo(routeNo);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    protected void showEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (emptyText != null) {
            emptyText.setText(R.string.message_fail_to_request);
        }
        if (fab != null) {
            fab.hide();
        }
    }

    protected void showLoadingView() {
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (emptyText != null) {
            emptyText.setText(R.string.message_loading);
        }
        if (fab != null) {
            fab.hide();
        }
    }

    protected void loadRouteNo(String no) {
        if (pagerAdapter != null) {
            pagerAdapter.clearSequence();
        }
        if (TextUtils.isEmpty(no)) {
            showEmptyView();
            return;
        }
        showLoadingView();
    }

    protected void onCompleteRoute(List<Route> routes, String companyCode) {
        if (pagerAdapter == null || TextUtils.isEmpty(companyCode)) return;
        for (Route route : routes) {
            if (route == null) continue;
            if (TextUtils.isEmpty(route.getName()) || !route.getName().equals(routeNo)) continue;
            companyCode = route.getCompanyCode();
            pagerAdapter.addSequence(route);
            if (stopFromIntent != null && route.isSpecial() != null && !route.isSpecial() &&
                    route.getCompanyCode() != null && route.getSequence() != null &&
                    route.getCompanyCode().equals(stopFromIntent.getCompanyCode()) &&
                    route.getSequence().equals(stopFromIntent.getDirection())) {
                // TODO: handle select which page from stopFromIntent, i.e. service type
                fragNo = pagerAdapter.getCount();
                isScrollToPage = true;
            }
        }
        if (getSupportActionBar() != null) {
            String routeName = RouteUtil.getCompanyName(this, companyCode, routeNo) + " " + routeNo;
            getSupportActionBar().setTitle(routeName);
        }
        if (routes.size() > 0 && !TextUtils.isEmpty(companyCode)) {
            SearchHistory history = SearchHistoryUtil.createInstance(routeNo, companyCode);
            getContentResolver().insert(SuggestionProvider.CONTENT_URI, SearchHistoryUtil.toContentValues(history));
        }
    }
}
