package com.alvinhkh.buseta.lwb.ui;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
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
import com.alvinhkh.buseta.model.SearchHistory;
import com.alvinhkh.buseta.lwb.LwbService;
import com.alvinhkh.buseta.lwb.model.LwbRouteBound;
import com.alvinhkh.buseta.lwb.model.network.LwbRouteBoundRes;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.ui.route.RoutePagerAdapter;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.google.android.gms.ads.AdView;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LwbActivity extends BaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final LwbService lwbService = LwbService.retrofit.create(LwbService.class);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private AdView adView;

    private FrameLayout adViewContainer;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    public RoutePagerAdapter pagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    public ViewPager viewPager;

    private FloatingActionButton fab;

    private View emptyView;

    private ProgressBar progressBar;

    private TextView emptyText;

    private BusRouteStop stopFromIntent;
    
    private String routeNo;

    private Fragment currentFragment;

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
            adView = AdViewUtil.banner(adViewContainer, adView);
        }
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (currentFragment != null) {
                if (currentFragment instanceof LwbStopListFragment) {
                    LwbStopListFragment f = (LwbStopListFragment) currentFragment;
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

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
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
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        if (adView != null) {
            adView.pause();
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    private void showEmptyView() {
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

    private void showLoadingView() {
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

    private void loadRouteNo(String no) {
        if (TextUtils.isEmpty(no)) {
            showEmptyView();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(no);
        }
        showLoadingView();

        disposables.add(lwbService.getRouteBound(no, Math.random())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(getRouteBoundObserver()));
    }

    DisposableObserver<LwbRouteBoundRes> getRouteBoundObserver() {
        return new DisposableObserver<LwbRouteBoundRes>() {
            @Override
            public void onNext(LwbRouteBoundRes res) {
                if (res != null && res.bus_arr != null) {
                    pagerAdapter.setRoute(routeNo);
                    pagerAdapter.clearSequence();
                    int i = 1;
                    for (LwbRouteBound bound : res.bus_arr) {
                        if (bound == null) continue;
                        BusRoute busRoute = new BusRoute();
                        busRoute.setCompanyCode(BusRoute.COMPANY_KMB);
                        busRoute.setLocationEndName(bound.destination_tc);
                        busRoute.setLocationStartName(bound.origin_tc);
                        busRoute.setName(routeNo);
                        busRoute.setSequence(String.valueOf(i++));
                        pagerAdapter.addSequence(busRoute);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                showEmptyView();
                if (emptyText != null) {
                    if (!ConnectivityUtil.isConnected(getApplicationContext())) {
                        emptyText.setText(R.string.message_no_internet_connection);
                    } else {
                        emptyText.setText(R.string.message_fail_to_request);
                    }
                }
            }

            @Override
            public void onComplete() {
                if (stopFromIntent != null) {
                    viewPager.setCurrentItem(Integer.parseInt(stopFromIntent.direction) - 1, false);
                }
                if (pagerAdapter.getCount() > 0) {
                    // TODO: SearchHistory
                    ContentValues values = new ContentValues();
                    values.put(SuggestionTable.COLUMN_TEXT, routeNo);
                    values.put(SuggestionTable.COLUMN_COMPANY, "KMB");
                    values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_HISTORY);
                    values.put(SuggestionTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
                    getContentResolver().insert(SuggestionProvider.CONTENT_URI, values);

                    SearchHistory history = new SearchHistory();
                    history.route = routeNo;
                    history.recordType = SuggestionTable.TYPE_HISTORY;
                    history.company = "KMB";
                    // getContentResolver().insert(SuggestionProvider.CONTENT_URI, SearchHistoryUtil.toContentValues(history));

                    if (emptyView != null) {
                        emptyView.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyView();
                }
            }
        };
    }
}
