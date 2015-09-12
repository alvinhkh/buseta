package com.alvinhkh.buseta.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.database.EtaTable;
import com.alvinhkh.buseta.database.FavouriteProvider;
import com.alvinhkh.buseta.database.FavouriteTable;
import com.alvinhkh.buseta.holder.EtaAdapterHelper;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopETA;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class EtaWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor;
    private int mAppWidgetId;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
        if (mCursor != null)
            mCursor.close();
    }

    public int getCount() {
        return mCursor.getCount();
    }

    private String getColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    private RouteStop getItem(int position) {
        RouteStop routeStop = null;
        if (mCursor.moveToPosition(position)) {
            // Load data from cursor and return it...
            RouteBound routeBound = new RouteBound();
            routeBound.route_no = getColumnString(mCursor, FavouriteTable.COLUMN_ROUTE);
            routeBound.route_bound = getColumnString(mCursor, FavouriteTable.COLUMN_BOUND);
            routeBound.origin_tc = getColumnString(mCursor, FavouriteTable.COLUMN_ORIGIN);
            routeBound.destination_tc = getColumnString(mCursor, FavouriteTable.COLUMN_DESTINATION);
            RouteStopETA routeStopETA = null;
            String apiVersion = getColumnString(mCursor, EtaTable.COLUMN_ETA_API);
            if (null != apiVersion && !apiVersion.equals("")) {
                routeStopETA = new RouteStopETA();
                routeStopETA.api_version = Integer.valueOf(apiVersion);
                routeStopETA.seq = getColumnString(mCursor, EtaTable.COLUMN_STOP_SEQ);
                routeStopETA.etas = getColumnString(mCursor, EtaTable.COLUMN_ETA_TIME);
                routeStopETA.expires = getColumnString(mCursor, EtaTable.COLUMN_ETA_EXPIRE);
                routeStopETA.server_time = getColumnString(mCursor, EtaTable.COLUMN_SERVER_TIME);
                routeStopETA.updated = getColumnString(mCursor, EtaTable.COLUMN_UPDATED);
            }
            routeStop = new RouteStop();
            routeStop.route_bound = routeBound;
            routeStop.stop_seq = getColumnString(mCursor, FavouriteTable.COLUMN_STOP_SEQ);
            routeStop.name_tc = getColumnString(mCursor, FavouriteTable.COLUMN_STOP_NAME);
            routeStop.code = getColumnString(mCursor, FavouriteTable.COLUMN_STOP_CODE);
            routeStop.favourite = true;
            routeStop.eta = routeStopETA;
            routeStop.eta_loading = getColumnString(mCursor, EtaTable.COLUMN_LOADING).equals("true");
            routeStop.eta_fail = getColumnString(mCursor, EtaTable.COLUMN_FAIL).equals("true");
        }
        return routeStop;
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        RouteStop object = getItem(position);
        // Return a proper item
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item_eta);
        final ConnectivityManager conMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            rv.setViewVisibility(R.id.textView_text, View.VISIBLE);
            rv.setTextViewText(R.id.textView_text,
                    mContext.getString(R.string.message_no_internet_connection));
        } else {
            rv.setViewVisibility(R.id.textView_text, View.GONE);
            rv.setTextViewText(R.id.textView_text, "");
        }
        if (null != object&& null != object.route_bound) {
            // load data
            rv.setTextViewText(R.id.stop_code, object.code);
            rv.setTextViewText(R.id.stop_seq, object.stop_seq);
            rv.setTextViewText(R.id.route_bound, object.route_bound.route_bound);
            rv.setTextViewText(R.id.stop_name, object.name_tc);
            rv.setTextViewText(R.id.route_no, object.route_bound.route_no);
            rv.setTextViewText(R.id.route_destination, object.route_bound.destination_tc);
            rv.setTextViewText(R.id.eta, "");
            rv.setTextViewText(R.id.eta_more, "");
            // eta
            if (object.eta_loading != null && object.eta_loading) {
                rv.setTextViewText(R.id.eta_more, mContext.getString(R.string.message_loading));
            } else if (object.eta_fail != null && object.eta_fail) {
                rv.setTextViewText(R.id.eta_more, mContext.getString(R.string.message_fail_to_request));
            } else if (null != object.eta) {
                if (object.eta.etas.equals("") && object.eta.expires.equals("")) {
                    rv.setTextViewText(R.id.eta_more, mContext.getString(R.string.message_no_data)); // route does not support eta
                } else {
                    // Request Time
                    Date server_date = EtaAdapterHelper.serverDate(object);
                    // ETAs
                    if (object.eta.etas.equals("")) {
                        // eta not available
                        rv.setTextViewText(R.id.eta, mContext.getString(R.string.message_no_data));
                    } else {
                        Document doc = Jsoup.parse(object.eta.etas);
                        //Log.d("RouteStopAdapter", doc.toString());
                        String text = doc.text().replaceAll(" ?　?預定班次", "");
                        String[] etas = text.split(", ?");
                        Pattern pattern = Pattern.compile("到達([^/離開]|$)");
                        Matcher matcher = pattern.matcher(text);
                        int count = 0;
                        while (matcher.find())
                            count++; //count any matched pattern
                        if (count > 1 && count == etas.length) {
                            // more than one and all same, more likely error
                            rv.setTextViewText(R.id.eta, mContext.getString(R.string.message_please_click_once_again));
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < etas.length; i++) {
                                sb.append(etas[i]);
                                String estimate = EtaAdapterHelper.etaEstimate(object, etas, i, server_date,
                                        null, null, null);
                                sb.append(estimate);
                                if (i == 0) {
                                    rv.setTextViewText(R.id.eta, sb.toString());
                                    sb = new StringBuilder();
                                } else {
                                    if (i < etas.length - 1)
                                        sb.append(" ");
                                }
                            }
                            rv.setTextViewText(R.id.eta_more, sb.toString());
                        }
                    }
                }
            }
            //rv.setScrollPosition(R.id.listView, 5);

            // Set the click intent
            final Intent fillInIntent = new Intent();
            final Bundle extras = new Bundle();
            extras.putParcelable(Constants.BUNDLE.STOP_OBJECT, object);
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.listView, fillInIntent);
        }

        return rv;
    }
    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        // Technically, we have two types of views (the dark and light background views)
        return 2;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // Refresh the cursor
        if (null != mCursor)
            mCursor.close();
        mCursor = mContext.getContentResolver().query(
                FavouriteProvider.CONTENT_URI, null, null, null,
                FavouriteTable.COLUMN_DATE + " DESC");
    }

}