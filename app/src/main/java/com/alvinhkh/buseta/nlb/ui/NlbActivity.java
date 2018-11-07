package com.alvinhkh.buseta.nlb.ui;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nlb.model.NlbRoute;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.ConnectivityUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class NlbActivity extends RouteActivityAbstract {

    private final NlbService nlbService = NlbService.api.create(NlbService.class);

    @Override
    protected void loadRouteNo(String no) {
        super.loadRouteNo(no);
        getDisposables().add(nlbService.getDatabase()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(databaseObserver()));
    }

    DisposableObserver<NlbDatabase> databaseObserver() {
        return new DisposableObserver<NlbDatabase>() {

            List<Route> routes = new ArrayList<>();

            @Override
            public void onNext(NlbDatabase database) {
                if (database != null && database.routes != null) {
                    for (NlbRoute nlbRoute : database.routes) {
                        if (nlbRoute == null) continue;
                        if (nlbRoute.route_no.equals(getRouteNo())) {
                            Route route = new Route();
                            route.setCompanyCode(C.PROVIDER.NLB);
                            String[] location = nlbRoute.route_name_c.split(" > ");
                            route.setOrigin(location[0]);
                            if (location.length > 1) {
                                route.setDestination(location[1]);
                            }
                            route.setName(nlbRoute.route_no);
                            route.setSequence(nlbRoute.route_id);
                            route.setCode(nlbRoute.route_id);
                            routes.add(route);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                runOnUiThread(() -> {
                    showEmptyView();
                    if (getEmptyText() != null) {
                        if (!ConnectivityUtil.isConnected(getApplicationContext())) {
                            getEmptyText().setText(R.string.message_no_internet_connection);
                        } else {
                            getEmptyText().setText(R.string.message_fail_to_request);
                        }
                    }
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> onCompleteRoute(routes, C.PROVIDER.NLB));
            }
        };
    }
}
