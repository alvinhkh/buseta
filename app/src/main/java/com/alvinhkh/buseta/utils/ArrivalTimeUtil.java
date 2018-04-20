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
        if (!TextUtils.isEmpty(object.getCompanyCode())) {
            switch (object.getCompanyCode()) {
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
        if (stop == null || TextUtils.isEmpty(stop.getCompanyCode())) {
            return null;
        }
        if (TextUtils.isEmpty(eta.getExpire())) {
            eta.setExpire("");
        }
        if (TextUtils.isEmpty(eta.getText())) {
            eta.setText("");
        }
        ContentValues values = new ContentValues();
        values.put(EtaEntry.COLUMN_ROUTE_COMPANY, stop.getCompanyCode());
        values.put(EtaEntry.COLUMN_ROUTE_NO, stop.getRoute());
        values.put(EtaEntry.COLUMN_ROUTE_SEQ, stop.getDirection());
        values.put(EtaEntry.COLUMN_STOP_SEQ, stop.getSequence());
        values.put(EtaEntry.COLUMN_STOP_ID, stop.getCode());
        values.put(EtaEntry.COLUMN_ETA_EXPIRE, eta.getExpire());
        values.put(EtaEntry.COLUMN_ETA_ID, eta.getId());
        values.put(EtaEntry.COLUMN_ETA_MISC,
                Boolean.toString(eta.getHasWheelchair()) + "," +
                        Integer.toString(eta.getCapacity()) + "," +
                        Boolean.toString(eta.getHasWifi()) + "," +
                        eta.getIsoTime().replaceAll(",", "") + "," +
                        Float.toString(eta.getDistanceKM()) + "," +
                        (TextUtils.isEmpty(eta.getPlate()) ? "" : eta.getPlate()) + "," +
                        Double.toString(eta.getLatitude()) + "," +
                        Double.toString(eta.getLongitude()) + "," +
                        (TextUtils.isEmpty(eta.getPlatform()) ? "" : eta.getPlatform()) + "," +
                        (TextUtils.isEmpty(eta.getDestination()) ? "" : eta.getDestination()) + "," +
                        (TextUtils.isEmpty(eta.getDirection()) ? "" : eta.getDirection()) + "," +
                        (TextUtils.isEmpty(eta.getNote()) ? "" : eta.getNote())
        );
        values.put(EtaEntry.COLUMN_ETA_SCHEDULED, Boolean.toString(eta.isSchedule()));
        values.put(EtaEntry.COLUMN_ETA_TIME, eta.getText());
        values.put(EtaEntry.COLUMN_ETA_URL, stop.getEtaGet());
        values.put(EtaEntry.COLUMN_GENERATED_AT, eta.getGeneratedAt());
        values.put(EtaEntry.COLUMN_UPDATED_AT, eta.getUpdatedAt());
        return values;
    }

    public static ArrivalTime fromCursor(@NonNull Cursor cursor) {
        ArrivalTime object = new ArrivalTime();
        object.setCompanyCode(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ROUTE_COMPANY)));
        object.setExpire(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_EXPIRE)));
        object.setId(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_ID)));
        String[] misc = cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_MISC)).split(",");
        object.setHasWheelchair(Boolean.valueOf(misc[0]));
        if (misc.length > 1) {
            if (!TextUtils.isEmpty(object.getCompanyCode()) && object.getCompanyCode().equals(C.PROVIDER.KMB)) {
                object.setCapacity(KmbEtaUtil.parseCapacity(misc[1]));
            }
        }
        if (misc.length > 2) {
            object.setHasWifi(Boolean.valueOf(misc[2]));
        }
        if (misc.length > 3) {
            object.setIsoTime(misc[3]);
        }
        if (misc.length > 4 && !TextUtils.isEmpty(misc[4])) {
            object.setDistanceKM(Float.valueOf(misc[4]));
        }
        if (misc.length > 5 && !TextUtils.isEmpty(misc[5])) {
            object.setPlate(misc[5]);
        }
        if (misc.length > 6 && !TextUtils.isEmpty(misc[6])) {
            object.setLatitude(Double.parseDouble(misc[6]));
        }
        if (misc.length > 7 && !TextUtils.isEmpty(misc[7])) {
            object.setLongitude(Double.parseDouble(misc[7]));
        }
        if (misc.length > 8 && !TextUtils.isEmpty(misc[8])) {
            object.setPlatform(misc[8]);
        }
        if (misc.length > 9 && !TextUtils.isEmpty(misc[9])) {
            object.setDestination(misc[9]);
        }
        if (misc.length > 10 && !TextUtils.isEmpty(misc[10])) {
            object.setDirection(misc[10]);
        }
        if (misc.length > 11 && !TextUtils.isEmpty(misc[11])) {
            object.setNote(misc[11]);
        }
        object.setSchedule(Boolean.valueOf(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_SCHEDULED))));
        object.setText(cursor.getString(cursor.getColumnIndex(EtaEntry.COLUMN_ETA_TIME)));
        object.setGeneratedAt(cursor.getLong(cursor.getColumnIndex(EtaEntry.COLUMN_GENERATED_AT)));
        object.setUpdatedAt(cursor.getLong(cursor.getColumnIndex(EtaEntry.COLUMN_UPDATED_AT)));
        return object;
    }

    public static ArrivalTime emptyInstance(@NonNull Context context) {
        ArrivalTime object = new ArrivalTime();
        object.setId("0");
        object.setText(context.getString(R.string.message_no_data));
        object.setCapacity(-1);
        object.setUpdatedAt(System.currentTimeMillis());
        object.setGeneratedAt(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        object.setExpire(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(calendar.getTime()));
        return object;
    }
}
