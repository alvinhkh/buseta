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

    public static Observable<Cursor> queryAt(@NonNull Context context) {
        String selection = SuggestionTable.COLUMN_TEXT + " LIKE ?"
                + " AND " + SuggestionTable.COLUMN_TYPE + " = ?";
        Cursor query = context.getContentResolver().query(SuggestionProvider.CONTENT_URI,  null,
                selection, new String[] {
                        "%%",
                        SuggestionTable.TYPE_HISTORY
                }, SuggestionTable.COLUMN_DATE + " DESC");
        return Observable.fromIterable(RxCursorIterable.from(query)).doFinally(() -> {
            if (query != null && !query.isClosed()) {
                query.close();
            }
        });
    }

    public static Observable<Cursor> query(@NonNull Context context, SearchHistory history) {
        String selection = SuggestionTable.COLUMN_TEXT + " = '?'"
                + " AND " + SuggestionTable.COLUMN_TYPE + " = ?";
        Cursor query = context.getContentResolver().query(SuggestionProvider.CONTENT_URI,  null,
                selection, new String[] {
                        history == null ? "%%" : history.route,
                        SuggestionTable.TYPE_HISTORY
                }, SuggestionTable.COLUMN_DATE + " DESC");
        return Observable.fromIterable(RxCursorIterable.from(query)).doFinally(() -> {
            if (query != null && !query.isClosed()) {
                query.close();
            }
        });
    }

    public static Integer delete(@NonNull Context context, SearchHistory history) {
        return context.getContentResolver().delete(SuggestionProvider.CONTENT_URI,
                SuggestionTable.COLUMN_TYPE + "=? AND "
                        + SuggestionTable.COLUMN_TEXT + "=?",
                new String[]{
                        history.record_type,
                        history.route
                });
    }

    public static Integer deleteAll(@NonNull Context context) {
        return context.getContentResolver().delete(SuggestionProvider.CONTENT_URI, null, null);
    }

    public static ContentValues toContentValues(@NonNull SearchHistory history) {
        ContentValues values = new ContentValues();
        values.put(SuggestionTable.COLUMN_TEXT, history.route);
        values.put(SuggestionTable.COLUMN_TYPE, SuggestionTable.TYPE_HISTORY);
        values.put(SuggestionTable.COLUMN_DATE, String.valueOf(System.currentTimeMillis() / 1000L));
        return values;
    }

    public static SearchHistory fromCursor(@NonNull Cursor cursor) {
        SearchHistory object = new SearchHistory();
        object.route = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TEXT));
        object.record_type = cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TYPE));
        return object;
    }


    public static Cursor query(@NonNull Context context, Integer count) {
        Uri uri = SuggestionProvider.CONTENT_URI_SUGGESTIONS;
        String selection = SuggestionTable.COLUMN_TEXT + " LIKE '%%' AND " +
                SuggestionTable.COLUMN_TYPE + " = '" + SuggestionTable.TYPE_HISTORY + "'";
        String sortOrder = SuggestionTable.COLUMN_DATE + " DESC LIMIT " + String.valueOf(count);
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
