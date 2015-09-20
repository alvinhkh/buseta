package com.alvinhkh.buseta.view.fragment;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.view.MainActivity;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteNews;
import com.alvinhkh.buseta.view.adapter.RouteNewsAdapter;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RouteNewsFragment extends Fragment
        implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "RouteNewsFragment";
    private static final String KEY_LIST_VIEW_STATE = "KEY_LIST_VIEW_STATE_ROUTE_BOUND";
    private static final String ROUTE_NEWS_TEXT = "ROUTE_NEWS_TEXT";

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;
    private AlertDialog mAlertDialog;

    private RouteNewsAdapter mAdapter;
    private String _route_no = null;
    private RouteNews _routeNews = null;

    public RouteNewsFragment() {
    }

    public static RouteNewsFragment newInstance(String _route_no) {
        RouteNewsFragment f = new RouteNewsFragment();
        Bundle args = new Bundle();
        args.putString("route_no", _route_no);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routenews, container, false);
        mContext = super.getActivity();
        // Get arguments
        _route_no = getArguments().getString("route_no");
        // Overview task
        setTaskDescription(_route_no + " " + getString(R.string.passenger_notice) +
                getString(R.string.interpunct) + getString(R.string.launcher_name));
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setTitle(R.string.app_name);
            mActionBar.setSubtitle(null);
            mActionBar.setDisplayHomeAsUpEnabled(false);
        }
        setHasOptionsMenu(true);
        // Set List Adapter
        mAdapter = new RouteNewsAdapter(mContext);
        if (savedInstanceState != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState);
        }
        //
        TextView mTextView_routeNo = (TextView) view.findViewById(R.id.route_no);
        mTextView_routeNo.setText(_route_no);
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
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_LIST_VIEW_STATE)) {
            mListView.onRestoreInstanceState(savedInstanceState
                    .getParcelable(KEY_LIST_VIEW_STATE));
            mEmptyText.setText(savedInstanceState.getString("EmptyText", ""));
        } else {
            // Get Route Bounds
            getNotices(_route_no);
        }
        if (savedInstanceState != null
                && savedInstanceState.containsKey(ROUTE_NEWS_TEXT)) {
            _routeNews = savedInstanceState.getParcelable(ROUTE_NEWS_TEXT);
            showDialog();
        }
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        //mListView.setOnItemLongClickListener(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("_route_no", _route_no);
        if (null != mAdapter) {
            mAdapter.onSaveInstanceState(outState);
            outState.putParcelable(KEY_LIST_VIEW_STATE, mListView.onSaveInstanceState());
            outState.putParcelable(ROUTE_NEWS_TEXT, _routeNews);
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
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(false);
        if (null != mListView)
            mListView.setAdapter(null);
        if (null != mProgressBar)
            mProgressBar.setVisibility(View.GONE);
        if (null != mEmptyText)
            mEmptyText.setVisibility(View.GONE);
        if (null != mAlertDialog)
            mAlertDialog.dismiss();
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
            onRefresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view != null) {
            _routeNews = null;
            TextView textView_title = (TextView) view.findViewById(android.R.id.text1);
            TextView textView_link= (TextView) view.findViewById(R.id.notice_link);
            RouteNews routeNews = new RouteNews();
            routeNews.title = textView_title.getText().toString();
            routeNews.link = textView_link.getText().toString();
            if (routeNews.link.contains(".jpg")) {
                ((MainActivity) getActivity()).showNoticeImageFragment(routeNews);
            } else if (routeNews.link.contains(".htm")) {
                getNoticeText(routeNews);
            } else {
                Log.d(TAG, routeNews.title + "\n" + routeNews.link);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    @Override
    public void onRefresh() {
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(true);
        getNotices(_route_no);
    }

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

    private void showDialog() {
        if (null == _routeNews || null == _routeNews.title || null == _routeNews.text) return;
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setTitle(_routeNews.title)
                .setMessage(_routeNews.text)
                .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        _routeNews = null;
                        dialoginterface.cancel();
                    }
                }).show();
    }

    private void getNotices(final String _route_no) {

        if (null != mAdapter) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
        }

        // Check internet connection
        final ConnectivityManager conMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content),
                    R.string.message_no_internet_connection, Snackbar.LENGTH_LONG);
            TextView tv = (TextView)
                    snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            snackbar.show();
            if (mProgressBar != null)
                mProgressBar.setVisibility(View.GONE);
            if (mEmptyText != null)
                mEmptyText.setText(R.string.message_fail_to_request);
            return;
        }

        if (mEmptyText != null)
            mEmptyText.setText(R.string.message_loading);
        if (mProgressBar != null)
            mProgressBar.setVisibility(View.VISIBLE);

        Uri routeInfoUri = Uri.parse(Constants.URL.ROUTE_NEWS)
                .buildUpon()
                .appendQueryParameter("lang", "chi")
                .appendQueryParameter("routeno", _route_no)
                .build();

        Ion.with(mContext)
                .load(routeInfoUri.toString())
                        //.setLogging("Ion", Log.DEBUG)
                .progressBar(mProgressBar)
                .setHeader("Referer", Constants.URL.HTML_SEARCH)
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        if (e != null) {
                            Log.d(TAG, e.toString());
                            if (mEmptyText != null)
                                mEmptyText.setText(R.string.message_fail_to_request);
                        }
                        if (result != null) {
                            Document doc = Jsoup.parse(result);
                            Elements li = doc.select("li");
                            if (li.size() > 0) {
                                //  Got Bus Route News
                                for (int i = 0; i < li.size(); i++) {
                                    RouteNews routeNews = new RouteNews();
                                    routeNews.title = li.get(i).text();
                                    String link = li.get(i).select("a").attr("href");
                                    Pattern pattern = Pattern.compile("^/tc/news/realtimenews\\.html\\?page=(.*)$");
                                    Matcher matcher = pattern.matcher(link);
                                    if (matcher.find())
                                        routeNews.link = matcher.group(1);
                                    mAdapter.add(routeNews);
                                }
                            } else {
                                RouteNews routeNews = new RouteNews();
                                routeNews.title = getString(R.string.message_no_passenger_notice);
                                mAdapter.add(routeNews);
                            }
                            if (mEmptyText != null)
                                mEmptyText.setText("");
                        }
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.GONE);
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void getNoticeText(final RouteNews routeNews) {
        if (null == routeNews || null == routeNews.title || null == routeNews.link) return;

        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);
        Ion.with(mContext)
                .load(Constants.URL.ROUTE_NOTICES + routeNews.link)
                .setHeader("Referer", Constants.URL.ROUTE_NEWS + "?lang=chi&routeno=" + _route_no)
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        Boolean shown = false;
                        if (e != null)
                            Log.d(TAG, e.toString());
                        if (result != null) {
                            Document doc = Jsoup.parse(result);
                            Element body = doc.select("div.itemContent").first();
                            if (null != body) {
                                Elements p = body.select("p");
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < p.size(); i++) {
                                    sb.append(p.get(i).text());
                                    if (i < p.size() - 1)
                                        sb.append("\n\n");
                                }
                                if (!sb.toString().equals("")) {
                                    shown = true;
                                    routeNews.text = sb.toString();
                                    _routeNews = routeNews;
                                    showDialog();
                                }
                            }
                        }
                        if (!shown) {
                            Snackbar snackbar = Snackbar.make(getActivity().findViewById(R.id.fragment_container),
                                    R.string.message_fail_to_request,
                                    Snackbar.LENGTH_SHORT);
                            TextView tv = (TextView)
                                    snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                            tv.setTextColor(Color.WHITE);
                            snackbar.show();
                        }
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

}