package com.example.util

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object LicenseManager {
    const val SECRET = "MUP2026_SECRET"

    fun getAndroidId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId?.uppercase() ?: "UNKNOWN_ID"
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun isValidLicense(licenseKey: String, androidId: String): Boolean {
        val trimmed = licenseKey.trim().uppercase()
        if (!trimmed.contains("-")) return false
        val parts = trimmed.split("-")
        if (parts.size != 2) return false
        val daysCodeAndVersion = parts[0]
        val checksum = parts[1]
        if (daysCodeAndVersion.length != 4) return false
        val daysCode = daysCodeAndVersion.substring(0, 3)
        val version = daysCodeAndVersion.substring(3, 4)
        
        if (version != "1") return false

        val checksumSource = androidId + daysCode + version + SECRET
        val computedChecksum = sha1(checksumSource).uppercase().take(4)
        return computedChecksum == checksum
    }

    fun getLicenseDays(licenseKey: String): Int {
        val trimmed = licenseKey.trim().uppercase()
        if (!trimmed.contains("-")) return 0
        val parts = trimmed.split("-")
        if (parts.size != 2) return 0
        val daysCodeAndVersion = parts[0]
        if (daysCodeAndVersion.length != 4) return 0
        val daysCode = daysCodeAndVersion.substring(0, 3)
        if (daysCode == "ZZZ") return -1 // Lifetime
        return try {
            daysCode.toInt(36)
        } catch (e: Exception) {
            0
        }
    }

    fun isKeyUsed(context: Context, licenseKey: String): Boolean {
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val usedKeys = prefs.getStringSet("used_license_keys", emptySet()) ?: emptySet()
        return usedKeys.contains(licenseKey.trim().uppercase())
    }

    fun markKeyAsUsed(context: Context, licenseKey: String) {
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val usedKeys = prefs.getStringSet("used_license_keys", emptySet()) ?: emptySet()
        val updatedKeys = usedKeys.toMutableSet()
        updatedKeys.add(licenseKey.trim().uppercase())
        prefs.edit().putStringSet("used_license_keys", updatedKeys).apply()
    }

    fun registerLicense(context: Context, licenseKey: String): RegisterResult {
        val trimmedKey = licenseKey.trim().uppercase()
        val androidId = getAndroidId(context)
        
        if (isKeyUsed(context, trimmedKey)) {
            return RegisterResult.AlreadyUsed
        }
        
        if (!isValidLicense(trimmedKey, androidId)) {
            return RegisterResult.Invalid
        }
        
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("license_key", trimmedKey)
            .putLong("license_activation_time", System.currentTimeMillis())
            .apply()
            
        markKeyAsUsed(context, trimmedKey)
        return RegisterResult.Success
    }

    fun checkLicenseStatus(context: Context): LicenseStatus {
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val licenseKey = prefs.getString("license_key", "") ?: ""
        if (licenseKey.isEmpty()) {
            return LicenseStatus(isValid = false, isLifetime = false, remainingDays = 0)
        }
        val androidId = getAndroidId(context)
        if (!isValidLicense(licenseKey, androidId)) {
            return LicenseStatus(isValid = false, isLifetime = false, remainingDays = 0)
        }

        val days = getLicenseDays(licenseKey)
        if (days == -1) {
            return LicenseStatus(isValid = true, isLifetime = true, remainingDays = -1)
        }

        // Retrieve activation time. If not saved, save it now.
        // To be safe and avoid user losing days due to reinstall, we can store it.
        var activationTime = prefs.getLong("license_activation_time", 0L)
        if (activationTime == 0L) {
            activationTime = System.currentTimeMillis()
            prefs.edit().putLong("license_activation_time", activationTime).apply()
        }

        val current = System.currentTimeMillis()
        val elapsedMillis = current - activationTime
        val elapsedDays = (elapsedMillis.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
        val remainingDays = days - elapsedDays
        
        if (remainingDays <= 0) {
            return LicenseStatus(isValid = false, isLifetime = false, remainingDays = 0)
        }

        return LicenseStatus(isValid = true, isLifetime = false, remainingDays = remainingDays)
    }
}

data class LicenseStatus(
    val isValid: Boolean,
    val isLifetime: Boolean,
    val remainingDays: Int
)

enum class RegisterResult {
    Success,
    AlreadyUsed,
    Invalid
}
