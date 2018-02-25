package com.alvinhkh.buseta.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.util.KmbEtaUtil;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.mtr.model.AESEtaBus;
import com.alvinhkh.buseta.mtr.model.MtrSchedule;
import com.alvinhkh.buseta.nlb.util.NlbEtaUtil;
import com.alvinhkh.buseta.nwst.util.NwstEtaUtil;
import com.alvinhkh.buseta.provider.EtaContract.EtaEntry;
import com.alvinhkh.buseta.provider.RxCursorIterable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.reactivex.Observable;


public class ArrivalTimeUtil {

    public static SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss dd/MM", Locale.ENGLISH);

    public static ArrivalTime estimate(@NonNull Context context, @NonNull ArrivalTime object) {
        if (!TextUtils.isEmpty(object.companyCode)) {
            switch (object.companyCode) {
                case C.PROVIDER.AESBUS:
                    return AESEtaBus.Companion.estimate(context, object);
                case C.PROVIDER.KMB:
                    return KmbEtaUtil.estimate(context, object);
                case C.PROVIDER.CTB:
                case C.PROVIDER.NWFB:
                case C.PROVIDER.NWST:
                    return NwstEtaUtil.estimate(context, object);
                case C.PROVIDER.NLB:
                    return NlbEtaUtil.estimate(context, object);
                case C.PROVIDER.MTR:
                    return MtrSchedule.Companion.estimate(context, object);
            }
        }
        return object;
    }

    public static Observable<Cursor> query(@NonNull Context context, RouteStop stop) {
        String selection = EtaEntry.COLUMN_ROUTE_COMPANY + "=? AND "
                + EtaEntry.COLUMN_ROUTE_NO + "=? AND "
                + EtaEntry.COLUMN_ROUTE_SEQ + "=? AND "
                + EtaEntry.COLUMN_STOP_ID + "=? AND "
                + EtaEntry.COLUMN_STOP_SEQ + "=? AND "
                + EtaEntry.COLUMN_UPDATED_AT + ">? AND "
                + EtaEntry.COLUMN_ETA_EXPIRE + ">?";
        Cursor query = context.getContentResolver().query(EtaEntry.CONTENT_URI,  null,
                selection, new String[] {
                        stop.getCompanyCode(),
                        stop.getRoute(),
                        stop.getDirection(),
                        stop.getCode(),
                        stop.getSequence(),
                        String.valueOf(System.currentTimeMillis() - 600000),
                        String.valueOf(System.currentTimeMillis())
                }, null);
        return Observable.fromIterable(RxCursorIterable.from(query)).doAfterNext(cursor -> {
            if (cursor.getPosition() == cursor.getCount() - 1) {
                cursor.close();
            }
        });
    }
    
    public static ContentValues toContentValues(RouteStop stop, ArrivalTime eta) {
        if (TextUtils.isEmpty(eta.expire)) {
            eta.expire = "";
        }
        if (eta.capacity == null) {
            eta.capacity = -1;
        }
        if (TextUtils.isEmpty(eta.text)) {
            eta.text = "";
        }
        ContentValues values = new ContentValues();
        values.put(EtaEntry.COLUMN_ROUTE_COMPANY, stop.getCompanyCode());
        values.put(EtaEntry.COLUMN_ROUTE_NO, stop.getRoute());
        values.put(EtaEntry.COLUMN_ROUTE_SEQ, stop.getDirection());
        values.put(EtaEntry.COLUMN_STOP_SEQ, stop.getSequence());
        values.put(EtaEntry.COLUMN_STOP_ID, stop.getCode());
        values.put(EtaEntry.COLUMN_ETA_EXPIRE, eta.expire);
        values.put(EtaEntry.COLUMN_ETA_ID, eta.id);
        values.put(EtaEntry.COLUMN_ETA_MISC,
                Boolean.toString(eta.hasWheelchair) + "," +
                        Integer.toString(eta.capacity) + "," +
                        Boolean.toString(eta.hasWifi) + "," +
                        eta.isoTime.replaceAll(",", "") + "," +
                        Float.toString(eta.distanceKM) + "," +
                        (TextUtils.isEmpty(eta.plate) ? "" : eta.plate) + "," +
                        Double.toString(eta.latitude) + "," +
                        Double.toString(eta.longitude) + "," +
                        (TextUtils.isEmpty(eta.platform) ? "" : eta.platform) + "," +
                        (TextUtils.isEmpty(eta.destination) ? "" : eta.destination) + "," +
                        (TextUtils.isEmpty(eta.direction) ? "" : eta.direction)
        );
        values.put(EtaEntry.COLUMN_ETA_SCHEDULED, Boolean.toString(eta.isSchedule));
        values.put(EtaEntry.COLUMN_ETA_TIME, eta.text);
        values.put(EtaEntry.COLUMN_ETA_URL, stop.getEtaGet());
        values.put(EtaEntry.COLUMN_GENERATED_AT, eta.generatedAt);
        values.put(EtaEntry.COLUMN_UPDATED_AT, eta.updatedAt);
        return values;
    }

    public static ArrivalTime fromCursor(@NonNull Cursor cursor) {
        ArrivalTime object = new ArrivalTime();
        object.companyCode = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ROUTE_COMPANY));
        object.expire = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_EXPIRE));
        object.id = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_ID));
        String[] misc = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_MISC)).split(",");
        object.hasWheelchair = Boolean.valueOf(misc[0]);
        if (misc.length > 1) {
            if (object.companyCode.equals(C.PROVIDER.KMB)) {
                object.capacity = KmbEtaUtil.parseCapacity(misc[1]);
            }
        }
        if (misc.length > 2) {
            object.hasWifi = Boolean.valueOf(misc[2]);
        }
        if (misc.length > 3) {
            object.isoTime = misc[3];
        }
        if (misc.length > 4 && !TextUtils.isEmpty(misc[4])) {
            object.distanceKM = Float.valueOf(misc[4]);
        }
        if (misc.length > 5 && !TextUtils.isEmpty(misc[5])) {
            object.plate = misc[5];
        }
        if (misc.length > 6 && !TextUtils.isEmpty(misc[6])) {
            object.latitude = Double.valueOf(misc[6]);
        }
        if (misc.length > 7 && !TextUtils.isEmpty(misc[7])) {
            object.longitude = Double.valueOf(misc[7]);
        }
        if (misc.length > 8 && !TextUtils.isEmpty(misc[8])) {
            object.platform = misc[8];
        }
        if (misc.length > 9 && !TextUtils.isEmpty(misc[9])) {
            object.destination = misc[9];
        }
        if (misc.length > 10 && !TextUtils.isEmpty(misc[10])) {
            object.direction = misc[10];
        }
        object.isSchedule = Boolean.valueOf(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_SCHEDULED)));
        object.text = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_TIME));
        object.generatedAt = cursor.getLong(cursor.getColumnIndex(EtaEntry.COLUMN_GENERATED_AT));
        object.updatedAt = cursor.getLong(cursor.getColumnIndex(EtaEntry.COLUMN_UPDATED_AT));
        return object;
    }

    public static ArrivalTime emptyInstance(@NonNull Context context) {
        ArrivalTime object = new ArrivalTime();
        object.id = "0";
        object.text = context.getString(R.string.message_no_data);
        object.capacity = -1;
        object.updatedAt = System.currentTimeMillis();
        object.generatedAt = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        object.expire = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(calendar.getTime());
        return object;
    }
}
