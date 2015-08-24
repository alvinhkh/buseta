package com.alvinhkh.buseta.preference;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;

import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.SuggestionsDatabase;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private SuggestionsDatabase mDatabase;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mDatabase = new SuggestionsDatabase(getActivity().getApplicationContext());
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
            // Set Default values from XML attribute
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
            // Set Summary
            initSummary(getPreferenceScreen());
            // Clear History
            Preference clearHistory = (Preference) getPreferenceScreen().findPreference("clear_history");
            clearHistory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.message_confirm_clear_search_history))
                            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    dialoginterface.cancel();
                                }
                            })
                            .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    Intent intent = new Intent(Constants.MESSAGE.HISTORY_UPDATED);
                                    intent.putExtra(Constants.MESSAGE.HISTORY_UPDATED, true);
                                    getActivity().sendBroadcast(intent);
                                    Snackbar snackbar = Snackbar.make(
                                            getActivity().findViewById(R.id.fragment_container),
                                            mDatabase.clearHistory() ?
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
            // App Name
            Preference appName = (Preference) getPreferenceScreen().findPreference("app_name");
            appName.setTitle(getString(R.string.title_app_name, getString(R.string.app_name)));
            // App Version
            Preference appVersion = (Preference) getPreferenceScreen().findPreference("app_version");
            int versionCode = BuildConfig.VERSION_CODE;
            String versionName = BuildConfig.VERSION_NAME;
            appVersion.setSummary(getString(R.string.summary_app_version, versionName, versionCode));
            // Developer
            Preference developer = (Preference) getPreferenceScreen().findPreference("developer");
            developer.setSummary(getString(R.string.summary_developer) + " (" + getString(R.string.url_developer) + ")");
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Unregister the listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            if (null != mDatabase)
                mDatabase.close();
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
            if (p instanceof SwitchPreference) {
                SwitchPreference switchPref = (SwitchPreference) p;
                p.setSummary(switchPref.isChecked() ?
                        getString(R.string.enabled) :
                        getString(R.string.disabled));
            }
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
            if (p instanceof MultiSelectListPreference) {
                EditTextPreference editTextPref = (EditTextPreference) p;
                p.setSummary(editTextPref.getText());
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
        return;
    }

}