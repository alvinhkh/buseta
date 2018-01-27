package com.alvinhkh.buseta.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.model.ArrivalTime;
import com.alvinhkh.buseta.model.BusRouteStop;
import com.alvinhkh.buseta.service.NotificationService;
import com.alvinhkh.buseta.ui.search.SearchActivity;

import java.util.Calendar;
import java.util.Date;

public class NotificationUtil {

    public static final String ETA_CHANNEL_ID = "CHANNEL_ID_ETA";

    public static NotificationCompat.Builder showArrivalTime(@NonNull Context context, @NonNull BusRouteStop object) {
        SpannableStringBuilder smallContentTitle = new SpannableStringBuilder();
        SpannableStringBuilder smallText = new SpannableStringBuilder();
        SpannableStringBuilder bigText = new SpannableStringBuilder();
        SpannableStringBuilder bigContentTitle = new SpannableStringBuilder();
        SpannableStringBuilder bigSummaryText = new SpannableStringBuilder();
        SpannableStringBuilder subText = new SpannableStringBuilder();
        SpannableStringBuilder contentInfo = new SpannableStringBuilder();

        bigContentTitle.append(object.route);
        bigContentTitle.append(" ");
        bigContentTitle.append(object.name);
        bigContentTitle.append(" ");
        bigContentTitle.append(context.getString(R.string.destination, object.destination));
        subText.append(object.route);
        subText.append(" ");
        subText.append(object.name);
        subText.append(" ");
        subText.append(context.getString(R.string.destination, object.destination));

        ArrivalTimeUtil.query(context, object).subscribe(cursor -> {
            // Cursor has been moved +1 position forward.
            ArrivalTime arrivalTime = ArrivalTimeUtil.fromCursor(cursor);
            arrivalTime = ArrivalTimeUtil.estimate(context, arrivalTime);

            if (arrivalTime.id != null) {
                SpannableStringBuilder etaSmallText = new SpannableStringBuilder(arrivalTime.text);
                SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.text);
                Integer pos = Integer.parseInt(arrivalTime.id);
                Integer colorInt = ContextCompat.getColor(context,
                        arrivalTime.expired ? R.color.grey :
                                (pos > 0 ? R.color.black : R.color.colorPrimaryA700));
                if (arrivalTime.isSchedule) {
                    etaSmallText.append("*");
                    etaText.append(" ").append(context.getString(R.string.scheduled_bus));
                }
                if (!TextUtils.isEmpty(arrivalTime.estimate)) {
                    etaSmallText.append(" (").append(arrivalTime.estimate).append(")");
                    etaText.append(" (").append(arrivalTime.estimate).append(")");
                }
                if (arrivalTime.distanceKM >= 0) {
                    etaText.append(" ").append(context.getString(R.string.km, arrivalTime.distanceKM));
                }
                if (!TextUtils.isEmpty(arrivalTime.plate)) {
                    etaText.append(" ").append(arrivalTime.plate);
                }
                if (arrivalTime.capacity >= 0) {
                    String capacity = "";
                    if (arrivalTime.capacity == 0) {
                        capacity = context.getString(R.string.capacity_empty);
                    } else if (arrivalTime.capacity > 0 && arrivalTime.capacity <= 3) {
                        capacity = "¼";
                    } else if (arrivalTime.capacity > 3 && arrivalTime.capacity <= 6) {
                        capacity = "½";
                    } else if (arrivalTime.capacity > 6 && arrivalTime.capacity <= 9) {
                        capacity = "¾";
                    } else if (arrivalTime.capacity >= 10) {
                        capacity = context.getString(R.string.capacity_full);
                    }
                    if (!TextUtils.isEmpty(capacity)) {
                        etaText.append(" [").append(capacity).append("]");
                    }
                }
                if (arrivalTime.hasWheelchair) {
                    etaText.append(" \u267F");
                }
                if (arrivalTime.hasWifi) {
                    etaText.append(" [WIFI]");
                }
                ForegroundColorSpan textColour = new ForegroundColorSpan(colorInt);
                if (etaSmallText.length() > 0) {
                    etaSmallText.setSpan(textColour, 0, etaSmallText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (etaText.length() > 0) {
                    etaText.setSpan(textColour, 0, etaText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                switch(pos) {
                    case 0:
                        smallText.append(etaSmallText);
                        bigText.append(etaText);
                        break;
                    case 1:
                    case 2:
                    default:
                        smallText.append(" ").append(etaSmallText);
                        bigText.append("\n").append(etaText);
                        break;
                }
            }

            if (arrivalTime.generatedAt != null && arrivalTime.generatedAt > 0) {
                // Request server time
                Date date = new Date(arrivalTime.generatedAt);
                bigSummaryText.append(ArrivalTimeUtil.displayDateFormat.format(date));
            } else if (arrivalTime.updatedAt != null && arrivalTime.updatedAt > 0) {
                // last updated time
                Date date = new Date(arrivalTime.updatedAt);
                bigSummaryText.append(ArrivalTimeUtil.displayDateFormat.format(date));
            }
        });
        contentInfo.append(context.getString(R.string.app_name));

        // Foreground Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ETA_CHANNEL_ID);
        int color = ContextCompat.getColor(context, R.color.colorPrimary);
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        wearableExtender.setHintScreenTimeout(NotificationCompat.WearableExtender.SCREEN_TIMEOUT_LONG);
    /*
    // TODO: image
    if (null != object.image) {
      File filePath = new File(getCacheDir().getAbsolutePath() +
          File.separator + "images" + File.separator + object.image);
      Log.d(TAG, "image file: " + filePath.getPath());
      if (filePath.exists()) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath.getPath(), options);
        wearableExtender.setBackground(bitmap);
      }
    }
    // TODO: map uri
    if (!TextUtils.isEmpty(object.latitude) && !TextUtils.isEmpty(object.longitude)) {
      Uri uri = new Uri.Builder().scheme("geo")
          .appendPath(object.latitude + "," + object.longitude)
          .appendQueryParameter("q", object.latitude + "," + object.longitude +
              "(" + object.name + ")")
          .build();
      Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
      if (null != mapIntent.resolveActivity(getPackageManager())) {
        // only add open map action if proper geo app installed
        PendingIntent pendingIntent =
            PendingIntent.getActivity(this, 0, mapIntent, 0);
        NotificationCompat.Action actionOpenMap =
            new NotificationCompat.Action.Builder(R.drawable.ic_map_white_48dp,
                getString(R.string.show_map), pendingIntent)
                .build();
        wearableExtender.addAction(actionOpenMap);
      }
    }
    */
        Integer notificationId = getNotificationId(object);
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
        notificationIntent.setClass(context, SearchActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        notificationIntent.putExtra(C.EXTRA.STOP_OBJECT, object);
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentTitle(smallContentTitle.length() > 0 ? smallContentTitle : null)
                .setContentText(smallText.length() > 0 ? smallText : null)
                .setSubText(subText.length() > 0 ? subText : null)
                .setContentInfo(contentInfo.length() > 0 ? contentInfo : null)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(bigContentTitle.length() > 0 ? bigContentTitle : null)
                        .setSummaryText(bigSummaryText.length() > 0 ? bigSummaryText : null)
                        .bigText(bigText.length() > 0 ? bigText : null))
                .setSmallIcon(R.drawable.ic_directions_bus_white_24dp)
                .setColor(color)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setShowWhen(true)
                .setWhen(Calendar.getInstance().getTimeInMillis())
                .setContentIntent(contentIntent)
                .setDeleteIntent(createDeleteIntent(context, notificationId))
                .extend(wearableExtender);
        return builder;
    }

    public static Integer getNotificationId(BusRouteStop object) {
        Integer notificationId = 1000;
        if (!TextUtils.isEmpty(object.route)) {
            for (int i = 0; i < object.route.length(); i++) {
                notificationId += object.route.charAt(i);
            }
        }
        if (!TextUtils.isEmpty(object.name)) {
            notificationId += object.name.codePointAt(0);
            notificationId -= object.name.codePointAt(object.name.length()-1);
        }
        if (!TextUtils.isEmpty(object.companyCode)) {
            notificationId += object.companyCode.codePointAt(0);
        }
        if (!TextUtils.isEmpty(object.destination)) {
            notificationId += object.destination.codePointAt(0);
        }
        notificationId = Math.abs(notificationId);
        return notificationId;
    }

    private static PendingIntent createDeleteIntent(Context context, int notificationId) {
        Intent deleteIntent = new Intent(context, NotificationService.class);
        deleteIntent.setAction(C.ACTION.CANCEL);
        deleteIntent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId);
        return PendingIntent.getService(context.getApplicationContext(), notificationId,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
