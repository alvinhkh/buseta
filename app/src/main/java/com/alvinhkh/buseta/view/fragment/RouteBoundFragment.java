package com.alvinhkh.buseta.view.fragment;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alvinhkh.buseta.Connectivity;
import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.RouteProvider;
import com.alvinhkh.buseta.provider.RouteBoundTable;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.service.RouteService;
import com.alvinhkh.buseta.view.MainActivity;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.view.adapter.RouteBoundAdapter;
import com.koushikdutta.ion.Ion;

import java.lang.ref.WeakReference;


public class RouteBoundFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = RouteBoundFragment.class.getSimpleName();
    private static final String KEY_LIST_VIEW_STATE = "KEY_LIST_VIEW_STATE_ROUTE_BOUND";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;

    private RouteBoundAdapter mAdapter;
    private UpdateViewReceiver mReceiver_view;
    private String _routeNo = null;

    public RouteBoundFragment() {}

    public static RouteBoundFragment newInstance(String _route_no) {
        RouteBoundFragment f = new RouteBoundFragment();
        Bundle args = new Bundle();
        args.putString("route_no", _route_no);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route, container, false);
        mContext = super.getActivity();
        // Get arguments
        _routeNo = getArguments().getString("route_no");
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
            mActionBar.setDisplayHomeAsUpEnabled(false);
        }
        setTaskDescription(_routeNo + getString(R.string.interpunct) + getString(R.string.app_name));
        setHasOptionsMenu(true);
        // Set List Adapter
        mAdapter = new RouteBoundAdapter(mContext);
        if (savedInstanceState != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState);
        }
        //
        TextView mTextView_routeNo = (TextView) view.findViewById(R.id.route_no);
        mTextView_routeNo.setText(_routeNo);
        // Button
        Button mButton_routeNews = (Button) view.findViewById(R.id.button_news);
        mButton_routeNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showRouteNewsFragment(_routeNo);
            }
        });
        //
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);
        // Set Listview
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setDividerHeight(2);
        mEmptyText = (TextView) view.findViewById(android.R.id.empty);
        mEmptyText.setText("");
        mListView.setEmptyView(view.findViewById(R.id.empty));
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        // mListView.setOnItemLongClickListener(this);
        // Broadcast Receiver
        if (null != mContext) {
            mReceiver_view = new UpdateViewReceiver();
            IntentFilter mFilter_view = new IntentFilter(Constants.MESSAGE.BOUNDS_UPDATED);
            mFilter_view.addAction(Constants.MESSAGE.BOUNDS_UPDATED);
            mContext.registerReceiver(mReceiver_view, mFilter_view);
        }
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_LIST_VIEW_STATE)) {
            mListView.onRestoreInstanceState(savedInstanceState
                    .getParcelable(KEY_LIST_VIEW_STATE));
            mEmptyText.setText(savedInstanceState.getString("EmptyText", ""));
        } else if (null != mContext) {
            final Cursor c = mContext.getContentResolver().query(
                    RouteProvider.CONTENT_URI_BOUND,
                    null,
                    RouteBoundTable.COLUMN_ROUTE + "=? ",
                    new String[]{
                            _routeNo,
                    }, RouteBoundTable.COLUMN_BOUND + "* 1 ASC");
            if (null != c && c.getCount() > 0) {
                Intent intent = new Intent(Constants.MESSAGE.BOUNDS_UPDATED);
                intent.putExtra(Constants.MESSAGE.BOUNDS_UPDATED, true);
                intent.putExtra(Constants.BUNDLE.ROUTE_NO, _routeNo);
                intent.putExtra(Constants.BUNDLE.UPDATE_MESSAGE, Constants.STATUS.UPDATED_BOUNDS);
                mContext.sendBroadcast(intent);
            } else {
                requestRouteBound();
            }
            if (null != c)
                c.close();
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("_route_no", _routeNo);
        if (null != mAdapter) {
            mAdapter.onSaveInstanceState(outState);
            outState.putParcelable(KEY_LIST_VIEW_STATE, mListView.onSaveInstanceState());
        }
        if (null != mEmptyText)
            outState.putString("EmptyText", mEmptyText.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
        }
    }

    @Override
    public void onDestroyView() {
        Ion.getDefault(mContext).cancelAll(mContext);
        if (null != mContext && null != mReceiver_view)
            mContext.unregisterReceiver(mReceiver_view);
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(false);
        if (null != mListView)
            mListView.setAdapter(null);
        if (null != mProgressBar)
            mProgressBar.setVisibility(View.GONE);
        if (null != mEmptyText)
            mEmptyText.setVisibility(View.GONE);
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            if (null != mContext) {
                int rowsDeleted_route = mContext.getContentResolver().delete(
                        RouteProvider.CONTENT_URI_BOUND_FILTER, null, null);
                Log.d(TAG, "Deleted Route Records: " + rowsDeleted_route);
                int rowsDeleted_routeStop = mContext.getContentResolver().delete(
                        RouteProvider.CONTENT_URI_STOP_FILTER, null, null);
                Log.d(TAG, "Deleted Stops Records: " + rowsDeleted_routeStop);
                int rowsDeleted_eta = mContext.getContentResolver().delete(
                        FollowProvider.CONTENT_URI_ETA_JOIN, null, null);
                Log.d(TAG, "Deleted ETA Records: " + rowsDeleted_eta);
            }
            requestRouteBound();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view != null) {
            TextView textView_origin_tc = (TextView) view.findViewById(R.id.origin);
            TextView textView_destination_tc = (TextView) view.findViewById(R.id.destination);
            RouteBound routeBound = new RouteBound();
            routeBound.route_no = _routeNo;
            routeBound.route_bound = String.valueOf(position + 1);
            routeBound.origin_tc = textView_origin_tc.getText().toString();
            routeBound.destination_tc = textView_destination_tc.getText().toString();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI.BOUND));
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(Constants.BUNDLE.BOUND_OBJECT, routeBound);
            getActivity().startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    @Override
    public void onRefresh() {}

    private void setTaskDescription(String title) {
        // overview task
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription taskDesc =
                    new ActivityManager.TaskDescription(title, bm,
                            ContextCompat.getColor(mContext, R.color.primary_600));
            ((AppCompatActivity) mContext).setTaskDescription(taskDesc);
        }
    }

    private void requestRouteBound() {
        if (null != mAdapter) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
        }
        if (null != mProgressBar)
            mProgressBar.setVisibility(View.VISIBLE);
        Intent intent = new Intent(mContext, RouteService.class);
        intent.putExtra(Constants.BUNDLE.ROUTE_NO, _routeNo);
        mContext.startService(intent);
    }

    UpdateViewHandler mViewHandler = new UpdateViewHandler(this);
    static class UpdateViewHandler extends Handler {
        WeakReference<RouteBoundFragment> mFrag;

        UpdateViewHandler(RouteBoundFragment aFragment) {
            mFrag = new WeakReference<>(aFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            RouteBoundFragment f = mFrag.get();
            if (null == f) return;
            Bundle bundle = msg.getData();
            Boolean aBoolean = bundle.getBoolean(Constants.MESSAGE.BOUNDS_UPDATED);
            if (null != f.mAdapter && aBoolean) {
                if (!Connectivity.isConnected(f.mContext)) {
                    // Check internet connection
                    if (null != f.getView()) {
                        if (null != f.getView().findViewById(android.R.id.content)) {
                            Snackbar snackbar = Snackbar.make(f.getView().findViewById(android.R.id.content),
                                    R.string.message_no_internet_connection, Snackbar.LENGTH_LONG);
                            TextView tv = (TextView)
                                    snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            tv.setTextColor(Color.WHITE);
                            snackbar.show();
                        } else if (null != f.getView().getContext()) {
                            Toast.makeText(f.getView().getContext(),
                                    R.string.message_no_internet_connection,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    if (null != f.mProgressBar)
                        f.mProgressBar.setVisibility(View.GONE);
                    if (null != f.mEmptyText)
                        f.mEmptyText.setText(R.string.message_no_internet_connection);
                    return;
                }
                String routeNo = bundle.getString(Constants.BUNDLE.ROUTE_NO);
                String message = bundle.getString(Constants.BUNDLE.UPDATE_MESSAGE, "");
                switch (message) {
                    case Constants.STATUS.UPDATED_BOUNDS:
                        if (null != routeNo && null != f._routeNo && routeNo.equals(f._routeNo)) {
                            final Cursor c = f.mContext.getContentResolver().query(
                                    RouteProvider.CONTENT_URI_BOUND,
                                    null,
                                    RouteBoundTable.COLUMN_ROUTE + "=? ",
                                    new String[]{
                                            f._routeNo,
                                    }, RouteBoundTable.COLUMN_BOUND + "* 1 ASC");
                            f.mAdapter.clear();
                            while (null != c && c.moveToNext()) {
                                RouteBound object = new RouteBound();
                                object.route_no = getColumnString(c, RouteBoundTable.COLUMN_ROUTE);
                                object.route_bound = getColumnString(c, RouteBoundTable.COLUMN_BOUND);
                                object.destination_tc = getColumnString(c, RouteBoundTable.COLUMN_DESTINATION);
                                object.destination_en = getColumnString(c, RouteBoundTable.COLUMN_DESTINATION_EN);
                                object.origin_tc = getColumnString(c, RouteBoundTable.COLUMN_ORIGIN);
                                object.origin_en = getColumnString(c, RouteBoundTable.COLUMN_ORIGIN_EN);
                                f.mAdapter.add(object);
                            }
                            f.mAdapter.notifyDataSetChanged();
                            if (null != c)
                                c.close();
                            // save history
                            ContentValues values = new ContentValues();
                            values.put(SuggestionTable.COLUMN_TEXT, routeNo);
                            values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_HISTORY);
                            values.put(SuggestionTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
                            f.mContext.getContentResolver().insert(SuggestionProvider.CONTENT_URI, values);
                            if (null != f.mEmptyText)
                                f.mEmptyText.setText("");
                            if (null != f.mProgressBar)
                                f.mProgressBar.setVisibility(View.GONE);
                            if (null != f.mSwipeRefreshLayout)
                                f.mSwipeRefreshLayout.setRefreshing(false);
                        }
                        break;
                    case Constants.STATUS.CONNECTIVITY_INVALID:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(R.string.message_no_internet_connection);
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.GONE);
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(false);
                        break;
                    case Constants.STATUS.CONNECT_FAIL:
                    case Constants.STATUS.CONNECT_404:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(R.string.message_fail_to_request);
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.GONE);
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(false);
                        break;
                    case Constants.STATUS.UPDATING_BOUNDS:
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.VISIBLE);
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(R.string.message_loading);
                        break;
                    default:
                        if (null != f.mEmptyText)
                            f.mEmptyText.setText(message);
                        if (null != f.mProgressBar)
                            f.mProgressBar.setVisibility(View.GONE);
                        if (null != f.mSwipeRefreshLayout)
                            f.mSwipeRefreshLayout.setRefreshing(false);
                        break;
                }
            }
        }

        private String getColumnString(Cursor cursor, String column) {
            int index = cursor.getColumnIndex(column);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        }
    }

    class UpdateViewReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Message message = mViewHandler.obtainMessage();
                    message.setData(bundle);
                    mViewHandler.sendMessage(message);
                }
            };
            thread.run();
        }
    }

}