package com.alvinhkh.buseta.adapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * An {@code ArrayAdapter} that also knows how to save and restore its state.
 * Basically all it does is save/restore the array of objects being managed by
 * the Adapter.
 * </p>
 *
 * <p>
 * Note that this only saves the items and not things like checked item
 * positions. Those belong to the {@link ListView} itself. Consider using
 * {@link ListView#onSaveInstanceState()} and
 * {@link ListView#onRestoreInstanceState(Parcelable)} for this purpose.
 * </p>
 *
 *
 * @author Kiran Rao
 * @url https://gist.github.com/curioustechizen/6ed0981b013f63236f0b
 *
 * @param <T>
 *          The type of objects managed by this Adapter. Note that it must be a
 *          Parcelable.
 */

public class StateSavingArrayAdapter<T extends Parcelable> extends
        ArrayAdapter<T> {

    private static final String KEY_ADAPTER_STATE = "StateSavingArrayAdapter.KEY_ADAPTER_STATE";

    /**
     * Saves the instance state of this {@link ArrayAdapter}. It saves the array
     * of currently managed by this adapter
     *
     * @param outState
     *          The bundle into which the state is saved
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_ADAPTER_STATE, getAllItems());
    }

    /**
     * Restore the instance state of the {@link ArrayAdapter}. It re-initializes
     * the array of objects being managed by this adapter with the state retrieved
     * from {@code savedInstanceState}
     *
     * @param savedInstanceState
     *          The bundle containing the previously saved state
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(KEY_ADAPTER_STATE)) {
            ArrayList<T> objects = savedInstanceState
                    .getParcelableArrayList(KEY_ADAPTER_STATE);
            setItems(objects);
        }
    }

    /**
     * Gets all the items in this adapter as an {@code ArrayList}
     */
    public ArrayList<T> getAllItems(){
        ArrayList<T> objects = new ArrayList<T>(getCount());
        for (int i = 0; i < getCount(); i++) {
            objects.add(getItem(i));
        }
        return objects;
    }

    /*
     * Replaces the items in the adapter with those in this list
     * @param items The items to set into the adapter.
     */
    public void setItems(ArrayList<T> items){
        clear();
        addAll(items);
    }

  /* Constructors and other boilerplate */

    public StateSavingArrayAdapter(Context context, int resource) {
        super(context, resource);
    }

    public StateSavingArrayAdapter(Context context, int resource,
                                   int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public StateSavingArrayAdapter(Context context, int resource, T[] objects) {
        super(context, resource, objects);
    }

    public StateSavingArrayAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);
    }

    public StateSavingArrayAdapter(Context context, int resource,
                                   int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public StateSavingArrayAdapter(Context context, int resource,
                                   int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

}