package com.alvinhkh.buseta.ui.search;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;

import com.alvinhkh.buseta.provider.SuggestionTable;

import timber.log.Timber;

public class SuggestionSimpleCursorAdapter extends SimpleCursorAdapter {

    public SuggestionSimpleCursorAdapter(Context context, int layout, Cursor c,
                                         String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public CharSequence convertToString(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(SuggestionTable.COLUMN_TEXT));
    }
}