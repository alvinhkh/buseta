package com.alvinhkh.buseta.mtr.ui;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.mtr.dao.AESBusDatabase;
import com.alvinhkh.buseta.mtr.model.AESBusDistrict;
import com.alvinhkh.buseta.mtr.model.AESBusRoute;
import com.alvinhkh.buseta.ui.route.RouteActivityAbstract;
import com.alvinhkh.buseta.utils.DatabaseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class AESBusActivity extends RouteActivityAbstract {

    @Override
    protected void loadRouteNo(String no) {
        super.loadRouteNo(no);

        List<Route> routes = new ArrayList<>();
        if (getApplicationContext() != null) {
            AESBusDatabase database = DatabaseUtil.Companion.getAESBusDatabase(getApplicationContext());
            disposables.add(database.aesBusDao().getAllDistricts()
                    .subscribe(aesBusDistricts -> {
                        HashMap<String, String> districts = new HashMap<>();
                        if (aesBusDistricts != null) {
                            for (AESBusDistrict aesBusDistrict: aesBusDistricts) {
                                districts.put(aesBusDistrict.getDistrictID(), aesBusDistrict.getDistrictCn());
                            }
                        }
                        disposables.add(database.aesBusDao().getAllRoutes()
                                .subscribe(aesBusRoutes -> {
                                    if (aesBusRoutes != null) {
                                        for (AESBusRoute aesBusRoute: aesBusRoutes) {
                                            Route route = new Route();
                                            route.setCompanyCode(C.PROVIDER.AESBUS);
                                            route.setName(aesBusRoute.getBusNumber());
                                            if (aesBusRoute.getDistrictID() > 0) {
                                                route.setOrigin(districts.get(String.valueOf(aesBusRoute.getDistrictID())));
                                            }
                                            route.setDescription(aesBusRoute.getServiceHours());
                                            route.setSequence("0");
                                            routes.add(route);
                                        }
                                    }
                                    onCompleteRoute(routes, C.PROVIDER.AESBUS);
                                }, Timber::d));
                    }, Timber::d));
        }
    }
}
