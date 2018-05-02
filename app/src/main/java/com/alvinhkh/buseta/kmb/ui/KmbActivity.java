package com.alvinhkh.buseta.kmb.ui;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.KmbRoute;
import com.alvinhkh.buseta.kmb.model.KmbRouteBound;
import com.alvinhkh.buseta.kmb.model.network.KmbRouteBoundRes;
import com.alvinhkh.buseta.kmb.model.network.KmbSpecialRouteRes;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.HKSCSUtil;
import com.alvinhkh.buseta.utils.RouteUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class KmbActivity extends RouteActivityAbstract {

    private final KmbService kmbService = KmbService.webSearch.create(KmbService.class);

    private List<Route> routeList = new ArrayList<>();

    @Override
    protected void loadRouteNo(String no) {
        super.loadRouteNo(no);
        disposables.add(kmbService.getRouteBound(no)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeBoundObserver())
        );
    }

    DisposableObserver<KmbRouteBoundRes> routeBoundObserver() {
        return new DisposableObserver<KmbRouteBoundRes>() {
            @Override
            public void onNext(KmbRouteBoundRes res) {
                routeList.clear();
                if (res != null && res.data != null) {
                    List<Integer> list = new ArrayList<>();
                    for (KmbRouteBound bound : res.data) {
                        if (list.contains(bound.bound)) continue;
                        list.add(bound.bound);
                        disposables.add(kmbService.getSpecialRoute(bound.route, String.valueOf(bound.bound))
                                .retryWhen(new RetryWithDelay(5, 3000))
                                .subscribeOn(Schedulers.io())
                                .doOnNext(r -> {
                                    for (KmbRoute route : r.data.routes) {
                                        route.destinationTc = HKSCSUtil.convert(route.destinationTc);
                                        route.originTc = HKSCSUtil.convert(route.originTc);
                                        route.descTc = HKSCSUtil.convert(route.descTc);
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(specialRouteObserver(bound.route)));
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                runOnUiThread(() -> {
                    showEmptyView();
                    if (emptyText != null) {
                        emptyText.setText(e.getMessage());
                    }
                });
            }

            @Override
            public void onComplete() {}
        };
    }

    DisposableObserver<KmbSpecialRouteRes> specialRouteObserver(String routeNo) {
        return new DisposableObserver<KmbSpecialRouteRes>() {

            @Override
            public void onNext(KmbSpecialRouteRes res) {
                if (res != null && res.data != null) {
                    for (KmbRoute route : res.data.routes) {
                        if (route == null || route.route == null || !route.route.equals(routeNo)) continue;
                        routeList.add(RouteUtil.fromKmb(route));
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
                runOnUiThread(() -> onCompleteRoute(routeList, C.PROVIDER.KMB));
            }
        };
    }
}
