package com.alvinhkh.buseta.ui.follow;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.SearchHistory;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;
import com.alvinhkh.buseta.ui.search.SearchActivity;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.alvinhkh.buseta.utils.PreferenceUtil;
import com.alvinhkh.buseta.utils.SearchHistoryUtil;

import java.util.ArrayList;
import java.util.List;

/*
 * An adapter that handle both follow stop and search history
 * display follow item above history item
 */
public class FollowAndHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int ITEM_VIEW_TYPE_FOLLOW = 0;

    public static final int ITEM_VIEW_TYPE_HISTORY = 1;

    private Context context;

    private Cursor historyCursor;

    private Cursor followCursor;

    FollowAndHistoryAdapter(Context context) {
        this.context = context;
        this.historyCursor = null;
        this.followCursor = null;
    }

    public void close() {
        if (followCursor != null) {
            followCursor.close();
        }
        if (historyCursor != null) {
            historyCursor.close();
        }
    }

    @Override
    public int getItemCount() {
        return getHistoryCount() + getFollowCount();
    }

    public Object getItem(int position) {
        if (getItemViewType(position) == ITEM_VIEW_TYPE_FOLLOW) {
            return followItem(position);
        } else if (getItemViewType(position) == ITEM_VIEW_TYPE_HISTORY) {
            return historyItem(position - getFollowCount());
        }
        return null;
    }

    int getHistoryCount() {
        return (historyCursor == null) ? 0 : historyCursor.getCount();
    }

    int getFollowCount() {
        return (followCursor == null) ? 0 : followCursor.getCount();
    }

    public List<FollowStop> getFollowItems() {
        List<FollowStop> list = new ArrayList<>();
        for (int i = 0; i < getFollowCount(); i++) {
            list.add((FollowStop) getItem(i));
        }
        return list;
    }

    public List<SearchHistory> getHistoryItems() {
        List<SearchHistory> list = new ArrayList<>();
        for (int i = getFollowCount(); i < getHistoryCount(); i++) {
            list.add((SearchHistory) getItem(i));
        }
        return list;
    }

    void updateHistory() {
        if (context == null) return;
        Cursor newCursor = context.getContentResolver().query(SuggestionProvider.CONTENT_URI, null,
                SuggestionTable.COLUMN_TEXT + " LIKE ?" + " AND " + SuggestionTable.COLUMN_TYPE + " = ?",
                new String[] {
                        "%%",
                        SuggestionTable.TYPE_HISTORY
                }, SuggestionTable.COLUMN_DATE + " DESC");
        if (historyCursor == newCursor) return;
        Cursor oldCursor = historyCursor;
        this.historyCursor = newCursor;
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    void updateFollow() {
        if (context == null) return;
        Cursor newCursor = FollowStopUtil.queryAll(context);
        if (followCursor == newCursor) return;
        Cursor oldCursor = followCursor;
        this.followCursor = newCursor;
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    FollowStop followItem(int position) {
        if (followCursor == null || followCursor.isClosed()) return null;
        followCursor.moveToPosition(position);
        return FollowStopUtil.fromCursor(followCursor);
    }

    SearchHistory historyItem(int position) {
        if (historyCursor == null || historyCursor.isClosed()) return null;
        historyCursor.moveToPosition(position);
        return SearchHistoryUtil.fromCursor(historyCursor);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof FollowViewHolder) {
            FollowViewHolder vh = (FollowViewHolder) viewHolder;
            Context context = vh.itemView.getContext();
            FollowStop object = followItem(position);
            vh.nameText.setText(object.name);
            vh.routeNo.setText(object.route);
            vh.routeLocationEnd.setText(context.getString(R.string.destination, object.locationEnd));
            vh.etaText.setText(null);
            vh.etaNextText.setText(null);
            vh.itemView.setOnClickListener(null);
            vh.itemView.setOnLongClickListener(null);

            vh.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(context, SearchActivity.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT, BusRouteStopUtil.fromFollowStop(object));
                context.startActivity(intent);
            });
            vh.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle(object.route + "?")
                        .setMessage(context.getString(R.string.message_remove_from_follow_list))
                        .setNegativeButton(R.string.action_cancel, (dialoginterface, i) -> dialoginterface.cancel())
                        .setPositiveButton(R.string.action_confirm, (dialoginterface, i) -> {
                            int pos = position;
                            int j = 0;
                            for (FollowStop stop: getFollowItems()) {
                                if (stop._id.equals(object._id)) {
                                    pos = j;
                                    break;
                                }
                                j++;
                            }
                            if (FollowStopUtil.delete(context, object) > 0) {
                                updateFollow();
                                notifyItemRemoved(pos);
                            }
                        })
                        .show();
                return true;
            });

            // ETA
            ArrivalTimeUtil.query(context, BusRouteStopUtil.fromFollowStop(object)).subscribe(cursor -> {
                // Cursor has been moved +1 position forward.
                ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
                if (arrivalTime == null) return;
                arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime);
                if (arrivalTime == null) return;
                if (arrivalTime.id != null) {
                    SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.text);
                    Integer pos = Integer.parseInt(arrivalTime.id);
                    Integer colorInt = ContextCompat.getColor(context,
                            arrivalTime.expired ? R.color.textDiminish :
                                    (pos > 0 ? R.color.textPrimary : R.color.textHighlighted));
                    if (arrivalTime.isSchedule) {
                        etaText.append("*");
                    }
                    if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                        etaText.append(" (").append(arrivalTime.estimate).append(")");
                    }
                    if (arrivalTime.distanceKM >= 0) {
                        etaText.append(" ").append(context.getString(R.string.km_short, arrivalTime.distanceKM));
                    }
                    if (arrivalTime.capacity >= 0) {
                        Drawable drawable = null;
                        if (arrivalTime.capacity == 0) {
                            drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_0_black);
                        } else if (arrivalTime.capacity > 0 && arrivalTime.capacity <= 3) {
                            drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_20_black);
                        } else if (arrivalTime.capacity > 3 && arrivalTime.capacity <= 6) {
                            drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_50_black);
                        } else if (arrivalTime.capacity > 6 && arrivalTime.capacity <= 9) {
                            drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_80_black);
                        } else if (arrivalTime.capacity >= 10) {
                            drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_100_black);
                        }
                        if (drawable != null) {
                            drawable = DrawableCompat.wrap(drawable);
                            if (pos == 0) {
                                drawable.setBounds(0, 0, vh.etaText.getLineHeight(), vh.etaText.getLineHeight());
                            } else {
                                drawable.setBounds(0, 0, vh.etaNextText.getLineHeight(), vh.etaNextText.getLineHeight());
                            }
                            DrawableCompat.setTint(drawable.mutate(), colorInt);
                            ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                            etaText.append(" ");
                            if (etaText.length() > 0) {
                                etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                        }
                    }
                    if (arrivalTime.hasWheelchair && PreferenceUtil.isShowWheelchairIcon(context)) {
                        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_accessible_black_18dp);
                        drawable = DrawableCompat.wrap(drawable);
                        if (pos == 0) {
                            drawable.setBounds(0, 0, vh.etaText.getLineHeight(), vh.etaText.getLineHeight());
                        } else {
                            drawable.setBounds(0, 0, vh.etaNextText.getLineHeight(), vh.etaNextText.getLineHeight());
                        }
                        DrawableCompat.setTint(drawable.mutate(), colorInt);
                        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                        etaText.append(" ");
                        if (etaText.length() > 0) {
                            etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        }
                    }
                    if (arrivalTime.hasWifi && PreferenceUtil.isShowWifiIcon(context)) {
                        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_network_wifi_black_18dp);
                        drawable = DrawableCompat.wrap(drawable);
                        if (pos == 0) {
                            drawable.setBounds(0, 0, vh.etaText.getLineHeight(), vh.etaText.getLineHeight());
                        } else {
                            drawable.setBounds(0, 0, vh.etaNextText.getLineHeight(), vh.etaNextText.getLineHeight());
                        }
                        DrawableCompat.setTint(drawable.mutate(), colorInt);
                        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                        etaText.append(" ");
                        if (etaText.length() > 0) {
                            etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        }
                    }
                    etaText.setSpan(new ForegroundColorSpan(colorInt), 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    switch(pos) {
                        case 0:
                            vh.etaText.setText(etaText);
                            vh.etaNextText.setText(null);
                            break;
                        case 1:
                            etaText.insert(0, vh.etaNextText.getText());
                            vh.etaNextText.setText(etaText);
                            break;
                        case 2:
                        default:
                            etaText.insert(0, "  ");
                            etaText.insert(0, vh.etaNextText.getText());
                            vh.etaNextText.setText(etaText);
                            break;
                    }
                }
            });
        }

        if (viewHolder instanceof HistoryViewHolder) {
            SearchHistory object = historyItem(position - getFollowCount());
            HistoryViewHolder vh = (HistoryViewHolder) viewHolder;
            vh.routeText.setText(object.getRoute());
            Drawable drawable;
            switch (object.getType()) {
                case SuggestionTable.TYPE_HISTORY:
                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_history_black_24dp);
                    break;
                case SuggestionTable.TYPE_DEFAULT:
                default:
                    drawable = ContextCompat.getDrawable(context, R.drawable.ic_directions_bus_black_24dp);
                    break;
            }
            if (vh.iconImage != null && drawable != null) {
                vh.iconImage.setImageDrawable(drawable);
            }
            vh.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                if (context == null) return;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(context, SearchActivity.class);
                intent.putExtra(C.EXTRA.COMPANY_CODE, object.getCompanyCode());
                intent.putExtra(C.EXTRA.ROUTE_NO, object.getRoute());
                context.startActivity(intent);
            });
            vh.itemView.setOnLongClickListener(v -> {
                Context context = v.getContext();
                if (context == null) return false;
                new AlertDialog.Builder(context)
                        .setTitle(object.getRoute() + "?")
                        .setMessage(context.getString(R.string.message_remove_from_search_history))
                        .setNegativeButton(R.string.action_cancel, (dialoginterface, i) -> dialoginterface.cancel())
                        .setPositiveButton(R.string.action_confirm, (dialoginterface, i) -> {
                            int pos = position;
                            int j = 0;
                            for (SearchHistory history: getHistoryItems()) {
                                if (history.equals(object)) {
                                    pos = j + getFollowCount();
                                    break;
                                }
                                j++;
                            }
                            if (SearchHistoryUtil.delete(context, object) > 0) {
                                updateHistory();
                                notifyItemRemoved(pos);
                            }
                        })
                        .show();
                return true;
            });
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        if (viewType == ITEM_VIEW_TYPE_FOLLOW) {
            return new FollowViewHolder(inflater.inflate(R.layout.item_route_follow, viewGroup, false));
        }
        return new HistoryViewHolder(inflater.inflate(R.layout.item_route_history, viewGroup, false));
    }

    @Override
    public int getItemViewType(int position) {
        return position < getFollowCount() ? ITEM_VIEW_TYPE_FOLLOW : ITEM_VIEW_TYPE_HISTORY;
    }

    private static class FollowViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView nameText;

        TextView etaText;

        TextView routeNo;

        TextView routeLocationEnd;

        TextView etaNextText;

        FollowViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.nameText = itemView.findViewById(R.id.name);
            this.etaText = itemView.findViewById(R.id.eta);
            this.etaNextText = itemView.findViewById(R.id.eta_next);
            this.routeNo = itemView.findViewById(R.id.route_no);
            this.routeLocationEnd = itemView.findViewById(R.id.route_location_end);
        }

    }

    private static class HistoryViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView routeText;

        ImageView iconImage;

        HistoryViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            routeText = itemView.findViewById(android.R.id.text1);
            iconImage = itemView.findViewById(R.id.icon);
        }

    }
}