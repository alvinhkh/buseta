package com.alvinhkh.buseta.kmb.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.model.KmbEta;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;

import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;


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

    public static ArrivalTime estimate(@NonNull Context context, @NonNull ArrivalTime object) {
        SimpleDateFormat etaDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
        SimpleDateFormat etaExpireDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        Date generatedDate = object.getGeneratedAt() > 0L ? new Date() : new Date(object.getGeneratedAt());
        // given timeText
        if (!TextUtils.isEmpty(object.getText()) && object.getText().matches(".*\\d.*") && !object.getText().contains("unexpected")) {
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
                if (minutes >= 0 && minutes < 24 * 60) {
                    // minutes should be 0 to within a day
                    estimateMinutes = String.valueOf(minutes);
                }
                if (minutes > 60) {
                    // calculation error
                    // they only provide eta within 60 minutes
                    estimateMinutes = "";
                }
                object.setExpired(minutes <= -3);  // time past
                object.setExpired(object.getExpired() | TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.getUpdatedAt()) >= 5); // maybe outdated
                object.setExpired(object.getExpired() | TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - generatedDate.getTime()) >= 5);  // maybe outdated
                if (!TextUtils.isEmpty(object.getExpire())) {
                    Date etaExpireDate = etaExpireDateFormat.parse(object.getExpire());
                    if (etaExpireDate != null)
                        object.setExpired(object.getExpired() | TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - etaExpireDate.getTime()) >= 0);  // expired
                }
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

    public static ArrivalTime toArrivalTime(@NonNull Context context,
                                            @NonNull KmbEta eta,
                                            @NonNull Long generatedTime) {
        ArrivalTime object = new ArrivalTime();
        object.setCompanyCode(C.PROVIDER.KMB);
        object.setCapacity(parseCapacity(eta.ol));
        object.setExpire(eta.expire);
        object.setSchedule(!TextUtils.isEmpty(eta.schedule) && eta.schedule.equals("Y"));
        object.setHasWheelchair(!TextUtils.isEmpty(eta.wheelchair) && eta.wheelchair.equals("Y"));
        object.setHasWifi(!TextUtils.isEmpty(eta.wifi) && eta.wifi.equals("Y"));
        object.setText(text(eta.time));
        object.setGeneratedAt(generatedTime);
        object.setUpdatedAt(System.currentTimeMillis());
        object = ArrivalTimeUtil.estimate(context, object);
        return object;
    }
}
