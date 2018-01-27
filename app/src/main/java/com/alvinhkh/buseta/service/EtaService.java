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
import com.alvinhkh.buseta.mtr.MtrService;
import com.alvinhkh.buseta.mtr.model.AESEtaBus;
import com.alvinhkh.buseta.mtr.model.AESEtaBusStop;
import com.alvinhkh.buseta.mtr.model.AESEtaBusRes;
import com.alvinhkh.buseta.mtr.model.AESEtaBusStopsRequest;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbEtaRequest;
import com.alvinhkh.buseta.nlb.model.NlbEtaRes;
import com.alvinhkh.buseta.nlb.util.NlbEtaUtil;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstEta;
import com.alvinhkh.buseta.nwst.util.NwstEtaUtil;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.provider.EtaContract.EtaEntry;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.HashUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class EtaService extends IntentService {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MtrService aesService = MtrService.aes.create(MtrService.class);

    private final KmbService kmbEtaApi = KmbService.etav3.create(KmbService.class);

    private final NwstService nwstApi = NwstService.api.create(NwstService.class);

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
                notifyUpdate(routeStop, C.EXTRA.UPDATING, widgetId, notificationId, row);
                switch (routeStop.companyCode) {
                    case BusRoute.COMPANY_KMB:
                        disposables.add(kmbEtaApi.getEta(routeStop.etaGet)
                                .timeout(30, TimeUnit.SECONDS)
                                .retryWhen(new RetryWithDelay(3, 3000))
                                .subscribeWith(kmbEtaObserver(routeStop, widgetId, notificationId, row, i == busRouteStopList.size() - 1)));
                        break;
                    case BusRoute.COMPANY_NLB:
                        NlbEtaRequest request = new NlbEtaRequest(routeStop.routeId, routeStop.code, "zh");
                        disposables.add(nlbApi.eta(request)
                                .timeout(30, TimeUnit.SECONDS)
                                .retryWhen(new RetryWithDelay(3, 3000))
                                .subscribeWith(nlbEtaObserver(routeStop, widgetId, notificationId, row, i == busRouteStopList.size() - 1)));
                        break;
                    case BusRoute.COMPANY_CTB:
                    case BusRoute.COMPANY_NWFB:
                    case BusRoute.COMPANY_NWST:
                        Map<String, String> options = new HashMap<>();
                        options.put(QUERY_STOP_ID, Integer.toString(Integer.parseInt(routeStop.code)));
                        options.put(QUERY_SERVICE_NO, routeStop.route);
                        options.put("removeRepeatedSuspend", "Y");
                        options.put("interval", "60");
                        options.put(QUERY_BOUND, routeStop.direction);
                        options.put(QUERY_STOP_SEQ, routeStop.sequence);
                        options.put(QUERY_RDV, routeStop.routeId.replaceAll("-1$", "-2")); // TODO: why -1 to -2
                        options.put("showtime", "Y");
                        options.put(QUERY_LANGUAGE, LANGUAGE_TC);
                        options.put(QUERY_PLATFORM, PLATFORM);
                        options.put(QUERY_APP_VERSION, APP_VERSION);
                        options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
                        disposables.add(nwstApi.eta(options)
                                .timeout(30, TimeUnit.SECONDS)
                                .retryWhen(new RetryWithDelay(3, 3000))
                                .subscribeWith(nwstEtaObserver(routeStop, widgetId, notificationId, row, i == busRouteStopList.size() - 1)));
                        break;
                    case BusRoute.COMPANY_LRTFEEDER:
                        ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                        arrivalTime.companyCode = BusRoute.COMPANY_LRTFEEDER;
                        arrivalTime.text = getString(R.string.provider_no_eta);
                        getContentResolver().insert(EtaEntry.CONTENT_URI,
                                ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                        notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row);
                        break;
                    case BusRoute.COMPANY_AESBUS:
                        String key = HashUtil.md5("mtrMobile_" + new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH).format(new Date()));
                        // Timber.d("key: %s", key);
                        if (!TextUtils.isEmpty(key)) {
                            disposables.add(aesService.getBusStopsDetail(new AESEtaBusStopsRequest(routeStop.route, "2", "zh", key))
                                    .timeout(30, TimeUnit.SECONDS)
                                    .retryWhen(new RetryWithDelay(3, 3000))
                                    .subscribeWith(aesBusEtaObserver(routeStop, widgetId, notificationId, row, i == busRouteStopList.size() - 1)));
                        } else {
                            notifyUpdate(routeStop, C.EXTRA.FAIL, widgetId, notificationId, row);
                        }
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
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_KMB;
                arrivalTime.text = getString(R.string.message_fail_to_request);
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
                arrivalTime.text = getString(R.string.message_fail_to_request);
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

    DisposableObserver<ResponseBody> nwstEtaObserver(@NonNull final BusRouteStop busRouteStop,
                                                     final Integer widgetId,
                                                     final Integer notificationId,
                                                     final Integer rowNo,
                                                     final Boolean isLast) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                try {
                    String text = body.string();
                    String serverTime = text.split("\\|")[0].trim();
                    String[] data = text.trim().replaceFirst("^[^|]*\\|##\\|", "").split("<br>");
                    for (int i = 0; i < data.length; i++) {
                        NwstEta nwstEta = NwstEta.Companion.fromString(data[i]);
                        if (nwstEta == null) continue;
                        nwstEta.setServerTime(serverTime);
                        ArrivalTime arrivalTime = NwstEtaUtil.toArrivalTime(getApplicationContext(), nwstEta);
                        arrivalTime.id = Integer.toString(i);
                        getContentResolver().insert(EtaEntry.CONTENT_URI,
                                ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                    }
                    notifyUpdate(busRouteStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo);
                    return;
                } catch (IOException e) {
                    Timber.d(e);
                }
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_NWST;
                arrivalTime.generatedAt = System.currentTimeMillis();
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_NWST;
                arrivalTime.text = getString(R.string.message_fail_to_request);
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

    DisposableObserver<AESEtaBusRes> aesBusEtaObserver(@NonNull final BusRouteStop busRouteStop,
                                                       final Integer widgetId,
                                                       final Integer notificationId,
                                                       final Integer rowNo,
                                                       final Boolean isLast) {
        return new DisposableObserver<AESEtaBusRes>() {

            Boolean isError = false;

            @Override
            public void onNext(AESEtaBusRes res) {
                if (res != null) {
                    if (res.getRouteName() == null || !res.getRouteName().equals(busRouteStop.route)) return;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
                    Date statusTime = new Date();
                    if (res.getRouteStatusTime() != null) {
                        try {
                            statusTime = sdf.parse(res.getRouteStatusTime());
                        } catch (ParseException ignored) { }
                    }
                    Boolean isAvailable = false;
                    List<AESEtaBusStop> etas = res.getBusStops();
                    if (etas != null && etas.size() > 0) {
                        // TODO: better way to store and show aes eta
                        for (int i = 0; i < etas.size(); i++) {
                            AESEtaBusStop eta = etas.get(i);
                            if (eta.getBusStopId() == null) continue;
                            if (!eta.getBusStopId().equals(busRouteStop.code) && !eta.getBusStopId().equals("999")) continue;
                            if (eta.getBuses() != null && eta.getBuses().size() > 0) {
                                for (int j = 0; j < eta.getBuses().size(); j++) {
                                    isAvailable = true;
                                    AESEtaBus bus = eta.getBuses().get(j);
                                    ArrivalTime arrivalTime = AESEtaBus.Companion.toArrivalTime(getApplicationContext(), bus, statusTime, busRouteStop);
                                    arrivalTime.id = Integer.toString(j);
                                    arrivalTime.generatedAt = statusTime.getTime();
                                    arrivalTime.updatedAt = System.currentTimeMillis();
                                    getContentResolver().insert(EtaEntry.CONTENT_URI,
                                            ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                                }
                            }
                            if (isAvailable) {
                                notifyUpdate(busRouteStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo);
                            }
                        }
                    }
                    if (!isAvailable) {
                        ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                        arrivalTime.companyCode = BusRoute.COMPANY_AESBUS;
                        arrivalTime.text = res.getRouteStatusRemarkTitle();
                        arrivalTime.generatedAt = System.currentTimeMillis();
                        getContentResolver().insert(EtaEntry.CONTENT_URI,
                                ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                        notifyUpdate(busRouteStop, C.EXTRA.UPDATED, widgetId, notificationId, rowNo);
                    }
                } else {
                    isError = true;
                }
            }

            @Override
            public void onError(Throwable e) {
                isError = true;
                Timber.d(e);
                ArrivalTime arrivalTime = ArrivalTimeUtil.emptyInstance(getApplicationContext());
                arrivalTime.companyCode = BusRoute.COMPANY_AESBUS;
                arrivalTime.text = getString(R.string.message_fail_to_request);
                getContentResolver().insert(EtaEntry.CONTENT_URI,
                        ArrivalTimeUtil.toContentValues(busRouteStop, arrivalTime));
                notifyUpdate(busRouteStop, C.EXTRA.FAIL, widgetId, notificationId, rowNo);
            }

            @Override
            public void onComplete() {
                if (!isError) {
                    notifyUpdate(busRouteStop, C.EXTRA.COMPLETE, widgetId, notificationId, rowNo);
                }
            }
        };
    }
}
