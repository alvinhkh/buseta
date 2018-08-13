package com.alvinhkh.buseta.mtr.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.mtr.model.MtrLineStatus;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.search.ui.SearchActivity;


public class MtrLineStatusAdapter
        extends ArrayListRecyclerViewAdapter<MtrLineStatusAdapter.ViewHolder> {

    public MtrLineStatusAdapter(@NonNull RecyclerView recyclerView) {
        super(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return ViewHolder.createViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.bindItem(this, items.get(position), position);
    }

    static abstract class ViewHolder extends ArrayListRecyclerViewAdapter.ViewHolder {

        ViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
        }

        public static ViewHolder createViewHolder(final ViewGroup parent, final int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root;

            switch (viewType) {
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_railway_status, parent, false);
                    return new DataViewHolder(root, viewType);
                case Item.TYPE_FOOTER:
                    root = inflater.inflate(R.layout.item_footer, parent, false);
                    return new FooterViewHolder(root, viewType);
                default:
                    return null;
            }
        }

        abstract public void bindItem(MtrLineStatusAdapter adapter, Item item, int position);
    }

    static class FooterViewHolder extends ViewHolder {

        TextView labelTv;

        public FooterViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            labelTv = itemView.findViewById(R.id.section_label);
        }

        @Override
        public void bindItem(MtrLineStatusAdapter adapter, Item item, int position) {
            if (item.getObject() != null && !TextUtils.isEmpty(item.getObject().toString())) {
                labelTv.setText(item.getObject().toString());
            }
        }
    }

    static class DataViewHolder extends ViewHolder {

        Context context;

        ImageView iconIv;

        TextView nameTv;

        ImageView openUrlIv;

        ImageView circleIv;

        public DataViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            iconIv = itemView.findViewById(R.id.icon);
            nameTv = itemView.findViewById(R.id.name);
            openUrlIv = itemView.findViewById(R.id.open_url);
            circleIv = itemView.findViewById(R.id.circle);
            context = itemView.getContext();
        }

        @Override
        public void bindItem(MtrLineStatusAdapter adapter, Item item, int position) {
            MtrLineStatus status = (MtrLineStatus) item.getObject();
            assert status != null;
            nameTv.setText(status.getLineName());
            iconIv.setImageResource(R.drawable.ic_directions_railway_black_24dp);
            if (!TextUtils.isEmpty(status.getLineColour())) {
                iconIv.setColorFilter(Color.parseColor(status.getLineColour()));
            }
            if (!TextUtils.isEmpty(status.getStatus())) {
                switch (status.getStatus().toLowerCase()) {
                    case "green":
                    case "yellow":
                    case "pink":
                    case "red":
                    case "grey":
                    case "typhoon":
                        circleIv.setColorFilter(getColorByName(context, "mtr_status_" + status.getStatus().toLowerCase()));
                        break;
                }
            }
            if (!TextUtils.isEmpty(status.getUrlTc())) {
                openUrlIv.setVisibility(View.VISIBLE);
                openUrlIv.setOnClickListener(l -> {
                    Uri link = Uri.parse(status.getUrlTc());
                    if (link != null) {
                        try {
                            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                            builder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
                            CustomTabsIntent customTabsIntent = builder.build();
                            customTabsIntent.launchUrl(context, link);
                        } catch (Exception ignored) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, link);
                            if (intent.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(intent);
                            }
                        }
                    }
                });
            } else {
                openUrlIv.setVisibility(View.GONE);
            }
            view.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(context, SearchActivity.class);
                intent.putExtra(C.EXTRA.TYPE, C.TYPE.RAILWAY);
                intent.putExtra(C.EXTRA.LINE_CODE, status.getLineCode());
                intent.putExtra(C.EXTRA.LINE_COLOUR, status.getLineColour());
                intent.putExtra(C.EXTRA.LINE_NAME, status.getLineName());
                context.startActivity(intent);
            });
        }

        private int getColorByName(@NonNull Context context, @NonNull String aString) {
            String packageName = context.getPackageName();
            int resId = context.getResources().getIdentifier(aString, "color", packageName);
            if (resId == 0) {
                return R.color.grey;
            } else {
                return ContextCompat.getColor(context, resId);
            }
        }
    }

}