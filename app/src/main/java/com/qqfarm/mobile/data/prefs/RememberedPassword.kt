package com.qqfarm.mobile.data.prefs

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object RememberedPassword {
    private const val alias = "farm.login.password"
    private const val provider = "AndroidKeyStore"
    private const val algorithm = "AES/GCM/NoPadding"

    private fun existingKey(): SecretKey? =
        KeyStore.getInstance(provider).apply { load(null) }.getKey(alias, null) as SecretKey?

    fun seal(password: String, profileId: String): String {
        val key = existingKey() ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider).run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(profileId.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(cipher.iv) + "." +
            encoder.encodeToString(cipher.doFinal(password.toByteArray(Charsets.UTF_8)))
    }

    fun open(sealed: String, profileId: String): String {
        if (sealed.isEmpty()) return ""
        return try {
            val parts = sealed.split('.', limit = 2)
            if (parts.size != 2) return ""
            val decoder = Base64.getDecoder()
            val iv = decoder.decode(parts[0])
            val encrypted = decoder.decode(parts[1])
            if (iv.isEmpty() || encrypted.size < 16) return "" // AES-GCM 的认证标签占 16 字节。
            val key = existingKey() ?: return ""
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.updateAAD(profileId.toByteArray(Charsets.UTF_8))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            "" // 密钥失效时允许重新输入密码。
        } catch (_: ProviderException) {
            ""
        } catch (_: IOException) {
            ""
        } catch (_: IllegalArgumentException) {
            ""
        }
    }
}
