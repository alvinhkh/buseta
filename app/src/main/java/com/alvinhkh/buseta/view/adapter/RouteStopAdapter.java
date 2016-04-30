package com.alvinhkh.buseta.view.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.EtaAdapterHelper;
import com.alvinhkh.buseta.holder.RouteStop;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteStopAdapter extends StateSavingArrayAdapter<RouteStop> {

    private Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView stop_code;
        TextView stop_lat;
        TextView stop_lng;
        TextView stop_name;
        TextView eta;
        TextView eta_more;
        TextView fare;
        TextView updated_time;
        ImageView follow;
    }

    public RouteStopAdapter(Context context) {
        super(context, R.layout.row_routestop);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        RouteStop object = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_routestop, parent, false);
            viewHolder.stop_code = (TextView) convertView.findViewById(R.id.stop_code);
            viewHolder.stop_lat = (TextView) convertView.findViewById(R.id.stop_lat);
            viewHolder.stop_lng = (TextView) convertView.findViewById(R.id.stop_lng);
            viewHolder.stop_name = (TextView) convertView.findViewById(R.id.stop_name);
            viewHolder.eta = (TextView) convertView.findViewById(R.id.eta);
            viewHolder.eta_more = (TextView) convertView.findViewById(R.id.eta_more);
            viewHolder.fare = (TextView) convertView.findViewById(R.id.fare);
            viewHolder.updated_time = (TextView) convertView.findViewById(R.id.updated_time);
            viewHolder.follow = (ImageView) convertView.findViewById(R.id.follow);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        if (object != null) {
            viewHolder.stop_name.setText(object.name_tc);
            viewHolder.stop_code.setText(object.code);
            viewHolder.follow.setVisibility(object.follow ? View.VISIBLE : View.GONE);
            viewHolder.eta.setText("");
            viewHolder.eta.setTextColor(ContextCompat.getColor(mContext, R.color.highlighted_text));
            viewHolder.eta_more.setText("");
            viewHolder.eta_more.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text));
            viewHolder.updated_time.setText("");
            viewHolder.fare.setText("");
            if (object.eta_loading != null && object.eta_loading) {
                viewHolder.eta_more.setText(R.string.message_loading);
            } else if (object.eta_fail != null && object.eta_fail) {
                viewHolder.eta_more.setText(R.string.message_fail_to_request);
            } else if (null != object.eta) {
                if (object.eta.etas.equals("") && object.eta.expires.equals("")) {
                    viewHolder.eta_more.setText(R.string.message_no_data); // route does not support eta
                    viewHolder.updated_time.setText("");
                } else {
                    // Request Time
                    String server_time = "";
                    Date server_date = null;
                    if (null != object.eta.server_time && !object.eta.server_time.equals("")) {
                        server_date = EtaAdapterHelper.serverDate(object);
                        server_time = (null != server_date) ?
                                EtaAdapterHelper.display_format.format(server_date) : object.eta.server_time;
                    }
                    viewHolder.updated_time.setText(server_time);
                    viewHolder.updated_time.setVisibility(View.GONE);
                    // ETAs
                    if (object.eta.etas.equals("")) {
                        // eta not available
                        viewHolder.eta.setText(R.string.message_no_data);
                    } else {
                        String etaText = EtaAdapterHelper.getText(object.eta.etas);
                        String[] etas = etaText.split(", ?");
                        Pattern pattern = Pattern.compile("到達([^/離開]|$)");
                        Matcher matcher = pattern.matcher(etaText);
                        String[] scheduled = object.eta.scheduled.split(", ?");
                        String[] wheelchairs = object.eta.wheelchair.split(", ?");
                        int count = 0;
                        while (matcher.find())
                            count++; //count any matched pattern
                        if (count > 1 && count == etas.length) {
                            // more than one and all same, more likely error
                            viewHolder.eta.setText(R.string.message_please_click_once_again);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < etas.length; i++) {
                                if (scheduled.length > i && scheduled[i] != null
                                        && scheduled[i].equals("Y")) {
                                    // scheduled bus
                                    sb.append("*");
                                }
                                sb.append(etas[i]);
                                String estimate = EtaAdapterHelper.etaEstimate(object, etas, i, server_date
                                        , mContext, viewHolder.eta, viewHolder.eta_more);
                                sb.append(estimate);
                                if (wheelchairs.length > i && wheelchairs[i] != null
                                        && wheelchairs[i].equals("Y")) {
                                    // wheelchair emoji
                                    sb.append(" ");
                                    sb.append(new String(Character.toChars(0x267F)));
                                }
                                if (i == 0) {
                                    viewHolder.eta.setText(sb.toString());
                                    sb = new StringBuilder();
                                } else {
                                    if (i < etas.length - 1)
                                        sb.append(" ");
                                }
                            }
                            viewHolder.eta_more.setText(sb.toString());
                        }
                    }
                }
            }
            if (null != object.details) {
                if (null != object.details.air_cond_fare &&
                        !object.details.air_cond_fare.equals("$0.00"))
                    viewHolder.fare.setText(object.details.air_cond_fare);
                if (null != object.details.lat)
                    viewHolder.stop_lat.setText(object.details.lat);
                if (null != object.details.lat)
                    viewHolder.stop_lng.setText(object.details.lng);
            }
        }
        // Return the completed view to render on screen
        return convertView;
    }
}