package com.alvinhkh.buseta.ui.route;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.DataSetObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
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
import com.alvinhkh.buseta.follow.dao.FollowDatabase;
import com.alvinhkh.buseta.follow.model.Follow;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.search.dao.SuggestionDatabase;
import com.alvinhkh.buseta.search.model.Suggestion;
import com.alvinhkh.buseta.search.ui.SearchActivity;
import com.alvinhkh.buseta.ui.BaseActivity;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.RouteUtil;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.maps.MapView;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public abstract class RouteActivityAbstract extends BaseActivity {

    protected final CompositeDisposable disposables = new CompositeDisposable();

    protected static FollowDatabase followDatabase = null;

    protected static SuggestionDatabase suggestionDatabase = null;

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

    private Suggestion suggestion = null;

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
                routeNo = stopFromIntent.getRouteNo();
            }
        }

        followDatabase = FollowDatabase.Companion.getInstance(this);
        suggestionDatabase = SuggestionDatabase.Companion.getInstance(this);

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
    public void onPause() {
        super.onPause();
        updateAppShortcuts();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        if (suggestion != null) {
            appIndexStop(suggestion);
        }
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
        pagerAdapter.clearSequence();
        for (Route route : routes) {
            if (route == null) continue;
            if (TextUtils.isEmpty(route.getName()) || !route.getName().equals(routeNo)) continue;
            companyCode = route.getCompanyCode();
            pagerAdapter.addSequence(route);
            if (stopFromIntent != null && route.getCompanyCode() != null
                    && route.getSequence() != null && route.getServiceType() != null
                    && route.getCompanyCode().equals(stopFromIntent.getCompanyCode())
                    && route.getSequence().equals(stopFromIntent.getRouteSeq())
                    && route.getServiceType().equals(stopFromIntent.getRouteServiceType())
            ) {
                fragNo = pagerAdapter.getCount();
                isScrollToPage = true;
            }
        }
        if (getSupportActionBar() != null) {
            String routeName = RouteUtil.getCompanyName(this, companyCode, routeNo) + " " + routeNo;
            getSupportActionBar().setTitle(routeName);
        }
        if (routes.size() > 0 && !TextUtils.isEmpty(companyCode)) {
            suggestion = Suggestion.Companion.createInstance();
            suggestion.setCompanyCode(companyCode);
            suggestion.setRoute(routeNo);
            suggestion.setType(Suggestion.TYPE_HISTORY);
            if (suggestionDatabase != null) {
                suggestionDatabase.suggestionDao().insert(suggestion);
            }
            appIndexStart(suggestion);
        }

        if (isScrollToPage) {
            if (viewPager != null && fragNo - 1 >= 0) {
                viewPager.setCurrentItem(fragNo - 1, false);
            }
            isScrollToPage = false;
        }
    }

    public Action getIndexApiAction(@NonNull Suggestion suggestion) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(suggestion.getRoute(), Uri.parse(C.URI.ROUTE).buildUpon()
                        .appendPath(suggestion.getRoute()).build().toString())
                // Keep action data for personal content on the device
                //.setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private void appIndexStart(@NonNull Suggestion suggestion) {
        if (TextUtils.isEmpty(suggestion.getRoute())) return;
        FirebaseUserActions.getInstance().start(getIndexApiAction(suggestion))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Timber.d("App Indexing: Recorded start successfully");
                    } else {
                        Timber.d("App Indexing: fail");
                    }
                });
        Answers.getInstance().logContentView(new ContentViewEvent()
                .putContentName("search")
                .putContentType("route")
                .putCustomAttribute("route no", suggestion.getRoute())
                .putCustomAttribute("company", suggestion.getCompanyCode())
        );
    }

    private void appIndexStop(@NonNull Suggestion suggestion) {
        if (TextUtils.isEmpty(routeNo)) return;
        FirebaseUserActions.getInstance().end(getIndexApiAction(suggestion))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Timber.d("App Indexing: Recorded end successfully");
                    } else {
                        Timber.d("App Indexing: fail");
                    }
                });
    }

    private void updateAppShortcuts() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
        if (getApplicationContext() == null) return;
        // Dynamic App Shortcut
        try {
            ShortcutManager shortcutManager = getApplicationContext().getSystemService(ShortcutManager.class);
            if (shortcutManager == null) return;
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<>();
            List<Follow> followList = followDatabase.followDao().getList();
            Integer maxShortcutCount = shortcutManager.getMaxShortcutCountPerActivity();
            for (int i = 0; i < maxShortcutCount-1 && i < followList.size(); i++) {
                Follow follow = followList.get(i);
                RouteStop routeStop = RouteStopUtil.fromFollow(follow);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(getApplicationContext(), SearchActivity.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT_STRING, new Gson().toJson(routeStop));
                shortcuts.add(new ShortcutInfo.Builder(getApplicationContext(), "buseta-" + routeStop.getCompanyCode() + routeStop.getRouteNo() + routeStop.getRouteSeq() + routeStop.getStopId())
                        .setShortLabel(routeStop.getRouteNo() + " " + routeStop.getName())
                        .setLongLabel(routeStop.getRouteNo() + " " + routeStop.getName() + " " + getString(R.string.destination, routeStop.getRouteDestination()))
                        .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_shortcut_directions_bus))
                        .setIntent(intent)
                        .build());
            }
            if (followList.size() < maxShortcutCount - 1) {
                List<Suggestion> historyList = suggestionDatabase.suggestionDao().historyList(maxShortcutCount);
                for (int i = 0; i < maxShortcutCount-followList.size()-1 && i < historyList.size(); i++) {
                    Suggestion suggestion = historyList.get(i);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClass(getApplicationContext(), SearchActivity.class);
                    intent.putExtra(C.EXTRA.ROUTE_NO, suggestion.getRoute());
                    intent.putExtra(C.EXTRA.COMPANY_CODE, suggestion.getCompanyCode());
                    shortcuts.add(new ShortcutInfo.Builder(getApplicationContext(), "buseta-q-" + suggestion.getCompanyCode() + suggestion.getRoute())
                            .setShortLabel(suggestion.getRoute())
                            .setLongLabel(suggestion.getRoute())
                            .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_shortcut_search))
                            .setIntent(intent)
                            .build());
                }
            }
            shortcutManager.setDynamicShortcuts(shortcuts);
        } catch (NoClassDefFoundError|NoSuchMethodError ignored) {}
    }
}
