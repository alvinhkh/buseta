package com.alvinhkh.buseta;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    private Context mContext = super.getActivity();
    private ActionBar mActionBar = null;
    private TextView mTextView_routeNo;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mListView;
    private TextView mEmptyText;
    private ProgressBar mProgressBar;

    private RouteNewsAdapter mAdapter;
    private String _route_no = null;

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
        // Set Toolbar
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setSubtitle(null);
        mActionBar.setDisplayHomeAsUpEnabled(false);
        // Set List Adapter
        mAdapter = new RouteNewsAdapter(mContext);
        if (savedInstanceState != null) {
            mAdapter.onRestoreInstanceState(savedInstanceState);
        }
        //
        mTextView_routeNo = (TextView) view.findViewById(R.id.route_no);
        mTextView_routeNo.setText(_route_no);
        //
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_route);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled(true);
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
        } else {
            // Get Route Bounds
            getNotices(_route_no);
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
        }
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
        if (null != mSwipeRefreshLayout)
            mSwipeRefreshLayout.setRefreshing(false);
        if (null != mListView)
            mListView.setAdapter(null);
        View view = getView();
        if (null != view)
            view.setVisibility(View.GONE);
        Ion.getDefault(mContext).cancelAll(mContext);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view != null) {
            TextView textView_title = (TextView) view.findViewById(android.R.id.text1);
            TextView textView_link= (TextView) view.findViewById(R.id.notice_link);
            String noticeTitle = textView_title.getText().toString();
            String noticeLink = textView_link.getText().toString();
            if (noticeLink.contains(".jpg")) {
                ((MainActivity) getActivity()).showNoticeImageFragment(
                        noticeTitle,
                        noticeLink);
            } else if (noticeLink.contains(".htm")) {
                getNoticeText(noticeTitle, noticeLink);
            } else {
                Log.d(TAG, noticeTitle + "\n" + noticeLink);
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
        if (null != mAdapter)
            mAdapter.clear();
        getNotices(_route_no);
    }

    private void getNotices(final String _route_no) {

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

                                if (mEmptyText != null)
                                    mEmptyText.setText("");

                            } else {
                                if (mEmptyText != null)
                                    mEmptyText.setText(R.string.message_no_passenger_notice);
                            }
                        }
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.GONE);
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void getNoticeText(final String title, final String link) {

        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);

        Ion.with(mContext)
                .load(Constants.URL.ROUTE_NOTICES + link)
                        //.setLogging("Ion", Log.DEBUG)
                .setHeader("Referer", Constants.URL.ROUTE_NEWS + "?lang=chi&routeno=" + _route_no)
                .setHeader("Pragma", "no-cache")
                .setHeader("User-Agent", Constants.URL.REQUEST_UA)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        Boolean shown = false;
                        if (e != null) {
                            Log.d(TAG, e.toString());
                        }
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
                                    new AlertDialog.Builder(mContext)
                                            .setTitle(title)
                                            .setMessage(sb.toString())
                                            .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialoginterface, int i) {
                                                    dialoginterface.cancel();
                                                }
                                            })
                                            .show();
                                }
                            }
                        }
                        if (shown == false) {
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