package com.alvinhkh.buseta.ui.setting;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import android.view.View;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase;
import com.alvinhkh.buseta.follow.dao.FollowDatabase;
import com.alvinhkh.buseta.model.AppUpdate;
import com.alvinhkh.buseta.route.dao.RouteDatabase;
import com.alvinhkh.buseta.search.dao.SuggestionDatabase;
import com.alvinhkh.buseta.service.ProviderUpdateService;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.utils.PreferenceUtil;

import androidx.work.WorkManager;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

public class SettingActivity extends BasePreferenceActivity {

    private final static int PERMISSIONS_REQUEST_ACCESS_LOCATION = 10;

    private final CompositeDisposable disposables = new CompositeDisposable();

    private static ArrivalTimeDatabase arrivalTimeDatabase = null;

    private static FollowDatabase followDatabase = null;

    private static RouteDatabase routeDatabase = null;

    private static SuggestionDatabase suggestionDatabase = null;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        arrivalTimeDatabase = ArrivalTimeDatabase.Companion.getInstance(this);
        followDatabase = FollowDatabase.Companion.getInstance(this);
        routeDatabase = RouteDatabase.Companion.getInstance(this);
        suggestionDatabase = SuggestionDatabase.Companion.getInstance(this);
        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .commit();
        disposables.add(RxBroadcastReceiver.create(this,
                new IntentFilter(C.ACTION.APP_UPDATE))
                .share().subscribeWith(appUpdateObserver()));
        disposables.add(RxBroadcastReceiver.create(this,
                new IntentFilter(C.ACTION.SUGGESTION_ROUTE_UPDATE))
                .share().subscribeWith(suggestionRouteUpdateObserver()));
    }


    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        private Activity mActivity;

        private SwitchPreferenceCompat locationPermission;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            mActivity = super.getActivity();
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            // Set Default values from XML attribute
            PreferenceManager.setDefaultValues(mActivity, R.xml.preferences, false);
            // Set Summary
            initSummary(getPreferenceScreen());
            // Clear History
            Preference clearHistory = getPreferenceScreen().findPreference("clear_history");
            clearHistory.setOnPreferenceClickListener(this);
            // Clear Follow / Clear All Route Data
            Preference clearFollow = getPreferenceScreen().findPreference("clear_follow");
            clearFollow.setOnPreferenceClickListener(this);
            // Clear route data
            Preference clearCached = getPreferenceScreen().findPreference("clear_cached_route");
            clearCached.setOnPreferenceClickListener(this);
            // Permission - Location
            locationPermission = (SwitchPreferenceCompat) getPreferenceScreen().findPreference("location_permission");
            if (locationPermission != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    locationPermission.setEnabled(false);
                    locationPermission.setChecked(true);
                } else {
                    locationPermission.setOnPreferenceClickListener(this);
                    Boolean granted = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    locationPermission.setChecked(granted);
                }
            }
            // App Name
            Preference appName = getPreferenceScreen().findPreference("app_name");
            appName.setTitle(getString(R.string.title_app_name, getString(R.string.app_name)));
            // App Version
            Preference appVersion = getPreferenceScreen().findPreference("app_version");
            final int versionCode = BuildConfig.VERSION_CODE;
            final String versionName = BuildConfig.VERSION_NAME;
            appVersion.setSummary(versionName + " (" + versionCode +")");
            // Developer
            Preference developer = getPreferenceScreen().findPreference("developer");
            developer.setSummary(getString(R.string.summary_developer) + " (" + getString(R.string.url_developer) + ")");
            //
            Preference shareApp = getPreferenceScreen().findPreference("share_app");
            shareApp.setOnPreferenceClickListener(this);
            // hide ad
            SwitchPreferenceCompat hideAd = (SwitchPreferenceCompat) getPreferenceScreen().findPreference(C.PREF.AD_HIDE);
            if (hideAd != null) {
                hideAd.setVisible(false);
                hideAd.setOnPreferenceClickListener(preference -> {
                    SwitchPreferenceCompat switchPref = (SwitchPreferenceCompat) preference;
                    if (switchPref.isChecked()) {
                        final Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                R.string.message_hide_ad, Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                    return true;
                });
            }
            //
            Preference nwstApi = getPreferenceScreen().findPreference("nwst_api");
            nwstApi.setOnPreferenceChangeListener(this);
            //
            Preference checkAppUpdate = getPreferenceScreen().findPreference("check_app_update");
            checkAppUpdate.setOnPreferenceClickListener(this);

            // Set up a listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(mActivity)
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            // hide divider
            setDivider(new ColorDrawable(Color.TRANSPARENT));
            setDividerHeight(0);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            // Unregister the listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(mActivity)
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case "nwst_api": {
                    clearCache();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case "clear_follow": {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(mActivity.getString(R.string.message_confirm_clear_follow))
                            .setNegativeButton(R.string.action_cancel, (dialoginterface, i) -> dialoginterface.cancel())
                            .setPositiveButton(R.string.action_confirm, (dialoginterface, i) -> {
                                if (mActivity != null) {
                                    int rowDeleted = 0;
                                    if (followDatabase != null) {
                                        rowDeleted = followDatabase.followDao().clear();
                                    }
                                    Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                            rowDeleted > 0
                                                    ? R.string.message_clear_success_search_history
                                                    : R.string.message_clear_fail_search_history,
                                            Snackbar.LENGTH_SHORT);
                                    snackbar.show();
                                }
                            })
                            .show();
                    return true;
                }
                case "clear_history": {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(mActivity.getString(R.string.message_confirm_clear_search_history))
                            .setNegativeButton(R.string.action_cancel, (dialoginterface, i) -> dialoginterface.cancel())
                            .setPositiveButton(R.string.action_confirm, (dialoginterface, i) -> {
                                if (mActivity != null) {
                                    int rowDeleted = 0;
                                    if (suggestionDatabase != null) {
                                        WorkManager.getInstance().cancelAllWorkByTag("FollowRouteWorker");
                                        rowDeleted = suggestionDatabase.suggestionDao().clearHistory();
                                    }
                                    Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                            rowDeleted > 0
                                                    ? R.string.message_clear_success_search_history
                                                    : R.string.message_clear_fail_search_history,
                                            Snackbar.LENGTH_SHORT);
                                    snackbar.show();
                                }
                            })
                            .show();
                    return true;
                }
                case "clear_cached_route": {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(mActivity.getString(R.string.message_confirm_clear_route))
                            .setNegativeButton(R.string.action_cancel, (dialoginterface, i) -> dialoginterface.cancel())
                            .setPositiveButton(R.string.action_confirm, (dialoginterface, i) -> {
                                clearCache();
                            })
                            .show();
                    return true;
                }
                case "location_permission": {
                    if (ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        locationPermission.setChecked(false);
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_REQUEST_ACCESS_LOCATION);
                    } else {
                        locationPermission.setChecked(true);
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
                    }
                    return true;
                }
                case "share_app": {
                    startActivity(Intent.createChooser(PreferenceUtil.INSTANCE.shareAppIntent(getContext()), getString(R.string.message_share_text)));
                    return true;
                }
                case "check_app_update": {
                    Intent intent = new Intent(mActivity, ProviderUpdateService.class);
                    intent.putExtra(C.EXTRA.MANUAL, true);
                    mActivity.startService(intent);
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String permissions[],
                                               @NonNull int[] grantResults) {
            switch (requestCode) {
                case PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                    // If request is cancelled, the result arrays are empty.
                    if (locationPermission != null) {
                        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                            locationPermission.setChecked(true);
                        } else {
                            locationPermission.setChecked(false);
                        }
                    }
                    break;
                }
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePrefSummary(findPreference(key));
        }

        private void initSummary(Preference p) {
            if (p instanceof PreferenceGroup) {
                PreferenceGroup pGrp = (PreferenceGroup) p;
                for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                    initSummary(pGrp.getPreference(i));
                }
            } else {
                updatePrefSummary(p);
            }
        }

        private void updatePrefSummary(Preference p) {
            if (p instanceof ListPreference) {
                ListPreference listPref = (ListPreference) p;
                p.setSummary(listPref.getEntry());
            }
        }

        private void clearCache() {
            if (mActivity != null) {
                WorkManager.getInstance().cancelAllWork();
                int rowDeleted = 0;
                if (arrivalTimeDatabase != null) {
                    arrivalTimeDatabase.arrivalTimeDao().clear();
                }
                if (routeDatabase != null) {
                    rowDeleted = routeDatabase.routeDao().clear();
                    rowDeleted += routeDatabase.routeStopDao().clear();
                    try {
                        Intent intent = new Intent(mActivity, ProviderUpdateService.class);
                        intent.putExtra(C.EXTRA.MANUAL, true);
                        mActivity.startService(intent);
                    } catch (Exception ignored) {
                    }
                }
                Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                        rowDeleted > 0
                                ? R.string.message_clear_success_route
                                : R.string.message_clear_fail_route,
                        Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        }
    }

    DisposableObserver<Intent> suggestionRouteUpdateObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Boolean isManual = bundle.getBoolean(C.EXTRA.MANUAL, false);
                Boolean isUpdated = bundle.getBoolean(C.EXTRA.UPDATED, false);
                if (isUpdated && isManual) {
                    int messageId = bundle.getInt(C.EXTRA.MESSAGE_RID);
                    String name = getResources().getResourceName(messageId);
                    if (name != null && name.startsWith(getPackageName())) {
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                                messageId, Snackbar.LENGTH_LONG);
                        if (messageId == R.string.message_database_updating) {
                            snackbar.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
                        } else if (messageId == R.string.message_database_updated) {
                            int count = 0;
                            if (suggestionDatabase != null) {
                                count = suggestionDatabase.suggestionDao().countDefault();
                            }
                            snackbar.setText(getString(messageId) + " " +
                                    getString(R.string.message_total_routes, String.valueOf(count)));
                        }
                        snackbar.show();
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    DisposableObserver<Intent> appUpdateObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Boolean isUpdated = bundle.getBoolean(C.EXTRA.UPDATED, false);
                Boolean isManual = bundle.getBoolean(C.EXTRA.MANUAL, false);
                AppUpdate appUpdate = bundle.getParcelable(C.EXTRA.APP_UPDATE_OBJECT);
                if (isUpdated && appUpdate != null) {
                    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    int oVersionCode = mPrefs.getInt(C.PREF.APP_UPDATE_VERSION, BuildConfig.VERSION_CODE);
                    StringBuilder message = new StringBuilder();
                    message.append(appUpdate.updated);
                    message.append("\n");
                    message.append(appUpdate.content);
                    Timber.d("AppVersion:%s DB:%s APK:%s ", appUpdate.version_code, oVersionCode, BuildConfig.VERSION_CODE);
                    if ((isManual && appUpdate.version_code >= BuildConfig.VERSION_CODE)
                            || appUpdate.notify && (appUpdate.version_code > oVersionCode
                            || (appUpdate.force && appUpdate.version_code > BuildConfig.VERSION_CODE))) {
                        Boolean isInstalled = appUpdate.version_code <= BuildConfig.VERSION_CODE;
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                                .setTitle(appUpdate.version_name)
                                .setMessage(message)
                                .setNegativeButton(R.string.action_cancel, (dialoginterface, i) -> dialoginterface.cancel());
                        if (!isInstalled) {
                            alertDialog.setTitle(getString(R.string.message_app_update, appUpdate.version_name));
                            alertDialog.setPositiveButton(R.string.action_update, (dialoginterface, i) -> {
                                        Uri link = Uri.parse(appUpdate.url);
                                        if (null == link) {
                                            link = Uri.parse(getString(R.string.url_app));
                                        }
                                        Intent intent1 = new Intent(Intent.ACTION_VIEW, link);
                                        if (intent1.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent1);
                                        }
                                    });
                        }
                        alertDialog.show();
                    }
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putInt(C.PREF.APP_UPDATE_VERSION, appUpdate.version_code);
                    editor.apply();
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
            }
        };
    }
}