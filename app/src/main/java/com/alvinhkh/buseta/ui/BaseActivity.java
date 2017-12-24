package com.alvinhkh.buseta.ui;

import android.app.ActivityManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.AppUpdate;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.search.SearchActivity;
import com.alvinhkh.buseta.ui.setting.SettingActivity;
import com.alvinhkh.buseta.utils.AdViewUtil;
import com.alvinhkh.buseta.utils.NightModeUtil;
import com.alvinhkh.buseta.utils.PreferenceUtil;
import com.alvinhkh.buseta.ui.search.SuggestionSimpleCursorAdapter;
import com.alvinhkh.buseta.utils.SearchHistoryUtil;
import com.google.android.gms.ads.AdView;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;


abstract public class BaseActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, FilterQueryProvider {

    private final CompositeDisposable disposables = new CompositeDisposable();

    protected AdView adView;

    protected FrameLayout adViewContainer;

    private Context context;

    // SearchView Suggestion, reference: http://stackoverflow.com/a/13773625
    protected MenuItem searchMenuItem;

    protected SearchView searchView;

    protected SuggestionSimpleCursorAdapter adapter;

    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        setTaskDescription(getString(R.string.app_name));

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        disposables.add(RxBroadcastReceiver.create(this,
                new IntentFilter(C.ACTION.APP_UPDATE))
                .share().subscribeWith(appUpdateObserver()));

