package com.alvinhkh.buseta.nwst.util;

import android.text.TextUtils;

import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;
import com.alvinhkh.buseta.utils.HashUtil;

import java.util.Random;

public class NwstRequestUtil {

    public static String syscode() {
        StringBuilder randomInt = new StringBuilder(Integer.toString(new Random().nextInt(1000)));
        while (randomInt.length() < 4) {
            randomInt.append("0");
        }
        String timestamp = Integer.toString(Math.round((float) (System.currentTimeMillis() / 1000)));
        timestamp = timestamp.substring(timestamp.length() - 6);
        String secret = "firstbusmwymwy";
        return timestamp + randomInt + HashUtil.md5(timestamp + randomInt + secret);
    }

    public static String paramInfo(BusRoute busRoute) {
        if (busRoute == null) {
            return null;
        }
        String routeInfo = busRoute.getChildKey();
        if (TextUtils.isEmpty(routeInfo)) {
            return null;
        }
        NwstVariant variant = NwstVariant.Companion.parseInfo(routeInfo);
        if (variant == null) {
            return null;
        }
        return "1|*|" + busRoute.getCompanyCode() + "||" + variant.getRdv() + "||" + variant.getStartSequence() + "||" + variant.getEndSequence() + "||" + busRoute.getSequence();
    }
}
