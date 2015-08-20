package com.alvinhkh.buseta;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteStopAdapter extends StateSavingArrayAdapter<RouteStop> {
    // View lookup cache
    private static class ViewHolder {
        TextView stop_name;
        TextView stop_code;
        TextView eta;
        TextView server_time;
        TextView fare;
    }

    public RouteStopAdapter(Context context) {
        super(context, R.layout.row_routestop);
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
            viewHolder.stop_name = (TextView) convertView.findViewById(R.id.stop_name);
            viewHolder.stop_code = (TextView) convertView.findViewById(R.id.stop_code);
            viewHolder.eta = (TextView) convertView.findViewById(R.id.eta);
            viewHolder.server_time = (TextView) convertView.findViewById(R.id.server_time);
            viewHolder.fare = (TextView) convertView.findViewById(R.id.fare);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        if (object != null) {
            viewHolder.stop_name.setText(object.name_tc);
            viewHolder.stop_code.setText(object.code);
            if (object.eta_loading != null && object.eta_loading == true) {
                viewHolder.eta.setText(R.string.message_loading);
            } else if (object.eta_fail != null && object.eta_fail == true) {
                viewHolder.eta.setText(R.string.message_fail_to_request);
            } else if (null != object.eta) {
                if (object.eta.etas.equals("") && object.eta.expires.equals("")) {
                    // route does not support eta
                    viewHolder.eta.setText(R.string.message_no_data);
                    //viewHolder.eta.setText(R.string.message_route_not_support_eta);
                    viewHolder.server_time.setText("");
                } else {
                    if (object.eta.etas.equals("")) {
                        // eta not available
                        viewHolder.eta.setText(R.string.message_no_data);
                    } else {
                        // TODO: format result
                        Document doc = Jsoup.parse(object.eta.etas);
                        //Log.d("RouteStopAdapter", doc.toString());
                        String text = doc.text().replaceAll(" ?　?預定班次", "");
                        String[] texts = text.split(",");
                        Pattern pattern = Pattern.compile("到達([^/離開]|$)");
                        Matcher matcher = pattern.matcher(text);
                        int count = 0;
                        while (matcher.find())
                            count++; //count any matched pattern
                        if (count > 1 && count == texts.length) {
                            // more than one and all same, more likely error
                            viewHolder.eta.setText(R.string.message_please_click_once_again);
                        } else {
                            viewHolder.eta.setText(text);
                        }
                    }

                    SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    SimpleDateFormat display_format = new SimpleDateFormat("HH:mm:ss");
                    String server_time = "";
                    try {
                        Date date = date_format.parse(object.eta.server_time);
                        server_time = display_format.format(date);
                    } catch (ParseException ep) {
                        ep.printStackTrace();
                        server_time = object.eta.server_time;
                    }
                    viewHolder.server_time.setText(server_time);
                }
            } else {
                viewHolder.eta.setText("");
                viewHolder.server_time.setText("");
            }
            if (null != object.fare) {
                viewHolder.fare.setText(object.fare);
            } else {
                viewHolder.fare.setText("");
            }
        }
        // Return the completed view to render on screen
        return convertView;
    }
}