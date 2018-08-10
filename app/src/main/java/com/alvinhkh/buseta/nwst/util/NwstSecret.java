package com.alvinhkh.buseta.nwst.util;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class NwstSecret {

    private static String a(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            stringBuilder.append("0123456789ABCDEF".charAt((b & 240) >> 4)).append("0123456789ABCDEF".charAt(b & 15));
        }
        return stringBuilder.toString();
    }

    private static String a(int[] iArr) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < iArr.length; i++) {
            if (i % 2 == 0) {
                str.append(Character.toString((char) (iArr[i] - 35)));
            }
        }
        return str.toString();
    }

    private static SecretKeySpec a(String str) {
        byte[] bArr = null;
        if (str == null) {
            str = "";
        }
        StringBuilder stringBuffer = new StringBuilder(16);
        stringBuffer.append(str);
        while (stringBuffer.length() < 16) {
            stringBuffer.append("0");
        }
        if (stringBuffer.length() > 16) {
            stringBuffer.setLength(16);
        }
        try {
            bArr = stringBuffer.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Timber.d(e);
        }
        return new SecretKeySpec(bArr, "AES");
    }

    private static byte[] a(byte[] bArr, String str, String str2) {
        try {
            Key a = a(str);
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            instance.init(1, a, b(str2));
            return instance.doFinal(bArr);
        } catch (Exception e) {
            Timber.d(e);
            return null;
        }
    }

    private static String a(String str, String str2, String str3) {
        byte[] bArr = null;
        try {
            bArr = str.getBytes("UTF-8");
        } catch (Exception e) {
            Timber.d(e);
        }
        return a(a(bArr, str2, str3));
    }

    private static IvParameterSpec b(String str) {
        byte[] bArr = null;
        if (str == null) {
            str = "";
        }
        StringBuilder stringBuffer = new StringBuilder(16);
        stringBuffer.append(str);
        while (stringBuffer.length() < 16) {
            stringBuffer.append("0");
        }
        if (stringBuffer.length() > 16) {
            stringBuffer.setLength(16);
        }
        try {
            bArr = stringBuffer.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Timber.d(e);
        }
        return new IvParameterSpec(bArr);
    }

    private static byte[] j(String str) {
        int length = str.length();
        byte[] bArr = new byte[(length / 2)];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    public static String syscode2() {
        // unmodified code
        StringBuilder num = new StringBuilder(Integer.toString(new Random().nextInt(10000)));
        while (num.length() < 5) {
            num.append("0");
        }
        String valueOf = String.valueOf(System.currentTimeMillis() / 1000);
        String str = valueOf.substring(2, 3) + valueOf.substring(9, 10) + valueOf.substring(4, 5) + valueOf.substring(6, 7) + valueOf.substring(3, 4) + valueOf.substring(0, 1) + valueOf.substring(8, 9) + valueOf.substring(7, 8) + valueOf.substring(5, 6) + valueOf.substring(1, 2);
        valueOf = a(new int[]{145, 82, 154, 104, 150, 77, 151, 53, 144, 53, 154, 112, 156, 100, 140, 79, 145, 50, 137, 50, 146, 74, 147, 115, 149, 57, 146, 94});
        String str2 = valueOf.substring(2, 3) + valueOf.substring(7, 8) + valueOf.substring(5, 6) + valueOf.substring(4, 5) + valueOf.substring(6, 7) + valueOf.substring(3, 4) + valueOf.substring(0, 1) + valueOf.substring(1, 2);
        valueOf = "";
        try {
            MessageDigest instance = MessageDigest.getInstance("SHA-256");
            instance.update((str + str2 + num).getBytes());
            valueOf = a(instance.digest());
        } catch (NoSuchAlgorithmException e) {
            Timber.d(e);
        }
        num.insert(0, str + valueOf.toLowerCase());
        valueOf = a(new int[]{145, 53, 154, 104, 150, 106, 151, 107, 144, 90, 154, 56, 156, 100, 140, 80, 145, 104, 137, 89, 146, 113, 147, 71, 149, 120, 146, 87, 149, 56, 151, 96, 150, 81, 85, 71});
        valueOf = valueOf.substring(2, 3) + valueOf.substring(7, 8) + valueOf.substring(5, 6) + valueOf.substring(4, 5) + valueOf.substring(6, 7) + valueOf.substring(3, 4) + valueOf.substring(0, 1) + valueOf.substring(1, 2) + "infomwyy";
        str2 = a(new int[]{83, 108, 85, 52, 137, 71, 135, 89, 86, 51, 86, 101, 136, 118, 136, 94, 132, 63, 89, 90, 83, 94, 89, 118, 83, 109, 83, 74, 137, 52, 86, 115});
        return Base64.encodeToString(j(a(num.toString(), valueOf, str2.substring(8, 9) + str2.substring(1, 2) + str2.substring(13, 14) + str2.substring(15, 16) + str2.substring(5, 6) + str2.substring(0, 1) + str2.substring(6, 7) + str2.substring(2, 3) + str2.substring(3, 4) + str2.substring(4, 5) + str2.substring(14, 15) + str2.substring(9, 10) + str2.substring(10, 11) + str2.substring(11, 12) + str2.substring(12, 13) + str2.substring(7, 8))), 0).replaceAll("\r", "").replaceAll("\n", "").replaceAll("=", "");
    }
}
