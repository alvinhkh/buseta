package com.alvinhkh.buseta.lwb.ui;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.lwb.LwbService;
import com.alvinhkh.buseta.lwb.model.LwbRouteBound;
import com.alvinhkh.buseta.lwb.model.network.LwbRouteBoundRes;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.ui.route.RouteActivity;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LwbActivity extends RouteActivity {

    private final LwbService lwbService = LwbService.retrofit.create(LwbService.class);

    @Override
    protected void loadRouteNo(String no) {
        super.loadRouteNo(no);
        disposables.add(lwbService.getRouteBound(no, Math.random())
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeBoundObserver()));
    }

    DisposableObserver<LwbRouteBoundRes> routeBoundObserver() {
        return new DisposableObserver<LwbRouteBoundRes>() {

            Boolean isScrollToPage = false;

            @Override
            public void onNext(LwbRouteBoundRes res) {
                if (res != null && res.bus_arr != null) {
                    pagerAdapter.setRoute(routeNo);
                    int i = 1;
                    for (LwbRouteBound bound : res.bus_arr) {
                        if (bound == null) continue;
                        BusRoute busRoute = new BusRoute();
                        busRoute.setCompanyCode(BusRoute.COMPANY_KMB);
                        busRoute.setLocationEndName(bound.destination_tc);
                        busRoute.setLocationStartName(bound.origin_tc);
                        busRoute.setName(routeNo);
                        busRoute.setSequence(String.valueOf(i++));
                        pagerAdapter.addSequence(busRoute);
                        if (stopFromIntent != null && busRoute.getSequence().equals(stopFromIntent.direction)) {
                            fragNo = pagerAdapter.getCount();
                            isScrollToPage = true;
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.d(e);
                showEmptyView();
                if (emptyText != null) {
                    if (!ConnectivityUtil.isConnected(getApplicationContext())) {
                        emptyText.setText(R.string.message_no_internet_connection);
                    } else {
                        emptyText.setText(R.string.message_fail_to_request);
                    }
                }
            }

            @Override
            public void onComplete() {
                onCompleteRoute(isScrollToPage, BusRoute.COMPANY_KMB);
            }
        };
    }
}
