package com.alvinhkh.buseta.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;


import com.alvinhkh.buseta.route.model.RouteStop;

import java.util.Calendar;
import java.util.List;

import timber.log.Timber;

public class Utils {

    public static boolean isPackageInstalled(String name, Context context) {
        if (null == context) return false;
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static Boolean isSunOnLeftSide(List<RouteStop> list) {
        if (list.size() < 1) return false;
        // This function will calculate the direction of the sun @winstonma
        Location startingLocation = new Location("");
        startingLocation.setLatitude(Double.parseDouble(list.get(0).getLatitude()));
        startingLocation.setLongitude(Double.parseDouble(list.get(0).getLongitude()));

        int count = list.size() - 1;
        Location endLocation = new Location("");
        endLocation.setLatitude(Double.parseDouble(list.get(count).getLatitude()));
        endLocation.setLongitude(Double.parseDouble(list.get(count).getLongitude()));

        int upDirection, downDirection;
        downDirection = 0;

        if (startingLocation.getLatitude() == endLocation.getLatitude()) {
            Location midLocation = new Location("");
            count = list.size() / 2;  // Assuming half is farest
            midLocation.setLatitude(Double.parseDouble(list.get(count).getLatitude()));
            midLocation.setLongitude(Double.parseDouble(list.get(count).getLongitude()));
            upDirection = (int) startingLocation.bearingTo(midLocation);
            downDirection = (int) midLocation.bearingTo(endLocation);
        } else {
            upDirection = (int) startingLocation.bearingTo(endLocation);
        }

        // Determine the sun location
        int hour = Calendar.getInstance().get(Calendar.HOUR);
        int sunPosition = (hour > 12) ? 90 : -90;

        // Calculate the sun location
        int sunLocation = upDirection - sunPosition;
        if (sunLocation > 0 && sunLocation < 180) {
            Timber.d("The sun should be at the left hand side");
            return true;
        } else {
            Timber.d("The sun should be at the right hand side");
            return false;
        }
    }
}
