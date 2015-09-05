package com.alvinhkh.buseta.view;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteNews;
import com.alvinhkh.buseta.service.UpdateSuggestionService;
import com.alvinhkh.buseta.view.adapter.SuggestionSimpleCursorAdapter;
import com.alvinhkh.buseta.database.SuggestionsDatabase;
import com.alvinhkh.buseta.view.fragment.MainFragment;
import com.alvinhkh.buseta.view.fragment.NoticeImageFragment;
import com.alvinhkh.buseta.view.fragment.RouteBoundFragment;
import com.alvinhkh.buseta.view.fragment.RouteNewsFragment;
import com.alvinhkh.buseta.view.fragment.RouteStopFragment;
import com.alvinhkh.buseta.preference.SettingsActivity;
import com.koushikdutta.ion.Ion;

public class MainActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener,
        SearchView.OnSuggestionListener,
        FilterQueryProvider {

    private static final String TAG = "MainActivity";

    SharedPreferences mPrefs;

    // SearchView Suggestion, reference: http://stackoverflow.com/a/13773625
    private SuggestionsDatabase mDatabase;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;
    private SuggestionSimpleCursorAdapter mAdapter;
    private Cursor mCursor;
    private UpdateSuggestionReceiver mReceiver;
    private Snackbar mSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = new SuggestionsDatabase(getApplicationContext());
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
        IntentFilter mFilter = new IntentFilter(Constants.ROUTES.SUGGESTION_UPDATE);
        mReceiver = new UpdateSuggestionReceiver();
        mFilter.addAction(Constants.ROUTES.SUGGESTION_UPDATE);
        registerReceiver(mReceiver, mFilter);
        // Set Suggestion Adapter
        String[] columns = new String[] {
                SuggestionsDatabase.COLUMN_TEXT,
                SuggestionsDatabase.COLUMN_TYPE,
        };
        int[] columnTextId = new int[] {
                android.R.id.text1,
                R.id.icon,
        };
        mAdapter = new SuggestionSimpleCursorAdapter(getApplicationContext(),
                R.layout.row_route, mCursor, columns, columnTextId, 0);
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {
                if (aColumnIndex == aCursor.getColumnIndexOrThrow(SuggestionsDatabase.COLUMN_TYPE)) {
                    String icon_text = aCursor.getString(aColumnIndex);
                    Integer image;
                    ImageView imageView = (ImageView) aView.findViewById(R.id.icon);
                    switch (icon_text) {
                        case SuggestionsDatabase.TYPE_HISTORY:
                            image = R.drawable.ic_history_black_24dp;
                            break;
                        case SuggestionsDatabase.TYPE_DEFAULT:
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
        // update available route records
        int routeVersion = mPrefs.getInt(Constants.PREF.VERSION_RECORD, 0);
        if (Constants.ROUTES.VERSION > routeVersion) {
            Intent intent = new Intent(this, UpdateSuggestionService.class);
            startService(intent);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

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
    protected void onPause() {
        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(new View(this).getWindowToken(), 0);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (null != mReceiver)
            unregisterReceiver(mReceiver);
        if (null != mDatabase)
            mDatabase.close();
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
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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
        SQLiteCursor cursor = (SQLiteCursor) mSearchView.getSuggestionsAdapter().getItem(position);
        int indexColumnSuggestion = cursor.getColumnIndex(SuggestionsDatabase.COLUMN_TEXT);
        showRouteBoundFragment(cursor.getString(indexColumnSuggestion));
        cursor.close();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        collapseSearchView();
        showRouteBoundFragment(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public Cursor runQuery(CharSequence constraint) {
        if (null == constraint || constraint.length() < 1) {
            mCursor = mDatabase.getHistory("5"); // avoid show too many results, show history only
        } else {
            mCursor = mDatabase.get(constraint.toString().trim().replace(" ", ""));
        }
        return mCursor;
    }

    private void collapseSearchView() {
        if (null != mSearchMenuItem)
            mSearchMenuItem.collapseActionView();
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

    public class UpdateSuggestionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Boolean aBoolean = bundle.getBoolean(Constants.ROUTES.SUGGESTION_UPDATE);
            if (aBoolean) {
                int resourceId = bundle.getInt(Constants.ROUTES.MESSAGE_ID);
                String name = getResources().getResourceName(resourceId);
                if (name != null && name.startsWith(getPackageName())) {
                    if (null != mSnackbar)
                        mSnackbar.dismiss();
                    mSnackbar = Snackbar.make(findViewById(R.id.coordinator) != null ?
                                    findViewById(R.id.coordinator) : findViewById(R.id.main_content),
                            resourceId, Snackbar.LENGTH_LONG);
                    TextView tv = (TextView)
                            mSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    if (resourceId == R.string.message_database_updating)
                        mSnackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                    else if (resourceId == R.string.message_database_updated) {
                        Cursor cursor = mDatabase.getByType("%", SuggestionsDatabase.TYPE_DEFAULT);
                        mSnackbar.setText(getString(resourceId) + " " +
                                getString(R.string.message_total_routes, cursor == null ? 0 : cursor.getCount()));
                        if (cursor != null)
                            cursor.close();
                    }
                    mSnackbar.show();
                }
            }
        }
    }

}
