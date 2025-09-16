package com.parthenios.skillmatch.utils

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class CryptoManager(context: Context) {

    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encrypt(plaintext: ByteArray, associatedData: ByteArray? = null): ByteArray {
        return aead.encrypt(plaintext, associatedData)
    }

    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray? = null): ByteArray {
        return aead.decrypt(ciphertext, associatedData)
    }

    fun encryptToBase64(plaintext: String, aad: String? = null): String {
        val ct = encrypt(plaintext.toByteArray(Charsets.UTF_8), aad?.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    fun decryptFromBase64(ciphertextB64: String, aad: String? = null): String {
        val ct = Base64.decode(ciphertextB64, Base64.NO_WRAP)
        val pt = decrypt(ct, aad?.toByteArray(Charsets.UTF_8))
        return String(pt, Charsets.UTF_8)
    }

    companion object {
        private const val KEYSET_NAME = "chat_keyset"
        private const val PREF_FILE_NAME = "tink_keys"
        private const val MASTER_KEY_URI = "android-keystore://skillmatch_master_key"
    }
}


