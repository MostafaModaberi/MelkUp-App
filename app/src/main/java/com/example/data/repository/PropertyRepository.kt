package com.example.data.repository

import com.example.data.db.PropertyDao
import com.example.data.model.Property
import com.example.data.model.Region
import kotlinx.coroutines.flow.Flow

class PropertyRepository(private val propertyDao: PropertyDao) {

    val allProperties: Flow<List<Property>> = propertyDao.getAllProperties()
    val allRegions: Flow<List<Region>> = propertyDao.getAllRegions()

    suspend fun getPropertyById(id: Long): Property? {
        return propertyDao.getPropertyById(id)
    }

    suspend fun insertProperty(property: Property): Long {
        // Save/update region
        val cleanRegion = property.region.trim()
        if (cleanRegion.isNotEmpty()) {
            saveRegion(cleanRegion)
        }
        return propertyDao.insertProperty(property)
    }

    suspend fun updateProperty(property: Property) {
        // Save/update region
        val cleanRegion = property.region.trim()
        if (cleanRegion.isNotEmpty()) {
            saveRegion(cleanRegion)
        }
        propertyDao.updateProperty(property)
    }

    suspend fun deleteProperty(property: Property) {
        propertyDao.deleteProperty(property)
    }

    suspend fun generateNextCode(): String {
        val maxId = propertyDao.getMaxId() ?: 0L
        val nextNumber = 1001 + maxId
        return "MUP-$nextNumber"
    }

    private suspend fun saveRegion(regionName: String) {
        val cleanName = regionName.trim()
        // Simple search / increment
        try {
            propertyDao.insertRegion(Region(name = cleanName, usageCount = 1))
        } catch (e: Exception) {
            // Region might already exist, increment it
            propertyDao.incrementRegionUsage(cleanName)
        }
    }
}
