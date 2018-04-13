package com.alvinhkh.buseta.nwst.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.nwst.model.NwstEta;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;

import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;


public class NwstEtaUtil {

    public static String text(String text) {
        return Jsoup.parse(text).text().replaceAll("　", " ")
                .replaceAll(" ?預定班次", "")
                .replaceAll(" ?预定班次", "")
                .replaceAll(" ?Scheduled", "")
                .replaceAll("預計未來([0-9]+)分鐘沒有抵站班次或服務時間已過", "$1分鐘+/已過服務時間")
                .replaceAll("预计未来([0-9]+)分钟没有抵站班次或服务时间已过", "$1分钟+/已过服务时间")
                .replaceAll("No departure estimated in the next ([0-9]+) min or outside service hours", "$1 mins+/outside service hours")
                .replaceAll("。$", "").replaceAll("\\.$", "");
    }

    public static ArrivalTime estimate(@NonNull Context context, @NonNull ArrivalTime object) {
        SimpleDateFormat isoDf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        Date generatedDate = object.generatedAt == null ? new Date() : new Date(object.generatedAt);
        if (!TextUtils.isEmpty(object.isoTime)) {
            long differences = new Date().getTime() - generatedDate.getTime(); // get device timeText and compare to server timeText
            try {
                String estimateMinutes = "";
                Date etaDate = isoDf.parse(object.isoTime);
                int minutes = (int) ((etaDate.getTime() / 60000) - ((generatedDate.getTime() + differences) / 60000));
                if (minutes >= 0 && minutes < 24 * 60) {
                    // minutes should be 0 to within a day
                    estimateMinutes = String.valueOf(minutes);
                }
                if (minutes > 120) {
                    // potentially calculation error
                    estimateMinutes = "";
                }
                if (!TextUtils.isEmpty(estimateMinutes)) {
                    if (estimateMinutes.equals("0")) {
                        object.estimate = context.getString(R.string.now);
                    } else {
                        object.estimate = context.getString(R.string.minutes, estimateMinutes);
                    }
                }
                object.expired = minutes <= -3;  // time past
                object.expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.updatedAt) >= 2; // maybe outdated
            } catch (ParseException |ArrayIndexOutOfBoundsException ep) {
                Timber.d(ep);
            }
        }
        return object;
    }

    private static Float parseDistance(String text) {
        if (TextUtils.isEmpty(text)) return -1.0f;
        Matcher matcher = Pattern.compile("[距離|距离|Distance]: (\\d*\\.?\\d*)").matcher(text);
        if (matcher.find()) {
            Float distanceKM = Float.valueOf(matcher.group(1));
            if (distanceKM < 0.0f) {
                distanceKM = -1.0f;
            } else if (distanceKM > 10000.0f) {
                // filter out extreme value
                distanceKM = -1.0f;
            }
            return distanceKM;
        }
        return -1.0f;
    }

    public static ArrivalTime toArrivalTime(@NonNull Context context,
                                            @NonNull NwstEta nwstEta) {
        ArrivalTime object = ArrivalTimeUtil.emptyInstance(context);
        object.companyCode = C.PROVIDER.NWST;
        if (nwstEta.getCompanyCode().equals(C.PROVIDER.CTB) || nwstEta.getCompanyCode().equals(C.PROVIDER.CTB)) {
            object.companyCode = nwstEta.getCompanyCode();
        }
        if (TextUtils.isEmpty(nwstEta.getEtaIsoTime())) {
            object.text = text(nwstEta.getTitle());
        } else {
            object.text = nwstEta.getEtaTime();
        }
        String subtitle = text(nwstEta.getSubtitle());
        if (!TextUtils.isEmpty(subtitle)) {
            if (subtitle.contains("距離") || subtitle.contains("距离") || subtitle.contains("Distance")) {
                object.distanceKM = parseDistance(subtitle);
            }
            if (object.distanceKM < 0) {
                object.text += " " + subtitle;
            }
        }
        if (!TextUtils.isEmpty(nwstEta.getBoundText())) {
            object.text += " " + nwstEta.getBoundText();
        }
        object.isoTime = nwstEta.getEtaIsoTime();
        object.isSchedule = !TextUtils.isEmpty(nwstEta.getSubtitle()) && (nwstEta.getSubtitle().contains("預定班次") || nwstEta.getSubtitle().contains("预定班次") || nwstEta.getSubtitle().contains("Scheduled"));
        SimpleDateFormat generatedAtDf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        try {
            object.generatedAt = generatedAtDf.parse(nwstEta.getServerTime()).getTime();
        } catch (ParseException ignored) {}
        object.updatedAt = System.currentTimeMillis();
        object = ArrivalTimeUtil.estimate(context, object);
        return object;
    }
}
