package com.alvinhkh.buseta.nwst.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.nwst.model.NwstNotice;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class NwstNoticeAdapter extends ArrayListRecyclerViewAdapter<NwstNoticeAdapter.ViewHolder> {

    public NwstNoticeAdapter(@NonNull RecyclerView recyclerView) {
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
                case Item.TYPE_SECTION:
                    root = inflater.inflate(R.layout.item_section, parent, false);
                    return new SectionViewHolder(root, viewType);
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_route_announce, parent, false);
                    return new DataViewHolder(root, viewType);
                case Item.TYPE_FOOTER:
                    root = inflater.inflate(R.layout.item_footer, parent, false);
                    return new SectionViewHolder(root, viewType);
                default:
                    return null;
            }
        }

        abstract public void bindItem(NwstNoticeAdapter adapter, Item item, int position);
    }

    static class SectionViewHolder extends ViewHolder {

        TextView mSectionLabel;

        public SectionViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            mSectionLabel = itemView.findViewById(R.id.section_label);
        }

        @Override
        public void bindItem(NwstNoticeAdapter adapter, Item item, int position) {
            mSectionLabel.setText(item.getText());
        }
    }

    static class DataViewHolder extends ViewHolder {

        Context context;

        ImageView iconImageView;

        TextView iconTextView;

        TextView titleTextView;

        public DataViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            iconImageView = itemView.findViewById(R.id.icon);
            iconTextView = itemView.findViewById(R.id.iconText);
            titleTextView = itemView.findViewById(R.id.title);
            context = itemView.getContext();
        }

        @Override
        public void bindItem(NwstNoticeAdapter adapter, Item item, int position) {
            NwstNotice notice = (NwstNotice) item.getObject();
            if (notice != null) {
                titleTextView.setText(notice.getTitle());
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                    SimpleDateFormat d = new SimpleDateFormat("dd/MM", Locale.ENGLISH);
                    SimpleDateFormat t = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
                    Date date = sdf.parse(notice.getReleaseDate());
                    String iconText = d.format(date);
                    if (!t.format(date).equals("00:00")) {
                        iconText += "\n" + t.format(date);
                    }
                    iconImageView.setVisibility(View.GONE);
                    iconTextView.setVisibility(View.VISIBLE);
                    iconTextView.setText(iconText);
                } catch (ParseException ignored) {
                    iconImageView.setVisibility(View.VISIBLE);
                    iconTextView.setVisibility(View.GONE);
                }
                view.setOnClickListener(v -> {
                    if (Patterns.WEB_URL.matcher(notice.getLink()).matches()) {
                        if (notice.getLink().contains(".pdf")) {
                            openPdf(notice.getLink());
                        } else {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(notice.getLink()));
                            context.startActivity(intent);
                        }
                    }
                });
            }
        }

        private void openPdf(@NonNull String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setPackage("com.google.android.apps.docs");
                intent.setDataAndType(Uri.parse(url), "application/pdf");
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(
                            Uri.parse("https://docs.google.com/viewer?embedded=true&url=" +
                                    URLEncoder.encode(url, "utf-8")),
                            "text/html");
                    context.startActivity(intent);
                } catch (UnsupportedEncodingException ignored) {}
            }
        }
    }

}