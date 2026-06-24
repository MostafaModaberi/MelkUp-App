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
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
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

    // Single Property Telegram Output
    fun generateSingleTelegramOutput(p: Property, format: String = "standard"): String {
        return generateSingleTelegramOutput(null, p, format)
    }

    fun generateSingleTelegramOutput(context: Context?, p: Property, format: String = "standard"): String {
        if (format == "compact") {
            val parts = mutableListOf<String>()
            val prefs = context?.getSharedPreferences("melkup_prefs", Context.MODE_PRIVATE)
            
            val showType = prefs?.getBoolean("compact_show_type", true) ?: true
            val showRegion = prefs?.getBoolean("compact_show_region", true) ?: true
            val showArea = prefs?.getBoolean("compact_show_area", true) ?: true
            val showBedrooms = prefs?.getBoolean("compact_show_bedrooms", true) ?: true
            val showPrice = prefs?.getBoolean("compact_show_price", true) ?: true
            val showCode = prefs?.getBoolean("compact_show_code", true) ?: true

            if (showType) {
                val typeIcon = when (p.type) {
                    "آپارتمان" -> "🏠"
                    "ویلایی" -> "🏡"
                    "باغ ویلا" -> "🌳"
                    else -> "🏢"
                }
                parts.add("$typeIcon ${p.type}")
            }
            
            if (showRegion) {
                parts.add(p.region)
            }
            
            if (showArea) {
                parts.add("${formatArea(p.area)} متر")
            }
            
            if (showBedrooms) {
                parts.add("${formatNumber(p.bedrooms)} خواب")
            }
            
            if (showPrice) {
                val priceText = when (p.financialMode) {
                    "RENT_AND_MORTGAGE" -> "${formatPrice(p.depositAmount)} رهن / ${formatPrice(p.rentAmount)} اجاره"
                    "FULL_MORTGAGE" -> "${formatPrice(p.depositAmount)} رهن کامل"
                    "FULL_RENT" -> "${formatPrice(p.rentAmount)} اجاره کامل"
                    else -> ""
                }
                if (priceText.isNotEmpty()) {
                    parts.add(priceText)
                }
            }
            
            val baseText = parts.joinToString(" - ")
            
            return if (showCode) {
                if (baseText.isNotEmpty()) {
                    "$baseText [کد: ${p.code}]"
                } else {
                    "[کد: ${p.code}]"
                }
            } else {
                baseText
            }
        }

        val sb = StringBuilder()
        val isPlain = format == "plain"
        
        val typeIcon = if (isPlain) "" else when (p.type) {
            "آپارتمان" -> "🏠 "
            "ویلایی" -> "🏡 "
            "باغ ویلا" -> "🌳 "
            else -> "🏢 "
        }
        
        sb.append("$typeIcon${p.type}\n")
        sb.append(if (isPlain) "منطقه: " else "📍 منطقه: ").append("${p.region}\n")
        sb.append(if (isPlain) "متراژ: " else "📐 متراژ: ").append("${formatArea(p.area)} متر\n")
        sb.append(if (isPlain) "تعداد خواب: " else "🛏 تعداد خواب: ").append("${formatNumber(p.bedrooms)} خواب\n")
        
        if (p.type == "آپارتمان") {
            if (p.totalFloors != null) sb.append(if (isPlain) "تعداد طبقات: " else "🏢 تعداد طبقات: ").append("${formatNumber(p.totalFloors)} طبقه\n")
            if (p.unitFloor != null) sb.append(if (isPlain) "طبقه واحد: " else "🚪 طبقه واحد: ").append("${formatNumber(p.unitFloor)}\n")
        }
        
        val parkingText = if (p.hasParking) "دارد" else "ندارد"
        sb.append(if (isPlain) "پارکینگ: " else "🚗 پارکینگ: ").append("$parkingText\n")
        
        if (p.cabinetType.isNotEmpty()) {
            sb.append(if (isPlain) "کابینت: " else "🧰 کابینت: ").append("${p.cabinetType}\n")
        }
        
        if (p.otherAmenities.isNotEmpty()) {
            sb.append(if (isPlain) "امکانات: " else "✨ امکانات: ").append("${p.otherAmenities}\n")
        }
        
        sb.append("\n💰 ")
        when (p.financialMode) {
            "RENT_AND_MORTGAGE" -> {
                sb.append("رهن: ${formatPrice(p.depositAmount)} تومان\n")
                sb.append(if (isPlain) "اجاره: " else "💵 اجاره: ").append("${formatPrice(p.rentAmount)} تومان\n")
            }
            "FULL_MORTGAGE" -> {
                sb.append("رهن کامل: ${formatPrice(p.depositAmount)} تومان\n")
            }
            "FULL_RENT" -> {
                sb.append(if (isPlain) "اجاره کامل: " else "اجاره کامل: ").append("${formatPrice(p.rentAmount)} تومان\n")
            }
        }
        
        if (p.description.trim().isNotEmpty()) {
            sb.append("\n").append(if (isPlain) "توضیحات: " else "📝 توضیحات: ").append("${p.description}\n")
        }
        
        sb.append("\n").append(if (isPlain) "کد فایل: " else "📋 کد فایل: ").append(p.code)
        
        return sb.toString()
    }

    // Group Summary Output (One line per property)
    fun generateGroupSummaryOutput(properties: List<Property>): String {
        val sb = StringBuilder()
        sb.append("📋 لیست فایلهای ملکی MelkUp:\n\n")
        properties.forEachIndexed { index, p ->
            val typeIcon = when (p.type) {
                "آپارتمان" -> "🏠"
                "ویلایی" -> "🏡"
                "باغ ویلا" -> "🌳"
                else -> "🏢"
            }
            val priceText = when (p.financialMode) {
                "RENT_AND_MORTGAGE" -> "${formatPrice(p.depositAmount)} رهن / ${formatPrice(p.rentAmount)} اجاره"
                "FULL_MORTGAGE" -> "${formatPrice(p.depositAmount)} رهن کامل"
                "FULL_RENT" -> "${formatPrice(p.rentAmount)} اجاره کامل"
                else -> ""
            }
            val line = "${index + 1}. $typeIcon ${p.type} - ${p.region} - ${formatArea(p.area)} متر - ${formatNumber(p.bedrooms)} خواب - $priceText [کد: ${p.code}]"
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    // Group Full Output
    fun generateGroupFullOutput(properties: List<Property>, format: String = "standard", context: Context? = null): String {
        val sb = StringBuilder()
        sb.append("🔥 لیست کامل فایلهای ملکی منتخب MelkUp:\n")
        sb.append("=========================================\n\n")
        properties.forEach { p ->
            sb.append(generateSingleTelegramOutput(context, p, format))
            sb.append("\n\n-----------------------------------------\n\n")
        }
        return sb.toString()
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
