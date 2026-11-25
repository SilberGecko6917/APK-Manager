package me.geckotv.apkmanager.core.auth.internal

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/** Encrypts the GitHub token before it is written to disk. */
internal interface TokenCipher {
    fun encrypt(plaintext: String): String

    /** Returns `null` when the data cannot be decrypted, e.g. after the key was lost. */
    fun decrypt(ciphertext: String): String?
}

/**
 * AES-256-GCM with a key that never leaves the Android Keystore. The stored value is
 * `base64(iv) + ":" + base64(ciphertext)`.
 */
internal class KeystoreTokenCipher
    @Inject
    constructor() : TokenCipher {
        override fun encrypt(plaintext: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return encoder.encodeToString(cipher.iv) + SEPARATOR + encoder.encodeToString(encrypted)
        }

        override fun decrypt(ciphertext: String): String? =
            try {
                val (iv, encrypted) = ciphertext.split(SEPARATOR, limit = 2).map(decoder::decode)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
                String(cipher.doFinal(encrypted), Charsets.UTF_8)
            } catch (_: GeneralSecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IndexOutOfBoundsException) {
                null
            }

        private fun key(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec
                    .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build(),
            )
            return generator.generateKey()
        }

        private companion object {
            const val ANDROID_KEYSTORE = "AndroidKeyStore"
            const val KEY_ALIAS = "github_token_key"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val TAG_LENGTH_BITS = 128
            const val KEY_SIZE_BITS = 256
            const val SEPARATOR = ":"
            val encoder: Base64.Encoder = Base64.getEncoder()
            val decoder: Base64.Decoder = Base64.getDecoder()
        }
    }
