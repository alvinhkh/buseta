package com.alvinhkh.buseta.nwst.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.route.model.RouteStop;
import com.alvinhkh.buseta.nwst.model.NwstEta;

import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
                .replaceAll("。$", "").replaceAll("\\.$", "")
                .replaceAll("往: ", "往")
                .replaceAll(" ?新巴", "")
                .replaceAll(" ?城巴", "");
    }

    public static ArrivalTime estimate(@NonNull Context context, @NonNull ArrivalTime object) {
        SimpleDateFormat isoDf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        Date generatedDate = object.getGeneratedAt() > 0L ? new Date() : new Date(object.getGeneratedAt());

        // given iso time
        if (!TextUtils.isEmpty(object.getIsoTime())) {
            long differences = new Date().getTime() - generatedDate.getTime(); // get device timeText and compare to server timeText
            try {
                String estimateMinutes = "";
                Date etaDate = isoDf.parse(object.getIsoTime());
                int minutes = (int) ((etaDate.getTime() / 60000) - ((generatedDate.getTime() + differences) / 60000));
                if (minutes >= 0 && minutes < 1440) {
                    // minutes should be 0 to within a day
                    estimateMinutes = String.valueOf(minutes);
                }
                if (minutes > 120) {
                    // likely calculation error
                    estimateMinutes = "";
                }
                if (!TextUtils.isEmpty(estimateMinutes)) {
                    if (estimateMinutes.equals("0")) {
                        object.setEstimate(context.getString(R.string.now));
                    } else {
                        object.setEstimate(context.getString(R.string.minutes, estimateMinutes));
                    }
                }
                Boolean expired = minutes <= -3;  // time past
                expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.getUpdatedAt()) >= 2; // maybe outdated
                object.setExpired(expired);
                return object;
            } catch (ParseException |ArrayIndexOutOfBoundsException ep) {
                Timber.d(ep);
            }
        }

        SimpleDateFormat etaDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
        // given timeText
        if (!TextUtils.isEmpty(object.getText()) && object.getText().matches(".*\\d.*")
                && !object.getText().contains("unexpected") && object.getText().contains(":")) {
            // if text has digit
            String estimateMinutes = "";
            long differences = new Date().getTime() - generatedDate.getTime(); // get device timeText and compare to server timeText
            try {
                Date etaCompareDate = generatedDate;
                // first assume eta timeText and server timeText is on the same date
                Date etaDate = etaDateFormat.parse(
                        new SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                new SimpleDateFormat("MM", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                new SimpleDateFormat("dd", Locale.ENGLISH).format(etaCompareDate) + " " + object.getText());
                // if not minutes will get negative integer
                int minutes = (int) ((etaDate.getTime() / 60000) - ((generatedDate.getTime() + differences) / 60000));
                if (minutes < -12 * 60) {
                    // plus one day to get correct eta date
                    etaCompareDate = new Date(generatedDate.getTime() + 24 * 60 * 60 * 1000);
                    etaDate = etaDateFormat.parse(
                            new SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                    new SimpleDateFormat("MM", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                    new SimpleDateFormat("dd", Locale.ENGLISH).format(etaCompareDate) + " " + object.getText());
                    minutes = (int) ((etaDate.getTime() / 60000) - ((generatedDate.getTime() + differences) / 60000));
                }
                if (minutes >= 0 && minutes < 1440) {
                    // minutes should be 0 to within a day
                    estimateMinutes = String.valueOf(minutes);
                }
                if (minutes > 120) {
                    // likely calculation error
                    estimateMinutes = "";
                }
                Boolean expired = minutes <= -3;  // time past
                expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.getUpdatedAt()) >= 2; // maybe outdated
                object.setExpired(expired);
            } catch (ParseException |ArrayIndexOutOfBoundsException ep) {
                Timber.d(ep);
            }
            if (!TextUtils.isEmpty(estimateMinutes)) {
                if (estimateMinutes.equals("0")) {
                    object.setEstimate(context.getString(R.string.now));
                } else {
                    object.setEstimate(context.getString(R.string.minutes, estimateMinutes));
                }
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
                                            @NonNull RouteStop routeStop,
                                            @NonNull NwstEta nwstEta) {
        ArrivalTime object = ArrivalTime.Companion.emptyInstance(context, routeStop);
        object.setCompanyCode(C.PROVIDER.NWST);
        if (nwstEta.getCompanyCode().equals(C.PROVIDER.CTB) || nwstEta.getCompanyCode().equals(C.PROVIDER.CTB)) {
            object.setCompanyCode(nwstEta.getCompanyCode());
        }
        object.setText(text(nwstEta.getTitle()));
        String subtitle = text(nwstEta.getSubtitle());
        if (!TextUtils.isEmpty(subtitle)) {
            if (subtitle.contains("距離") || subtitle.contains("距离") || subtitle.contains("Distance")) {
                object.setDistanceKM(parseDistance(subtitle));
            }
            if (object.getDistanceKM() < 0) {
                object.setText(object.getText() + " " + subtitle);
            }
        }
        object.setNote(nwstEta.getBoundText().trim());
        object.setIsoTime(nwstEta.getEtaIsoTime());
        object.setSchedule(!TextUtils.isEmpty(nwstEta.getSubtitle()) && (nwstEta.getSubtitle().contains("預定班次") || nwstEta.getSubtitle().contains("预定班次") || nwstEta.getSubtitle().contains("Scheduled")));
        String[] data = nwstEta.getServerTime().split(":");
        if (data.length == 3) {
            try {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR, Integer.parseInt(data[0]));
                c.set(Calendar.MINUTE, Integer.parseInt(data[1]));
                c.set(Calendar.SECOND, Integer.parseInt(data[2]));
                object.setGeneratedAt(c.getTime().getTime());
            } catch (NumberFormatException ignored) {}
        }
        object.setUpdatedAt(System.currentTimeMillis());
        object = ArrivalTime.Companion.estimate(context, object);
        return object;
    }
}
