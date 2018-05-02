package com.alvinhkh.buseta.nwst.ui;

import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.nwst.NwstService;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.RouteUtil;
import com.alvinhkh.buseta.utils.ConnectivityUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.alvinhkh.buseta.nwst.NwstService.*;

public class NwstActivity extends RouteActivityAbstract {

    private final NwstService nwstService = NwstService.api.create(NwstService.class);

    private final List<Route> routeList = new ArrayList<>();

    @Override
    protected void loadRouteNo(String no) {
        loadRouteNo(no, TYPE_ALL_ROUTES);
    }

    private void loadRouteNo(String no, String mode) {
        super.loadRouteNo(no);
        String sysCode = NwstRequestUtil.syscode();
        Map<String, String> options = new HashMap<>();
        options.put(QUERY_ROUTE_NO, mode.equals(TYPE_ALL_ROUTES) ? "" : no);
        options.put(QUERY_MODE, mode);
        options.put(QUERY_LANGUAGE, LANGUAGE_TC);
        options.put(QUERY_PLATFORM, PLATFORM);
        options.put(QUERY_APP_VERSION, APP_VERSION);
        options.put(QUERY_SYSCODE, sysCode);
        disposables.add(nwstService.routeList(options)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(routeListObserver(no, sysCode)));
    }

    DisposableObserver<ResponseBody> routeListObserver(String routeNo, String sysCode) {
        return new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody body) {
                try {
                    routeList.clear();
                    String[] routeArray = body.string().split("\\|\\*\\|", -1);
                    Map<String, String> options;
                    for (String route : routeArray) {
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
                Timber.e(e);
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
            public void onComplete() { }
        };
    }

    DisposableObserver<ResponseBody> variantListObserver(NwstRoute nwstRoute) {
        return new DisposableObserver<ResponseBody>() {

            @Override
            public void onNext(ResponseBody body) {
                try {
                    String b = body.string();
                    String[] datas = b.split("<br>");
                    for (String data: datas) {
                        String text = data.trim();
                        if (TextUtils.isEmpty(text)) continue;
                        NwstVariant variant = NwstVariant.Companion.fromString(text);
                        Route route = RouteUtil.fromNwst(nwstRoute, variant);
                        if (route.getName().equals(routeNo)) {
                            routeList.add(route);
                        }
                    }
                } catch (IOException e) {
                    Timber.d(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
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
                runOnUiThread(() -> onCompleteRoute(routeList, C.PROVIDER.NWST));
            }
        };
    }
}
