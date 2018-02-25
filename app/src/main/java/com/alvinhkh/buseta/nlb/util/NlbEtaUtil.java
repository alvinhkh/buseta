package com.alvinhkh.buseta.nlb.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NlbEtaUtil {

    public static String text(String text) {
        return Jsoup.parse(text).text().replaceAll("　", " ")
                .replaceAll(" ?預計時間", "")
                .replaceAll(" ?Estimated time", "")
                .replaceAll(" ?預定班次", "")
                .replaceAll(" ?Scheduled", "")
                .replaceAll("此路線於未來([0-9]+)分鐘沒有班次途經本站", "$1分鐘+")
                .replaceAll("This route has no departure via this stop in next ([0-9]+) mins", "$1 mins+")
                .replaceAll("此路線的巴士預計抵站時間查詢服務將於稍後推出", "此路線未有預計抵站時間服務")
                .replaceAll("The inquiry service of bus estimated time of arrival on this route will be come soon", "ETA service on this route will be come soon");
    }

    public static ArrivalTime estimate(@NonNull Context context,
                                       @NonNull ArrivalTime object) {
        if (!TextUtils.isEmpty(object.text)) {
            Pattern p = Pattern.compile("(\\d*)(分鐘| min\\(s\\))");
            Matcher m = p.matcher(object.text);
            if (m.find()) {
                int minutes = Integer.parseInt(m.group(1));
                if (minutes > 0 && minutes < 60) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, minutes);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                    object.estimate = sdf.format(calendar.getTime());
                }
                object.expired = minutes <= -3;  // time past
                object.expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.updatedAt) >= 5; // maybe outdated
            }
        }
        return object;
    }

    public static ArrivalTime toArrivalTime(@NonNull Context context,
                                            @NonNull Element div) {
        ArrivalTime object = ArrivalTimeUtil.emptyInstance(context);
        object.companyCode = C.PROVIDER.NLB;
        String text = div.text();
        object.text = text(text);
        object.isSchedule = !TextUtils.isEmpty(text) && (text.contains("預定班次") || text.contains("Scheduled"));
        object.hasWheelchair = (div.getElementsByAttributeValueContaining("alt", "Wheelchair").size() > 0 ||
                div.getElementsByAttributeValueContaining("alt", "輪椅").size() > 0);
        object = ArrivalTimeUtil.estimate(context, object);
        return object;
    }
}
