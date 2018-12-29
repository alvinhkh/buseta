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
}
