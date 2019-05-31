package com.alvinhkh.buseta.nlb.ui;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.nlb.NlbService;
import com.alvinhkh.buseta.nlb.model.NlbNews;
import com.alvinhkh.buseta.nlb.model.NlbNewsRequest;
import com.alvinhkh.buseta.nlb.model.NlbNewsRes;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;
import com.alvinhkh.buseta.ui.webview.WebViewActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class NlbNewsAdapter
        extends ArrayListRecyclerViewAdapter<NlbNewsAdapter.ViewHolder> {

    public NlbNewsAdapter(@NonNull RecyclerView recyclerView) {
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

        abstract public void bindItem(NlbNewsAdapter adapter, Item item, int position);
    }

    static class FooterViewHolder extends ViewHolder {

        TextView labelTv;

        public FooterViewHolder(final View itemView, final int viewType) {
            super(itemView, viewType);
            labelTv = itemView.findViewById(R.id.section_label);
        }

        @Override
        public void bindItem(NlbNewsAdapter adapter, Item item, int position) {
            if (item.getObject() != null && !TextUtils.isEmpty(item.getObject().toString())) {
                labelTv.setText(item.getObject().toString());
            }
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
        public void bindItem(NlbNewsAdapter adapter, Item item, int position) {
            final NlbNews news = (NlbNews) item.getObject();
            assert news != null;
            titleTextView.setText(news.getTitle());
            iconImageView.setImageResource(R.drawable.ic_outline_event_note_24dp);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM", Locale.ENGLISH);
                Date date = sdf.parse(news.getPublishDate());
                iconImageView.setVisibility(View.GONE);
                iconTextView.setVisibility(View.VISIBLE);
                iconTextView.setText(displaySdf.format(date));
            } catch (ParseException ignored) {
                iconImageView.setVisibility(View.VISIBLE);
                iconTextView.setVisibility(View.GONE);
            }
            view.setOnClickListener(v -> {
                NlbService nlbService = NlbService.api.create(NlbService.class);
                nlbService.getNew(new NlbNewsRequest(news.getNewsId(), "zh"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(newsObserver());
            });
        }

        DisposableObserver<NlbNewsRes> newsObserver() {
            return new DisposableObserver<NlbNewsRes>() {
                @Override
                public void onNext(NlbNewsRes res) {
                    if (context == null) return;
                    if (res == null || res.getNews() == null || TextUtils.isEmpty(res.getNews().getContent())) return;
                    Intent intent = new Intent(context, WebViewActivity.class);
                    intent.putExtra(WebViewActivity.TITLE, res.getNews().getTitle());
                    intent.putExtra(WebViewActivity.HTML, res.getNews().getContent());
                    context.startActivity(intent);
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