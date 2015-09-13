package com.alvinhkh.buseta.preference;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;

import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.Utils;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.UpdateSuggestionService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static final String TAG = "SettingsActivity";

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTaskDescription(getString(R.string.app_name));
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .commit();

        createAdView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        createAdView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != mAdView)
            mAdView.resume();
    }

    @Override
    protected void onPause() {
        if (null != mAdView)
            mAdView.pause();
        super.onPause();
    }

    private void createAdView() {
        // Admob
        final FrameLayout adViewContainer = (FrameLayout) findViewById(R.id.adView_container);
        adViewContainer.setVisibility(View.GONE);
        if (null != mAdView) {
            mAdView.destroy();
            mAdView.setVisibility(View.GONE);
        }
        boolean hideAdView = false;
        if (!hideAdView) {
            mAdView = new AdView(this);
            mAdView.setAdUnitId(getString(R.string.ad_banner_unit_id));
            mAdView.setAdSize(AdSize.SMART_BANNER);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    adViewContainer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    adViewContainer.setVisibility(View.GONE);
                }
            });
            adViewContainer.addView(mAdView);
            AdRequest mAdRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)  // All emulators
                    .addTestDevice(getString(R.string.ad_test_device))
                    .build();
            mAdView.loadAd(mAdRequest);
            // get device md5 id
            String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = Utils.md5(android_id).toUpperCase();
            boolean isTestDevice = mAdRequest.isTestDevice(this);
            Log.v(TAG, "is Admob Test Device ? " + deviceId + " " + isTestDevice);
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private Activity mActivity;
        private UpdateSuggestionReceiver mReceiver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mActivity = super.getActivity();
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            // Set Default values from XML attribute
            PreferenceManager.setDefaultValues(mActivity, R.xml.preferences, false);
            // Set Summary
            initSummary(getPreferenceScreen());
            // Broadcast Receiver
            IntentFilter mFilter = new IntentFilter(Constants.ROUTES.SUGGESTION_UPDATE);
            mReceiver = new UpdateSuggestionReceiver();
            mFilter.addAction(Constants.ROUTES.SUGGESTION_UPDATE);
            mActivity.registerReceiver(mReceiver, mFilter);
            // Clear History
            Preference clearHistory = getPreferenceScreen().findPreference("clear_history");
            clearHistory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(mActivity.getString(R.string.message_confirm_clear_search_history))
                            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    dialoginterface.cancel();
                                }
                            })
                            .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    Intent intent = new Intent(Constants.MESSAGE.HISTORY_UPDATED);
                                    intent.putExtra(Constants.MESSAGE.HISTORY_UPDATED, true);
                                    mActivity.sendBroadcast(intent);
                                    int rowDeleted =
                                            mActivity.getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                                                    SuggestionTable.COLUMN_TYPE + "=?",
                                                    new String[]{SuggestionTable.TYPE_HISTORY});
                                    Snackbar snackbar = Snackbar.make(
                                            mActivity.findViewById(android.R.id.content),
                                            rowDeleted > 0 ?
                                                    R.string.message_clear_search_history_success :
                                                    R.string.message_clear_search_history_fail,
                                            Snackbar.LENGTH_SHORT);
                                    TextView tv = (TextView)
                                            snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    snackbar.show();
                                }
                            })
                            .show();
                    return true;
                }
            });
            // update suggestions
            Preference updateSuggestion = getPreferenceScreen().findPreference("update_route_suggestion");
            updateSuggestion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(mActivity, UpdateSuggestionService.class);
                    mActivity.startService(intent);
                    return true;
                }
            });
            PreferenceCategory categoryDebug = (PreferenceCategory) findPreference("category_debug");
            // hide ad
            SwitchPreference hideAd = (SwitchPreference) getPreferenceScreen().findPreference(Constants.PREF.AD_HIDE);
            hideAd.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SwitchPreference switchPref = (SwitchPreference) preference;
                    if (switchPref.isChecked()) {
                        final Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                R.string.message_hide_ad, Snackbar.LENGTH_LONG);
                        TextView tv = (TextView)
                                snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        snackbar.show();
                    }
                    return true;
                }
            });
            categoryDebug.removePreference(hideAd); // comment this line to show the preference
            // App Name
            Preference appName = getPreferenceScreen().findPreference("app_name");
            appName.setTitle(getString(R.string.title_app_name, getString(R.string.app_name)));
            // App Version
            Preference appVersion = getPreferenceScreen().findPreference("app_version");
            int versionCode = BuildConfig.VERSION_CODE;
            String versionName = BuildConfig.VERSION_NAME;
            appVersion.setSummary(getString(R.string.summary_app_version, versionName, versionCode));
            // hide ad tips
            appVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int clickCounter = 0;
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickCounter++;
                    if (clickCounter == 3 || clickCounter == 6 || clickCounter >= 10) {
                        String msg;
                        if (clickCounter >= 10)
                            msg = getString(R.string.message_hide_ad_tip, Constants.PREF.AD_KEY, Constants.PREF.AD_SHOW);
                        else
                            msg = getString(R.string.message_hide_ad_smile);
                        final Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                msg, Snackbar.LENGTH_INDEFINITE);
                        TextView tv = (TextView)
                                snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        snackbar.show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (snackbar == null) return;
                                snackbar.dismiss();
                            }
                        }, 6000);
                    }
                    return true;
                }
            });
            // Developer
            Preference developer = getPreferenceScreen().findPreference("developer");
            developer.setSummary(getString(R.string.summary_developer) + " (" + getString(R.string.url_developer) + ")");
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(mActivity).registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister the listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(mActivity).unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            if (null != mReceiver)
                mActivity.unregisterReceiver(mReceiver);
            super.onDestroy();
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
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
            if (p instanceof EditTextPreference) {
                EditTextPreference editTextPref = (EditTextPreference) p;
                p.setSummary(editTextPref.getText());
            }
            /*
            if (p instanceof SwitchPreference) {
                SwitchPreference switchPref = (SwitchPreference) p;
                p.setSummary(switchPref.isChecked() ?
                        getString(R.string.enabled) :
                        getString(R.string.disabled));
            }*/
            if (p instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPref = (CheckBoxPreference) p;
                p.setSummary(checkBoxPref.isChecked() ?
                        getString(R.string.enabled) :
                        getString(R.string.disabled));
            }
            if (p instanceof ListPreference) {
                ListPreference listPref = (ListPreference) p;
                p.setSummary(listPref.getEntry());
            }
        }

        public class UpdateSuggestionReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                Boolean aBoolean = bundle.getBoolean(Constants.ROUTES.SUGGESTION_UPDATE);
                if (aBoolean) {
                    int resourceId = bundle.getInt(Constants.ROUTES.MESSAGE_ID);
                    String name = getResources().getResourceName(resourceId);
                    if (name != null && name.startsWith(mActivity.getPackageName())) {
                        final Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                resourceId, Snackbar.LENGTH_LONG);
                        TextView tv = (TextView)
                                snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        if (resourceId == R.string.message_database_updating)
                            snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                        else if (resourceId == R.string.message_database_updated) {
                            Cursor cursor = mActivity.getContentResolver().query(SuggestionProvider.CONTENT_URI,
                                    null, SuggestionTable.COLUMN_TEXT + " LIKE '%%'" + " AND " +
                                            SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_DEFAULT + "'",
                                    null, null);
                            snackbar.setText(getString(resourceId) + " " +
                                    getString(R.string.message_total_routes, cursor == null ? 0 : cursor.getCount()));
                            if (cursor != null)
                                cursor.close();
                        }
                        snackbar.show();
                    }
                }
            }
        }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setTaskDescription(String title) {
        // overview task
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(title, bm,
                            ContextCompat.getColor(this, R.color.primary_600));
            setTaskDescription(taskDesc);
        }
    }

}