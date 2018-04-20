package com.alvinhkh.buseta.ui.route;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.Route;
import com.alvinhkh.buseta.model.RouteStop;
import com.alvinhkh.buseta.service.EtaService;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.utils.ArrivalTimeUtil;
import com.alvinhkh.buseta.utils.RouteStopUtil;
import com.alvinhkh.buseta.utils.FollowStopUtil;
import com.alvinhkh.buseta.utils.PreferenceUtil;

import java.text.DecimalFormat;
import java.util.Locale;

import io.reactivex.disposables.CompositeDisposable;


public class RouteStopListAdapter
        extends ArrayListRecyclerViewAdapter<RouteStopListAdapter.ViewHolder> {

    private Route route;

    private FragmentManager fragmentManager;

    private Location currentLocation;
    
    public RouteStopListAdapter(@NonNull FragmentManager fragmentManager,
                                @NonNull RecyclerView recyclerView, @NonNull Route route) {
        super(recyclerView);
        this.fragmentManager = fragmentManager;
        this.route = route;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ViewHolder.createViewHolder(parent, viewType, onClickItemListener, fragmentManager, route, currentLocation);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.bindItem(this, items.get(position), position);
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    static abstract class ViewHolder extends ArrayListRecyclerViewAdapter.ViewHolder {

        ViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
        }

        static ViewHolder createViewHolder(ViewGroup parent, int viewType,
                                           OnClickItemListener listener,
                                           FragmentManager fm, Route route, Location currentLocation) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root;

            switch (viewType) {
                case Item.TYPE_HEADER:
                case Item.TYPE_FOOTER:
                    root = inflater.inflate(R.layout.item_note, parent, false);
                    return new NoteViewHolder(root, viewType, listener, route);
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_route_stop, parent, false);
                    return new DataViewHolder(root, viewType, listener, fm, route, currentLocation);
                default:
                    root = inflater.inflate(R.layout.item_note, parent, false);
                    return new EmptyViewHolder(root, viewType, listener);
            }
        }

        abstract public void bindItem(RouteStopListAdapter adapter, Item item, int position);
    }

    static class EmptyViewHolder extends ViewHolder {

        EmptyViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
            itemView.setVisibility(View.GONE);
        }

        @Override
        public void bindItem(RouteStopListAdapter adapter, Item item, int position) { }
    }

    static class NoteViewHolder extends ViewHolder {

        View itemView;
        TextView noteText;
        Route route;

        NoteViewHolder(View itemView, int viewType, OnClickItemListener listener, Route route) {
            super(itemView, viewType, listener);
            this.itemView = itemView;
            this.noteText = itemView.findViewById(R.id.note);
            this.route = route;
        }

        @Override
        public void bindItem(RouteStopListAdapter adapter, Item item, int position) {
            this.noteText.setText((String) item.getObject());
        }
    }

    static class DataViewHolder extends ViewHolder {
        
        View itemView;
        TextView nameText;
        TextView distanceText;
        TextView etaText;
        TextView eta2Text;
        TextView eta3Text;
        TextView fareText;
        ImageView followImage;
        ImageView nearbyImage;
        Route route;
        FragmentManager fragmentManager;
        Location currentLocation;

        CompositeDisposable disposable = new CompositeDisposable();

        DataViewHolder(View itemView, int viewType,
                       OnClickItemListener listener,
                       FragmentManager fragmentManager, Route route, Location currentLocation) {
            super(itemView, viewType, listener);
            this.itemView = itemView;
            this.nameText = itemView.findViewById(R.id.name);
            this.distanceText = itemView.findViewById(R.id.distance);
            this.etaText = itemView.findViewById(R.id.eta);
            this.eta2Text = itemView.findViewById(R.id.eta2);
            this.eta3Text = itemView.findViewById(R.id.eta3);
            this.fareText = itemView.findViewById(R.id.fare);
            this.followImage = itemView.findViewById(R.id.follow);
            this.nearbyImage = itemView.findViewById(R.id.nearby);
            this.route = route;
            this.fragmentManager = fragmentManager;
            this.currentLocation = currentLocation;
        }

        @Override
        public void bindItem(RouteStopListAdapter adapter, Item item, int position) {
            RouteStop stop = (RouteStop) item.getObject();
            if (stop == null) return;
            this.nameText.setText(stop.getName());
            this.distanceText.setText(null);
            if (!TextUtils.isEmpty(stop.getFare()) && Float.valueOf(stop.getFare()) > 0) {
                this.fareText.setVisibility(View.VISIBLE);
                this.fareText.setText(String.format(Locale.ENGLISH, "$%1$,.1f", Float.valueOf(stop.getFare())));
            } else {
                this.fareText.setVisibility(View.INVISIBLE);
            }
            this.etaText.setText(null);
            this.eta2Text.setText(null);
            this.eta3Text.setText(null);
            this.followImage.setVisibility(View.GONE);
            this.nearbyImage.setVisibility(View.GONE);

            if (!TextUtils.isEmpty(stop.getLatitude()) && !TextUtils.isEmpty(stop.getLongitude())) {
                Location location = new Location("");
                location.setLatitude(Double.parseDouble(stop.getLatitude()));
                location.setLongitude(Double.parseDouble(stop.getLongitude()));
                if (currentLocation != null) {
                    Float distance = currentLocation.distanceTo(location);
                    // TODO: a better way, to show nearest stop
                    if (distance < 200) {
                        this.distanceText.setText(new DecimalFormat("~#.##km").format(distance / 1000));
                        this.nearbyImage.setVisibility(View.VISIBLE);
                        Drawable drawable = this.nearbyImage.getDrawable();
                        drawable.setBounds(0, 0, this.distanceText.getLineHeight(), this.distanceText.getLineHeight());
                        this.nearbyImage.setImageDrawable(drawable);
                    }
                }
            }

            this.itemView.setOnClickListener(null);
            this.itemView.setOnLongClickListener(null);
            this.itemView.setOnClickListener(v -> {
                if (this.listener != null) {
                    this.listener.onClickItem(item, position);
                }
                this.etaText.setText(null);
                this.eta2Text.setText(null);
                this.eta3Text.setText(null);
                Intent intent = new Intent(v.getContext(), EtaService.class);
                intent.putExtra(C.EXTRA.STOP_OBJECT, stop);
                v.getContext().startService(intent);
            });

            this.itemView.setOnLongClickListener(v -> {
                try {
                    BottomSheetDialogFragment bottomSheetDialogFragment = RouteStopFragment.newInstance(stop);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                } catch (IllegalStateException ignored) {}
                return true;
            });

            Context context = this.itemView.getContext();
            if (context != null && disposable != null) {
                // Follow
                disposable.add(FollowStopUtil.query(context, RouteStopUtil.toFollowStop(stop)).subscribe(cursor -> {
                    this.followImage.setVisibility(cursor.getCount() > 0 ? View.VISIBLE : View.GONE);
                    if (cursor != null) {
                        cursor.close();
                    }
                }));
                // ETA
                disposable.add(ArrivalTimeUtil.query(context, stop).subscribe(cursor -> {
                    // Cursor has been moved +1 position forward.
                    ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
                    arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime);

                    if (!TextUtils.isEmpty(arrivalTime.getId())) {
                        SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.getText());
                        Integer pos = Integer.parseInt(arrivalTime.getId());
                        Integer colorInt = ContextCompat.getColor(context,
                                arrivalTime.getExpired() ? R.color.textDiminish :
                                        (pos > 0 ? R.color.textPrimary : R.color.textHighlighted));
                        if (!TextUtils.isEmpty(arrivalTime.getNote())) {
                            etaText.append("#");
                        }
                        if (arrivalTime.isSchedule()) {
                            etaText.append("*");
                        }
                        if (!TextUtils.isEmpty(arrivalTime.getEstimate())) {
                            etaText.append(" (").append(arrivalTime.getEstimate()).append(")");
                        }
                        if (arrivalTime.getDistanceKM() >= 0) {
                            etaText.append(" ").append(context.getString(R.string.km_short, arrivalTime.getDistanceKM()));
                        }
                        if (!TextUtils.isEmpty(arrivalTime.getPlate())) {
                            etaText.append(" ").append(arrivalTime.getPlate());
                        }
                        if (arrivalTime.getCapacity() >= 0) {
                            Drawable drawable = null;
                            if (arrivalTime.getCapacity() == 0) {
                                drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_0_black);
                            } else if (arrivalTime.getCapacity() > 0 && arrivalTime.getCapacity() <= 3) {
                                drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_20_black);
                            } else if (arrivalTime.getCapacity() > 3 && arrivalTime.getCapacity() <= 6) {
                                drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_50_black);
                            } else if (arrivalTime.getCapacity() > 6 && arrivalTime.getCapacity() <= 9) {
                                drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_80_black);
                            } else if (arrivalTime.getCapacity() >= 10) {
                                drawable = ContextCompat.getDrawable(context, R.drawable.ic_capacity_100_black);
                            }
                            if (drawable != null) {
                                drawable = DrawableCompat.wrap(drawable);
                                if (pos == 0) {
                                    drawable.setBounds(0, 0, this.etaText.getLineHeight(), this.etaText.getLineHeight());
                                } else if (pos == 1) {
                                    drawable.setBounds(0, 0, this.eta2Text.getLineHeight(), this.eta2Text.getLineHeight());
                                } else {
                                    drawable.setBounds(0, 0, this.eta3Text.getLineHeight(), this.eta3Text.getLineHeight());
                                }
                                DrawableCompat.setTint(drawable.mutate(), colorInt);
                                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                                etaText.append(" ");
                                if (etaText.length() > 0) {
                                    etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            }
                        }
                        if (arrivalTime.getHasWheelchair() && PreferenceUtil.isShowWheelchairIcon(context)) {
                            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_accessible_black_18dp);
                            if (drawable != null) {
                                drawable = DrawableCompat.wrap(drawable);
                                if (pos == 0) {
                                    drawable.setBounds(0, 0, this.etaText.getLineHeight(), this.etaText.getLineHeight());
                                } else if (pos == 1) {
                                    drawable.setBounds(0, 0, this.eta2Text.getLineHeight(), this.eta2Text.getLineHeight());
                                } else {
                                    drawable.setBounds(0, 0, this.eta3Text.getLineHeight(), this.eta3Text.getLineHeight());
                                }
                                DrawableCompat.setTint(drawable.mutate(), colorInt);
                                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                                etaText.append(" ");
                                if (etaText.length() > 0) {
                                    etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            }
                        }
                        if (arrivalTime.getHasWifi() && PreferenceUtil.isShowWifiIcon(context)) {
                            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_network_wifi_black_18dp);
                            if (drawable != null) {
                                drawable = DrawableCompat.wrap(drawable);
                                if (pos == 0) {
                                    drawable.setBounds(0, 0, this.etaText.getLineHeight(), this.etaText.getLineHeight());
                                } else if (pos == 1) {
                                    drawable.setBounds(0, 0, this.eta2Text.getLineHeight(), this.eta2Text.getLineHeight());
                                } else {
                                    drawable.setBounds(0, 0, this.eta3Text.getLineHeight(), this.eta3Text.getLineHeight());
                                }
                                DrawableCompat.setTint(drawable.mutate(), colorInt);
                                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                                etaText.append(" ");
                                if (etaText.length() > 0) {
                                    etaText.setSpan(imageSpan, etaText.length() - 1, etaText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                                }
                            }
                        }
                        if (etaText.length() > 0) {
                            etaText.setSpan(new ForegroundColorSpan(colorInt), 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        switch(pos) {
                            case 0:
                                this.etaText.setText(etaText);
                                this.eta2Text.setText(null);
                                this.eta3Text.setText(null);
                                break;
                            case 1:
                                etaText.insert(0, this.eta2Text.getText());
                                this.eta2Text.setText(etaText);
                                break;
                            case 2:
                                etaText.insert(0, this.eta3Text.getText());
                                this.eta3Text.setText(etaText);
                                break;
                        }
                    }
                }));
            }
        }
    }

}