package com.alvinhkh.buseta.lwb.ui;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.lwb.LwbService;
import com.alvinhkh.buseta.lwb.model.LwbRouteBound;
import com.alvinhkh.buseta.lwb.model.network.LwbRouteBoundRes;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;

public class LwbActivity extends RouteActivityAbstract {

    private final LwbService lwbService = LwbService.retrofit.create(LwbService.class);

    @Override
    protected void loadRouteNo(String no) {
        super.loadRouteNo(no);
        disposables.add(lwbService.getRouteBound(no, Math.random())
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeWith(routeBoundObserver()));
    }

    DisposableObserver<LwbRouteBoundRes> routeBoundObserver() {
        return new DisposableObserver<LwbRouteBoundRes>() {

            List<Route> routes = new ArrayList<>();

            @Override
            public void onNext(LwbRouteBoundRes res) {
                if (res != null && res.bus_arr != null) {
                    int i = 1;
                    for (LwbRouteBound bound : res.bus_arr) {
                        if (bound == null) continue;
                        Route route = new Route();
                        route.setCompanyCode(C.PROVIDER.KMB);
                        route.setOrigin(bound.destination_tc);
                        route.setDestination(bound.origin_tc);
                        route.setName(routeNo);
                        route.setSequence(String.valueOf(i++));
                        routes.add(route);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                runOnUiThread(() -> {
                    showEmptyView();
                    if (emptyText != null) {
                        if (!ConnectivityUtil.isConnected(getApplicationContext())) {
                            emptyText.setText(R.string.message_no_internet_connection);
                        } else {
                            emptyText.setText(R.string.message_fail_to_request);
                        }
                    }
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> onCompleteRoute(routes, C.PROVIDER.KMB));
            }
        };
    }
}
