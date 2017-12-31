package com.alvinhkh.buseta.nlb.ui;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbDatabase;
import com.alvinhkh.buseta.nlb.model.NlbRoute;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

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
        disposables.add(nlbService.getDatabase()
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(databaseObserver()));
    }

    DisposableObserver<NlbDatabase> databaseObserver() {
        return new DisposableObserver<NlbDatabase>() {

            List<BusRoute> busRoutes = new ArrayList<>();

            @Override
            public void onNext(NlbDatabase database) {
                if (database != null && database.routes != null) {
                    for (NlbRoute route : database.routes) {
                        if (route == null) continue;
                        if (route.route_no.equals(routeNo)) {
                            BusRoute busRoute = new BusRoute();
                            busRoute.setCompanyCode(BusRoute.COMPANY_NLB);
                            String[] location = route.route_name_c.split(" > ");
                            if (location.length > 1) {
                                busRoute.setLocationEndName(location[1]);
                            }
                            busRoute.setLocationStartName(location[0]);
                            busRoute.setName(route.route_no);
                            busRoute.setSequence(route.route_id);
                            busRoutes.add(busRoute);
                        }
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
                runOnUiThread(() -> onCompleteRoute(busRoutes, BusRoute.COMPANY_KMB));
            }
        };
    }
}
