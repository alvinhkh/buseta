package com.alvinhkh.buseta.view.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.provider.EtaTable;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.FollowTable;
import com.alvinhkh.buseta.holder.EtaAdapterHelper;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.holder.RouteStopETA;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RecyclerViewHolder;
import com.alvinhkh.buseta.holder.SearchHistory;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * An adapter that handle both follow stop and search history
 * show follow in front of search history
 */
public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.ViewHolder> {

    private static final String TAG = FeatureAdapter.class.getSimpleName();

    private static final int ITEM_VIEW_TYPE_FOLLOW = 0;
    private static final int ITEM_VIEW_TYPE_HISTORY = 1;

    private Activity mActivity;
    private Cursor mCursor_history;
    private Cursor mCursor_follow;

    public FeatureAdapter(Activity activity, Cursor cursor_history, Cursor cursor_follow) {
        mActivity = activity;
        mCursor_history = cursor_history;
        mCursor_follow = cursor_follow;
    }

    @Override
    public int getItemCount() {
        return getHistoryCount() + getFollowCount();
    }

    public int getHistoryCount() {
        return (mCursor_history == null) ? 0 : mCursor_history.getCount();
    }

    public int getFollowCount() {
        return (mCursor_follow == null) ? 0 : mCursor_follow.getCount();
    }

    public Cursor swapHistoryCursor(Cursor cursor) {
        if (mCursor_history == cursor)
            return null;
        Cursor oldCursor = mCursor_history;
        this.mCursor_history = cursor;
        if (cursor != null)
            this.notifyDataSetChanged();
        return oldCursor;
    }

    public Cursor swapFollowCursor(Cursor cursor) {
        if (mCursor_follow == cursor)
            return null;
        Cursor oldCursor = mCursor_follow;
        this.mCursor_follow = cursor;
        if (cursor != null)
            this.notifyDataSetChanged();
        return oldCursor;
    }

    private String getFollowColumnString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    public RouteStop getFollowItem(int position) {
        mCursor_follow.moveToPosition(position);
        // Load data from dataCursor and return it...
        RouteBound routeBound = new RouteBound();
        routeBound.route_no = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_ROUTE);
        routeBound.route_bound = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_BOUND);
        routeBound.origin_tc = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_ORIGIN);
        routeBound.destination_tc = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_DESTINATION);
        RouteStopETA routeStopETA = null;
        String apiVersion = getFollowColumnString(mCursor_follow, EtaTable.COLUMN_ETA_API);
        if (null != apiVersion && !apiVersion.equals("")) {
            routeStopETA = RouteStopETA.create(mCursor_follow);
            routeStopETA.api_version = Integer.valueOf(apiVersion);
        }
        RouteStop routeStop = new RouteStop();
        routeStop.route_bound = routeBound;
        routeStop.stop_seq = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_STOP_SEQ);
        routeStop.name_tc = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_STOP_NAME);
        routeStop.code = getFollowColumnString(mCursor_follow, FollowTable.COLUMN_STOP_CODE);
        routeStop.follow = true;
        routeStop.eta = routeStopETA;
        routeStop.eta_loading = getFollowColumnString(mCursor_follow, EtaTable.COLUMN_LOADING).equals("true");
        routeStop.eta_fail = getFollowColumnString(mCursor_follow, EtaTable.COLUMN_FAIL).equals("true");
        return routeStop;
    }

    private SearchHistory getHistoryItem(int position) {
        mCursor_history.moveToPosition(position);
        // Load data from dataCursor and return it...
        String text = mCursor_history.getString(mCursor_history.getColumnIndex(SuggestionTable.COLUMN_TEXT));
        String type = mCursor_history.getString(mCursor_history.getColumnIndex(SuggestionTable.COLUMN_TYPE));

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.route = text;
        searchHistory.record_type = type;
        return searchHistory;
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, int position) {
        if (vh instanceof FollowViewHolder) {
            FollowViewHolder viewHolder =  (FollowViewHolder) vh;
            RouteStop object = getFollowItem(position);
            if (null != object && null != object.route_bound) {
                viewHolder.stop_code.setText(object.code);
                viewHolder.stop_seq.setText(object.stop_seq);
                viewHolder.route_bound.setText(object.route_bound.route_bound);
                viewHolder.stop_name.setText(object.name_tc);
                viewHolder.route_no.setText(object.route_bound.route_no);
                viewHolder.route_destination.setText(object.route_bound.destination_tc);
                viewHolder.eta.setText("");
                viewHolder.eta_more.setText("");
                // eta
                if (object.eta_loading != null && object.eta_loading) {
                    viewHolder.eta_more.setText(R.string.message_loading);
                } else if (object.eta_fail != null && object.eta_fail) {
                    viewHolder.eta_more.setText(R.string.message_fail_to_request);
                } else if (null != object.eta) {
                    if (object.eta.etas.equals("") && object.eta.expires.equals("")) {
                        viewHolder.eta_more.setText(R.string.message_no_data); // route does not support eta
                    } else {
                        // Request Time
                        Date server_date = EtaAdapterHelper.serverDate(object);
                        // ETAs
                        if (object.eta.etas.equals("")) {
                            // eta not available
                            viewHolder.eta.setText(R.string.message_no_data);
                        } else {
                            String etaText = EtaAdapterHelper.getText(object.eta.etas);
                            String[] etas = etaText.split(", ?");
                            Pattern pattern = Pattern.compile("到達([^/離開]|$)");
                            Matcher matcher = pattern.matcher(etaText);
                            String[] scheduled = object.eta.scheduled.split(", ?");
                            String[] wheelchairs = object.eta.wheelchair.split(", ?");
                            int count = 0;
                            while (matcher.find())
                                count++; //count any matched pattern
                            if (count > 1 && count == etas.length) {
                                // more than one and all same, more likely error
                                viewHolder.eta.setText(R.string.message_please_click_once_again);
                            } else {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < etas.length; i++) {
                                    if (scheduled.length > i && scheduled[i] != null
                                            && scheduled[i].equals("Y")) {
                                        // scheduled bus
                                        sb.append("*");
                                    }
                                    sb.append(etas[i]);
                                    String estimate = EtaAdapterHelper.etaEstimate(object, etas, i, server_date
                                            , mActivity, viewHolder.eta, viewHolder.eta_more);
                                    sb.append(estimate);
                                    if (wheelchairs.length > i && wheelchairs[i] != null
                                            && wheelchairs[i].equals("Y")
                                            && EtaAdapterHelper.isShowWheelchairIcon(mActivity)) {
                                        // wheelchair emoji
                                        sb.append(" ");
                                        sb.append(new String(Character.toChars(0x267F)));
                                    }
                                    if (i == 0) {
                                        viewHolder.eta.setText(sb.toString());
                                        sb = new StringBuilder();
                                    } else {
                                        if (i < etas.length - 1)
                                            sb.append(" ");
                                    }
                                }
                                viewHolder.eta_more.setText(sb.toString());
                            }
                        }
                        if (viewHolder.eta.getText().equals("")) {
                            viewHolder.eta.setText(viewHolder.eta_more.getText());
                            viewHolder.eta.setTextColor(ContextCompat.getColor(mActivity, R.color.diminish_text));
                            viewHolder.eta_more.setVisibility(View.GONE);
                        }
                    }
                }
            }
        }
        if (vh instanceof HistoryViewHolder) {
            SearchHistory info = getHistoryItem(position - getFollowCount());
            HistoryViewHolder viewHolder = (HistoryViewHolder) vh;
            viewHolder.vRoute.setText(info.route);
            Integer image;
            switch (info.record_type) {
                case SuggestionTable.TYPE_HISTORY:
                    image = R.drawable.ic_history_black_24dp;
                    break;
                case SuggestionTable.TYPE_DEFAULT:
                default:
                    image = R.drawable.ic_directions_bus_black_24dp;
                    break;
            }
            if (viewHolder.vRecordType != null && image > 0) {
                viewHolder.vRecordType.setImageResource(image);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_FOLLOW) {
            View v = LayoutInflater.from(viewGroup.getContext()).
                    inflate(R.layout.card_follow, viewGroup, false);
            return new FollowViewHolder(v, new RecyclerViewHolder.ViewHolderClicks() {
                private RouteStop getObject(View caller) {
                    TextView tRouteNo = (TextView) caller.findViewById(R.id.route_no);
                    TextView tRouteBound = (TextView) caller.findViewById(R.id.route_bound);
                    TextView tOrigin = (TextView) caller.findViewById(R.id.route_origin);
                    TextView tDestination = (TextView) caller.findViewById(R.id.route_destination);
                    TextView tStopSeq = (TextView) caller.findViewById(R.id.stop_seq);
                    TextView tStopCode = (TextView) caller.findViewById(R.id.stop_code);
                    TextView tStopName = (TextView) caller.findViewById(R.id.stop_name);
                    RouteBound routeBound = new RouteBound();
                    routeBound.route_no = tRouteNo.getText().toString();
                    routeBound.route_bound = tRouteBound.getText().toString();
                    routeBound.origin_tc = tOrigin.getText().toString();
                    routeBound.destination_tc = tDestination.getText().toString();
                    RouteStop routeStop = new RouteStop();
                    routeStop.route_bound = routeBound;
                    routeStop.stop_seq = tStopSeq.getText().toString();
                    routeStop.code = tStopCode.getText().toString();
                    routeStop.name_tc = tStopName.getText().toString();
                    routeStop.follow = true;
                    return routeStop;
                }
                public void onClickView(View caller) {
                    if (null == mActivity) return;
                    RouteStop routeStop = getObject(caller);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI.STOP));
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                    mActivity.startActivity(intent);
                }
                public boolean onLongClickView(View caller) {
                    if (null == mActivity) return false;
                    final RouteStop object = getObject(caller);
                    if (null == object || null == object.route_bound) return false;
                    new AlertDialog.Builder(mActivity)
                            .setTitle(object.route_bound.route_no + " " +
                                    object.name_tc + "?")
                            .setMessage(mActivity.getString(R.string.message_remove_from_follow_list))
                            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    dialoginterface.cancel();
                                }
                            })
                            .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    mActivity.getContentResolver().delete(
                                            FollowProvider.CONTENT_URI_FOLLOW,
                                            FollowTable.COLUMN_ROUTE + " = ?" +
                                                    " AND " + FollowTable.COLUMN_BOUND + " = ?" +
                                                    " AND " + FollowTable.COLUMN_STOP_CODE + " = ?",
                                            new String[]{
                                                    object.route_bound.route_no,
                                                    object.route_bound.route_bound,
                                                    object.code
                                            });
                                    Intent intent = new Intent(Constants.MESSAGE.FOLLOW_UPDATED);
                                    intent.putExtra(Constants.MESSAGE.FOLLOW_UPDATED, true);
                                    mActivity.sendBroadcast(intent);
                                }
                            })
                            .show();
                    return true;
                }
            });
        }
        View v = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.card_history, viewGroup, false);
        return new HistoryViewHolder(v, new RecyclerViewHolder.ViewHolderClicks() {
            public void onClickView(View caller) {
                if (null == mActivity) return;
                TextView textView = (TextView) caller.findViewById(android.R.id.text1);
                String _route_no = textView.getText().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URI.ROUTE + _route_no));
                mActivity.startActivity(intent);
            }

            public boolean onLongClickView(View caller) {
                TextView textView = (TextView) caller.findViewById(android.R.id.text1);
                final String _route_no = textView.getText().toString();
                new AlertDialog.Builder(mActivity)
                        .setTitle(_route_no + "?")
                        .setMessage(mActivity.getString(R.string.message_remove_from_search_history))
                        .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                dialoginterface.cancel();
                            }})
                        .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                mActivity.getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                                        SuggestionTable.COLUMN_TYPE + "=? AND " + SuggestionTable.COLUMN_TEXT + "=?",
                                        new String[]{
                                                SuggestionTable.TYPE_HISTORY,
                                                _route_no
                                        });
                                Intent intent = new Intent(Constants.MESSAGE.HISTORY_UPDATED);
                                intent.putExtra(Constants.MESSAGE.HISTORY_UPDATED, true);
                                mActivity.sendBroadcast(intent);
                            }
                        })
                        .show();
                return true;
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return position < getFollowCount() ? ITEM_VIEW_TYPE_FOLLOW : ITEM_VIEW_TYPE_HISTORY;
    }

    public static class FollowViewHolder extends ViewHolder {

        protected TextView stop_code;
        protected TextView stop_seq;
        protected TextView route_bound;
        protected TextView stop_name;
        protected TextView eta;
        protected TextView route_no;
        protected TextView route_destination;
        protected TextView eta_more;

        public FollowViewHolder(View v, ViewHolderClicks clicks) {
            super(v, clicks);
            stop_code = (TextView) v.findViewById(R.id.stop_code);
            stop_seq = (TextView) v.findViewById(R.id.stop_seq);
            route_bound = (TextView) v.findViewById(R.id.route_bound);
            stop_name = (TextView) v.findViewById(R.id.stop_name);
            eta = (TextView) v.findViewById(R.id.eta);
            route_no = (TextView) v.findViewById(R.id.route_no);
            route_destination = (TextView) v.findViewById(R.id.route_destination);
            eta_more = (TextView) v.findViewById(R.id.eta_more);
        }

    }

    public static class HistoryViewHolder extends ViewHolder {

        protected TextView vRoute;
        protected ImageView vRecordType;

        public HistoryViewHolder(View v, ViewHolderClicks clicks) {
            super(v, clicks);
            vRoute = (TextView) v.findViewById(android.R.id.text1);
            vRecordType = (ImageView)  v.findViewById(R.id.icon);
        }

    }

    public static class ViewHolder extends RecyclerViewHolder {

        public ViewHolder(View v, ViewHolderClicks clicks) {
            super(v, clicks);
        }

    }
}