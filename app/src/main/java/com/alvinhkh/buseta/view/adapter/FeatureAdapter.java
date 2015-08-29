package com.alvinhkh.buseta.view.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.Constants;
import com.alvinhkh.buseta.database.FavouriteDatabase;
import com.alvinhkh.buseta.holder.RouteBound;
import com.alvinhkh.buseta.holder.RouteStop;
import com.alvinhkh.buseta.view.MainActivity;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RecyclerViewHolder;
import com.alvinhkh.buseta.holder.SearchHistory;
import com.alvinhkh.buseta.database.SuggestionsDatabase;
import com.alvinhkh.buseta.view.dialog.RouteEtaDialog;

/*
 * An adapter that handle both favourite stop and search history
 * show favourite in front of search history
 */
public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.ViewHolder> {

    private static final String TAG = "FeatureAdapter";

    private static final int ITEM_VIEW_TYPE_FAVOURITE = 0;
    private static final int ITEM_VIEW_TYPE_HISTORY = 1;

    private Activity mActivity;
    private Cursor mCursor_history;
    private Cursor mCursor_favourite;

    public FeatureAdapter(Activity activity, Cursor cursor_history, Cursor cursor_favourite) {
        mActivity = activity;
        mCursor_history = cursor_history;
        mCursor_favourite = cursor_favourite;
    }

    @Override
    public int getItemCount() {
        return getHistoryCount() + getFavouriteCount();
    }

    public int getHistoryCount() {
        return (mCursor_history == null) ? 0 : mCursor_history.getCount();
    }

    public int getFavouriteCount() {
        return (mCursor_favourite == null) ? 0 : mCursor_favourite.getCount();
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

    public Cursor swapFavouriteCursor(Cursor cursor) {
        if (mCursor_favourite == cursor)
            return null;
        Cursor oldCursor = mCursor_favourite;
        this.mCursor_favourite = cursor;
        if (cursor != null)
            this.notifyDataSetChanged();
        return oldCursor;
    }

    private RouteStop getFavouriteItem(int position) {
        mCursor_favourite.moveToPosition(position);
        // Load data from dataCursor and return it...
        RouteBound routeBound = new RouteBound();
        routeBound.route_no = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_ROUTE));
        routeBound.route_bound = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_BOUND));
        routeBound.origin_tc = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_ORIGIN));
        routeBound.destination_tc = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_DESTINATION));
        RouteStop routeStop = new RouteStop();
        routeStop.route_bound = routeBound;
        routeStop.stop_seq = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_STOP_SEQ));
        routeStop.name_tc = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_STOP_NAME));
        routeStop.code = mCursor_favourite.getString(mCursor_favourite.getColumnIndex(FavouriteDatabase.COLUMN_STOP_CODE));
        routeStop.favourite = true;
        return routeStop;
    }

    private SearchHistory getHistoryItem(int position) {
        mCursor_history.moveToPosition(position);
        // Load data from dataCursor and return it...
        String text = mCursor_history.getString(mCursor_history.getColumnIndex(SuggestionsDatabase.COLUMN_TEXT));
        String type = mCursor_history.getString(mCursor_history.getColumnIndex(SuggestionsDatabase.COLUMN_TYPE));

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.route = text;
        searchHistory.record_type = type;
        return searchHistory;
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, int position) {
        if (vh instanceof FavouriteViewHolder) {
            FavouriteViewHolder viewHolder =  (FavouriteViewHolder) vh;
            RouteStop info = getFavouriteItem(position);
            if (null != info && null != info.route_bound) {
                viewHolder.stop_code.setText(info.code);
                viewHolder.stop_seq.setText(info.stop_seq);
                viewHolder.route_bound.setText(info.route_bound.route_bound);
                viewHolder.stop_name.setText(info.name_tc);
                viewHolder.route_no.setText(info.route_bound.route_no);
                viewHolder.route_destination.setText(info.route_bound.destination_tc);
                viewHolder.eta.setText("");
                viewHolder.eta_more.setText("");
            }
        }
        if (vh instanceof HistoryViewHolder) {
            SearchHistory info = getHistoryItem(position - getFavouriteCount());
            HistoryViewHolder viewHolder = (HistoryViewHolder) vh;
            viewHolder.vRoute.setText(info.route);
            Integer image;
            switch (info.record_type) {
                case SuggestionsDatabase.TYPE_HISTORY:
                    image = R.drawable.ic_history_black_24dp;
                    break;
                case SuggestionsDatabase.TYPE_DEFAULT:
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
        if (viewType == ITEM_VIEW_TYPE_FAVOURITE) {
            View v = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.card_favourite, viewGroup, false);
            ViewHolder vh = new FavouriteViewHolder(v, new RecyclerViewHolder.ViewHolderClicks() {
                public void onClickView(View caller) {
                    if (null == mActivity) return;
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
                    routeStop.favourite = true;
                    // Go to route stop fragment
                    ((MainActivity) mActivity).showRouteBoundFragment(routeBound.route_no);
                    ((MainActivity) mActivity).showRouteStopFragment(routeBound);
                    // Open stop dialog
                    Intent intent = new Intent(caller.getContext(), RouteEtaDialog.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Constants.BUNDLE.ITEM_POSITION, routeStop.stop_seq);
                    intent.putExtra(Constants.BUNDLE.STOP_OBJECT, routeStop);
                    // mActivity.startActivity(intent);
                }
                public boolean onLongClickView(View caller) {
                    TextView tRouteNo = (TextView) caller.findViewById(R.id.route_no);
                    TextView tRouteBound = (TextView) caller.findViewById(R.id.route_bound);
                    TextView tStopCode = (TextView) caller.findViewById(R.id.stop_code);
                    TextView tStopName = (TextView) caller.findViewById(R.id.stop_name);
                    final String route_no = tRouteNo.getText().toString();
                    final String route_bound = tRouteBound.getText().toString();
                    final String stop_code = tStopCode.getText().toString();
                    final String stop_name = tStopName.getText().toString();
                    new AlertDialog.Builder(mActivity)
                            .setTitle(stop_name + "?")
                            .setMessage(mActivity.getString(R.string.message_remove_from_favourite))
                            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    dialoginterface.cancel();
                                }})
                            .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialoginterface, int i) {
                                    FavouriteDatabase mDatabase = new FavouriteDatabase(mActivity.getApplicationContext());
                                    mDatabase.delete(route_no, route_bound, stop_code);
                                    mCursor_favourite = mDatabase.get();
                                    notifyDataSetChanged();
                                }
                            })
                            .show();
                    return true;
                }
            });
            return vh;
        }
        View v = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_history, viewGroup, false);
        ViewHolder vh = new HistoryViewHolder(v, new RecyclerViewHolder.ViewHolderClicks() {
            public void onClickView(View caller) {
                TextView textView = (TextView) caller.findViewById(android.R.id.text1);
                String _route_no = textView.getText().toString();
                ((MainActivity) mActivity).showRouteBoundFragment(_route_no);
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
                                SuggestionsDatabase mDatabase = new SuggestionsDatabase(mActivity.getApplicationContext());
                                mDatabase.deleteHistory(_route_no);
                                mCursor_history = mDatabase.getHistory();
                                notifyDataSetChanged();
                            }
                        })
                        .show();
                return true;
            }
        });
        return vh;
    }
    @Override
    public int getItemViewType(int position) {
        return position < getFavouriteCount() ? ITEM_VIEW_TYPE_FAVOURITE : ITEM_VIEW_TYPE_HISTORY;
    }

    public static class FavouriteViewHolder extends ViewHolder {

        protected TextView stop_code;
        protected TextView stop_seq;
        protected TextView route_bound;
        protected TextView stop_name;
        protected TextView eta;
        protected TextView route_no;
        protected TextView route_destination;
        protected TextView eta_more;

        public FavouriteViewHolder(View v, ViewHolderClicks clicks) {
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