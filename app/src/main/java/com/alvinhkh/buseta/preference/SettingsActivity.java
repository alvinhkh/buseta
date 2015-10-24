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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.Toast;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.Utils;
import com.alvinhkh.buseta.holder.AppUpdate;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.CheckUpdateService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.koushikdutta.ion.Ion;

import java.io.File;
import java.io.IOException;

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
        private CheckUpdateReceiver mReceiver;

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
            IntentFilter mFilter = new IntentFilter(Constants.MESSAGE.CHECKING_UPDATED);
            mReceiver = new CheckUpdateReceiver();
            mFilter.addAction(Constants.MESSAGE.CHECKING_UPDATED);
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
                                    if (null == mActivity) return;
                                    int rowDeleted =
                                            mActivity.getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                                                    SuggestionTable.COLUMN_TYPE + "=?",
                                                    new String[]{SuggestionTable.TYPE_HISTORY});
                                    Snackbar snackbar = Snackbar.make(
                                            mActivity.findViewById(android.R.id.content),
                                            rowDeleted > 0 ?
                                                    R.string.message_clear_success_search_history :
                                                    R.string.message_clear_fail_search_history,
                                            Snackbar.LENGTH_SHORT);
                                    TextView tv = (TextView)
                                            snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    snackbar.show();
                                    Intent intent = new Intent(Constants.MESSAGE.HISTORY_UPDATED);
                                    intent.putExtra(Constants.MESSAGE.HISTORY_UPDATED, true);
                                    mActivity.sendBroadcast(intent);
                                }
                            })
                            .show();
                    return true;
                }
            });
            // Clear Follow / Clear All Route Data
            Preference clearFollow = getPreferenceScreen().findPreference("clear_follow");
            clearFollow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(mActivity)
                            .setTitle(mActivity.getString(R.string.message_confirm_clear_follow))
                            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    dialoginterface.cancel();
                                }
                            })
                            .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    if (null == mActivity) return;
                                    // delete all follow
                                    int rowDeleted = mActivity.getContentResolver().delete(
                                            FollowProvider.CONTENT_URI_FOLLOW, null, null);
                                    // delete all bound, stop, eta
                                    mActivity.getContentResolver().delete(
                                            RouteProvider.CONTENT_URI_BOUND, null, null);
                                    mActivity.getContentResolver().delete(
                                            RouteProvider.CONTENT_URI_STOP, null, null);
                                    mActivity.getContentResolver().delete(
                                            FollowProvider.CONTENT_URI_ETA, null, null);
                                    Snackbar snackbar = Snackbar.make(
                                            mActivity.findViewById(android.R.id.content),
                                            rowDeleted > 0 ?
                                                    R.string.message_clear_success_follow :
                                                    R.string.message_clear_fail_follow,
                                            Snackbar.LENGTH_SHORT);
                                    // Clear All Ion Request
                                    Ion.getDefault(mActivity.getBaseContext()).cancelAll(mActivity.getBaseContext());
                                    Ion.getDefault(mActivity.getBaseContext()).getCache().clear();
                                    Ion.getDefault(mActivity.getBaseContext()).getBitmapCache().clear();
                                    TextView tv = (TextView)
                                            snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                                    tv.setTextColor(Color.WHITE);
                                    snackbar.show();
                                    Intent intent = new Intent(Constants.MESSAGE.FOLLOW_UPDATED);
                                    intent.putExtra(Constants.MESSAGE.FOLLOW_UPDATED, true);
                                    mActivity.sendBroadcast(intent);
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
                    Intent intent = new Intent(mActivity, CheckUpdateService.class);
                    intent.putExtra(Constants.MESSAGE.SUGGESTION_FORCE_UPDATE, true);
                    mActivity.startService(intent);
                    return true;
                }
            });
            // send logcat
            Preference sendLogcat = getPreferenceScreen().findPreference("send_logcat");
            sendLogcat.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sendLogcat();
                    return true;
                }
            });
            // check app update
            Preference appUpdate = getPreferenceScreen().findPreference("check_app_update");
            appUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(mActivity, CheckUpdateService.class);
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
            final int versionCode = BuildConfig.VERSION_CODE;
            final String versionName = BuildConfig.VERSION_NAME;
            appVersion.setSummary(versionName);
            // hide ad tips
            appVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int clickCounter = 0;
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickCounter++;
                    if (clickCounter % 3 == 0) {
                        String msg;
                        if (clickCounter > 10)
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
                                snackbar.dismiss();
                            }
                        }, 6000);
                    } else {
                        Toast.makeText(mActivity, getString(R.string.title_app_version) + " " +
                                getString(R.string.summary_app_version, versionName, versionCode),
                                Toast.LENGTH_SHORT).show();
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

        public void sendLogcat() {
            // save logcat in file
            File outputFile = new File(Environment.getExternalStorageDirectory(), "buseta-logcat.txt");
            try {
                Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            // send file using email
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                    getString(R.string.email_developer)
            });
            // the attachment
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + outputFile.getAbsolutePath()));
            // the mail subject
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
            startActivity(Intent.createChooser(emailIntent , getString(R.string.action_send_email)));
        }

        public class CheckUpdateReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Bundle bundle = intent.getExtras();
                final Boolean aBoolean_suggestion =
                        bundle.getBoolean(Constants.STATUS.UPDATED_SUGGESTION, false);
                final Boolean aBoolean_app =
                        bundle.getBoolean(Constants.STATUS.UPDATED_APP_FOUND, false);
                if (aBoolean_suggestion) {
                    int resourceId = bundle.getInt(Constants.BUNDLE.MESSAGE_ID);
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

                if (aBoolean_app) {
                    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                    final AppUpdate appUpdate = bundle.getParcelable(Constants.BUNDLE.APP_UPDATE_OBJECT);
                    if (null != appUpdate) {
                        final int versionCode = appUpdate.version_code;
                        final String versionName = appUpdate.version_name;
                        final String content = appUpdate.content;
                        final String updated = appUpdate.updated;
                        final String url = appUpdate.url;
                        final Boolean isForced = appUpdate.force;
                        final Boolean isDownload = appUpdate.download;
                        final int oVersionCode = mPrefs.getInt(Constants.PREF.APP_UPDATE_VERSION,
                                BuildConfig.VERSION_CODE);
                        final StringBuilder message = new StringBuilder();
                        message.append(updated);
                        message.append("\n");
                        message.append(content);
                        Log.d(TAG, "AppVersion: " + versionCode + " " + oVersionCode + " " + BuildConfig.VERSION_CODE);
                        if (BuildConfig.VERSION_CODE >= versionCode) {
                            final Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                    R.string.message_no_app_update, Snackbar.LENGTH_LONG);
                            TextView tv = (TextView)
                                    snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            tv.setTextColor(Color.WHITE);
                            snackbar.show();
                        } else {
                            new AlertDialog.Builder(context)
                                    .setTitle(getString(R.string.message_app_update, versionName))
                                    .setMessage(message)
                                    .setNegativeButton(R.string.action_cancel,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialoginterface, int i) {
                                                    dialoginterface.cancel();
                                                }
                                            })
                                    .setPositiveButton(R.string.action_update,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialoginterface, int i) {
                                                    Uri link = Uri.parse(url);
                                                    if (!isDownload) {
                                                        if (null == link) {
                                                            link = Uri.parse(getString(R.string.url_app));
                                                        }
                                                        Intent intent = new Intent(Intent.ACTION_VIEW, link);
                                                        if (intent.resolveActivity(context.getPackageManager()) != null) {
                                                            startActivity(intent);
                                                        }
                                                    } else {
                                                        // TODO: implement download apk
                                                    }
                                                }
                                            })
                                    .show();
                        }
                        SharedPreferences.Editor editor = mPrefs.edit();
                        editor.putInt(Constants.PREF.APP_UPDATE_VERSION, versionCode);
                        editor.apply();
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