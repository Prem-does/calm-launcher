package com.calmlauncher.security

import com.calmlauncher.domain.repository.SettingsRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the optional settings PIN. Stores only a salted SHA-256 hash (never the PIN
 * itself) in settings. This guards the launcher's own settings/whitelist — it is a
 * friction lock, not a security boundary.
 */
@Singleton
class PinManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun setPin(pin: String) {
        val hash = hash(pin)
        settingsRepository.update { it.copy(pinEnabled = true, pinHash = hash) }
    }

    suspend fun clearPin() {
        settingsRepository.update { it.copy(pinEnabled = false, pinHash = null) }
    }

    suspend fun verify(pin: String): Boolean {
        val stored = settingsRepository.current().pinHash ?: return true
        return constantTimeEquals(stored, hash(pin))
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$SALT$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private companion object {
        const val SALT = "calm-launcher-v1::"
    }
}
