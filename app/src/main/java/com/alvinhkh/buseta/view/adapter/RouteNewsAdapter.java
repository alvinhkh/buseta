package com.alvinhkh.buseta.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.holder.RouteNews;

public class RouteNewsAdapter extends StateSavingArrayAdapter<RouteNews> {
    // View lookup cache
    private static class ViewHolder {
        TextView title;
        TextView image_link;

        private ViewHolder(View convertView) {
            title = (TextView) convertView.findViewById(android.R.id.text1);
            image_link = (TextView) convertView.findViewById(R.id.notice_link);
        }
    }

    public RouteNewsAdapter(Context context) {
        super(context, R.layout.row_routenews);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        RouteNews object = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_routenews, parent, false);
            viewHolder = new ViewHolder(convertView);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        viewHolder.title.setText(null != object.title ? object.title : "");
        viewHolder.image_link.setText(null != object.link ? object.link : "");
        viewHolder.image_link.setVisibility(View.GONE);
        // Return the completed view to render on screen
        return convertView;
    }
}