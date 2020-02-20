package com.alvinhkh.buseta.nwst.util;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;


public class NwstEtaUtil {

    public static ArrivalTime estimate(@NonNull Context context, @NonNull ArrivalTime object) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        SimpleDateFormat isoDf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
        Date generatedDate = object.getGeneratedAt() > 0L ? new Date() : new Date(object.getGeneratedAt());

        // given iso time
        if (!TextUtils.isEmpty(object.getIsoTime())) {
            long differences = new Date().getTime() - generatedDate.getTime(); // get device timeText and compare to server timeText
            String estimateMinutes = "";
            Date etaDate = null;
            try {
                etaDate = isoDf.parse(object.getIsoTime());
            } catch (ParseException e1) {
                try {
                    etaDate = df.parse(object.getIsoTime());
                } catch (ParseException ignored) {
                }
            }
            try {
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
                boolean expired = minutes <= -3;  // time past
                expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.getUpdatedAt()) >= 2; // maybe outdated
                object.setExpired(expired);
                return object;
            } catch (ArrayIndexOutOfBoundsException| NullPointerException ep) {
                Timber.d(ep);
            }
        }

        return object;
    }
}
