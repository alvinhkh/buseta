package com.alvinhkh.buseta.utils;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

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
                        history == null ? "%%" : history.getRoute(),
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
                        history.getType(),
                        history.getCompanyCode(),
                        history.getRoute()
                });
    }

    public static ContentValues toContentValues(@NonNull SearchHistory history) {
        ContentValues values = new ContentValues();
        values.put(SuggestionTable.COLUMN_TEXT, history.getRoute());
        values.put(SuggestionTable.COLUMN_COMPANY, history.getCompanyCode());
        values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_HISTORY);
        values.put(SuggestionTable.COLUMN_DATE, history.getTimestamp());
        return values;
    }

    public static SearchHistory fromCursor(@NonNull Cursor cursor) {
        SearchHistory object = new SearchHistory();
        object.setCompanyCode(cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_COMPANY)));
        object.setRoute(cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TEXT)));
        object.setTimestamp(Long.parseLong(cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_DATE))));
        object.setType(cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TYPE)));
        return object;
    }

    public static SearchHistory createInstance(@NonNull String routeNo, @NonNull String companyCode) {
        SearchHistory object = new SearchHistory();
        object.setCompanyCode(companyCode);
        object.setRoute(routeNo);
        object.setTimestamp(System.currentTimeMillis() / 1000L);
        object.setType(SuggestionTable.TYPE_HISTORY);
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
