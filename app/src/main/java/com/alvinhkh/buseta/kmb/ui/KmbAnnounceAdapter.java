package com.alvinhkh.buseta.kmb.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.kmb.KmbService;
import com.alvinhkh.buseta.kmb.model.KmbAnnounce;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.image.ImageActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;


public class KmbAnnounceAdapter
        extends ArrayListRecyclerViewAdapter<KmbAnnounceAdapter.ViewHolder> {

    public KmbAnnounceAdapter(@NonNull RecyclerView recyclerView) {
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
                    root = inflater.inflate(R.layout.item_route_announce, parent, false);
                    return new DataViewHolder(root, viewType);
                case Item.TYPE_FOOTER:
                    root = inflater.inflate(R.layout.item_footer, parent, false);
                    return new FooterViewHolder(root, viewType);
                default:
                    return null;
            }
        }

        abstract public void bindItem(KmbAnnounceAdapter adapter, Item item, int position);
    }

    static class FooterViewHolder extends ViewHolder {

        TextView labelTv;

        public FooterViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            labelTv = itemView.findViewById(R.id.section_label);
        }

        @Override
        public void bindItem(KmbAnnounceAdapter adapter, Item item, int position) {
            if (item.getObject() != null && !TextUtils.isEmpty(item.getObject().toString())) {
                labelTv.setText(item.getObject().toString());
            }
        }
    }

    static class DataViewHolder extends ViewHolder {

        Context context;

        ImageView iconImageView;

        TextView titleTextView;

        public DataViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            iconImageView = itemView.findViewById(R.id.icon);
            titleTextView = itemView.findViewById(R.id.title);
            context = itemView.getContext();
        }

        @Override
        public void bindItem(KmbAnnounceAdapter adapter, Item item, int position) {
            final KmbAnnounce announce = (KmbAnnounce) item.getObject();
            assert announce != null;
            titleTextView.setText(announce.titleTc);
            if (!TextUtils.isEmpty(announce.url) && announce.url.contains(".jpg")) {
                iconImageView.setImageResource(R.drawable.ic_outline_event_note_24dp);
            } else if (!TextUtils.isEmpty(announce.url) && announce.url.contains(".pdf")) {
                iconImageView.setImageResource(R.drawable.ic_outline_picture_as_pdf_24dp);
            } else {
                iconImageView.setImageResource(R.drawable.ic_outline_event_note_24dp);
            }
            view.setOnClickListener(v -> {
                if (announce.url.contains(".jpg")) {
                    Intent intent = new Intent(context, ImageActivity.class);
                    intent.putExtra(ImageActivity.IMAGE_TITLE, announce.titleTc);
                    intent.putExtra(ImageActivity.IMAGE_URL,
                            KmbService.ANNOUNCEMENT_PICTURE + announce.url);
                    context.startActivity(intent);
                } else if (announce.url.contains(".pdf")) {
                    openPdf(KmbService.ANNOUNCEMENT_PICTURE + announce.url);
                } else {
                    KmbService kmbService = KmbService.webSearchHtml.create(KmbService.class);
                    kmbService.getAnnouncementPicture(announce.url)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(getAnnouncement(announce));
                }
            });
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

        DisposableObserver<ResponseBody> getAnnouncement(final KmbAnnounce announce) {
            return new DisposableObserver<ResponseBody>() {
                @Override
                public void onNext(ResponseBody body) {
                    if (body == null) return;
                    String contentType = body.contentType() != null ? body.contentType().toString() : "";
                    if (contentType.contains("image")) {
                        Intent intent = new Intent(context, ImageActivity.class);
                        intent.putExtra(ImageActivity.IMAGE_TITLE, announce.titleTc);
                        intent.putExtra(ImageActivity.IMAGE_URL,
                                KmbService.ANNOUNCEMENT_PICTURE + announce.url);
                        context.startActivity(intent);
                    } else if (contentType.contains("html")) {
                        try {
                            Document doc = Jsoup.parse(body.string());
                            Element htmlBody = doc.select("body").first();
                            if (htmlBody != null) {
                                Elements p = htmlBody.select("p");
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < p.size(); i++) {
                                    sb.append(p.get(i).text());
                                    if (i < p.size() - 1)
                                        sb.append("\n\n");
                                }
                                if (!TextUtils.isEmpty(sb)) {
                                    // TODO: maybe another format instead of dialog
                                    new AlertDialog.Builder(context)
                                            .setTitle(announce.titleTc)
                                            .setMessage(sb)
                                            .setPositiveButton(R.string.action_confirm, (dialoginterface, i) -> dialoginterface.cancel()).show();
                                }
                            }
                        } catch (IOException e) {
                            Timber.e(e);
                        }
                    } else {
                        Timber.d(announce.toString());
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Timber.d(e);
                }

                @Override
                public void onComplete() {
                }
            };
        }

    }

}