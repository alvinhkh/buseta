package com.alvinhkh.buseta.holder;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.alvinhkh.buseta.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EtaAdapterHelper {

    private static final String TAG = EtaAdapterHelper.class.getSimpleName();

    public static SimpleDateFormat display_format = new SimpleDateFormat("HH:mm:ss dd/MM", Locale.US);

    public static Date serverDate(RouteStop object) {
        Date server_date = null;
        if (null != object.eta.server_time && !object.eta.server_time.equals("")) {
            if (object.eta.api_version == 2) {
                server_date = new Date(Long.parseLong(object.eta.server_time));
            } else if (object.eta.api_version == 1) {
                SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);
                try {
                    server_date = date_format.parse(object.eta.server_time);
                } catch (ParseException ep) {
                    ep.printStackTrace();
                }
            }
        }
        return server_date;
    }

    public static Date updatedDate(RouteStop object) {
        Date updated_date = null;
        if (null != object.eta.updated && !object.eta.updated.equals("")) {
            if (object.eta.api_version == 2) {
                updated_date = new Date(Long.parseLong(object.eta.updated));
            }
        }
        return updated_date;
    }

    public static int greyOutMinutes = 3;

    public static String etaEstimate(RouteStop object, String[] etas, int i, Date server_date,
                                     Context context, TextView tEta, TextView tEtaMore) {
        StringBuilder sb = new StringBuilder();
        // given time
        if (etas[i].matches(".*\\d.*")) {
            // if text has digit
            String etaMinutes = "";
            long differences = new Date().getTime() - server_date.getTime(); // get device time and compare to server time
            try {
                SimpleDateFormat time_format =
                        new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
                Date etaDateCompare = server_date;
                // first assume eta time and server time is on the same date
                Date etaDate = time_format.parse(
                        new SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaDateCompare) + "/" +
                                new SimpleDateFormat("MM", Locale.ENGLISH).format(etaDateCompare) + "/" +
                                new SimpleDateFormat("dd", Locale.ENGLISH).format(etaDateCompare) + " " +
                                etas[i]);
                // if not minutes will get negative integer
                int minutes = (int) ((etaDate.getTime() / 60000) -
                        ((server_date.getTime() + differences) / 60000));
                if (minutes < -12 * 60) {
                    // plus one day to get correct eta date
                    etaDateCompare = new Date(server_date.getTime() + 24 * 60 * 60 * 1000);
                    etaDate = time_format.parse(
                            new SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaDateCompare) + "/" +
                                    new SimpleDateFormat("MM", Locale.ENGLISH).format(etaDateCompare) + "/" +
                                    new SimpleDateFormat("dd", Locale.ENGLISH).format(etaDateCompare) + " " +
                                    etas[i]);
                    minutes = (int) ((etaDate.getTime() / 60000) -
                            ((server_date.getTime() + differences) / 60000));
                }
                if (minutes >= 0 && minutes < 24 * 60) {
                    // minutes should be 0 to within a day
                    etaMinutes = String.valueOf(minutes);
                }
                if (minutes >= 60) {
                    // calculation error
                    // they only provide eta within 60 minutes
                    etaMinutes = "";
                }
                // grey out
                if (null != context && null != tEta && null != tEtaMore)
                if (i == 0)
                    tEta.setTextColor((minutes <= -greyOutMinutes) ?
                            ContextCompat.getColor(context, R.color.diminish_text) :
                            ContextCompat.getColor(context, R.color.highlighted_text));
                else
                    tEtaMore.setTextColor(
                            (minutes <= -greyOutMinutes && i == etas.length - 1) ?
                                    ContextCompat.getColor(context, R.color.diminish_text) :
                                    ContextCompat.getColor(context, R.color.primary_text));
            } catch (ParseException ep) {
                Log.e(TAG, ep.getMessage());
            }
            if (!etaMinutes.equals("")) {
                sb.append(" (");
                if (etaMinutes.equals("0")) {
                    sb.append("現在");
                } else {
                    sb.append(etaMinutes);
                    sb.append("分鐘");
                }
                sb.append(")");
            }
        }
        return sb.toString();
    }
    
}