        // Set Suggestion Adapter
        String[] columns = new String[]{
                SuggestionTable.COLUMN_TEXT,
                SuggestionTable.COLUMN_COMPANY,
                SuggestionTable.COLUMN_TYPE,
        };
        int[] columnTextId = new int[]{
                android.R.id.text1,
                R.id.company,
                R.id.icon,
        };
        adapter = new SuggestionSimpleCursorAdapter(getApplicationContext(),
                R.layout.row_route, cursor, columns, columnTextId, 0);
        adapter.setViewBinder((aView, aCursor, aColumnIndex) -> {
            if (aColumnIndex == aCursor.getColumnIndexOrThrow(SuggestionTable.COLUMN_TYPE)) {
                String iconText = aCursor.getString(aColumnIndex);
                Drawable drawable;
                ImageView imageView = aView.findViewById(R.id.icon);
                switch (iconText) {
                    case SuggestionTable.TYPE_HISTORY:
                        drawable = ContextCompat.getDrawable(this, R.drawable.ic_history_black_24dp);
                        break;
                    case SuggestionTable.TYPE_DEFAULT:
                    default:
                        drawable = ContextCompat.getDrawable(this, R.drawable.ic_directions_bus_black_24dp);
                        break;
                }
                if (imageView != null) {
                    imageView.setImageDrawable(drawable);
                }
                return true;
            }
            if (aColumnIndex == aCursor.getColumnIndexOrThrow(SuggestionTable.COLUMN_COMPANY)) {
                String companyCode = aCursor.getString(aColumnIndex);
                TextView textView = aView.findViewById(R.id.company);
                switch (companyCode) {
                    case BusRoute.COMPANY_CTB:
                        textView.setText(R.string.provider_short_ctb);
                        break;
                    case BusRoute.COMPANY_KMB:
                        textView.setText(R.string.provider_short_kmb);
                        break;
                    case BusRoute.COMPANY_NLB:
                        textView.setText(R.string.provider_short_nlb);
                        break;
                    case BusRoute.COMPANY_NWFB:
                        textView.setText(R.string.provider_short_nwfb);
                        break;
                    case BusRoute.COMPANY_NWST:
                        textView.setText(R.string.provider_short_nwst);
                        break;
                    default:
                        textView.setText(companyCode);
                        break;
                }
                return true;
            }
            return false;
        });
        adapter.setFilterQueryProvider(this);
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        if (adView != null) {
            adView.pause();
        }
        disposables.clear();
        if (cursor != null) cursor.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Search View
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchMenuItem.getActionView();
        if (searchView != null && searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconified(false);
            searchView.onActionViewCollapsed();
            searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            searchView.setOnQueryTextListener(this);
            searchView.setSuggestionsAdapter(adapter);
            searchView.setOnSuggestionListener(this);
            searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    collapseSearchView();
                }
            });
            // SearchView does not work with less than 2 characters
            // https://stackoverflow.com/a/24279952/2411672
            AutoCompleteTextView searchAutoCompleteTextView = searchView.findViewById(R.id.search_src_text);
            if (searchAutoCompleteTextView != null) {
                searchAutoCompleteTextView.setThreshold(0);
            }
            collapseSearchView();
        }
        searchMenuItem.setOnMenuItemClickListener(item -> {
            searchMenuItem.expandActionView();
            return true;
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingActivity.class));
                break;
            }
            case R.id.action_share: {
                startActivity(Intent.createChooser(PreferenceUtil.shareAppIntent(this),
                        getString(R.string.message_share_text)));
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NightModeUtil.update(this)) {
            recreate();
            return;
        }
        if (adView != null) {
            adView.resume();
        }
        // Set Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (null != toolbar) {
            setSupportActionBar(toolbar);
            toolbar.setOnClickListener(v -> {
                if (null == searchMenuItem) return;
                searchMenuItem.expandActionView();
            });
        }
    }

    @Override
    protected void onPause() {
        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(new View(this).getWindowToken(), 0);
        }
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        switch (key) {
            case "app_theme":
                NightModeUtil.update(this);
                this.recreate();
                return;
            case C.PREF.AD_HIDE:
                if (adViewContainer != null) {
                    adView = AdViewUtil.banner(adViewContainer, adView, false);
                }
                return;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false);
        }
    }

    protected void setTaskDescription(String title) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(title, bm,
                ContextCompat.getColor(this, R.color.colorPrimary600)));
    }

    protected void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        collapseSearchView();
        Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
        String routeNo = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TEXT));
        String company = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_COMPANY));
        cursor.close();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, SearchActivity.class);
        intent.putExtra(C.EXTRA.ROUTE_NO, routeNo);
        intent.putExtra(C.EXTRA.COMPANY_CODE, company);
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (null == query) return false;
        collapseSearchView();
        boolean showMessage = true;
        query = query.toUpperCase();
        if (query.equals(C.PREF.AD_KEY) || query.equals(C.PREF.AD_SHOW)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Boolean hidden = preferences.getBoolean(C.PREF.AD_HIDE, false);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(C.PREF.AD_HIDE, query.equals(C.PREF.AD_KEY));
            editor.apply();
            if (showMessage) {
                // show snack bar
                int stringId = R.string.message_request_hide_ad;
                if (query.equals(C.PREF.AD_SHOW))
                    stringId = R.string.message_request_show_ad;
                if (hidden && query.equals(C.PREF.AD_KEY))
                    stringId = R.string.message_request_hide_ad_again;
                Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator) != null ?
                                findViewById(R.id.coordinator) : findViewById(android.R.id.content),
                        stringId, Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
                new Handler().postDelayed(snackbar::dismiss, 5000);
            }
        } else {
            Intent i = new Intent(Intent.ACTION_SEARCH);
            i.setClass(this, SearchActivity.class);
            i.putExtra(SearchManager.QUERY, query);
            startActivity(i);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public Cursor runQuery(CharSequence constraint) {
        if (!TextUtils.isEmpty(constraint)) {
            constraint = constraint.toString().replaceAll("[^a-zA-Z0-9]", "");
        }
        if (TextUtils.isEmpty(constraint)) {
            cursor = SearchHistoryUtil.query(this, 5);
        } else {
            Uri suggestionUri = Uri.parse(SuggestionProvider.CONTENT_URI_SUGGESTIONS + "/" + constraint);
            cursor = getContentResolver().query(suggestionUri, null, null, null, null);
        }
        return cursor;
    }

    private void collapseSearchView() {
        if (searchMenuItem != null) searchMenuItem.collapseActionView();
    }

    DisposableObserver<Intent> appUpdateObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                Boolean isUpdated = bundle.getBoolean(C.EXTRA.UPDATED, false);
                AppUpdate appUpdate = bundle.getParcelable(C.EXTRA.APP_UPDATE_OBJECT);
                if (isUpdated && appUpdate != null) {
                    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    int oVersionCode = mPrefs.getInt(C.PREF.APP_UPDATE_VERSION, BuildConfig.VERSION_CODE);
                    StringBuilder message = new StringBuilder();
                    message.append(appUpdate.updated);
                    message.append("\n");
                    message.append(appUpdate.content);
                    Timber.d("AppVersion:%s DB:%s APK:%s ", appUpdate.version_code, oVersionCode, BuildConfig.VERSION_CODE);
                    if (appUpdate.notify && (appUpdate.version_code > oVersionCode
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
