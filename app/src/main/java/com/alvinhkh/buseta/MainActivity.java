package com.alvinhkh.buseta;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements SearchView.OnQueryTextListener,
        SearchView.OnSuggestionListener {

    private static final String TAG = "MainActivity";

    SharedPreferences mPrefs;

    // SearchView Suggestion, reference: http://stackoverflow.com/a/13773625
    private SuggestionsDatabase mDatabase;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;
    private SuggestionSimpleCursorAdapter mAdapter;
    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = new SuggestionsDatabase(getApplicationContext());
        mPrefs = getPreferences(Context.MODE_PRIVATE);

        // Set Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        // Add available route records
        final Snackbar snackbar = Snackbar.make(findViewById(R.id.main_content),
                    R.string.message_database_updating, Snackbar.LENGTH_INDEFINITE);
        TextView tv = (TextView)
                snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (null != mDatabase) {
                    mDatabase.clearDefault();
                    String[] arr = Constants.ROUTES.AVAILABLE;
                    for (int i = 0; i < arr.length; i++) {
                        mDatabase.insertDefault(arr[i]);
                    }
                    if (null != mPrefs) {
                        SharedPreferences.Editor editor = mPrefs.edit();
                        editor.putInt(Constants.ROUTES.VERSION_RECORD, Constants.ROUTES.VERSION);
                        editor.commit();
                    }
                    if (null != snackbar) {
                        snackbar.setText(R.string.message_database_updated);
                    }
                    Log.d(TAG, "updated default available routes");
                }
                if (null != snackbar) {
                    snackbar.setDuration(Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        };
        int routeVersion = mPrefs.getInt(Constants.ROUTES.VERSION_RECORD, 0);
        if (Constants.ROUTES.VERSION > routeVersion) {
            if (null != snackbar) {
                snackbar.show();
            }
            handler.postDelayed(runnable, 1000);
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
                    .setHintTextColor(getResources().getColor(R.color.hint_foreground_material_dark));
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.onActionViewCollapsed();
            mSearchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            mSearchView.setOnQueryTextListener(this);
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
                onQueryTextChange(null);
                if (mSearchView != null) {
                    // focus
                    mSearchView.setIconified(false);
                    mSearchView.setFocusable(true);
                    mSearchView.requestFocus();
                }
                // show software keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                return true;
            }
        });

        return true;
    }

    @Override
    protected void onPause() {
        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(new View(this).getWindowToken(), 0);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (null != mDatabase)
            mDatabase.close();
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
        if (id == R.id.action_clear_history) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.fragment_container),
                    mDatabase.clearHistory() ?
                            R.string.message_clear_history_success : R.string.message_clear_history_fail,
                    Snackbar.LENGTH_SHORT);
            snackbar.show();
            TextView tv = (TextView)
                    snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            return true;
        } else if (id == R.id.action_about) {
            // About Dialog
            int versionCode = BuildConfig.VERSION_CODE;
            String versionName = BuildConfig.VERSION_NAME;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(getString(R.string.versioning, versionName, versionCode) + "\n" +
                                    getString(R.string.message_author) + "\n\n\n" +
                                    getString(R.string.message_notice_information)
                    )
                    .setPositiveButton(R.string.dotcom, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(Constants.URL.ALVINHKH));
                            startActivity(i);
                        }
                    })
                    .setNegativeButton(R.string.open_source_license, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent("com.alvinhkh.buseta.OpenSourceLicenses.OPEN");
                            startActivity(i);
                        }
                    })
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        SQLiteCursor cursor = (SQLiteCursor) mSearchView.getSuggestionsAdapter().getItem(position);
        int indexColumnSuggestion = cursor.getColumnIndex(SuggestionsDatabase.COLUMN_TEXT);
        mSearchView.setQuery(cursor.getString(indexColumnSuggestion), false);
        showRouteBoundFragment(mSearchView.getQuery().toString());
        collapseSearchView();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        showRouteBoundFragment(query);
        collapseSearchView();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.isEmpty()) {
            mCursor = mDatabase.getHistory("5"); // avoid show too many results, show history only
            if (null != mAdapter)
                mAdapter.swapCursor(mCursor);
        } else {
            mCursor = mDatabase.get(newText.trim().replace(" ", ""));
            if (null != mAdapter)
                mAdapter.swapCursor(mCursor);
        }
        if (null != mSearchView && null != mAdapter)
            mSearchView.setSuggestionsAdapter(mAdapter);
        return true;
    }

    private void collapseSearchView() {
        if (mSearchMenuItem != null)
            mSearchMenuItem.collapseActionView();
        mSearchView.clearFocus();
        mSearchView.setFocusable(false);
    }

    public void showRouteBoundFragment(String _route_no){
        if (null == _route_no) return;
        _route_no = _route_no.trim().replace(" ", "");
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        RouteBoundFragment f = null;
        f = RouteBoundFragment.newInstance(_route_no.toUpperCase());
        t.replace(R.id.fragment_container, f, "RouteBound_" + _route_no);
        t.addToBackStack(null);
        t.commit();
    }

    public void showRouteStopFragment(String _route_no,
                                      String _route_bound,
                                      String _route_origin,
                                      String _route_destination){
        if (null == _route_no) return;
        _route_no = _route_no.trim().replace(" ", "");
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        RouteStopFragment f = null;
        f = RouteStopFragment.newInstance(_route_no, _route_bound, _route_origin, _route_destination);
        t.replace(R.id.fragment_container, f, "RouteStop_" + _route_no + "_" + _route_bound);
        t.addToBackStack(null);
        t.commit();
    }

}
