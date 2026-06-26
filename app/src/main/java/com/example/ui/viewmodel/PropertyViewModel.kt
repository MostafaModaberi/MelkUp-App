package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Property
import com.example.data.model.Region
import com.example.data.repository.PropertyRepository
import com.example.util.BackupHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PropertyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PropertyRepository
    
    val allProperties: StateFlow<List<Property>>
    val allRegions: StateFlow<List<Region>>

    // Filtering & Searching States
    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("ALL") // ALL, ACTIVE, RENTED, ARCHIVED
    val selectedTypeFilter = MutableStateFlow("ALL") // ALL, آپارتمان, ویلایی, باغ ویلا
    val isSpecialFilter = MutableStateFlow(false)
    val sortBy = MutableStateFlow("DATE") // DATE, AREA, BEDROOMS, REGION
    val sortAscending = MutableStateFlow(false)

    // Selection mode for group output
    val isSelectionMode = MutableStateFlow(false)
    val selectedPropertyIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        val dao = AppDatabase.getDatabase(application).propertyDao()
        repository = PropertyRepository(dao)

        allProperties = repository.allProperties.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allRegions = repository.allRegions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Reactive filtered properties list
    val filteredProperties: StateFlow<List<Property>> = combine(
        allProperties,
        searchQuery,
        selectedStatusFilter,
        selectedTypeFilter,
        isSpecialFilter,
        sortBy,
        sortAscending
    ) { array ->
        val props = array[0] as List<Property>
        val query = array[1] as String
        val status = array[2] as String
        val type = array[3] as String
        val special = array[4] as Boolean
        val sort = array[5] as String
        val asc = array[6] as Boolean

        var list = props

        // 1. Search Query filter (checks code, type, region, area, bedrooms, description, amenities, owner info, etc.)
        if (query.trim().isNotEmpty()) {
            val q = query.trim().lowercase()
            list = list.filter { p ->
                p.code.lowercase().contains(q) ||
                p.type.lowercase().contains(q) ||
                p.region.lowercase().contains(q) ||
                p.area.toString().contains(q) ||
                p.bedrooms.toString().contains(q) ||
                p.description.lowercase().contains(q) ||
                p.otherAmenities.lowercase().contains(q) ||
                p.cabinetType.lowercase().contains(q) ||
                p.ownerName.lowercase().contains(q) ||
                p.ownerPhone.lowercase().contains(q) ||
                p.followUpStatus.lowercase().contains(q) ||
                (if (p.hasParking) "پارکینگ" else "").contains(q)
            }
        }

        // 2. Status filter
        if (status != "ALL") {
            list = list.filter { it.status == status }
        }

        // 3. Type filter
        if (type != "ALL") {
            list = list.filter { it.type == type }
        }

        // 4. Special filter
        if (special) {
            list = list.filter { it.isSpecial }
        }

        // 5. Sorting
        list = when (sort) {
            "AREA" -> if (asc) list.sortedBy { it.area } else list.sortedByDescending { it.area }
            "BEDROOMS" -> if (asc) list.sortedBy { it.bedrooms } else list.sortedByDescending { it.bedrooms }
            "REGION" -> if (asc) list.sortedBy { it.region } else list.sortedByDescending { it.region }
            "CODE" -> if (asc) list.sortedBy { it.code } else list.sortedByDescending { it.code }
            else -> if (asc) list.sortedBy { it.createdAt } else list.sortedByDescending { it.createdAt } // DATE
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Get statistics
    val stats = allProperties.map { props ->
        PropertyStats(
            total = props.size,
            active = props.count { it.status == "ACTIVE" },
            rented = props.count { it.status == "RENTED" },
            archived = props.count { it.status == "ARCHIVED" },
            special = props.count { it.isSpecial }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PropertyStats()
    )

    // Operations
    fun saveProperty(property: Property, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (property.id == 0L) {
                repository.insertProperty(property)
            } else {
                repository.updateProperty(property)
            }
            onComplete()
        }
    }

    fun deleteProperty(property: Property) {
        viewModelScope.launch {
            repository.deleteProperty(property)
        }
    }

    fun toggleSpecial(property: Property) {
        viewModelScope.launch {
            repository.updateProperty(property.copy(isSpecial = !property.isSpecial))
        }
    }

    fun updateStatus(property: Property, newStatus: String) {
        viewModelScope.launch {
            repository.updateProperty(property.copy(status = newStatus))
        }
    }

    fun getNextPropertyCode(callback: (String) -> Unit) {
        viewModelScope.launch {
            callback(repository.generateNextCode())
        }
    }

    // Selection management
    fun toggleSelection(propertyId: Long) {
        val current = selectedPropertyIds.value
        if (current.contains(propertyId)) {
            selectedPropertyIds.value = current - propertyId
        } else {
            selectedPropertyIds.value = current + propertyId
        }
    }

    fun clearSelection() {
        selectedPropertyIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun selectAllFiltered(filteredList: List<Property>) {
        selectedPropertyIds.value = filteredList.map { it.id }.toSet()
    }

    // Backup & Restore
    fun exportBackupJson(properties: List<Property> = allProperties.value): String {
        return BackupHelper.exportBackup(properties)
    }

    fun importBackupJson(json: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val imported = BackupHelper.importBackup(json)
            if (imported != null) {
                var count = 0
                for (prop in imported) {
                    // Strip the ID so Room creates a fresh entry and avoids conflicts, or keep it if clean overwrite.
                    // To safely merge, we can check if a property with the same code exists. If yes, overwrite or keep.
                    // Overwriting on exact code is best.
                    val cleanProp = prop.copy(id = 0L) // Ensure fresh insert to prevent primary key constraint fail
                    repository.insertProperty(cleanProp)
                    count++
                }
                onResult(true, count)
            } else {
                onResult(false, 0)
            }
        }
    }
}

data class PropertyStats(
    val total: Int = 0,
    val active: Int = 0,
    val rented: Int = 0,
    val archived: Int = 0,
    val special: Int = 0
)
