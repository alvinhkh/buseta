package com.alvinhkh.buseta.ui.follow;

import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.provider.FollowProvider;
import com.alvinhkh.buseta.provider.FollowTable;
import com.alvinhkh.buseta.provider.RxCursorIterable;
import com.alvinhkh.buseta.service.RxBroadcastReceiver;
import com.alvinhkh.buseta.ui.OnItemDragListener;
import com.alvinhkh.buseta.ui.SimpleItemTouchHelperCallback;
import com.alvinhkh.buseta.utils.FollowStopUtil;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import timber.log.Timber;


public class EditFollowFragment extends Fragment implements OnItemDragListener {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private ItemTouchHelper itemTouchHelper;

    private EditFollowAdapter adapter;

    private RecyclerView recyclerView;

    private View emptyView;

    public EditFollowFragment() {
    }

    /**
     * Returns a new instance of this fragment
     */
    public static EditFollowFragment newInstance() {
        EditFollowFragment fragment = new EditFollowFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_follow_edit, container, false);
        setHasOptionsMenu(true);

        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EditFollowAdapter(recyclerView, this);
        itemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);
        emptyView = rootView.findViewById(R.id.empty_view);

        FloatingActionButton fab = getActivity().findViewById(R.id.fab);
        if (fab != null) {
            fab.hide();
        }

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disposables.add(RxBroadcastReceiver.create(getContext(), new IntentFilter(C.ACTION.FOLLOW_UPDATE))
                .share().subscribeWith(followObserver()));
        if (recyclerView != null) {
            Snackbar.make(recyclerView, R.string.swipe_to_remove_follow_stop, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.edit_follow_list);
            }
        }
        FollowStopUtil.resetOrder(getContext(), adapter);
        if (emptyView != null) {
            emptyView.setVisibility(adapter != null && adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        if (adapter != null) {
            adapter.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            if (adapter != null) {
                adapter.reloadCursor();
                adapter.notifyDataSetChanged();
                if (emptyView != null) {
                    emptyView.setVisibility(adapter != null && adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private DisposableObserver<Intent> followObserver() {
        return new DisposableObserver<Intent>() {
            @Override
            public void onNext(Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) return;
                FollowStop followStop = bundle.getParcelable(C.EXTRA.STOP_OBJECT);
                if (followStop == null) return;
                if (bundle.getBoolean(C.EXTRA.UPDATED)) {
                    // TODO: only change item changed
                    //adapter.reloadCursor();
                    //adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                if (emptyView != null) {
                    emptyView.setVisibility(adapter != null && adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                }
            }
        };
    }

    private Integer dragItemPosition = -1;

    @Override
    public void onItemStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
        dragItemPosition = viewHolder.getAdapterPosition();
    }

    @Override
    public void onItemStopDrag(RecyclerView.ViewHolder viewHolder) {
        if (adapter != null && dragItemPosition >= 0
                && dragItemPosition != viewHolder.getAdapterPosition()) {
            Integer fromPosition = dragItemPosition;
            Integer toPosition = viewHolder.getAdapterPosition();
            FollowStop object = adapter.getItem(fromPosition);
            Cursor query = FollowStopUtil.queryAll(getContext());
            if (query != null) {
                if (fromPosition < toPosition) {  // down
                    query.moveToPosition(fromPosition - 1);
                } else if (fromPosition > toPosition) {  // up
                    query.moveToPosition(toPosition - 1);
                }
            }
            Observable.fromIterable(RxCursorIterable.from(query)).doFinally(() -> {
                if (query != null && !query.isClosed()) {
                    query.close();
                }
                ContentValues values = new ContentValues();
                if (fromPosition < toPosition) {  // down
                    values.put(FollowTable.COLUMN_DISPLAY_ORDER, toPosition);
                } else if (fromPosition > toPosition) {  // up
                    values.put(FollowTable.COLUMN_DISPLAY_ORDER, toPosition);
                }
                getContext().getContentResolver().update(FollowProvider.CONTENT_URI, values,
                        FollowTable.COLUMN_ID + "=?", new String[]{object._id});

                adapter.reloadCursor();
                //if (fromPosition < toPosition) {  // down
                //  adapter.notifyItemRangeChanged(fromPosition - 1, toPosition + 1);
                //} else if (fromPosition > toPosition) {  // up
                //  adapter.notifyItemRangeChanged(toPosition - 1, fromPosition + 1);
                //}
            }).doAfterNext(cursor -> {
                if (fromPosition < toPosition) {  // down
                    if (cursor.getPosition() >= toPosition) {
                        cursor.close();
                    }
                } else if (fromPosition > toPosition) {  // up
                    if (cursor.getPosition() >= fromPosition) {
                        cursor.close();
                    }
                } else {
                    cursor.close();
                }
            }).subscribe(cursor -> {
                String _ID = cursor.getString(cursor.getColumnIndex(FollowTable.COLUMN_ID));
                ContentValues values = new ContentValues();
                if (fromPosition < toPosition) {  // down
                    values.put(FollowTable.COLUMN_DISPLAY_ORDER, cursor.getPosition() - 1);
                } else if (fromPosition > toPosition) {  // up
                    values.put(FollowTable.COLUMN_DISPLAY_ORDER, cursor.getPosition() + 1);
                }
                getContext().getContentResolver().update(FollowProvider.CONTENT_URI, values,
                        FollowTable.COLUMN_ID + "=?", new String[]{_ID});
            });
        }
        dragItemPosition = -1;
    }
}
