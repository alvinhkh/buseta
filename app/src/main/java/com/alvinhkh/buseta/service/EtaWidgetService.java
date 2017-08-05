package com.alvinhkh.buseta.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.ui.search.SearchActivity;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.BusRouteStopUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.alvinhkh.buseta.utils.PreferenceUtil;
import com.alvinhkh.buseta.view.MainActivity;

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

        StackRemoteViewsFactory(@NonNull Context context, Intent intent) {
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        public void onCreate() {
            disposables.add(RxBroadcastReceiver.create(context, new IntentFilter(C.ACTION.ETA_UPDATE))
                    .share()
                    .subscribeWith(etaObserver()));
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
            BusRouteStop stop = BusRouteStopUtil.fromFollowStop(followStop);
            if (stop != null) {
                remoteViews.setTextViewText(R.id.stop_name, stop.name);
                remoteViews.setTextViewText(R.id.route_no, stop.route);
                remoteViews.setTextViewText(R.id.route_destination, stop.destination);
                remoteViews.setTextViewText(R.id.eta, null);
                remoteViews.setTextViewText(R.id.eta_more, null);
                // ETA
                // http://stackoverflow.com/a/20645908/2411672
                final long token = Binder.clearCallingIdentity();
                try {SpannableStringBuilder etaTexts = new SpannableStringBuilder();
                    ArrivalTimeUtil.query(context, stop).subscribe(cursor -> {
                        // Cursor has been moved +1 position forward.
                        ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
                        arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime);

                        if (arrivalTime.id != null) {
                            SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.text);
                            Integer pos = Integer.parseInt(arrivalTime.id);
                            Integer colorInt = ContextCompat.getColor(context,
                                    arrivalTime.expired ? R.color.widgetTextDiminish :
                                            (pos > 0 ? R.color.widgetTextPrimary : R.color.widgetTextHighlighted));
                            if (arrivalTime.isSchedule) {
                                etaText.append("*");
                            }
                            if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                                etaText.append(" (").append(arrivalTime.estimate).append(")");
                            }
                            if (arrivalTime.capacity >= 0) {
                                String capacity = "";
                                if (arrivalTime.capacity == 0) {
                                    capacity = context.getString(R.string.capacity_empty);
                                } else if (arrivalTime.capacity > 0 && arrivalTime.capacity <= 3) {
                                    capacity = "¼";
                                } else if (arrivalTime.capacity > 3 && arrivalTime.capacity <= 6) {
                                    capacity = "½";
                                } else if (arrivalTime.capacity > 6 && arrivalTime.capacity <= 9) {
                                    capacity = "¾";
                                } else if (arrivalTime.capacity >= 10) {
                                    capacity = context.getString(R.string.capacity_full);
                                }
                                if (!TextUtils.isEmpty(capacity)) {
                                    etaText.append(" [").append(capacity).append("]");
                                }
                            }
                            if (arrivalTime.hasWheelchair && PreferenceUtil.isShowWheelchairIcon(context)) {
                                etaText.append(" \u267F");
                            }
                            if (arrivalTime.hasWifi && PreferenceUtil.isShowWifiIcon(context)) {
                                etaText.append(" [W]");
                            }
                            etaText.setSpan(new ForegroundColorSpan(colorInt), 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            switch(pos) {
                                case 0:
                                    remoteViews.setTextViewText(R.id.eta, etaText);
                                    remoteViews.setTextViewText(R.id.eta_more, null);
                                    break;
                                case 1:
                                    etaText.insert(0, etaTexts);
                                    etaTexts.clear();
                                    etaTexts.append(etaText);
                                    remoteViews.setTextViewText(R.id.eta_more, etaTexts);
                                    break;
                                case 2:
                                default:
                                    // etaText.insert(0, "  ");
                                    // etaText.insert(0, etaTexts);
                                    // etaTexts.clear();
                                    // etaTexts.append(etaText);
                                    // remoteViews.setTextViewText(R.id.eta_more, etaTexts);
                                    break;
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
                remoteViews.setTextViewText(R.id.eta_more, "");
            }
            if (!ConnectivityUtil.isConnected(context)) {
                remoteViews.setTextViewText(R.id.eta_more, context.getString(R.string.message_no_internet_connection));
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
            Timber.d("onDataSetChanged");
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