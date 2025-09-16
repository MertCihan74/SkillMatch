package com.parthenios.skillmatch.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcm {
    private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    fun generateKeyB64(): String {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun encryptToB64(keyB64: String, plaintext: String, aad: String? = null): Pair<String, String> {
        val key = decodeKey(keyB64)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        if (aad != null) cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP) to Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    fun decryptFromB64(keyB64: String, ciphertextB64: String, ivB64: String, aad: String? = null): String {
        val key = decodeKey(keyB64)
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        if (aad != null) cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    private fun decodeKey(keyB64: String): SecretKey {
        val keyBytes = Base64.decode(keyB64, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, "AES")
    }
}


