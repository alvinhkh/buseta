package com.alvinhkh.buseta.utils;

import android.support.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import timber.log.Timber;

public class HashUtil {

    public static String md5(@NonNull String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes());
            return hexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Timber.d(e);
        }
        return null;
    }

    public static String sha1(@NonNull String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(text.getBytes());
            return hexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Timber.d(e);
        }
        return null;
    }

    public static String hexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte aMessageDigest : bytes) {
            StringBuilder h = new StringBuilder(Integer.toHexString(0xFF & aMessageDigest));
            while (h.length() < 2) {
                h.insert(0, "0");
            }
            hexString.append(h);
        }
        return hexString.toString();
    }

    public static String randomHexString(int numchars){
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, numchars);
    }
}
