package com.alvinhkh.buseta.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.ui.search.SearchActivity;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.alvinhkh.buseta.utils.PreferenceUtil;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class EtaWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(getApplicationContext(), intent);
    }

    /**
     * This is the factory that will provide data to the collection widget.
     */
    private class StackRemoteViewsFactory implements RemoteViewsFactory {

        private final CompositeDisposable disposables = new CompositeDisposable();

        private Context context;

        private Cursor cursor;

        private int appWidgetId;

        private Integer itemNoOfLines = 3;

        StackRemoteViewsFactory(@NonNull Context context, Intent intent) {
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            if (getApplicationContext() != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (preferences != null) {
                    Integer i = Integer.parseInt(preferences.getString("widget_item_lines", "3"));
                    if (i > 0) {
                        itemNoOfLines = i;
                    }
                }
            }
        }

        public void onDestroy() {
            disposables.clear();
            if (cursor != null) {
                cursor.close();
            }
        }

        @Override
        public int getCount() {
            return (cursor == null) ? 0 : cursor.getCount();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            // Get the data for this position from the content provider
            if (cursor == null) return null;
            cursor.moveToPosition(position);
            FollowStop followStop = FollowStopUtil.fromCursor(cursor);
            // Return a proper item
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_item_eta);
            if (followStop == null) return remoteViews;
            RouteStop stop = RouteStopUtil.fromFollowStop(followStop);
            if (stop != null) {
                remoteViews.setTextViewText(R.id.stop_name, null);
                remoteViews.setTextViewText(R.id.route_no, null);
                remoteViews.setTextViewText(R.id.route_destination, null);
                remoteViews.setTextViewText(R.id.eta, null);
                remoteViews.setTextViewText(R.id.eta2, null);
                remoteViews.setTextViewText(R.id.eta3, null);
                if (itemNoOfLines == 1) {
                    remoteViews.setViewVisibility(R.id.stop_name, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.route_no, View.GONE);
                    remoteViews.setViewVisibility(R.id.route_destination, View.GONE);
                    remoteViews.setViewVisibility(R.id.eta, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta2, View.GONE);
                    remoteViews.setViewVisibility(R.id.eta3, View.GONE);
                    remoteViews.setTextViewText(R.id.stop_name, stop.getRoute() + " " + stop.getName());
                } else if (itemNoOfLines == 2) {
                    remoteViews.setViewVisibility(R.id.stop_name, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.route_no, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.route_destination, View.GONE);
                    remoteViews.setViewVisibility(R.id.eta, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta2, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta3, View.GONE);
                    remoteViews.setTextViewText(R.id.stop_name, stop.getRoute() + " " + stop.getName());
                    remoteViews.setTextViewText(R.id.route_no, getString(R.string.destination, stop.getDestination()));
                } else {
                    remoteViews.setViewVisibility(R.id.stop_name, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.route_no, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.route_destination, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta2, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta3, View.VISIBLE);
                    remoteViews.setTextViewText(R.id.stop_name, stop.getName());
                    remoteViews.setTextViewText(R.id.route_no, stop.getRoute());
                    remoteViews.setTextViewText(R.id.route_destination, getString(R.string.destination, stop.getDestination()));
                }
                // ETA
                // http://stackoverflow.com/a/20645908/2411672
                final long token = Binder.clearCallingIdentity();
                try {
                    SpannableStringBuilder etaTexts = new SpannableStringBuilder();
                    ArrivalTimeUtil.query(context, stop).subscribe(cursor -> {
                        // Cursor has been moved +1 position forward.
                        ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
                        arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime);

                        if (!TextUtils.isEmpty(arrivalTime.getId())) {
                            SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.getText());
                            Integer pos = Integer.parseInt(arrivalTime.getId());
                            Integer colorInt = ContextCompat.getColor(context,
                                    arrivalTime.getExpired() ? R.color.widgetTextDiminish :
                                            (pos > 0 ? R.color.widgetTextPrimary : R.color.widgetTextHighlighted));
                            if (!TextUtils.isEmpty(arrivalTime.getNote())) {
                                etaText.append("#");
                            }
                            if (arrivalTime.isSchedule()) {
                                etaText.append("*");
                            }
                            if (itemNoOfLines > 1 || pos == 0) {
                                if (!TextUtils.isEmpty(arrivalTime.getEstimate())) {
                                    etaText.append(" (").append(arrivalTime.getEstimate()).append(")");
                                }
                            }
                            if (arrivalTime.getDistanceKM() >= 0) {
                                etaText.append(" ").append(context.getString(R.string.km_short, arrivalTime.getDistanceKM()));
                            }
                            if (!TextUtils.isEmpty(arrivalTime.getPlate())) {
                                etaText.append(" ").append(arrivalTime.getPlate());
                            }
                            if (arrivalTime.getCapacity() >= 0) {
                                String capacity = "";
                                if (arrivalTime.getCapacity() == 0) {
                                    capacity = context.getString(R.string.capacity_empty);
                                } else if (arrivalTime.getCapacity() > 0 && arrivalTime.getCapacity() <= 3) {
                                    capacity = "¼";
                                } else if (arrivalTime.getCapacity() > 3 && arrivalTime.getCapacity() <= 6) {
                                    capacity = "½";
                                } else if (arrivalTime.getCapacity() > 6 && arrivalTime.getCapacity() <= 9) {
                                    capacity = "¾";
                                } else if (arrivalTime.getCapacity() >= 10) {
                                    capacity = context.getString(R.string.capacity_full);
                                }
                                if (!TextUtils.isEmpty(capacity)) {
                                    etaText.append(" [").append(capacity).append("]");
                                }
                            }
                            if (arrivalTime.getHasWheelchair() && PreferenceUtil.isShowWheelchairIcon(context)) {
                                etaText.append(" \u267F");
                            }
                            if (arrivalTime.getHasWifi() && PreferenceUtil.isShowWifiIcon(context)) {
                                etaText.append(" [W]");
                            }

                            if (etaText.length() > 0) {
                                etaText.setSpan(new ForegroundColorSpan(colorInt), 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (itemNoOfLines == 1) {
                                switch (pos) {
                                    case 0:
                                        etaTexts.append(etaText);
                                        break;
                                    default:
                                        etaText.insert(0, "  ");
                                        etaTexts.append(etaText);
                                        break;
                                }
                                remoteViews.setTextViewText(R.id.eta, etaTexts);
                            } else if (itemNoOfLines == 2) {
                                switch (pos) {
                                    case 0:
                                        remoteViews.setTextViewText(R.id.eta, etaText);
                                        break;
                                    case 1:
                                        remoteViews.setTextViewText(R.id.eta2, etaText);
                                        etaTexts.append(etaText);
                                    case 2:
                                        etaText.insert(0, "  ");
                                        remoteViews.setTextViewText(R.id.eta2, etaTexts);
                                        break;
                                }
                            } else {
                                switch (pos) {
                                    case 0:
                                        remoteViews.setTextViewText(R.id.eta, etaText);
                                        break;
                                    case 1:
                                        remoteViews.setTextViewText(R.id.eta2, etaText);
                                        break;
                                    case 2:
                                        remoteViews.setTextViewText(R.id.eta3, etaText);
                                        break;
                                    default:
                                        // etaText.insert(0, "  ");
                                        // etaText.insert(0, etaTexts);
                                        // etaTexts.clear();
                                        // etaTexts.append(etaText);
                                        // remoteViews.setTextViewText(R.id.eta3, etaTexts);
                                        break;
                                }
                            }
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(context, SearchActivity.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
                remoteViews.setOnClickFillInIntent(R.id.widget_item_eta, intent);
            } else {
                remoteViews.setTextViewText(R.id.stop_name, "");
                remoteViews.setTextViewText(R.id.route_no, "");
                remoteViews.setTextViewText(R.id.route_destination, "");
                remoteViews.setTextViewText(R.id.eta, "");
                remoteViews.setTextViewText(R.id.eta2, "");
                remoteViews.setTextViewText(R.id.eta3, "");
            }
            if (!ConnectivityUtil.isConnected(context)) {
                remoteViews.setTextViewText(R.id.eta2, context.getString(R.string.message_no_internet_connection));
            }
            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDataSetChanged() {
            disposables.clear();
            disposables.add(RxBroadcastReceiver.create(context, new IntentFilter(C.ACTION.ETA_UPDATE))
                    .share()
                    .subscribeWith(etaObserver()));
            if (getApplicationContext() != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (preferences != null) {
                    Integer i = Integer.parseInt(preferences.getString("widget_item_lines", "3"));
                    if (i > 0) {
                        itemNoOfLines = i;
                    }
                }
            }
            // http://stackoverflow.com/a/20645908/2411672
            final long token = Binder.clearCallingIdentity();
            try {
                if (cursor != null) {
                    cursor.close();
                }
                cursor = FollowStopUtil.queryAll(context);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }


        DisposableObserver<Intent> etaObserver() {
            return new DisposableObserver<Intent>() {
                @Override
                public void onNext(Intent intent) {
                    Bundle bundle = intent.getExtras();
                    if (bundle == null) return;
                    if (bundle.getBoolean(C.EXTRA.COMPLETE) &&
                            bundle.getInt(C.EXTRA.WIDGET_UPDATE, -1) == appWidgetId) {
                        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_view);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Timber.d(e);
                }

                @Override
                public void onComplete() {
                }
            };
        }
    }
}