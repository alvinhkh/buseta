package com.alvinhkh.buseta.view;

import android.app.ActivityManager;
import android.app.SearchManager;
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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.BuildConfig;
import com.alvinhkh.buseta.Connectivity;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.AppUpdate;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteNews;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.CheckUpdateService;
import com.alvinhkh.buseta.view.adapter.SuggestionSimpleCursorAdapter;
import com.alvinhkh.buseta.view.dialog.RouteEtaActivity;
import com.alvinhkh.buseta.view.fragment.MainFragment;
import com.alvinhkh.buseta.view.fragment.NoticeImageFragment;
import com.alvinhkh.buseta.view.fragment.RouteBoundFragment;
import com.alvinhkh.buseta.view.fragment.RouteNewsFragment;
import com.alvinhkh.buseta.view.fragment.RouteStopFragment;
import com.alvinhkh.buseta.preference.SettingsActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.koushikdutta.ion.Ion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        SearchView.OnQueryTextListener, SearchView.OnSuggestionListener, FilterQueryProvider {

    private static final String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences mPrefs;
    // SearchView Suggestion, reference: http://stackoverflow.com/a/13773625
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;
    private SuggestionSimpleCursorAdapter mAdapter;
    private Cursor mCursor;
    private CheckUpdateReceiver mReceiver;
    private Snackbar mSnackbar;
    private AdView mAdView;
    private GoogleApiClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // check connectivity
        Connectivity.toLog(this);
        // Set up a listener whenever a key changes
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        // Overview task
        setTaskDescription(getString(R.string.app_name));
        // Set Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == mSearchMenuItem) return;
                mSearchMenuItem.expandActionView();
            }
        });
        // Broadcast Receiver
        IntentFilter mFilter = new IntentFilter(Constants.MESSAGE.CHECKING_UPDATED);
        mReceiver = new CheckUpdateReceiver();
        mFilter.addAction(Constants.MESSAGE.CHECKING_UPDATED);
        registerReceiver(mReceiver, mFilter);
        // Set Suggestion Adapter
        String[] columns = new String[] {
                SuggestionTable.COLUMN_TEXT,
                SuggestionTable.COLUMN_TYPE,
        };
        int[] columnTextId = new int[] {
                android.R.id.text1,
                R.id.icon,
        };
        mAdapter = new SuggestionSimpleCursorAdapter(getApplicationContext(),
                R.layout.row_route, mCursor, columns, columnTextId, 0);
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {
                if (aColumnIndex == aCursor.getColumnIndexOrThrow(SuggestionTable.COLUMN_TYPE)) {
                    String icon_text = aCursor.getString(aColumnIndex);
                    Integer image;
                    ImageView imageView = (ImageView) aView.findViewById(R.id.icon);
                    switch (icon_text) {
                        case SuggestionTable.TYPE_HISTORY:
                            image = R.drawable.ic_history_black_24dp;
                            break;
                        case SuggestionTable.TYPE_DEFAULT:
                        default:
                            image = R.drawable.ic_directions_bus_black_24dp;
                            break;
                    }
                    if (imageView != null && image > 0) {
                        imageView.setImageResource(image);
                    }
                    return true;
                }
                return false;
            }
        });
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            MainFragment mainFragment = new MainFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            mainFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mainFragment, "Home").commit();
        }
        // check app and suggestion database updates
        Intent intent = new Intent(this, CheckUpdateService.class);
        startService(intent);
        createAdView(); // Ad
        onNewIntent(getIntent());
    }

    private String routeNo;

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (null == intent) return;
        String action = intent.getAction();
        String data = intent.getDataString();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            showRouteBoundFragment(query);
            return;
        }
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            RouteStop routeStop = intent.getParcelableExtra(Constants.BUNDLE.STOP_OBJECT);
            if (null != routeStop) {
                // with stop object to open dialog
                showRouteBoundFragment(routeStop.route_bound.route_no);
                showRouteStopFragment(routeStop.route_bound);
                Intent dialogIntent = new Intent(getApplicationContext(), RouteEtaActivity.class);
                dialogIntent.setAction(Intent.ACTION_VIEW);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                dialogIntent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                startActivity(dialogIntent);
                return;
            }
            RouteBound routeBound = intent.getParcelableExtra(Constants.BUNDLE.BOUND_OBJECT);
            if (null != routeBound) {
                showRouteBoundFragment(routeBound.route_no);
                showRouteStopFragment(routeBound);
                return;
            }
            if (null != routeNo && !routeNo.equals(""))
                appIndexStop(routeNo);
            String regex = "/route/(.*)/?";
            Pattern regexPattern = Pattern.compile(regex);
            Matcher match = regexPattern.matcher(data);
            if (match.find()) {
                routeNo = match.group(1);
            } else {
                routeNo = data.substring(data.lastIndexOf("/") + 1);
            }
            showRouteBoundFragment(routeNo);
            appIndexStart(routeNo);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        appIndexStart(routeNo);
    }

    @Override
    public void onStop() {
        appIndexStop(routeNo);
        super.onStop();
    }

    private void appIndexStart(final String routeNo) {
        if (null == routeNo || routeNo.equals("")) return;
        final String route = routeNo.trim().replace(" ", "");
        if (!mClient.isConnected())
            mClient.connect();
        final String TITLE = route;
        final Uri APP_URI = Uri.parse(Constants.URI.APP).buildUpon().appendPath(route).build();
        // Log.d(TAG, APP_URI.toString());
        Action viewAction = Action.newAction(Action.TYPE_VIEW, TITLE, APP_URI);
        PendingResult<Status> result = AppIndex.AppIndexApi.start(mClient, viewAction);
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "App Indexing: Recorded " + route + " view successfully.");
                } else {
                    Log.e(TAG, "App Indexing: " + status.toString());
                }
            }
        });
    }

    private void appIndexStop(final String routeNo) {
        if (null == routeNo || routeNo.equals("")) return;
        // Call end() and disconnect the client
        final String TITLE = routeNo;
        final String route = routeNo.trim().replace(" ", "");
        final Uri APP_URI = Uri.parse(Constants.URI.APP).buildUpon().appendPath(route).build();
        Action viewAction = Action.newAction(Action.TYPE_VIEW, TITLE, APP_URI);
        PendingResult<Status> result = AppIndex.AppIndexApi.end(mClient, viewAction);
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "App Indexing: Recorded " + route + " view end successfully.");
                } else {
                    Log.e(TAG, "App Indexing: " + status.toString());
                }
            }
        });
        mClient.disconnect();
    }

    @Override
     public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        createAdView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (key.matches(Constants.PREF.AD_HIDE)) {
            createAdView();
        } else if (key.matches("eta_version")) {
            int rowsDeleted = getContentResolver().delete(FollowProvider.CONTENT_URI_ETA_JOIN, null, null);
            Log.d(TAG, "Deleted ETA Records: " + rowsDeleted);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        // Search View
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        if (mSearchView != null) {
            ((EditText) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text))
                    .setHintTextColor(ContextCompat.getColor(this, R.color.hint_foreground_material_dark));
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setIconified(false);
            mSearchView.onActionViewCollapsed();
            mSearchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            mSearchView.setOnQueryTextListener(this);
            mAdapter.setFilterQueryProvider(this);
            mSearchView.setSuggestionsAdapter(mAdapter);
            mSearchView.setOnSuggestionListener(this);
            mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        collapseSearchView();
                    }
                }
            });
            collapseSearchView();
        }
        mSearchMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mSearchMenuItem.expandActionView();
                return true;
            }
        });
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != mAdView)
            mAdView.resume();
    }

    @Override
    protected void onPause() {
        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(new View(this).getWindowToken(), 0);
        if (null != mAdView)
            mAdView.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        int rowsDeleted_route = getContentResolver().delete(
                RouteProvider.CONTENT_URI_BOUND_FILTER, null, null);
        Log.d(TAG, "Deleted Route Records: " + rowsDeleted_route);
        int rowsDeleted_routeStop = getContentResolver().delete(
                RouteProvider.CONTENT_URI_STOP_FILTER, null, null);
        Log.d(TAG, "Deleted Stops Records: " + rowsDeleted_routeStop);
        int rowsDeleted_eta = getContentResolver().delete(
                FollowProvider.CONTENT_URI_ETA_JOIN, null, null);
        Log.d(TAG, "Deleted ETA Records: " + rowsDeleted_eta);
        if (null != mAdView)
            mAdView.destroy();
        if (null != mReceiver)
            unregisterReceiver(mReceiver);
        if (null != mCursor)
            mCursor.close();
        Ion.getDefault(getBaseContext()).cancelAll(getBaseContext());
        // Clear Ion Image Cache
        Ion.getDefault(getBaseContext()).getCache().clear();
        Ion.getDefault(getBaseContext()).getBitmapCache().clear();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof RouteBoundFragment) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_share:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.message_share_text));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.message_share_title)));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        collapseSearchView();
        Cursor cursor = (Cursor) mSearchView.getSuggestionsAdapter().getItem(position);
        int indexColumnSuggestion = cursor.getColumnIndex(SuggestionTable.COLUMN_TEXT);
        String route_no = cursor.getString(indexColumnSuggestion);
        cursor.close();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI.ROUTE + route_no));
        startActivity(intent);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (null == query) return false;
        collapseSearchView();
        boolean showMessage = true;
        query = query.toUpperCase();
        if (query.equals(Constants.PREF.AD_KEY) ||
                query.equals(Constants.PREF.AD_SHOW)) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Boolean hidden = mPrefs.getBoolean(Constants.PREF.AD_HIDE, false);
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(Constants.PREF.AD_HIDE, query.equals(Constants.PREF.AD_KEY));
            editor.apply();
            if (showMessage) {
                // show snack bar
                int stringId = R.string.message_request_hide_ad;
                if (query.equals(Constants.PREF.AD_SHOW))
                    stringId = R.string.message_request_show_ad;
                if (hidden && query.equals(Constants.PREF.AD_KEY))
                    stringId = R.string.message_request_hide_ad_again;
                final Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator) == null ?
                                findViewById(android.R.id.content) : findViewById(R.id.coordinator),
                        stringId, Snackbar.LENGTH_INDEFINITE);
                TextView tv = (TextView)
                        snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                snackbar.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        snackbar.dismiss();
                    }
                }, 5000);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI.ROUTE + query));
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public Cursor runQuery(CharSequence constraint) {
        if (null == constraint || constraint.length() < 1) {
            // show history only
            mCursor = getContentResolver().query(SuggestionProvider.CONTENT_URI,
                    null, SuggestionTable.COLUMN_TEXT + " LIKE '%%'" + " AND " +
                            SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'",
                    null, SuggestionTable.COLUMN_DATE + " DESC LIMIT 5");
        } else {
            constraint = constraint.toString().trim().replace(" ", "");
            Uri suggestionUri = Uri.parse(SuggestionProvider.CONTENT_URI_SUGGESTIONS + "/" + constraint);
            mCursor = getContentResolver().query(suggestionUri, null, null, null, null);
        }
        return mCursor;
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

    private void collapseSearchView() {
        if (null != mSearchMenuItem)
            mSearchMenuItem.collapseActionView();
    }

    private void createAdView() {
        // Admob
        final FrameLayout adViewContainer = (FrameLayout) findViewById(R.id.adView_container);
        if (null == mPrefs) {
            adViewContainer.setVisibility(View.VISIBLE);
            return;
        }
        adViewContainer.setVisibility(View.GONE);
        if (null != mAdView) {
            mAdView.destroy();
            mAdView.setVisibility(View.GONE);
            mAdView = null;
        }
        if (!mPrefs.getBoolean(Constants.PREF.AD_HIDE, false)) {
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
                    adViewContainer.setVisibility(View.VISIBLE);
                }
            });
            adViewContainer.addView(mAdView);
            AdRequest mAdRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)  // All emulators
                    .addTestDevice(getString(R.string.ad_test_device))
                    .build();
            mAdView.loadAd(mAdRequest);
        }
    }

    public void showRouteBoundFragment(String _route_no){
        if (null == _route_no) return;
        _route_no = _route_no.trim().replace(" ", "").toUpperCase();
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        RouteBoundFragment f = RouteBoundFragment.newInstance(_route_no);
        t.replace(R.id.fragment_container, f, "RouteBound_" + _route_no);
        t.addToBackStack(null);
        t.commit();
    }

    public void showRouteStopFragment(RouteBound routeBound){
        if (null == routeBound || null == routeBound.route_no || null == routeBound.route_bound)
            return;
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        RouteStopFragment f = RouteStopFragment.newInstance(routeBound);
        t.replace(R.id.fragment_container, f, "RouteStop_" + routeBound.route_no + "_" + routeBound.route_bound);
        t.addToBackStack(null);
        t.commit();
    }

    public void showRouteNewsFragment(String _route_no){
        if (null == _route_no) return;
        _route_no = _route_no.trim().replace(" ", "").toUpperCase();
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        RouteNewsFragment f = RouteNewsFragment.newInstance(_route_no);
        t.replace(R.id.fragment_container, f, "RouteNews_" + _route_no);
        t.addToBackStack(null);
        t.commit();
    }

    public void showNoticeImageFragment(RouteNews notice){
        if (null == notice || null == notice.link) return;
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        NoticeImageFragment f = NoticeImageFragment.newInstance(notice);
        t.replace(R.id.fragment_container, f, "Notice_" + notice.link);
        t.addToBackStack(null);
        t.commit();
    }

    public class CheckUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            final Boolean aBoolean_suggestion =
                    bundle.getBoolean(Constants.STATUS.UPDATED_SUGGESTION, false);
            final Boolean aBoolean_app =
                    bundle.getBoolean(Constants.STATUS.UPDATED_APP_FOUND, false);
            if (aBoolean_suggestion) {
                final int messageId = bundle.getInt(Constants.BUNDLE.MESSAGE_ID);
                String name = getResources().getResourceName(messageId);
                if (name != null && name.startsWith(getPackageName())) {
                    if (null != mSnackbar)
                        mSnackbar.dismiss();
                    mSnackbar = Snackbar.make(findViewById(R.id.coordinator) != null ?
                                    findViewById(R.id.coordinator) : findViewById(R.id.main_content),
                            messageId, Snackbar.LENGTH_LONG);
                    TextView tv = (TextView)
                            mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    if (messageId == R.string.message_database_updating)
                        mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                    else if (messageId == R.string.message_database_updated) {
                        Cursor cursor = getContentResolver().query(SuggestionProvider.CONTENT_URI,
                                null, SuggestionTable.COLUMN_TEXT + " LIKE '%%'" + " AND " +
                                        SuggestionTable.COLUMN_TYPE + " = '" +
                                        SuggestionTable.TYPE_DEFAULT + "'",
                                null, null);
                        int count = 0;
                        if (cursor != null) {
                            count = cursor.getCount();
                            cursor.close();
                        }
                        mSnackbar.setText(getString(messageId) + " " +
                                getString(R.string.message_total_routes, count));
                    }
                    mSnackbar.show();
                }
            }
            if (aBoolean_app) {
                final AppUpdate appUpdate = bundle.getParcelable(Constants.BUNDLE.APP_UPDATE_OBJECT);
                if (null != appUpdate) {
                    final int versionCode = appUpdate.version_code;
                    final String versionName = appUpdate.version_name;
                    final String content = appUpdate.content;
                    final String updated = appUpdate.updated;
                    final String url = appUpdate.url;
                    final Boolean notify = appUpdate.notify;
                    final Boolean isForced = appUpdate.force;
                    final Boolean isDownload = appUpdate.download;
                    final int oVersionCode = mPrefs.getInt(Constants.PREF.APP_UPDATE_VERSION,
                            BuildConfig.VERSION_CODE);
                    final StringBuilder message = new StringBuilder();
                    message.append(updated);
                    message.append("\n");
                    message.append(content);
                    Log.d(TAG, "AppVersion: " + versionCode + " DB: " + oVersionCode +
                            " APK: " + BuildConfig.VERSION_CODE);
                    if (notify)
                    if (versionCode > oVersionCode || (isForced && versionCode > BuildConfig.VERSION_CODE))
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
                                                    if (intent.resolveActivity(getPackageManager()) != null) {
                                                        startActivity(intent);
                                                    }
                                                } else {
                                                    // TODO: implement download apk
                                                }
                                            }
                                        })
                                .show();
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putInt(Constants.PREF.APP_UPDATE_VERSION, versionCode);
                    editor.apply();
                }
            }
        }
    }

}
