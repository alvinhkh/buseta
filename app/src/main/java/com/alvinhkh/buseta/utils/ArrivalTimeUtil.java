package com.alvinhkh.buseta.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.util.KmbEtaUtil;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.nlb.util.NlbEtaUtil;
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
                case BusRoute.COMPANY_KMB:
                    return KmbEtaUtil.estimate(context, object);
                case BusRoute.COMPANY_NLB:
                    return NlbEtaUtil.estimate(context, object);
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
                        stop.companyCode,
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
        values.put(EtaEntry.COLUMN_ROUTE_COMPANY, stop.companyCode);
        values.put(EtaEntry.COLUMN_ROUTE_NO, stop.route);
        values.put(EtaEntry.COLUMN_ROUTE_SEQ, stop.direction);
        values.put(EtaEntry.COLUMN_STOP_SEQ, stop.sequence);
        values.put(EtaEntry.COLUMN_STOP_ID, stop.code);

        values.put(EtaEntry.COLUMN_ETA_EXPIRE, eta.expire);
        values.put(EtaEntry.COLUMN_ETA_ID, eta.id);
        values.put(EtaEntry.COLUMN_ETA_MISC, Boolean.toString(eta.hasWheelchair) + "," +
                Integer.toString(eta.capacity) + "," + Boolean.toString(eta.hasWifi));
        values.put(EtaEntry.COLUMN_ETA_SCHEDULED, Boolean.toString(eta.isSchedule));
        values.put(EtaEntry.COLUMN_ETA_TIME, eta.text);
        values.put(EtaEntry.COLUMN_ETA_URL, stop.etaGet);
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
        object.capacity = KmbEtaUtil.parseCapacity(misc[1]);
        object.hasWifi = Boolean.valueOf(misc[2]);
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
