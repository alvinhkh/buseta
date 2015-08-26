package com.alvinhkh.buseta.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteStop;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteStopAdapter extends StateSavingArrayAdapter<RouteStop> {

    private Context mContext;
    private int greyOutMinutes = 3;

    // View lookup cache
    private static class ViewHolder {
        TextView stop_name;
        TextView stop_code;
        TextView eta;
        TextView eta_more;
        TextView fare;
        TextView updated_time;
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
            viewHolder.stop_name = (TextView) convertView.findViewById(R.id.stop_name);
            viewHolder.stop_code = (TextView) convertView.findViewById(R.id.stop_code);
            viewHolder.eta = (TextView) convertView.findViewById(R.id.eta);
            viewHolder.eta_more = (TextView) convertView.findViewById(R.id.eta_more);
            viewHolder.fare = (TextView) convertView.findViewById(R.id.fare);
            viewHolder.updated_time = (TextView) convertView.findViewById(R.id.updated_time);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        if (object != null) {
            viewHolder.stop_name.setText(object.name_tc);
            viewHolder.stop_code.setText(object.code);
            viewHolder.eta.setText("");
            viewHolder.eta.setTextColor(ContextCompat.getColor(mContext, R.color.highlighted_text));
            viewHolder.eta_more.setText("");
            viewHolder.eta_more.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text));
            viewHolder.updated_time.setText("");
            viewHolder.fare.setText("");
            if (object.eta_loading != null && object.eta_loading == true) {
                viewHolder.eta_more.setText(R.string.message_loading);
            } else if (object.eta_fail != null && object.eta_fail == true) {
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
                        SimpleDateFormat display_format = new SimpleDateFormat("HH:mm:ss");
                        if (object.eta.api_version == 2) {
                            server_date = new Date(Long.parseLong(object.eta.server_time));
                        } else if (object.eta.api_version == 1) {
                            SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            try {
                                server_date = date_format.parse(object.eta.server_time);
                            } catch (ParseException ep) {
                                ep.printStackTrace();
                            }
                        }
                        server_time = (null != server_date) ?
                                display_format.format(server_date) : object.eta.server_time;
                    }
                    viewHolder.updated_time.setText(server_time);
                    viewHolder.updated_time.setVisibility(View.GONE);
                    // ETAs
                    if (object.eta.etas.equals("")) {
                        // eta not available
                        viewHolder.eta.setText(R.string.message_no_data);
                    } else {
                        // TODO: format result
                        Document doc = Jsoup.parse(object.eta.etas);
                        //Log.d("RouteStopAdapter", doc.toString());
                        String text = doc.text().replaceAll(" ?　?預定班次", "");
                        String[] etas = text.split(", ?");
                        Pattern pattern = Pattern.compile("到達([^/離開]|$)");
                        Matcher matcher = pattern.matcher(text);
                        int count = 0;
                        while (matcher.find())
                            count++; //count any matched pattern
                        if (count > 1 && count == etas.length) {
                            // more than one and all same, more likely error
                            viewHolder.eta.setText(R.string.message_please_click_once_again);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < etas.length; i++) {
                                sb.append(etas[i]);
                                if (object.eta.api_version == 1) {
                                    // API v1 from Web, with minutes no time
                                    String minutes = etas[i].replaceAll("[^\\.0123456789]", "");
                                    if (null != server_date && !minutes.equals("") &&
                                            etas[i].contains("分鐘")) {
                                        Long t = server_date.getTime();
                                        Date etaDate = new Date(t + (Integer.parseInt(minutes) * 60000));
                                        SimpleDateFormat eta_time_format = new SimpleDateFormat("HH:mm");
                                        String etaTime = eta_time_format.format(etaDate);
                                        sb.append(" (");
                                        sb.append(etaTime);
                                        sb.append(")");
                                        // grey out
                                        if (i == 0)
                                            viewHolder.eta.setTextColor((Integer.parseInt(minutes) <= -greyOutMinutes) ?
                                                    ContextCompat.getColor(mContext, R.color.diminish_text) :
                                                    ContextCompat.getColor(mContext, R.color.highlighted_text));
                                        else
                                            viewHolder.eta_more.setTextColor(
                                                    (Integer.parseInt(minutes) <= -greyOutMinutes && i == etas.length - 1) ?
                                                            ContextCompat.getColor(mContext, R.color.diminish_text) :
                                                            ContextCompat.getColor(mContext, R.color.primary_text));
                                    }
                                } else if (object.eta.api_version == 2) {
                                    // API v2 from Mobile v2, with exact time
                                    if (etas[i].matches(".*\\d.*")) {
                                        // if text has digit
                                        String etaMinutes = "";
                                        long differences = new Date().getTime() - server_date.getTime(); // get device time and compare to server time
                                        try {
                                            SimpleDateFormat time_format =
                                                    new SimpleDateFormat("yyyy/MM/dd HH:mm");
                                            Date etaDateCompare = server_date;
                                            // first assume eta time and server time is on the same date
                                            Date etaDate = time_format.parse(
                                                    new SimpleDateFormat("yyyy").format(etaDateCompare) + "/" +
                                                            new SimpleDateFormat("MM").format(etaDateCompare) + "/" +
                                                            new SimpleDateFormat("dd").format(etaDateCompare) + " " +
                                                            etas[i]);
                                            // if not minutes will get negative integer
                                            int minutes = (int) ((etaDate.getTime() / 60000) -
                                                    ((server_date.getTime() + differences) / 60000));
                                            if (minutes < -12 * 60) {
                                                // plus one day to get correct eta date
                                                etaDateCompare = new Date(server_date.getTime() + 1 * 24 * 60 * 60 * 1000);
                                                etaDate = time_format.parse(
                                                        new SimpleDateFormat("yyyy").format(etaDateCompare) + "/" +
                                                                new SimpleDateFormat("MM").format(etaDateCompare) + "/" +
                                                                new SimpleDateFormat("dd").format(etaDateCompare) + " " +
                                                                etas[i]);
                                                minutes = (int) ((etaDate.getTime() / 60000) -
                                                        ((server_date.getTime() + differences) / 60000));
                                            }
                                            if (minutes >= 0 && minutes < 24 * 60) {
                                                // minutes should be 0 to within a day
                                                etaMinutes = String.valueOf(minutes);
                                            }
                                            // grey out
                                            if (i == 0)
                                                viewHolder.eta.setTextColor((minutes <= -greyOutMinutes) ?
                                                        ContextCompat.getColor(mContext, R.color.diminish_text) :
                                                        ContextCompat.getColor(mContext, R.color.highlighted_text));
                                            else
                                                viewHolder.eta_more.setTextColor(
                                                        (minutes <= -greyOutMinutes && i == etas.length - 1) ?
                                                                ContextCompat.getColor(mContext, R.color.diminish_text) :
                                                                ContextCompat.getColor(mContext, R.color.primary_text));
                                        } catch (ParseException ep) {
                                            ep.printStackTrace();
                                        }
                                        if (!etaMinutes.equals("")) {
                                            sb.append(" (");
                                            if (etaMinutes.equals("0")) {
                                                sb.append("現在");
                                            } else {
                                                sb.append(etaMinutes);
                                                sb.append("分鐘");
                                            }
                                            sb.append(")");
                                        }
                                    }
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
            if (null != object.fare && !object.fare.equals("$0.00")) {
                viewHolder.fare.setText(object.fare);
            }
        }
        // Return the completed view to render on screen
        return convertView;
    }
}