package com.alvinhkh.buseta.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.network.KmbEtaRes;
import com.alvinhkh.buseta.kmb.util.KmbEtaUtil;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbEtaRequest;
import com.alvinhkh.buseta.nlb.model.NlbEtaRes;
import com.alvinhkh.buseta.nlb.util.NlbEtaUtil;
import com.alvinhkh.buseta.provider.EtaContract.EtaEntry;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

public class EtaService extends IntentService {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final KmbService kmbEtaApi = KmbService.etav3.create(KmbService.class);

    private final NlbService nlbApi = NlbService.api.create(NlbService.class);

    public EtaService() {
        super(EtaService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        if (!ConnectivityUtil.isConnected(this)) return;    // network connection check

        int widgetId = extras.getInt(C.EXTRA.WIDGET_UPDATE, -1);
        int notificationId = extras.getInt(C.EXTRA.NOTIFICATION_ID, -1);
        int row = extras.getInt(C.EXTRA.ROW, -1);

        List<BusRouteStop> busRouteStopList = extras.getParcelableArrayList(C.EXTRA.STOP_LIST);
        if (busRouteStopList == null) {
            busRouteStopList = new ArrayList<>();
        }
        BusRouteStop busRouteStop = extras.getParcelable(C.EXTRA.STOP_OBJECT);
        if (busRouteStop != null) {
            busRouteStopList.add(busRouteStop);
        }
        for (int i = 0; i < busRouteStopList.size(); i++) {
            BusRouteStop routeStop = busRouteStopList.get(i);
            if (!TextUtils.isEmpty(routeStop.companyCode)) {

                // notifyUpdate(routeStop, C.EXTRA.UPDATING, widgetId, notificationId, row);
                switch (routeStop.companyCode) {
                    case BusRoute.COMPANY_KMB:
                        disposables.add(kmbEtaApi.getEta(routeStop.etaGet)
                                .subscribeWith(kmbEtaObserver(routeStop, widgetId, notificationId, row, i == busRouteStopList.size() - 1)));
                        break;
                    case BusRoute.COMPANY_NLB:
                        NlbEtaRequest request = new NlbEtaRequest(routeStop.routeId, routeStop.code, "zh");
                        disposables.add(nlbApi.eta(request)
                                .subscribeWith(nlbEtaObserver(routeStop, widgetId, notificationId, row, i == busRouteStopList.size() - 1)));
                        break;
                    default:
                        notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row);
                        break;
                }
            } else {
                notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row);
            }
        }
    }

    private void notifyUpdate(@NonNull BusRouteStop stop, @NonNull String status,
                               Integer widgetId, Integer notificationId, Integer row) {
        Intent intent = new Intent(C.ACTION.ETA_UPDATE);
        intent.putExtra(status, true);
        if (widgetId >= 0) {
            intent.putExtra(C.EXTRA.WIDGET_UPDATE, widgetId);
        }
        if (notificationId >= 0) {
            intent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId);
        }
        if (row >= 0) {
            intent.putExtra(C.EXTRA.ROW, row);
        }
        intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
        sendBroadcast(intent);
    }

    DisposableObserver<KmbEtaRes> kmbEtaObserver(@NonNull final BusRouteStop busRouteStop,
                                                 final Integer widgetId,
                                                 final Integer notificationId,
                                                 final Integer rowNo,
                                                 final Boolean isLast) {
        // put kmb eta data to local eta database, EtaEntry
        return new DisposableObserver<KmbEtaRes>() {
            @Override
            public void onNext(KmbEtaRes res) {
                if (res != null && res.etas != null && res.etas.size() > 0) {
                    for (int i = 0; i < res.etas.size(); i++) {
                        ArrivalTime arrivalTime = KmbEtaUtil.toArrivalTime(getApplicationContext(), res.etas.get(i), res.generated);
                        arrivalTime.id = Integer.toString(i);
                        getContentResolver().insert(EtaEntry.CONTENT_URI,
                                ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                    }
                    notifyUpdate(busRouteStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo);
                    return;
                }
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_KMB;
                arrivalTime.generatedAt = res.generated;
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_KMB;
                arrivalTime.text = e.getMessage();
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onComplete() {
                if (isLast) notifyUpdate(busRouteStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo);
            }
        };
    }

    DisposableObserver<NlbEtaRes> nlbEtaObserver(@NonNull final BusRouteStop busRouteStop,
                                                 final Integer widgetId,
                                                 final Integer notificationId,
                                                 final Integer rowNo,
                                                 final Boolean isLast) {
        return new DisposableObserver<NlbEtaRes>() {
            @Override
            public void onNext(NlbEtaRes res) {
                if (res != null && res.estimatedArrivalTime != null && !TextUtils.isEmpty(res.estimatedArrivalTime.html)) {
                    Timber.d("%s", res.estimatedArrivalTime.html);
                    Document doc = Jsoup.parse(res.estimatedArrivalTime.html);
                    Elements divs = doc.body().getElementsByTag("div");
                    if (divs != null && divs.size() > 0) {
                        int s = divs.size();
                        if (s > 1) {
                            s -= 1;
                        }
                        for (int i = 0; i < s; i++) {
                            ArrivalTime arrivalTime = NlbEtaUtil.toArrivalTime(getApplicationContext(), divs.get(i));
                            arrivalTime.id = Integer.toString(i);
                            getContentResolver().insert(EtaEntry.CONTENT_URI,
                                    ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                        }
                        notifyUpdate(busRouteStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo);
                        return;
                    }
                }
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_NLB;
                arrivalTime.generatedAt = System.currentTimeMillis();
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_NLB;
                arrivalTime.text = e.getMessage();
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onComplete() {
                if (isLast) notifyUpdate(busRouteStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo);
            }
        };
    }
}
