package com.alvinhkh.buseta.preference;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.AppUpdate;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.CheckUpdateService;

import java.io.File;
import java.io.IOException;

public class AdvancedSettingsActivity extends BasePreferenceActivity {

    private static final String TAG = AdvancedSettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (null != getSupportActionBar())
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new AdvancedSettingsFragment())
                .commit();
    }

    public static class AdvancedSettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private Activity mActivity;
        private CheckUpdateReceiver mReceiver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mActivity = super.getActivity();
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences_advanced);
            // Set Default values from XML attribute
            PreferenceManager.setDefaultValues(mActivity, R.xml.preferences_advanced, false);
            // Set Summary
            initSummary(getPreferenceScreen());
            // Broadcast Receiver
            IntentFilter mFilter = new IntentFilter(Constants.MESSAGE.CHECKING_UPDATED);
            mReceiver = new CheckUpdateReceiver();
            mFilter.addAction(Constants.MESSAGE.CHECKING_UPDATED);
            mActivity.registerReceiver(mReceiver, mFilter);
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
            // send logcat
            Preference sendLogcat = getPreferenceScreen().findPreference("send_logcat");
            sendLogcat.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sendLogcat();
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
            if (p instanceof ListPreference) {
                ListPreference listPref = (ListPreference) p;
                p.setSummary(listPref.getEntry());
            }
        }

        public void sendLogcat() {
            // save logcat in file
            File outputFile = new File(mActivity.getExternalFilesDir(null), "buseta-logcat.txt");
            try {
                Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(mActivity, Uri.fromFile(outputFile).toString(), Toast.LENGTH_SHORT).show();
            // send file using email
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{
                    getString(R.string.email_developer)
            });
            // the attachment
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
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
                    if (null != name && name.startsWith(context.getPackageName())) {
                        if (null != mActivity) {
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
                        } else {
                            Toast.makeText(context, resourceId, Toast.LENGTH_SHORT).show();
                        }
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
                            if (null != mActivity) {
                                final Snackbar snackbar = Snackbar.make(mActivity.findViewById(android.R.id.content),
                                        R.string.message_no_app_update, Snackbar.LENGTH_LONG);
                                TextView tv = (TextView)
                                        snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                                tv.setTextColor(Color.WHITE);
                                snackbar.show();
                            } else {
                                Toast.makeText(context, R.string.message_no_app_update,
                                        Toast.LENGTH_SHORT).show();
                            }
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
                                                    }
                                                    // TODO: implement download apk
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

}