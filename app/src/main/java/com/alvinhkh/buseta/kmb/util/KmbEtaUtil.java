package com.alvinhkh.buseta.kmb.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.kmb.model.KmbEta;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;

import org.jsoup.Jsoup;


public class KmbEtaUtil {

    public static String text(String text) {
        return Jsoup.parse(text).text().replaceAll("　", " ")
                .replaceAll(" ?預定班次", "").replaceAll(" ?時段班次", "")
                .replaceAll(" ?Scheduled", "");
    }

    public static Integer parseCapacity(String ol) {
        if (!TextUtils.isEmpty(ol)) {
            if (ol.equalsIgnoreCase("f")) {
                return 10;
            } else if (ol.equalsIgnoreCase("e")) {
                return 0;
            } else if (ol.equalsIgnoreCase("n")) {
                return -1;
            }
            return Integer.parseInt(ol);
        }
        return -1;
    }

    public static ArrivalTime toArrivalTime(@NonNull Context context,
                                            @NonNull KmbEta eta, @NonNull Long generatedTime) {
        ArrivalTime object = new ArrivalTime();
        object.capacity = parseCapacity(eta.ol);
        object.expire = eta.expire;
        object.isSchedule = !TextUtils.isEmpty(eta.schedule) && eta.schedule.equals("Y");
        object.hasWheelchair = !TextUtils.isEmpty(eta.wheelchair) && eta.wheelchair.equals("Y");
        object.hasWifi = !TextUtils.isEmpty(eta.wifi) && eta.wifi.equals("Y");
        object.text = text(eta.time);
        object.generatedAt = generatedTime;
        object.updatedAt = System.currentTimeMillis();
        object = ArrivalTimeUtil.estimate(context, object);
        return object;
    }
}
