package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.example.data.model.Property
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PersianUtils {

    fun formatPrice(amount: Double?): String {
        if (amount == null || amount == 0.0) return "۰"
        val formatter = NumberFormat.getInstance(Locale("fa", "IR"))
        
        // If it's in millions
        return if (amount >= 1_000_000_000) {
            val billions = amount / 1_000_000_000.0
            val clean = if (billions % 1.0 == 0.0) billions.toInt().toString() else String.format("%.1f", billions)
            formatToPersianNumbers(clean) + " میلیارد"
        } else if (amount >= 1_000_000) {
            val millions = amount / 1_000_000.0
            val clean = if (millions % 1.0 == 0.0) millions.toInt().toString() else String.format("%.1f", millions)
            formatToPersianNumbers(clean) + " میلیون"
        } else {
            formatToPersianNumbers(formatter.format(amount.toLong()))
        }
    }

    fun formatNumber(num: Int?): String {
        if (num == null) return "۰"
        return formatToPersianNumbers(num.toString())
    }

    fun formatArea(area: Double): String {
        val clean = if (area % 1.0 == 0.0) area.toInt().toString() else String.format("%.1f", area)
        return formatToPersianNumbers(clean)
    }

    fun formatToPersianNumbers(input: String): String {
        var result = input
        val englishChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val persianChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        for (i in englishChars.indices) {
            result = result.replace(englishChars[i], persianChars[i])
        }
        return result
    }

    fun formatDate(timestamp: Long?): String {
        return getPersianDate(timestamp)
    }

    fun getPersianDate(timestamp: Long?): String {
        if (timestamp == null) return "ثبت نشده"
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply { timeInMillis = timestamp }
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        
        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        val monthName = when (jalali[1]) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }
        return formatToPersianNumbers("${jalali[2]} $monthName ${jalali[0]}")
    }

    fun getPersianTodayString(): String {
        return "امروز " + getPersianDate(System.currentTimeMillis())
    }

    fun getPersianDateCode(timestamp: Long? = null): String {
        val ts = timestamp ?: System.currentTimeMillis()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply { timeInMillis = ts }
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        return String.format("%04d%02d%02d", jalali[0], jalali[1], jalali[2])
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        
        val gy2 = gy
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
            jy = gy2 - 621
        } else {
            jy = gy2 - 622
            val prevYearLeap = (gy2 - 1) % 4 == 0 && ((gy2 - 1) % 100 != 0 || (gy2 - 1) % 400 == 0)
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
        return intArrayOf(jy, jm, jd)
    }

    fun getCustomEmoji(context: Context?, key: String, default: String): String {
        if (context == null) return default
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        return prefs.getString("custom_emoji_$key", default) ?: default
    }

    fun getGroupJoinSeparator(context: Context?): String {
        if (context == null) return "\n\n"
        val prefs = context.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val separatorEnabled = prefs.getBoolean("separator_enabled", false)
        if (!separatorEnabled) return "\n\n"
        val separatorText = prefs.getString("separator_text", "---") ?: "---"
        return "\n\n$separatorText\n\n"
    }

    fun getPropertyTypeEmoji(context: Context?, type: String): String {
        return when {
            type.contains("آپارتمان") -> getCustomEmoji(context, "apartment", "🏢")
            type.contains("ویلا") || type.contains("باغ") -> getCustomEmoji(context, "villa", "🏡")
            type.contains("خانه") || type.contains("کلنگی") || type.contains("مستغلات") -> getCustomEmoji(context, "house", "🏠")
            type.contains("تجاری") || type.contains("مغازه") || type.contains("دفتر") || type.contains("اداری") -> getCustomEmoji(context, "apartment", "🏢")
            else -> getCustomEmoji(context, "house", "🏠")
        }
    }

    fun getPropertyTypeEmoji(type: String): String {
        return getPropertyTypeEmoji(null, type)
    }

    fun generatePropertyPriceText(p: Property): String {
        val schemes = mutableListOf<String>()
        
        // 1. Full Mortgage (رهن کامل)
        if (p.fullDepositAmount != null && p.fullDepositAmount > 0) {
            schemes.add("رهن کامل: ${formatPrice(p.fullDepositAmount)}")
        }
        
        // 2. Rent and Mortgage (رهن و اجاره)
        if ((p.depositAmount != null && p.depositAmount > 0) || (p.rentAmount != null && p.rentAmount > 0)) {
            val parts = mutableListOf<String>()
            if (p.depositAmount != null && p.depositAmount > 0) {
                parts.add("رهن: ${formatPrice(p.depositAmount)}")
            }
            if (p.rentAmount != null && p.rentAmount > 0) {
                parts.add("اجاره: ${formatPrice(p.rentAmount)}")
            }
            schemes.add(parts.joinToString(" و "))
        }
        
        // 3. Full Rent (اجاره کامل)
        if (p.fullRentAmount != null && p.fullRentAmount > 0) {
            schemes.add("اجاره کامل: ${formatPrice(p.fullRentAmount)}")
        }
        
        // Fallback to legacy fields if none of the above are explicitly filled but financialMode was set
        if (schemes.isEmpty()) {
            return when (p.financialMode) {
                "RENT_AND_MORTGAGE" -> "رهن: ${formatPrice(p.depositAmount)} / اجاره: ${formatPrice(p.rentAmount)}"
                "FULL_MORTGAGE" -> "رهن کامل: ${formatPrice(p.depositAmount)}"
                "FULL_RENT" -> "اجاره کامل: ${formatPrice(p.rentAmount)}"
                else -> "توافقی"
            }
        }
        
        return schemes.joinToString(" یا ")
    }

    // Single Property Telegram Output
    fun generatePropertySingleLineText(context: Context?, p: Property): String {
        val parts = mutableListOf<String>()
        
        // 1. Emoji and Code/Type
        val emoji = getPropertyTypeEmoji(context, p.type)
        val firstLine = if (p.code.isNotEmpty()) "$emoji ${p.code} - ${p.type}" else "$emoji ${p.type}"
        parts.add(firstLine)
        
        // 2. Region
        if (p.region.isNotEmpty()) {
            val regionEmoji = getCustomEmoji(context, "region", "📍")
            parts.add("$regionEmoji ${p.region}")
        }
        
        // 3. Area
        val areaEmoji = getCustomEmoji(context, "area", "📐")
        parts.add("$areaEmoji ${formatArea(p.area)} متر")
        
        // 4. Bedrooms
        val bedroomsEmoji = getCustomEmoji(context, "bedrooms", "🛏")
        parts.add("$bedroomsEmoji ${formatNumber(p.bedrooms)} خواب")
        
        // 5. Floors (if applicable)
        if (p.type == "آپارتمان") {
            val floorParts = mutableListOf<String>()
            if (p.totalFloors != null && p.totalFloors > 0) {
                floorParts.add("کل طبقات: ${formatNumber(p.totalFloors)}")
            }
            if (p.unitFloor != null) {
                val floorText = if (p.unitFloor == 0) "همکف" else formatNumber(p.unitFloor)
                floorParts.add("طبقه: $floorText")
            }
            if (floorParts.isNotEmpty()) {
                parts.add(floorParts.joinToString("، "))
            }
        }
        
        // 6. Parking
        val parkingEmoji = getCustomEmoji(context, "parking", "🚗")
        parts.add("$parkingEmoji پارکینگ: ${if (p.hasParking) "دارد" else "ندارد"}")
        
        // 7. Cabinet and Amenities combined
        val cabAndAmenParts = mutableListOf<String>()
        if (p.cabinetType.isNotEmpty()) {
            val cabinetEmoji = getCustomEmoji(context, "cabinet", "🚪")
            cabAndAmenParts.add("$cabinetEmoji کابینت: ${p.cabinetType}")
        }
        if (p.otherAmenities.isNotEmpty()) {
            val amenitiesEmoji = getCustomEmoji(context, "amenities", "✨")
            cabAndAmenParts.add("$amenitiesEmoji امکانات: ${p.otherAmenities}")
        }
        if (cabAndAmenParts.isNotEmpty()) {
            parts.add(cabAndAmenParts.joinToString(" - "))
        }
        
        // 8. Financial Mode & Price
        val priceText = generatePropertyPriceText(p)
        val priceEmoji = getCustomEmoji(context, "price", "💰")
        parts.add("$priceEmoji $priceText")
        
        // 9. Description
        if (p.description.trim().isNotEmpty()) {
            val descriptionEmoji = getCustomEmoji(context, "description", "📝")
            parts.add("$descriptionEmoji ${p.description.trim()}")
        }
        
        return parts.joinToString("\n")
    }

    fun generatePropertyCompactSummaryText(p: Property): String {
        return generatePropertyCompactSummaryText(null, p)
    }

    fun generatePropertyCompactSummaryText(context: Context?, p: Property): String {
        val parts = mutableListOf<String>()
        val emoji = getPropertyTypeEmoji(context, p.type)
        
        // First part combined with emoji or starts with emoji
        if (p.code.isNotEmpty()) {
            parts.add("$emoji ${p.code}")
        } else {
            parts.add(emoji)
        }
        
        parts.add(p.type)
        if (p.region.isNotEmpty()) {
            parts.add(p.region)
        }
        parts.add("${formatArea(p.area)} متر")
        parts.add("${formatNumber(p.bedrooms)} خواب")
        
        // If apartment, add floor info if floor is 0 (همکف) or any other value
        if (p.type == "آپارتمان") {
            val floorText = if (p.unitFloor == 0) "همکف" else if (p.unitFloor != null) formatNumber(p.unitFloor) else ""
            if (floorText.isNotEmpty()) {
                parts.add("طبقه: $floorText")
            }
        }
        
        // Add cabinet type if present
        if (p.cabinetType.isNotEmpty()) {
            parts.add("کابینت: ${p.cabinetType}")
        }
        
        // Add other amenities if present
        if (p.otherAmenities.isNotEmpty()) {
            parts.add("امکانات: ${p.otherAmenities}")
        }
        
        val line1 = parts.joinToString(" | ")
        
        // Line 2 (newline separated, starts with 💰 and contains the price)
        val priceText = generatePropertyPriceText(p)
        val priceEmoji = getCustomEmoji(context, "price", "💰")
        val line2 = "$priceEmoji $priceText"
        
        return if (p.description.trim().isNotEmpty()) {
            val descriptionEmoji = getCustomEmoji(context, "description", "📝")
            "$line1\n$line2\n$descriptionEmoji ${p.description.trim()}"
        } else {
            "$line1\n$line2"
        }
    }

    fun buildCopyWithHeaderFooter(context: Context?, body: String): String {
        val prefs = context?.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
        val header = prefs?.getString("custom_header", "") ?: ""
        val footer = prefs?.getString("custom_footer", "") ?: ""
        
        val sb = StringBuilder()
        if (header.trim().isEmpty() && footer.trim().isEmpty()) {
            sb.append("اپلیکیشن ملکآپ، مدیریت هوشمند فایلها\n\n")
            sb.append(body)
        } else {
            if (header.trim().isNotEmpty()) {
                sb.append(header.trim()).append("\n\n")
            }
            sb.append(body)
            if (footer.trim().isNotEmpty()) {
                sb.append("\n\n").append(footer.trim())
            }
        }
        return sb.toString()
    }

    fun generateSingleTelegramOutput(p: Property): String {
        return generateSingleTelegramOutput(null, p, "standard")
    }

    fun generateSingleTelegramOutput(context: Context?, p: Property, format: String = "standard"): String {
        val baseText = if (format == "compact") {
            generatePropertyCompactSummaryText(context, p)
        } else {
            generatePropertySingleLineText(context, p)
        }
        return buildCopyWithHeaderFooter(context, baseText)
    }

    // Group Summary Output (One line per property with line spacing and custom header/footer)
    fun generateGroupSummaryOutput(properties: List<Property>): String {
        return generateGroupSummaryOutput(null, properties)
    }

    fun generateGroupSummaryOutput(context: Context?, properties: List<Property>): String {
        val separator = getGroupJoinSeparator(context)
        val body = properties.joinToString(separator) { p ->
            generatePropertyCompactSummaryText(context, p)
        }
        return buildCopyWithHeaderFooter(context, body)
    }

    // Group Full Output (Using single-line dash-separated format as default)
    fun generateGroupFullOutput(properties: List<Property>, format: String = "standard", context: Context? = null): String {
        val separator = getGroupJoinSeparator(context)
        val body = properties.joinToString(separator) { p ->
            if (format == "compact") {
                generatePropertyCompactSummaryText(context, p)
            } else {
                generatePropertySingleLineText(context, p)
            }
        }
        return buildCopyWithHeaderFooter(context, body)
    }

    // Copy to clipboard helper (Robust with Handler to ensure thread-safety on all Android versions)
    fun copyToClipboard(context: Context, text: String, label: String = "MelkUp Copy") {
        try {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(label, text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "کپی شد! آماده استفاده 🚀", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در کپی: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در کپی: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Convert number to Persian words equivalent (e.g. 150000000 -> صد و پنجاه میلیون تومان)
    fun numberToPersianWords(number: Long): String {
        if (number == 0L) return "صفر"
        
        val yekan = listOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
        val dahgan = listOf("", "ده", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
        val sadgan = listOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
        val dahTaBiyst = listOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
        
        val units = listOf("", "هزار", "میلیون", "میلیارد", "تریلیون")
        
        var temp = number
        val parts = mutableListOf<String>()
        var unitIndex = 0
        
        while (temp > 0) {
            val part = (temp % 1000).toInt()
            if (part > 0) {
                val partWords = convertThreeDigitsToWords(part, yekan, dahgan, sadgan, dahTaBiyst)
                val unit = units[unitIndex]
                val wordWithUnit = if (unit.isNotEmpty()) "$partWords $unit" else partWords
                parts.add(0, wordWithUnit)
            }
            temp /= 1000
            unitIndex++
        }
        
        return parts.joinToString(" و ")
    }

    private fun convertThreeDigitsToWords(
        num: Int,
        yekan: List<String>,
        dahgan: List<String>,
        sadgan: List<String>,
        dahTaBiyst: List<String>
    ): String {
        val s = num / 100
        val d = (num % 100) / 10
        val y = num % 10
        
        val parts = mutableListOf<String>()
        if (s > 0) {
            parts.add(sadgan[s])
        }
        
        if (d == 1) {
            parts.add(dahTaBiyst[y])
        } else {
            if (d > 1) {
                parts.add(dahgan[d])
            }
            if (y > 0) {
                parts.add(yekan[y])
            }
        }
        
        return parts.joinToString(" و ")
    }
}
