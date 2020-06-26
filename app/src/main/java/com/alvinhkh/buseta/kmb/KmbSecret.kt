package com.alvinhkh.buseta.kmb

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.android.gms.common.util.Hex
import timber.log.Timber
import java.math.BigInteger
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object KmbSecret {
    fun getSecrets(str: String, str2: String): SecretHolder {
        val bigInteger: BigInteger
        val secretHolder = SecretHolder()
        try {
            bigInteger = if (str2 == "") {
                BigInteger(50, Random())
            } else {
                BigInteger(str2)
            }
            val bigInteger2 = bigInteger.toString(16)
            val str3 = "00000000000000000000000000000000".substring(bigInteger2.length) + bigInteger2
            val instance = Cipher.getInstance("AES/CTR/NoPadding")
            instance.init(Cipher.ENCRYPT_MODE, key, getIV(str3))
            secretHolder.apiKey = Hex.bytesToStringUppercase(instance.doFinal(str.toByteArray()))
            secretHolder.ctr = bigInteger.toString()
            secretHolder.verifier = str3
        } catch (e2: Exception) {
            Timber.d(e2)
        }
        return secretHolder
    }

    fun decrypt(str: String?, str2: String): String {
        return try {
            val decodeHex = Hex.stringToBytes(str)
            val instance = Cipher.getInstance("AES/CTR/NoPadding")
            instance.init(Cipher.DECRYPT_MODE, key, getIV(str2))
            String(instance.doFinal(decodeHex))
        } catch (e2: Exception) {
            Timber.d(e2)
            ""
        }
    }

    @SuppressLint("HardwareIds")
    fun getVendorId(context: Context?): String {
        return try {
            if (context != null) Settings.Secure.getString(context.contentResolver, "android_id") else ""
        } catch (e2: NullPointerException) {
            Timber.d(e2)
            "qdb9jfu6bccb8ffs" // returning a random string is okay
        }
    }

    private fun getIV(str: String): IvParameterSpec? {
        return try {
            val bytes1 = Hex.stringToBytes(str)
            IvParameterSpec(bytes1)
        } catch (e2: Exception) {
            Timber.d(e2)
            null
        }
    }

    private val key: SecretKeySpec?
        get() = try {
            SecretKeySpec(Hex.stringToBytes("801C26C9AFB352FA4DF8C009BAB0FA72"), "AES")
        } catch (e2: Exception) {
            Timber.d(e2)
            null
        }

    data class SecretHolder(
            var apiKey: String = "",
            var ctr: String = "",
            var verifier: String = ""
    )
}