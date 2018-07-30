package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.datagovhk.model.MtrBusRoute;
import com.alvinhkh.buseta.kmb.model.KmbRoute;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.nwst.model.NwstRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;

import org.jsoup.parser.Parser;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;

public class RouteUtil {

    public static Route fromKmb(KmbRoute route) {
        if (route == null) return null;
        Route object = new Route();
        object.setCompanyCode(C.PROVIDER.KMB);
        object.setOrigin(route.originTc);
        object.setDestination(route.destinationTc);
        object.setName(route.route);
        object.setSequence(route.bound);
        object.setServiceType(TextUtils.isEmpty(route.serviceType) ? route.serviceType : route.serviceType.trim());
        String desc = route.descTc.trim();
        object.setDescription(desc);
        object.setSpecial(!TextUtils.isEmpty(desc));
        return object;
    }

    public static Route fromNwst(NwstRoute route, @Nullable NwstVariant variant) {
        if (route == null) return null;
        Route object = new Route();
        object.setCompanyCode(route.getCompanyCode());
        object.setDescription(route.getRemark());
        object.setOrigin(route.getPlaceFrom());
        object.setDestination(route.getPlaceTo());
        object.setName(route.getRouteNo());
        object.setServiceType(route.getRouteType());
        object.setRdv(route.getRdv());
        object.setSequence(route.getBound());
        if (variant != null) {
            object.setStopsStartSequence(variant.getStartSequence());
            object.setInfoKey(variant.getRouteInfo());
            object.setDescription(variant.getRemark());
            object.setSpecial(!TextUtils.isEmpty(variant.getRemark()) && !variant.getRemark().equals("正常路線"));
        }
        return object;
    }

    public static Route fromMtrBus(MtrBusRoute route) {
        if (route == null) return null;
        Route object = new Route();
        object.setCompanyCode(C.PROVIDER.LRTFEEDER);
        object.setName(route.getRouteId());
        if (!TextUtils.isEmpty(route.getRouteNameChi())) {
            route.setRouteNameChi(Parser.unescapeEntities(route.getRouteNameChi(), false));
            String[] routeName = route.getRouteNameChi().split("至");
            if (routeName.length == 2) {
                object.setDestination(routeName[0]);
                object.setOrigin(routeName[1]);
            } else {
                object.setOrigin(route.getRouteNameChi());
            }
        }
        object.setSequence("0");
        return object;
    }

    public static String getCompanyName(@NonNull Context context,
                                        @NonNull String companyCode,
                                        @Nullable String routeNo) {
        if (TextUtils.isEmpty(companyCode)) return "";
        String companyName = companyCode;
        switch (companyCode) {
            case C.PROVIDER.AESBUS:
                companyName = context.getString(R.string.provider_short_aes_bus);
                break;
            case C.PROVIDER.CTB:
                companyName = context.getString(R.string.provider_short_ctb);
                break;
            case C.PROVIDER.KMB:
                companyName = context.getString(R.string.provider_short_kmb);
                if (!TextUtils.isEmpty(routeNo)) {
                    if (routeNo.startsWith("NR")) {
                        companyName = context.getString(R.string.provider_short_residents);
                    } else if (routeNo.startsWith("A") || routeNo.startsWith("E") || routeNo.startsWith("NA")) {
                        companyName = context.getString(R.string.provider_short_lwb);
                    }
                }
                break;
            case C.PROVIDER.LRTFEEDER:
                companyName = context.getString(R.string.provider_short_lrtfeeder);
                break;
            case C.PROVIDER.MTR:
                companyName = context.getString(R.string.provider_short_mtr);
                break;
            case C.PROVIDER.NLB:
                companyName = context.getString(R.string.provider_short_nlb);
                break;
            case C.PROVIDER.NWFB:
                companyName = context.getString(R.string.provider_short_nwfb);
                break;
            case C.PROVIDER.NWST:
                companyName = context.getString(R.string.provider_short_nwst);
                break;
        }
        return companyName;
    }
}
