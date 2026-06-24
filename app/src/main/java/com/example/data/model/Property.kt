package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "properties")
@JsonClass(generateAdapter = true)
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // e.g. MUP-1001
    val type: String, // آپارتمان, ویلایی, باغ ویلا
    val region: String, // منطقه
    val area: Double, // متراژ
    val bedrooms: Int, // تعداد خواب
    val description: String, // توضیحات
    val totalFloors: Int? = null, // تعداد طبقات
    val unitFloor: Int? = null, // طبقه واحد
    val hasParking: Boolean = false, // پارکینگ
    val cabinetType: String = "", // جنس کابینت (MDF, فلزی, ...)
    val otherAmenities: String = "", // سایر امکانات
    val financialMode: String, // RENT_AND_MORTGAGE, FULL_MORTGAGE, FULL_RENT
    val depositAmount: Double? = null, // رهن
    val rentAmount: Double? = null, // اجاره
    val imagesString: String = "", // Comma-separated list of local image URIs or drawables
    val isCollaborative: Boolean = false, // وضعیت همکاری (همکاری دارد/ندارد)
    val ownerName: String = "", // نام مالک
    val ownerPhone: String = "", // شماره تماس مالک
    val lastContactDate: Long? = null, // تاریخ آخرین تماس با مالک (Timestamp)
    val followUpStatus: String = "", // وضعیت پیگیری (تماس گرفته شد، منتظر پاسخ، بازدید انجام شد و...)
    val status: String = "ACTIVE", // ACTIVE (فعال), RENTED (اجاره رفت), ARCHIVED (بایگانی)
    val isSpecial: Boolean = false, // فایل ویژه ⭐
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getImagesList(): List<String> {
        if (imagesString.isEmpty()) return emptyList()
        return imagesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "regions")
data class Region(
    @PrimaryKey val name: String,
    val usageCount: Int = 1
)
