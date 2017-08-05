package com.alvinhkh.buseta.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.FollowTable;
import com.alvinhkh.buseta.provider.RxCursorIterable;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

public class FollowStopUtil {

    public static Observable<Cursor> query(@NonNull Context context, FollowStop followStop) {
        String selection = FollowTable.COLUMN_ROUTE + " = ?" +
                " AND " + FollowTable.COLUMN_BOUND + " = ?" +
                " AND " + FollowTable.COLUMN_STOP_SEQ + " = ?";
        Cursor query = context.getContentResolver().query(FollowProvider.CONTENT_URI_FOLLOW, null,
                selection, new String[] {
                        followStop.route,
                        followStop.direction,
                        followStop.sequence
                }, null);
        return Observable.fromIterable(RxCursorIterable.from(query)).doFinally(() -> {
            if (query != null && !query.isClosed()) {
                query.close();
            }
        });
    }

    public static Cursor queryAll(@NonNull Context context) {
        return context.getContentResolver().query(FollowProvider.CONTENT_URI_FOLLOW, null, null, null,
                FollowTable.COLUMN_DATE + " DESC");
    }

    public static Integer delete(@NonNull Context context, FollowStop followStop) {
        return context.getContentResolver().delete(
                        FollowProvider.CONTENT_URI_FOLLOW,
                        FollowTable.COLUMN_ROUTE + " = ?" +
                        " AND " + FollowTable.COLUMN_BOUND + " = ?" +
                        " AND " + FollowTable.COLUMN_STOP_SEQ + " = ?",
                new String[] {
                        followStop.route,
                        followStop.direction,
                        followStop.sequence
                });
    }

    public static Integer deleteAll(@NonNull Context context) {
        return context.getContentResolver().delete(FollowProvider.CONTENT_URI_FOLLOW, null, null);
    }

    public static Uri insert(@NonNull Context context, FollowStop followStop) {
        followStop.updatedAt = System.currentTimeMillis();
        return context.getContentResolver().insert(FollowProvider.CONTENT_URI_FOLLOW, toContentValues(followStop));
    }

    public static ContentValues toContentValues(FollowStop stop) {
        ContentValues values = new ContentValues();
        values.put(FollowTable.COLUMN_ROUTE, stop.route);
        values.put(FollowTable.COLUMN_BOUND, stop.direction);
        values.put(FollowTable.COLUMN_STOP_CODE, stop.code);
        values.put(FollowTable.COLUMN_STOP_SEQ, stop.sequence);
        values.put(FollowTable.COLUMN_DESTINATION, stop.locationEnd);
        values.put(FollowTable.COLUMN_ORIGIN, stop.locationStart);
        values.put(FollowTable.COLUMN_STOP_NAME, stop.name);
        values.put(FollowTable.COLUMN_DATE, stop.updatedAt);
        return values;
    }
    
    public static FollowStop fromCursor(@NonNull Cursor cursor) {
        FollowStop object = new FollowStop();
        object._id = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_ID));
        object.route = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_ROUTE));
        object.direction = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_BOUND));
        object.code = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_STOP_CODE));
        object.sequence = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_STOP_SEQ));
        object.locationEnd = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_DESTINATION));
        object.locationStart = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_ORIGIN));
        object.name = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_STOP_NAME));
        object.updatedAt = cursor.getLong(cursor.getColumnIndex(FollowTable.COLUMN_DATE));

        object.company = BusRoute.COMPANY_KMB;
        object.etaGet = String.format("/?action=geteta&lang=tc&route=%s&bound=%s&stop=%s&stop_seq=%s", object.route, object.direction, object.code, object.sequence);
        return object;
    }

    public static List<FollowStop> toList(@NonNull Context context) {
        Cursor cursor = FollowStopUtil.queryAll(context);
        List<FollowStop> list = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                FollowStop stop = fromCursor(cursor);
                list.add(stop);
            }
            cursor.close();
        }
        return list;
    }
}
