package com.alvinhkh.buseta.nwst.util;

import android.support.annotation.NonNull;

import com.alvinhkh.buseta.model.BusRoute;
import com.alvinhkh.buseta.nwst.model.NwstVariant;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import timber.log.Timber;

public class NwstRequestUtil {

    public static String syscode() {
        StringBuilder randomInt = new StringBuilder(Integer.toString(new Random().nextInt(1000)));
        while (randomInt.length() < 4) {
            randomInt.append("0");
        }
        String timestamp = Integer.toString(Math.round((float) (System.currentTimeMillis() / 1000)));
        timestamp = timestamp.substring(timestamp.length() - 6);
        String secret = "firstbusmwymwy";
        return timestamp + randomInt + md5(timestamp + randomInt + secret);
    }

    public static String paramInfo(@NonNull BusRoute busRoute) {
        String routeInfo = busRoute.getChildKey();
        NwstVariant variant = NwstVariant.Companion.parseInfo(routeInfo);
        if (variant == null) {
            return "";
        }
        return "1|*|" + busRoute.getCompanyCode() + "||" + variant.getRdv() + "||" + variant.getStartSequence() + "||" + variant.getEndSequence() + "||" + busRoute.getSequence();
    }

    private static String md5(@NonNull String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & aMessageDigest));
                while (h.length() < 2) {
                    h.insert(0, "0");
                }
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Timber.d(e);
        }
        return null;
    }

}
