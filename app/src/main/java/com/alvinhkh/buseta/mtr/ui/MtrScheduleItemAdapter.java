package com.alvinhkh.buseta.mtr.ui;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.ui.ArrayListRecyclerViewAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MtrScheduleItemAdapter extends ArrayListRecyclerViewAdapter<MtrScheduleItemAdapter.ViewHolder> {

    private OnClickItemListener listener;

    public MtrScheduleItemAdapter(@NonNull RecyclerView recyclerView, OnClickItemListener listener) {
        super(recyclerView);
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return ViewHolder.createViewHolder(viewGroup, viewType, onClickItemListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        viewHolder.bindItem(this, items.get(position), position);
    }

    static abstract class ViewHolder extends ArrayListRecyclerViewAdapter.ViewHolder {

        ViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
        }

        static ViewHolder createViewHolder(ViewGroup viewGroup, int viewType, OnClickItemListener listener) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            View root;
            switch (viewType) {
                case Item.TYPE_DATA:
                    root = inflater.inflate(R.layout.item_railway_schedule, viewGroup, false);
                    return new DataViewHolder(root, viewType, listener);
                case Item.TYPE_SECTION:
                    root = inflater.inflate(R.layout.item_separator, viewGroup, false);
                    return new SeparatorViewHolder(root, viewType, listener);
                case Item.TYPE_FOOTER:
                default:
                    root = inflater.inflate(R.layout.item_footer, viewGroup, false);
                    return new FooterViewHolder(root, viewType, listener);
            }
        }

        abstract public void bindItem(MtrScheduleItemAdapter adapter, Item item, int position);
    }

    static class SeparatorViewHolder extends ViewHolder {

        SeparatorViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
        }

        @Override
        public void bindItem(MtrScheduleItemAdapter adapter, Item item, int position) { }
    }

    static class FooterViewHolder extends ViewHolder {

        TextView labelTv;

        FooterViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
            labelTv = itemView.findViewById(R.id.section_label);
        }

        @Override
        public void bindItem(MtrScheduleItemAdapter adapter, Item item, int position) {
            if (item.getObject() != null && !TextUtils.isEmpty(item.getObject().toString())) {
                labelTv.setText(item.getObject().toString());
            }
        }
    }

    static class DataViewHolder extends ViewHolder {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm", Locale.ENGLISH);

        View itemView;

        TextView titleTv;

        TextView textTv;

        DataViewHolder(View itemView, int viewType, OnClickItemListener listener) {
            super(itemView, viewType, listener);
            this.itemView = itemView;
            titleTv = itemView.findViewById(R.id.title);
            textTv = itemView.findViewById(R.id.text);
        }

        @Override
        public void bindItem(MtrScheduleItemAdapter adapter, Item item, int position) {
            ArrivalTime arrivalTime = (ArrivalTime) item.getObject();
            if (arrivalTime != null) {
                titleTv.setText(arrivalTime.platform);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    titleTv.setBackgroundTintList(ContextCompat.getColorStateList(titleTv.getContext(),
                            arrivalTime.expired ? R.color.textDiminish : R.color.textPrimary));
                }

                Integer colorInt = ContextCompat.getColor(textTv.getContext(),
                        arrivalTime.expired ? R.color.textDiminish : R.color.textPrimary);
                String timeText = arrivalTime.text;
                try {
                    timeText = timeFormat.format(dateFormat.parse(arrivalTime.text));
                } catch (ParseException ignored) {}
                SpannableStringBuilder etaText = new SpannableStringBuilder(String.format("%s %s", arrivalTime.destination, timeText));
                if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                    etaText.append(" (").append(arrivalTime.estimate).append(")");
                }
                if (etaText.length() > 0) {
                    etaText.setSpan(new ForegroundColorSpan(colorInt), 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                textTv.setText(etaText);
            }
        }
    }
}