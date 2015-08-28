package com.alvinhkh.buseta.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;

import com.alvinhkh.buseta.database.SuggestionsDatabase;

public class SuggestionSimpleCursorAdapter extends SimpleCursorAdapter {

    public SuggestionSimpleCursorAdapter(Context context, int layout, Cursor c,
                                         String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    public SuggestionSimpleCursorAdapter(Context context, int layout, Cursor c,
                                         String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public CharSequence convertToString(Cursor cursor) {
        int indexColumnSuggestion = cursor.getColumnIndex(SuggestionsDatabase.COLUMN_TEXT);
        return cursor.getString(indexColumnSuggestion);
    }

}