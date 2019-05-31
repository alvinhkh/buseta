package com.alvinhkh.buseta.nlb.util;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NlbEtaUtil {

    public static ArrivalTime estimate(@NonNull Context context,
                                       @NonNull ArrivalTime object) {
        if (!TextUtils.isEmpty(object.getText())) {
            Pattern p = Pattern.compile("(\\d*)(分鐘| min\\(s\\))");
            Matcher m = p.matcher(object.getText());
            if (m.find()) {
                int minutes = Integer.parseInt(m.group(1));
                if (minutes > 0 && minutes < 60) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MINUTE, minutes);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                    object.setEstimate(sdf.format(calendar.getTime()));
                }
                Boolean expired = minutes <= -3;  // time past
                expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - object.getUpdatedAt()) >= 5; // maybe outdated
                object.setExpired(expired);
            }
        }
        return object;
    }
}
