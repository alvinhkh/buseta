package com.alvinhkh.buseta.service;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.follow.dao.FollowDatabase;
import com.alvinhkh.buseta.follow.model.Follow;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.search.ui.SearchActivity;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.PreferenceUtil;

import java.util.List;

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

        private ArrivalTimeDatabase arrivalTimeDatabase;

        private FollowDatabase followDatabase;

        private Context context;

        private int appWidgetId;

        private Integer itemNoOfLines = 3;

        StackRemoteViewsFactory(@NonNull Context context, Intent intent) {
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            arrivalTimeDatabase = ArrivalTimeDatabase.Companion.getInstance(context);
            followDatabase = FollowDatabase.Companion.getInstance(context);
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
        }

        @Override
        public int getCount() {
            if (followDatabase == null) return 0;
            return followDatabase.followDao().count();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (context == null) return null;
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_item_eta);
            // Get the data for this position from the content provider
            if (followDatabase == null || position >= getCount()) return remoteViews;
            Follow follow = followDatabase.followDao().getList().get(position);
            if (follow == null) return remoteViews;
            RouteStop stop = RouteStopUtil.fromFollow(follow);
            if (stop != null) {
                remoteViews.setTextViewText(R.id.left_line1, null);
                remoteViews.setTextViewText(R.id.left_line2, null);
                remoteViews.setTextViewText(R.id.left_line3, null);
                remoteViews.setTextViewText(R.id.eta, null);
                remoteViews.setTextViewText(R.id.eta2, null);
                remoteViews.setTextViewText(R.id.eta3, null);
                if (itemNoOfLines == 1) {
                    remoteViews.setViewVisibility(R.id.left_line1, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.left_line2, View.GONE);
                    remoteViews.setViewVisibility(R.id.left_line3, View.GONE);
                    remoteViews.setViewVisibility(R.id.eta, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta2, View.GONE);
                    remoteViews.setViewVisibility(R.id.eta3, View.GONE);
                    remoteViews.setTextViewText(R.id.left_line1, stop.getRoute() + " " + stop.getName());
                } else if (itemNoOfLines == 2) {
                    remoteViews.setViewVisibility(R.id.left_line1, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.left_line2, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.left_line3, View.GONE);
                    remoteViews.setViewVisibility(R.id.eta, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta2, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta3, View.GONE);
                    if (!TextUtils.isEmpty(stop.getDestination())) {
                        remoteViews.setTextViewText(R.id.left_line1, stop.getRoute() + " " +
                                getString(R.string.destination, stop.getDestination()));
                    } else {
                        remoteViews.setTextViewText(R.id.left_line1, stop.getRoute());
                    }
                    remoteViews.setTextViewText(R.id.left_line2, stop.getName());
                } else {
                    remoteViews.setViewVisibility(R.id.left_line1, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.left_line2, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta2, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.eta3, View.VISIBLE);
                    remoteViews.setTextViewText(R.id.left_line1, stop.getName());
                    remoteViews.setTextViewText(R.id.left_line2, stop.getRoute());
                    if (!TextUtils.isEmpty(stop.getDestination())) {
                        remoteViews.setViewVisibility(R.id.left_line3, View.VISIBLE);
                        remoteViews.setTextViewText(R.id.left_line3, getString(R.string.destination, stop.getDestination()));
                    } else {
                        remoteViews.setViewVisibility(R.id.left_line3, View.GONE);
                    }
                }
                // ETA
                // http://stackoverflow.com/a/20645908/2411672
                final long token = Binder.clearCallingIdentity();
                try {
                    SpannableStringBuilder etaTexts = new SpannableStringBuilder();
                    if (arrivalTimeDatabase != null) {
                        List<ArrivalTime> arrivalTimeList = ArrivalTime.Companion.getList(arrivalTimeDatabase, stop);
                        String direction = "";
                        for (ArrivalTime arrivalTime: arrivalTimeList) {
                            arrivalTime = ArrivalTime.Companion.estimate(context, arrivalTime);
                            if (!TextUtils.isEmpty(arrivalTime.getOrder())) {
                                SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.getText());
                                Integer pos = Integer.parseInt(arrivalTime.getOrder());
                                Integer colorInt = ContextCompat.getColor(context,
                                        arrivalTime.getExpired() ? R.color.widgetTextDiminish :
                                                (pos > 0 ? R.color.widgetTextPrimary : R.color.widgetTextHighlighted));
                                if (arrivalTime.getCompanyCode().equals(C.PROVIDER.MTR)) {
                                    colorInt = ContextCompat.getColor(context, arrivalTime.getExpired() ?
                                            R.color.widgetTextDiminish : R.color.widgetTextPrimary);
                                }
                                if (!TextUtils.isEmpty(arrivalTime.getPlatform())) {
                                    etaText.insert(0, "[" + arrivalTime.getPlatform() + "] ");
                                }
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
                                    if (arrivalTime.getCompanyCode().equals(C.PROVIDER.MTR)) {
                                        if (!direction.equals(arrivalTime.getDirection())) {
                                            if (pos == 0) {
                                                etaTexts.append(etaText);
                                            } else {
                                                etaText.insert(0, "  ");
                                                etaTexts.append(etaText);
                                            }
                                        }
                                    } else {
                                        switch (pos) {
                                            case 0:
                                                etaTexts.append(etaText);
                                                break;
                                            default:
                                                etaText.insert(0, "  ");
                                                etaTexts.append(etaText);
                                                break;
                                        }
                                    }
                                    remoteViews.setTextViewText(R.id.eta, etaTexts);
                                } else if (itemNoOfLines == 2) {
                                    if (arrivalTime.getCompanyCode().equals(C.PROVIDER.MTR)) {
                                        if (!direction.equals(arrivalTime.getDirection())) {
                                            if (pos == 0) {
                                                remoteViews.setTextViewText(R.id.eta, etaText);
                                            } else {
                                                etaText.insert(0, "  ");
                                                remoteViews.setTextViewText(R.id.eta2, etaText);
                                            }
                                        }
                                    } else {
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
                                    }
                                } else {
                                    if (arrivalTime.getCompanyCode().equals(C.PROVIDER.MTR)) {
                                        if (!direction.equals(arrivalTime.getDirection())) {
                                            if (pos == 0) {
                                                remoteViews.setTextViewText(R.id.eta, etaText);
                                            } else {
                                                etaText.insert(0, "  ");
                                                remoteViews.setTextViewText(R.id.eta2, etaText);
                                            }
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
                            }
                            direction = arrivalTime.getDirection();
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(context, SearchActivity.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
                remoteViews.setOnClickFillInIntent(R.id.widget_item_eta, intent);
            } else {
                remoteViews.setTextViewText(R.id.left_line1, "");
                remoteViews.setTextViewText(R.id.left_line2, "");
                remoteViews.setTextViewText(R.id.left_line3, "");
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