package com.alvinhkh.buseta.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import com.alvinhkh.buseta.C;
import com.alvinhkh.buseta.R;
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime;
import com.alvinhkh.buseta.route.model.RouteStop;
import com.alvinhkh.buseta.service.EtaNotificationService;
import com.alvinhkh.buseta.search.ui.SearchActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationUtil {

    public static final String ETA_CHANNEL_ID = "CHANNEL_ID_ETA";

    public static SimpleDateFormat displayDateFormat = new SimpleDateFormat("HH:mm:ss dd/MM", Locale.ENGLISH);

    public static NotificationCompat.Builder showArrivalTime(@NonNull Context context, @NonNull RouteStop object, @NonNull List<ArrivalTime> arrivalTimes) {
        SpannableStringBuilder smallContentTitle = new SpannableStringBuilder();
        SpannableStringBuilder smallText = new SpannableStringBuilder();
        SpannableStringBuilder bigText = new SpannableStringBuilder();
        SpannableStringBuilder bigContentTitle = new SpannableStringBuilder();
        SpannableStringBuilder bigSummaryText = new SpannableStringBuilder();
        SpannableStringBuilder subText = new SpannableStringBuilder();
        SpannableStringBuilder contentInfo = new SpannableStringBuilder();

        bigContentTitle.append(object.getRouteNo());
        subText.append(object.getRouteNo());
        if (!TextUtils.isEmpty(object.getName())) {
            bigContentTitle.append(" ");
            bigContentTitle.append(object.getName());
            subText.append(" ");
            subText.append(object.getName());
        }
        if (!TextUtils.isEmpty(object.getRouteDestination())) {
            bigContentTitle.append(" ");
            bigContentTitle.append(context.getString(R.string.destination, object.getRouteDestination()));
            subText.append(" ");
            subText.append(context.getString(R.string.destination, object.getRouteDestination()));
        }

        Integer notificationSmallIcon = R.drawable.ic_outline_directions_bus_24dp;

        for (ArrivalTime arrivalTime : arrivalTimes) {
            arrivalTime = ArrivalTime.Companion.estimate(context, arrivalTime);
            if (!TextUtils.isEmpty(arrivalTime.getOrder())) {
                SpannableStringBuilder etaSmallText = new SpannableStringBuilder(arrivalTime.getText());
                SpannableStringBuilder etaText = new SpannableStringBuilder(arrivalTime.getText());
                Integer pos = Integer.parseInt(arrivalTime.getOrder());
                Integer colorInt = ContextCompat.getColor(context,
                        arrivalTime.getExpired() ? R.color.grey :
                                (pos > 0 ? R.color.black : R.color.colorPrimaryA700));
                if (arrivalTime.getCompanyCode().equals(C.PROVIDER.MTR)) {
                    notificationSmallIcon = R.drawable.ic_outline_directions_railway_24dp;
                    colorInt = ContextCompat.getColor(context, arrivalTime.getExpired() ?
                            R.color.grey : R.color.black);
                }
                if (!TextUtils.isEmpty(arrivalTime.getPlatform())) {
                    etaText.insert(0, "[" + arrivalTime.getPlatform() + "] ");
                }
                if (!TextUtils.isEmpty(arrivalTime.getNote())) {
                    etaSmallText.append("#");
                    etaText.append("#");
                }
                if (arrivalTime.isSchedule()) {
                    etaSmallText.append("*");
                    etaText.append(" ").append(context.getString(R.string.scheduled_bus));
                }
                if (!TextUtils.isEmpty(arrivalTime.getEstimate())) {
                    etaSmallText.append(" (").append(arrivalTime.getEstimate()).append(")");
                    etaText.append(" (").append(arrivalTime.getEstimate()).append(")");
                }
                if (arrivalTime.getDistanceKM() >= 0) {
                    etaText.append(" ").append(context.getString(R.string.km, arrivalTime.getDistanceKM()));
                }
                if (!TextUtils.isEmpty(arrivalTime.getPlate())) {
                    etaText.append(" ").append(arrivalTime.getPlate());
                }
                if (arrivalTime.getCapacity() >= 0) {
                    String capacity = "";
                    if (arrivalTime.getCapacity() == 0) {
                        capacity = context.getString(R.string.capacity_empty);
                    } else if (arrivalTime.getCapacity() > 0 && arrivalTime.getCapacity() <= 3) {
                        capacity = "¼";
                    } else if (arrivalTime.getCapacity() > 3 && arrivalTime.getCapacity() <= 6) {
                        capacity = "½";
                    } else if (arrivalTime.getCapacity() > 6 && arrivalTime.getCapacity() <= 9) {
                        capacity = "¾";
                    } else if (arrivalTime.getCapacity() >= 10) {
                        capacity = context.getString(R.string.capacity_full);
                    }
                    if (!TextUtils.isEmpty(capacity)) {
                        etaText.append(" [").append(capacity).append("]");
                    }
                }
                if (arrivalTime.getHasWheelchair() && PreferenceUtil.INSTANCE.isShowWheelchairIcon(context)) {
                    etaText.append(" \u267F");
                }
                if (arrivalTime.getHasWifi()) {
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

            if (arrivalTime.getGeneratedAt() > 0) {
                // Request server time
                Date date = new Date(arrivalTime.getGeneratedAt());
                bigSummaryText.append(displayDateFormat.format(date));
            } else if (arrivalTime.getUpdatedAt() > 0) {
                // last updated time
                Date date = new Date(arrivalTime.getUpdatedAt());
                bigSummaryText.append(displayDateFormat.format(date));
            }
        }
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
                .setSmallIcon(notificationSmallIcon)
                .setColor(color)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(true)
                .setWhen(Calendar.getInstance().getTimeInMillis())
                .setContentIntent(contentIntent)
                .setDeleteIntent(createDeleteIntent(context, notificationId))
                .extend(wearableExtender);
        return builder;
    }

    public static Integer getNotificationId(RouteStop object) {
        Integer notificationId = 1000;
        if (!TextUtils.isEmpty(object.getRouteNo())) {
            for (int i = 0; i < object.getRouteNo().length(); i++) {
                notificationId += object.getRouteNo().charAt(i);
            }
        }
        if (!TextUtils.isEmpty(object.getName())) {
            notificationId += object.getName().codePointAt(0);
            notificationId -= object.getName().codePointAt(object.getName().length()-1);
        }
        if (!TextUtils.isEmpty(object.getCompanyCode())) {
            notificationId += object.getCompanyCode().codePointAt(0);
        }
        if (!TextUtils.isEmpty(object.getRouteDestination())) {
            notificationId += object.getRouteDestination().codePointAt(0);
        }
        notificationId = Math.abs(notificationId);
        return notificationId;
    }

    private static PendingIntent createDeleteIntent(Context context, int notificationId) {
        Intent deleteIntent = new Intent(context, EtaNotificationService.class);
        deleteIntent.setAction(C.ACTION.CANCEL);
        deleteIntent.putExtra(C.EXTRA.NOTIFICATION_ID, notificationId);
        return PendingIntent.getService(context.getApplicationContext(), notificationId,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
