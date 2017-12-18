package com.alvinhkh.buseta.nwst.ui;

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
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.ui.route.RoutePagerAdapter;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.BusRouteUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;
import com.alvinhkh.buseta.utils.SearchHistoryUtil;
import com.google.android.gms.ads.AdView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class NwstActivity extends BaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

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

    private int pageNo = 0;

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
                if (currentFragment instanceof NwstStopListFragment) {
                    NwstStopListFragment f = (NwstStopListFragment) currentFragment;
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
        loadRouteNo(no, TYPE_ALL_ROUTES);
    }

    private void loadRouteNo(String no, String mode) {
        if (TextUtils.isEmpty(no)) {
            showEmptyView();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(String.format("%s %s", getString(R.string.provider_short_nwst), no));
        }
        showLoadingView();

        Map<String, String> options = new HashMap<>();
        options.put(QUERY_ROUTE_NO, mode.equals(TYPE_ALL_ROUTES) ? "" : no);
        options.put(QUERY_MODE, mode);
        options.put(QUERY_LANGUAGE, LANGUAGE_TC);
        options.put(QUERY_PLATFORM, PLATFORM);
        options.put(QUERY_APP_VERSION, APP_VERSION);
        options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
        disposables.add(nwstService.routeList(options)
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeListObserver(no)));
    }

    DisposableObserver<ResponseBody> routeListObserver(String routeNo) {

        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                try {
                    String[] routes = body.string().split("\\|\\*\\|", -1);
                    Map<String, String> options;
                    String sysCode = NwstRequestUtil.syscode();
                    for (String route : routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstRoute nwstRoute = NwstRoute.Companion.fromString(text);
                        if (nwstRoute != null &&
                                !TextUtils.isEmpty(nwstRoute.getRouteNo()) &&
                                nwstRoute.getRouteNo().equals(routeNo)) {
                            options = new HashMap<>();
                            options.put(QUERY_ID, nwstRoute.getRdv());
                            options.put(QUERY_LANGUAGE, LANGUAGE_TC);
                            options.put(QUERY_PLATFORM, PLATFORM);
                            options.put(QUERY_APP_VERSION, APP_VERSION);
                            options.put(QUERY_SYSCODE, sysCode);
                            disposables.add(nwstService.variantList(options)
                                    .retryWhen(new RetryWithDelay(5, 3000))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeWith(variantListObserver(nwstRoute)));
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
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
                pagerAdapter.setRoute(routeNo);
            }
        };
    }

    DisposableObserver<ResponseBody> variantListObserver(NwstRoute nwstRoute) {
        return new DisposableObserver<ResponseBody>() {

            String companyCode = BusRoute.COMPANY_NWST;

            Boolean isScrollToPage = false;

            @Override
            public void onNext(ResponseBody body) {
                try {
                    String[] routes = body.string().split("\\|\\*\\|", -1);
                    for (String route: routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstVariant variant = NwstVariant.Companion.fromString(text);
                        BusRoute busRoute = BusRouteUtil.fromNwst(nwstRoute, variant);
                        if (busRoute.getName().equals(routeNo)) {
                            companyCode = busRoute.getCompanyCode();
                            if (getSupportActionBar() != null) {
                                String companyName;
                                switch (busRoute.getCompanyCode()) {
                                    case BusRoute.COMPANY_CTB:
                                        companyName = getString(R.string.provider_short_ctb);
                                        break;
                                    case BusRoute.COMPANY_NWFB:
                                        companyName = getString(R.string.provider_short_nwfb);
                                        break;
                                    default:
                                        companyName = getString(R.string.provider_short_nwst);
                                        break;
                                }
                                getSupportActionBar().setTitle(String.format("%s %s", companyName, busRoute.getName()));
                            }
                            pagerAdapter.addSequence(busRoute);
                            if (stopFromIntent != null && busRoute.getSequence().equals(stopFromIntent.direction)) {
                                pageNo = pagerAdapter.getCount();
                                isScrollToPage = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
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
                if (isScrollToPage) viewPager.setCurrentItem(pageNo, false);
                if (pagerAdapter.getCount() > 0) {
                    getContentResolver().insert(SuggestionProvider.CONTENT_URI,
                            SearchHistoryUtil.toContentValues(
                                    SearchHistoryUtil.createInstance(routeNo, companyCode)));
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
