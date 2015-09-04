package com.alvinhkh.buseta.preference;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
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
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.SuggestionsDatabase;
import com.alvinhkh.buseta.service.UpdateSuggestionService;

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

        private Activity mActivity;
        private SuggestionsDatabase mDatabase;
        private UpdateSuggestionReceiver mReceiver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mActivity = super.getActivity();
            mDatabase = new SuggestionsDatabase(mActivity.getApplicationContext());
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
                                    Snackbar snackbar = Snackbar.make(
                                            mActivity.findViewById(R.id.fragment_container),
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
            // App Name
            Preference appName = getPreferenceScreen().findPreference("app_name");
            appName.setTitle(getString(R.string.title_app_name, getString(R.string.app_name)));
            // App Version
            Preference appVersion = getPreferenceScreen().findPreference("app_version");
            int versionCode = BuildConfig.VERSION_CODE;
            String versionName = BuildConfig.VERSION_NAME;
            appVersion.setSummary(getString(R.string.summary_app_version, versionName, versionCode));
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
            if (p instanceof MultiSelectListPreference) {
                EditTextPreference editTextPref = (EditTextPreference) p;
                p.setSummary(editTextPref.getText());
            }
        }

        public class UpdateSuggestionReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                Boolean aBoolean = bundle.getBoolean(Constants.ROUTES.SUGGESTION_UPDATE);
                if (aBoolean == true) {
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
                            Cursor cursor = mDatabase.getByType("%", SuggestionsDatabase.TYPE_DEFAULT);
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
        return;
    }

}