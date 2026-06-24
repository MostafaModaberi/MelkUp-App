package com.example.util

import com.example.data.model.Property
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object BackupHelper {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, Property::class.java)
    private val adapter = moshi.adapter<List<Property>>(listType)

    fun exportBackup(properties: List<Property>): String {
        return try {
            adapter.toJson(properties)
        } catch (e: Exception) {
            ""
        }
    }

    fun importBackup(json: String): List<Property>? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
