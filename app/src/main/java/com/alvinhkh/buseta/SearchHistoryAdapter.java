package com.alvinhkh.buseta;

import android.app.Activity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private Activity mActivity;
    private Cursor mCursor;

    public SearchHistoryAdapter(Activity activity, Cursor cursor) {
        mActivity = activity;
        mCursor = cursor;
    }

    @Override
    public int getItemCount() {
        return (mCursor == null) ? 0 : mCursor.getCount();
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    public Cursor swapCursor(Cursor cursor) {
        if (mCursor == cursor) {
            return null;
        }
        Cursor oldCursor = mCursor;
        this.mCursor = cursor;
        if (cursor != null) {
            this.notifyDataSetChanged();
        }
        return oldCursor;
    }

    private SearchHistory getItem(int position) {
        mCursor.moveToPosition(position);
        // Load data from dataCursor and return it...
        String text = mCursor.getString(mCursor.getColumnIndex(SuggestionsDatabase.COLUMN_TEXT));
        String type = mCursor.getString(mCursor.getColumnIndex(SuggestionsDatabase.COLUMN_TYPE));

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.route = text;
        searchHistory.record_type = type;
        return searchHistory;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        SearchHistory info = getItem(i);
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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.card_route, viewGroup, false);
        SearchHistoryAdapter.ViewHolder vh = new ViewHolder(v, new ViewHolder.ViewHolderClicks() {
            public void onClickView(View caller) {
                TextView textView = (TextView) caller.findViewById(android.R.id.text1);
                String _route_no = textView.getText().toString();
                ((MainActivity) mActivity).showRouteBoundFragment(_route_no);
            }

            public boolean onLongClickView(View caller) {
                TextView textView = (TextView) caller.findViewById(android.R.id.text1);
                final String _route_no = textView.getText().toString();
                new AlertDialog.Builder(mActivity)
                        .setTitle(mActivity.getString(R.string.message_confirm_delete_history))
                        .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                dialoginterface.cancel();
                            }})
                        .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                SuggestionsDatabase mDatabase = new SuggestionsDatabase(mActivity.getApplicationContext());
                                mDatabase.deleteHistory(_route_no);
                                mCursor = mDatabase.getHistory();
                                notifyDataSetChanged();
                            }
                        })
                        .show();
                return true;
            }
        });
        return vh;
    }

    public static class ViewHolder extends RecyclerViewHolder {

        protected TextView vRoute;
        protected ImageView vRecordType;

        public ViewHolder(View v, ViewHolderClicks clicks) {
            super(v, clicks);
            vRoute = (TextView) v.findViewById(android.R.id.text1);
            vRecordType = (ImageView)  v.findViewById(R.id.icon);
        }

    }
}