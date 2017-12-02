package com.alvinhkh.buseta.utils;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.model.SearchHistory;
import com.alvinhkh.buseta.provider.RxCursorIterable;
import com.alvinhkh.buseta.provider.SuggestionProvider;
import com.alvinhkh.buseta.provider.SuggestionTable;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

public class SearchHistoryUtil {

    public static Observable<Cursor> query(@NonNull Context context, SearchHistory history) {
        String selection = SuggestionTable.COLUMN_TEXT + " = '?'"
                + " AND " + SuggestionTable.COLUMN_TYPE + " = ?";
        Cursor query = context.getContentResolver().query(SuggestionProvider.CONTENT_URI,  null,
                selection, new String[] {
                        history == null ? "%%" : history.route,
                        SuggestionTable.TYPE_HISTORY
                }, SuggestionTable.COLUMN_DATE + " DESC, " + SuggestionTable.COLUMN_TEXT + " ASC");
        return Observable.fromIterable(RxCursorIterable.from(query)).doFinally(() -> {
            if (query != null && !query.isClosed()) {
                query.close();
            }
        });
    }

    public static Integer delete(@NonNull Context context, SearchHistory history) {
        return context.getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                SuggestionTable.COLUMN_TYPE + "=? AND "
                        + SuggestionTable.COLUMN_COMPANY + "=? AND "
                        + SuggestionTable.COLUMN_TEXT + "=?",
                new String[]{
                        history.recordType,
                        history.companyCode,
                        history.route
                });
    }

    public static ContentValues toContentValues(@NonNull SearchHistory history) {
        ContentValues values = new ContentValues();
        values.put(SuggestionTable.COLUMN_TEXT, history.route);
        values.put(SuggestionTable.COLUMN_COMPANY, history.companyCode);
        values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_HISTORY);
        values.put(SuggestionTable.COLUMN_DATE, history.timestamp);
        return values;
    }

    public static SearchHistory fromCursor(@NonNull Cursor cursor) {
        SearchHistory object = new SearchHistory();
        object.route = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TEXT));
        object.recordType = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TYPE));
        object.companyCode = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_COMPANY));
        object.timestamp = Long.parseLong(cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_DATE)));
        return object;
    }

    public static SearchHistory createInstance(@NonNull String routeNo, @NonNull String companyCode) {
        SearchHistory object = new SearchHistory();
        object.route = routeNo;
        object.companyCode = companyCode;
        object.recordType = SuggestionTable.TYPE_HISTORY;
        object.timestamp = System.currentTimeMillis() / 1000L;
        return object;
    }

    public static Cursor query(@NonNull Context context, Integer count) {
        Uri uri = SuggestionProvider.CONTENT_URI_SUGGESTIONS;
        String selection = SuggestionTable.COLUMN_TEXT + " LIKE '%%' AND " +
                SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'";
        String sortOrder = SuggestionTable.COLUMN_DATE + " DESC, " + SuggestionTable.COLUMN_TEXT + " ASC LIMIT " + String.valueOf(count);
        return context.getContentResolver().query(uri, null, selection, null, sortOrder);
    }

    public static List<SearchHistory> toList(Cursor cursor) {
        List<SearchHistory> suggestionList = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                SearchHistory history = SearchHistoryUtil.fromCursor(cursor);
                suggestionList.add(history);
            }
            cursor.close();
        }
        return suggestionList;
    }
}
