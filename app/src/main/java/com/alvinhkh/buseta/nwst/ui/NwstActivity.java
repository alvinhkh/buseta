package com.alvinhkh.buseta.nwst.ui;

import android.text.TextUtils;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.BusRouteUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;
import com.alvinhkh.buseta.utils.RetryWithDelay;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class NwstActivity extends RouteActivityAbstract {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    @Override
    protected void loadRouteNo(String no) {
        loadRouteNo(no, TYPE_ALL_ROUTES);
    }

    private void loadRouteNo(String no, String mode) {
        super.loadRouteNo(no);
        Map<String, String> options = new HashMap<>();
        options.put(QUERY_ROUTE_NO, mode.equals(TYPE_ALL_ROUTES) ? "" : no);
        options.put(QUERY_MODE, mode);
        options.put(QUERY_LANGUAGE, LANGUAGE_TC);
        options.put(QUERY_PLATFORM, PLATFORM);
        options.put(QUERY_APP_VERSION, APP_VERSION);
        options.put(QUERY_SYSCODE, NwstRequestUtil.syscode());
        disposables.add(nwstService.routeList(options)
                .retryWhen(new RetryWithDelay(5, 3000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeListObserver(no)));
    }

    DisposableObserver<ResponseBody> routeListObserver(String routeNo) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                try {
                    String[] routes = body.string().split("\\|\\*\\|", -1);
                    Map<String, String> options;
                    String sysCode = NwstRequestUtil.syscode();
                    for (String route : routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstRoute nwstRoute = NwstRoute.Companion.fromString(text);
                        if (nwstRoute != null &&
                                !TextUtils.isEmpty(nwstRoute.getRouteNo()) &&
                                nwstRoute.getRouteNo().equals(routeNo)) {
                            options = new HashMap<>();
                            options.put(QUERY_ID, nwstRoute.getRdv());
                            options.put(QUERY_LANGUAGE, LANGUAGE_TC);
                            options.put(QUERY_PLATFORM, PLATFORM);
                            options.put(QUERY_APP_VERSION, APP_VERSION);
                            options.put(QUERY_SYSCODE, sysCode);
                            disposables.add(nwstService.variantList(options)
                                    .retryWhen(new RetryWithDelay(5, 3000))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeWith(variantListObserver(nwstRoute)));
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
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
                pagerAdapter.setRoute(routeNo);
            }
        };
    }

    DisposableObserver<ResponseBody> variantListObserver(NwstRoute nwstRoute) {
        return new DisposableObserver<ResponseBody>() {

            String companyCode = BusRoute.COMPANY_NWST;

            Boolean isScrollToPage = false;

            @Override
            public void onNext(ResponseBody body) {
                try {
                    String[] routes = body.string().split("\\|\\*\\|", -1);
                    for (String route: routes) {
                        String text = route.replace("<br>", "").trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstVariant variant = NwstVariant.Companion.fromString(text);
                        BusRoute busRoute = BusRouteUtil.fromNwst(nwstRoute, variant);
                        if (busRoute.getName().equals(routeNo)) {
                            companyCode = busRoute.getCompanyCode();
                            pagerAdapter.addSequence(busRoute);
                            if (stopFromIntent != null && busRoute.getSequence().equals(stopFromIntent.direction)) {
                                fragNo = pagerAdapter.getCount();
                                isScrollToPage = true;
                            }
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
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
                onCompleteRoute(isScrollToPage, companyCode);
            }
        };
    }
}
