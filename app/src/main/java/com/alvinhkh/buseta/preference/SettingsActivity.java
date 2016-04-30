package com.alvinhkh.buseta.preference;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;

import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.PreferenceGroup;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.NightModeHelper;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.koushikdutta.ion.Ion;

public class SettingsActivity extends BasePreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private Activity mActivity;

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

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            updatePrefSummary(findPreference(key));
            if (key.matches("app_theme")) {
                NightModeHelper.update(getActivity());
                ((AppCompatPreferenceActivity) getActivity()).getDelegate().applyDayNight();
                getActivity().recreate();
            }
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

    }

}