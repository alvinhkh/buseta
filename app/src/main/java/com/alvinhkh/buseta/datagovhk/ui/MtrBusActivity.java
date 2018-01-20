package com.alvinhkh.buseta.datagovhk.ui;

import android.content.Intent;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.datagovhk.DataGovHkService;
import com.alvinhkh.buseta.datagovhk.model.MtrBusRoute;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.BusRouteUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class MtrBusActivity extends RouteActivityAbstract {

    private final DataGovHkService dataGovHkService = DataGovHkService.resource.create(DataGovHkService.class);

    @Override
    protected void loadRouteNo(String no) {
        super.loadRouteNo(no);
        disposables.add(dataGovHkService.mtrBusRoutes()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(mtrBusRoutesObserver()));
    }

    DisposableObserver<ResponseBody> mtrBusRoutesObserver() {
        return new DisposableObserver<ResponseBody>() {
            List<BusRoute> busRoutes = new ArrayList<>();

            @Override
            public void onNext(ResponseBody body) {
                if (body == null) return;
                try {
                    List<MtrBusRoute> routes = MtrBusRoute.Companion.fromCSV(body.string());
                    for (MtrBusRoute route: routes) {
                        if (TextUtils.isEmpty(route.getRouteId())) continue;
                        busRoutes.add(BusRouteUtil.fromMtrBus(route));
                    }
                } catch (IOException e) {
                    Timber.d(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
            }

            @Override
            public void onComplete() {
                onCompleteRoute(busRoutes, BusRoute.COMPANY_LRTFEEDER);
            }
        };
    }
}
