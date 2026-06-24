package com.example.data.db

import androidx.room.*
import com.example.data.model.Property
import com.example.data.model.Region
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    fun getAllProperties(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun getPropertyById(id: Long): Property?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property): Long

    @Update
    suspend fun updateProperty(property: Property)

    @Delete
    suspend fun deleteProperty(property: Property)

    @Query("SELECT MAX(id) FROM properties")
    suspend fun getMaxId(): Long?

    // Regions autocomplete
    @Query("SELECT * FROM regions ORDER BY usageCount DESC")
    fun getAllRegions(): Flow<List<Region>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: Region)

    @Query("UPDATE regions SET usageCount = usageCount + 1 WHERE name = :name")
    suspend fun incrementRegionUsage(name: String)
}
