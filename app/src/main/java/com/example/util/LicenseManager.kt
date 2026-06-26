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

    fun getTodayPersianInt(): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran"))
        val gYear = cal.get(java.util.Calendar.YEAR)
        val gMonth = cal.get(java.util.Calendar.MONTH) + 1
        val gDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return gregorianToJalaliInt(gYear, gMonth, gDay)
    }

    private fun gregorianToJalaliInt(gy: Int, gm: Int, gd: Int): Int {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        
        if (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)) {
            gDaysInMonth[2] = 29
        }
        
        var gdTotal = 0
        for (i in 1 until gm) {
            gdTotal += gDaysInMonth[i]
        }
        gdTotal += gd
        
        val jy: Int
        var jm: Int
        val jd: Int
        
        var gdTotalJ = gdTotal - 79
        if (gdTotalJ > 0) {
            jy = gy - 621
        } else {
            jy = gy - 622
            val prevYearLeap = (gy - 1) % 4 == 0 && ((gy - 1) % 100 != 0 || (gy - 1) % 400 == 0)
            gdTotalJ += if (prevYearLeap) 366 else 365
        }
        
        val isLeap = jy % 33 == 1 || jy % 33 == 5 || jy % 33 == 9 || jy % 33 == 13 || jy % 33 == 17 || jy % 33 == 22 || jy % 33 == 26 || jy % 33 == 30
        if (isLeap) {
            jDaysInMonth[12] = 30
        }
        
        jm = 1
        while (jm <= 12 && gdTotalJ > jDaysInMonth[jm]) {
            gdTotalJ -= jDaysInMonth[jm]
            jm++
        }
        jd = gdTotalJ
        return jy * 10000 + jm * 100 + jd
    }

    fun jalaliToAbsoluteDays(jy: Int, jm: Int, jd: Int): Int {
        val jY = jy - 979
        val jM = jm - 1
        val jD = jd - 1

        var jDayNo = 365 * jY + (jY / 33) * 8 + ((jY % 33 + 3) / 4)
        for (i in 0 until jM) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jD
        return jDayNo
    }

    fun jalaliIntToAbsoluteDays(jalaliInt: Int): Int {
        val jy = jalaliInt / 10000
        val jm = (jalaliInt % 10000) / 100
        val jd = jalaliInt % 100
        return jalaliToAbsoluteDays(jy, jm, jd)
    }

    fun isValidLicense(licenseKey: String, androidId: String, currentPersianDate: Int): Boolean {
        val trimmed = licenseKey.trim().uppercase()
        if (!trimmed.contains("-")) return false
        val parts = trimmed.split("-")
        if (parts.size != 2) return false
        val prefix = parts[0]
        val checksum = parts[1]
        if (prefix.length != 9) return false
        val daysCode = prefix.substring(0, 3)
        val dateCode = prefix.substring(3, 8)
        val version = prefix.substring(8, 9)
        
        if (version != "1") return false
        
        val checksumSource = androidId + daysCode + dateCode + version + SECRET
        val computedChecksum = sha1(checksumSource).uppercase().take(4)
        if (computedChecksum != checksum) return false

        val decodedDate = try {
            dateCode.toLong(36).toInt()
        } catch (e: Exception) {
            return false
        }
        
        val days = if (daysCode == "ZZZ") -1 else try { daysCode.toInt(36) } catch (e: Exception) { 0 }
        if (days == -1) {
            return true
        }

        val currentDays = jalaliIntToAbsoluteDays(currentPersianDate)
        val issueDays = jalaliIntToAbsoluteDays(decodedDate)
        return currentDays >= issueDays && currentDays < issueDays + days
    }

    fun getLicenseDays(licenseKey: String): Int {
        val trimmed = licenseKey.trim().uppercase()
        if (!trimmed.contains("-")) return 0
        val parts = trimmed.split("-")
        if (parts.size != 2) return 0
        val prefix = parts[0]
        if (prefix.length != 9) return 0
        val daysCode = prefix.substring(0, 3)
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
        val todayInt = getTodayPersianInt()
        
        if (isKeyUsed(context, trimmedKey)) {
            return RegisterResult.AlreadyUsed
        }
        
        if (!isValidLicense(trimmedKey, androidId, todayInt)) {
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

    fun getTotalDaysFromSavedLicense(context: Context): Int {
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val licenseKey = prefs.getString("license_key", "") ?: ""
        return getLicenseDays(licenseKey)
    }

    fun checkLicenseStatus(context: Context): LicenseStatus {
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val licenseKey = prefs.getString("license_key", "") ?: ""
        if (licenseKey.isEmpty()) {
            return LicenseStatus(isValid = false, isLifetime = false, remainingDays = 0)
        }
        val androidId = getAndroidId(context)
        val todayInt = getTodayPersianInt()
        if (!isValidLicense(licenseKey, androidId, todayInt)) {
            return LicenseStatus(isValid = false, isLifetime = false, remainingDays = 0)
        }

        val days = getLicenseDays(licenseKey)
        if (days == -1) {
            return LicenseStatus(isValid = true, isLifetime = true, remainingDays = -1)
        }

        val prefix = licenseKey.trim().uppercase().split("-")[0]
        val dateCode = prefix.substring(3, 8)
        val decodedDate = try {
            dateCode.toLong(36).toInt()
        } catch (e: Exception) {
            return LicenseStatus(isValid = false, isLifetime = false, remainingDays = 0)
        }

        val currentDays = jalaliIntToAbsoluteDays(todayInt)
        val issueDays = jalaliIntToAbsoluteDays(decodedDate)
        val remainingDays = (issueDays + days) - currentDays

        return LicenseStatus(isValid = true, isLifetime = false, remainingDays = maxOf(0, remainingDays))
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
