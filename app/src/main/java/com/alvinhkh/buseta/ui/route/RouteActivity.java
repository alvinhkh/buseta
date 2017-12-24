package com.alvinhkh.buseta.ui.route;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.SearchHistoryUtil;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

import io.reactivex.disposables.CompositeDisposable;

public abstract class RouteActivity extends BaseActivity {

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

    protected BusRouteStop stopFromIntent;

    protected String routeNo;

    protected Fragment currentFragment;

    protected int fragNo = 0;

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
                routeNo = stopFromIntent.route;
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
        fab.setOnClickListener(v -> {
            if (currentFragment != null) {
                if (currentFragment instanceof RouteStopListFragmentAbstract) {
                    RouteStopListFragmentAbstract f = (RouteStopListFragmentAbstract) currentFragment;
                    f.onRefresh();
                }
            }
        });

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
                ArrayList<BusRoute> busRoutes = new ArrayList<>(pagerAdapter.getRoutes());
                RouteSelectDialogFragment fragment = RouteSelectDialogFragment.newInstance(busRoutes, viewPager);
                fragment.show(getSupportFragmentManager(), "route_select_dialog_fragment");
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                currentFragment = pagerAdapter.getFragment(position);
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


        if (!TextUtils.isEmpty(routeNo)) {
            loadRouteNo(routeNo);
        } else {
            Toast.makeText(this, R.string.missing_input, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
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

    protected void onCompleteRoute(Boolean isScrollToPage, String companyCode) {
        if (getSupportActionBar() != null) {
            String companyName;
            switch (companyCode) {
                case BusRoute.COMPANY_CTB:
                    companyName = getString(R.string.provider_short_ctb);
                    break;
                case BusRoute.COMPANY_KMB:
                    companyName = getString(R.string.provider_short_kmb);
                    break;
                case BusRoute.COMPANY_NLB:
                    companyName = getString(R.string.provider_short_nlb);
                    break;
                case BusRoute.COMPANY_NWFB:
                    companyName = getString(R.string.provider_short_nwfb);
                    break;
                case BusRoute.COMPANY_NWST:
                    companyName = getString(R.string.provider_short_nwst);
                    break;
                default:
                    companyName = getString(R.string.route);
            }
            String routeName = companyName + " " + routeNo;
            getSupportActionBar().setTitle(routeName);
        }
        if (isScrollToPage) {
            viewPager.setCurrentItem(fragNo, false);
        }
        if (pagerAdapter.getCount() > 0) {
            getContentResolver().insert(SuggestionProvider.CONTENT_URI,
                    SearchHistoryUtil.toContentValues(
                            SearchHistoryUtil.createInstance(routeNo, companyCode)));
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
            viewPager.setOffscreenPageLimit(Math.min(pagerAdapter.getCount(), 10));
        } else {
            showEmptyView();
        }
    }
}
