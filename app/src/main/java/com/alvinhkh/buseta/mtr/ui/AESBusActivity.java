package com.alvinhkh.buseta.mtr.ui;

import com.alvinhkh.buseta.model.BusRoute;
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

        List<BusRoute> busRoutes = new ArrayList<>();
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
                                            BusRoute busRoute = new BusRoute();
                                            busRoute.setCompanyCode(BusRoute.COMPANY_AESBUS);
                                            busRoute.setName(aesBusRoute.getBusNumber());
                                            if (aesBusRoute.getDistrictID() != null) {
                                                busRoute.setLocationEndName(districts.get(String.valueOf(aesBusRoute.getDistrictID())));
                                            }
                                            busRoute.setDescription(aesBusRoute.getServiceHours());
                                            busRoute.setSequence("0");
                                            busRoutes.add(busRoute);
                                        }
                                    }
                                    onCompleteRoute(busRoutes, BusRoute.COMPANY_AESBUS);
                                }, Timber::d));
                    }, Timber::d));
        }
    }
}
