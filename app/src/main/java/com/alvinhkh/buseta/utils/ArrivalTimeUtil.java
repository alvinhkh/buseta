package com.alvinhkh.buseta.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.model.KmbEta;
import com.alvinhkh.buseta.kmb.util.KmbEtaUtil;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.provider.EtaContract.EtaEntry;
import com.alvinhkh.buseta.provider.RxCursorIterable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import timber.log.Timber;


public class ArrivalTimeUtil {

    public static SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss dd/MM", Locale.ENGLISH);

    public static ArrivalTime estimate(@NonNull Context context, @NonNull ArrivalTime object) {
        SimpleDateFormat etaDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH);
        SimpleDateFormat etaExpireDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        Date generatedDate = object.generatedAt == null ? new Date() : new Date(object.generatedAt);
        // given timeText
        if (!TextUtils.isEmpty(object.text) && object.text.matches(".*\\d.*")) {
            // if text has digit
            String estimateMinutes = "";
            long differences = new Date().getTime() - generatedDate.getTime(); // get device timeText and compare to server timeText
            try {
                Date etaCompareDate = generatedDate;
                // first assume eta timeText and server timeText is on the same date
                Date etaDate = etaDateFormat.parse(
                        new SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                new SimpleDateFormat("MM", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                new SimpleDateFormat("dd", Locale.ENGLISH).format(etaCompareDate) + " " + object.text);
                // if not minutes will get negative integer
                int minutes = (int) ((etaDate.getTime() / 60000) - ((generatedDate.getTime() + differences) / 60000));
                if (minutes < -12 * 60) {
                    // plus one day to get correct eta date
                    etaCompareDate = new Date(generatedDate.getTime() + 24 * 60 * 60 * 1000);
                    etaDate = etaDateFormat.parse(
                            new SimpleDateFormat("yyyy", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                    new SimpleDateFormat("MM", Locale.ENGLISH).format(etaCompareDate) + "/" +
                                    new SimpleDateFormat("dd", Locale.ENGLISH).format(etaCompareDate) + " " + object.text);
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
                // grey out
                object.expired = minutes <= -3    // time past
                        || TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - generatedDate.getTime()) >= 5;    // maybe outdated
                if (object.expire != null) {
                    Date etaExpireDate = etaExpireDateFormat.parse(object.expire);
                    if (etaExpireDate != null)
                    object.expired |= TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - etaExpireDate.getTime()) >= 0;    // expired
                }
            } catch (ParseException|ArrayIndexOutOfBoundsException ep) {
                Timber.e(ep);
            }
            if (!TextUtils.isEmpty(estimateMinutes)) {
                if (estimateMinutes.equals("0")) {
                    object.estimate = context.getString(R.string.now);
                } else {
                    object.estimate = context.getString(R.string.minutes, estimateMinutes);
                }
            }
        }
        return object;
    }

    public static Observable<Cursor> query(@NonNull Context context, BusRouteStop stop) {
        String selection = EtaEntry.COLUMN_ROUTE_COMPANY + "=? AND "
                + EtaEntry.COLUMN_ROUTE_NO + "=? AND "
                + EtaEntry.COLUMN_ROUTE_SEQ + "=? AND "
                + EtaEntry.COLUMN_STOP_SEQ + "=? AND "
                + EtaEntry.COLUMN_UPDATED_AT + ">? AND "
                + EtaEntry.COLUMN_ETA_EXPIRE + ">?";
        Cursor query = context.getContentResolver().query(EtaEntry.CONTENT_URI,  null,
                selection, new String[] {
                        stop.company,
                        stop.route,
                        stop.direction,
                        stop.sequence,
                        String.valueOf(System.currentTimeMillis() - 600000),
                        String.valueOf(System.currentTimeMillis())
                }, null);
        return Observable.fromIterable(RxCursorIterable.from(query)).doAfterNext(cursor -> {
            if (cursor.getPosition() == cursor.getCount() - 1) {
                cursor.close();
            }
        });
    }
    
    public static ContentValues toContentValues(BusRouteStop stop, ArrivalTime eta) {
        ContentValues values = new ContentValues();
        values.put(EtaEntry.COLUMN_ROUTE_COMPANY, stop.company);
        values.put(EtaEntry.COLUMN_ROUTE_NO, stop.route);
        values.put(EtaEntry.COLUMN_ROUTE_SEQ, stop.direction);
        values.put(EtaEntry.COLUMN_STOP_SEQ, stop.sequence);
        values.put(EtaEntry.COLUMN_STOP_ID, stop.code);

        values.put(EtaEntry.COLUMN_ETA_EXPIRE, eta.expire);
        values.put(EtaEntry.COLUMN_ETA_ID, eta.id);
        values.put(EtaEntry.COLUMN_ETA_MISC, Boolean.toString(eta.hasWheelchair) + "," + Integer.toString(eta.capacity) + "," + Boolean.toString(eta.hasWifi));
        values.put(EtaEntry.COLUMN_ETA_SCHEDULED, Boolean.toString(eta.isSchedule));
        values.put(EtaEntry.COLUMN_ETA_TIME, eta.text);
        values.put(EtaEntry.COLUMN_ETA_URL, stop.etaGet);
        values.put(EtaEntry.COLUMN_GENERATED_AT, eta.generatedAt);
        values.put(EtaEntry.COLUMN_UPDATED_AT, eta.updatedAt);
        return values;
    }

    public static ArrivalTime fromCursor(@NonNull Cursor cursor) {
        ArrivalTime object = new ArrivalTime();
        object.expire = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_EXPIRE));
        object.id = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_ID));
        String[] misc = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_MISC)).split(",");
        object.hasWheelchair = Boolean.valueOf(misc[0]);
        object.capacity = KmbEtaUtil.parseCapacity(misc[1]);
        object.hasWifi = Boolean.valueOf(misc[2]);
        object.isSchedule = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_SCHEDULED)));
        object.text = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_TIME));
        object.generatedAt = cursor.getLong(cursor.getColumnIndex(EtaEntry.COLUMN_GENERATED_AT));
        object.updatedAt = cursor.getLong(cursor.getColumnIndex(EtaEntry.COLUMN_UPDATED_AT));
        return object;
    }
}
