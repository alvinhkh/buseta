package com.alvinhkh.buseta.ui.follow;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.FollowStop;
import com.alvinhkh.buseta.ui.ItemTouchHelperAdapter;
import com.alvinhkh.buseta.ui.ItemTouchHelperViewHolder;
import com.alvinhkh.buseta.ui.OnItemDragListener;
import com.alvinhkh.buseta.utils.FollowStopUtil;

import java.util.Locale;


public class EditFollowAdapter extends RecyclerView.Adapter<EditFollowAdapter.ViewHolder>
    implements ItemTouchHelperAdapter {

  private static OnItemDragListener onItemDragListener;

  private Context context;

  private RecyclerView recyclerView;

  private Cursor followCursor;

  public EditFollowAdapter(@NonNull RecyclerView recyclerView, OnItemDragListener dragStartListener) {
    this.context = recyclerView.getContext();
    this.recyclerView = recyclerView;
    this.followCursor = null;
    onItemDragListener = dragStartListener;
  }

  public void close() {
    if (followCursor != null) {
      followCursor.close();
    }
  }

  @Override
  public int getItemCount() {
    return (followCursor == null) ? 0 : followCursor.getCount();
  }

  public void reloadCursor() {
    if (context == null) return;
    Cursor newCursor = FollowStopUtil.queryAll(context);
    if (followCursor == newCursor)
      return;
    Cursor oldCursor = followCursor;
    this.followCursor = newCursor;
    if (oldCursor != null) {
      oldCursor.close();
    }
  }

  public FollowStop getItem(int position) {
    if (followCursor == null || followCursor.isClosed()) return null;
    followCursor.moveToPosition(position);
    return FollowStopUtil.fromCursor(followCursor);
  }

  @Override
  public void onItemDismiss(int position) {
    if (context == null) return;
    FollowStop object = getItem(position);
    if (object == null) return;
    if (FollowStopUtil.delete(context, object) > 0) {
      reloadCursor();
      notifyItemRemoved(position);
      Snackbar.make(recyclerView, context.getString(R.string.removed_from_follow_list,
          String.format(Locale.ENGLISH, "%s %s", object.route, object.name)), Snackbar.LENGTH_SHORT)
          .addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
              switch (event) {
                case Snackbar.Callback.DISMISS_EVENT_ACTION:
                  FollowStopUtil.insert(context, object);
                  reloadCursor();
                  notifyItemInserted(position);
                  break;
                default:
                  break;
              }
            }
          })
          .setAction(R.string.undo, v -> {
            // do nothing
          }).show();
    }
  }

  @Override
  public boolean onItemMove(int fromPosition, int toPosition) {
    notifyItemMoved(fromPosition, toPosition);
    return true;
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position) {
    Context context = viewHolder.itemView.getContext();
    FollowStop object = getItem(position);

    viewHolder.dragHandle.setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        onItemDragListener.onItemStartDrag(viewHolder);
      }
      return true;
    });

    viewHolder.nameText.setText(object.name);
    viewHolder.routeNo.setText(object.route);
    viewHolder.routeLocationEnd.setText(context.getString(R.string.destination, object.locationEnd));

    viewHolder.itemView.setOnLongClickListener(v -> {
      onItemDragListener.onItemStartDrag(viewHolder);
      return true;
    });
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
    return new ViewHolder(inflater.inflate(R.layout.item_route_follow_edit, viewGroup, false));
  }

  static class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

    View itemView;

    ImageView dragHandle;

    TextView nameText;

    TextView routeNo;

    TextView routeLocationEnd;

    ViewHolder(View itemView) {
      super(itemView);
      this.itemView = itemView;
      this.dragHandle = itemView.findViewById(R.id.drag_handle);
      this.nameText = itemView.findViewById(R.id.name);
      this.routeNo = itemView.findViewById(R.id.route_no);
      this.routeLocationEnd = itemView.findViewById(R.id.route_location_end);
    }

    @Override
    public void onItemSelected() {
    }

    @Override
    public void onItemClear() {
      onItemDragListener.onItemStopDrag(this);
    }
  }

}