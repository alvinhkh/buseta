package com.alvinhkh.buseta.provider;

import android.database.Cursor;

import java.util.Iterator;

/*
 * https://gist.github.com/NikolaDespotoski/c09ace9f13a58883ea8a0c9da6472498
 *
  Observable.fromIterable(RxCursorIterable.from(cursor)).doAfterNext(new Consumer<Cursor>() {
    @Override
    public void accept(Cursor cursor) throws Exception {
      if (cursor.getPosition() == cursor.getCount() - 1) {
        cursor.close();
      }
    }
  }).subscribe(new Consumer<Cursor>() {
    @Override
    public void accept(Cursor cursor) throws Exception {
      //Do something. Cursor has been moved +1 position forward.
    }
  });
 *
 */
public class RxCursorIterable implements Iterable<Cursor> {

    private Cursor mIterableCursor;

    public RxCursorIterable(Cursor c) {
        mIterableCursor = c;
    }

    public static RxCursorIterable from(Cursor c) {
        return new RxCursorIterable(c);
    }

    @Override
    public Iterator<Cursor> iterator() {
        return RxCursorIterator.from(mIterableCursor);
    }

    static class RxCursorIterator implements Iterator<Cursor> {

        private final Cursor mCursor;

        public RxCursorIterator(Cursor cursor) {
            mCursor = cursor;
        }

        public static Iterator<Cursor> from(Cursor cursor) {
            return new RxCursorIterator(cursor);
        }

        @Override
        public boolean hasNext() {
            return !mCursor.isClosed() && mCursor.moveToNext();
        }

        @Override
        public Cursor next() {
            return mCursor;
        }
    }
}