package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.Property
import com.example.ui.theme.AccentGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Typography
import kotlinx.coroutines.flow.StateFlow
import com.example.ui.viewmodel.PropertyStats
import com.example.ui.viewmodel.PropertyViewModel
import com.example.util.PersianUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MelkUpApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelkUpApp(viewModel: PropertyViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }

    // Collect Reactive State Flows
    val properties by viewModel.filteredProperties.collectAsStateWithLifecycle()
    val rawProperties by viewModel.allProperties.collectAsStateWithLifecycle()
    val regions by viewModel.allRegions.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val isSpecialFilter by viewModel.isSpecialFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val sortAscending by viewModel.sortAscending.collectAsStateWithLifecycle()
    
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedPropertyIds by viewModel.selectedPropertyIds.collectAsStateWithLifecycle()

    // Dialog & overlay states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var propertyToEdit by remember { mutableStateOf<Property?>(null) }
    var propertyToView by remember { mutableStateOf<Property?>(null) }

    var currentTab by remember { mutableStateOf("home") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.End,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // MU Badge logo
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MU",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ملک‌آپ",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.offset(y = 2.dp)
                            )
                            Text(
                                text = "مدیریت هوشمند • ${PersianUtils.getPersianTodayString()}",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(y = 1.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Group Output Toggle
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.isSelectionMode.value = true
                                currentTab = "all_files" // Switch to All Files tab to select properties!
                            }
                        },
                        modifier = Modifier.testTag("selection_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.PlaylistAddCheckCircle else Icons.Outlined.PlaylistAddCheck,
                            contentDescription = "انتخاب چندتایی",
                            tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingNavItem(
                            selected = currentTab == "home",
                            onClick = { currentTab = "home" },
                            icon = { Icon(Icons.Default.Home, contentDescription = "داشبورد") },
                            label = "داشبورد"
                        )
                        FloatingNavItem(
                            selected = currentTab == "all_files",
                            onClick = { currentTab = "all_files" },
                            icon = { Icon(Icons.Default.List, contentDescription = "کل فایل‌ها") },
                            label = "کل فایل‌ها"
                        )
                        FloatingNavItem(
                            selected = currentTab == "prompt_generator",
                            onClick = { currentTab = "prompt_generator" },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "پرامپت استوری") },
                            label = "پرامپت استوری"
                        )
                        FloatingNavItem(
                            selected = currentTab == "settings",
                            onClick = { currentTab = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                            label = "تنظیمات"
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == "home" || currentTab == "all_files") {
                FloatingActionButton(
                    onClick = {
                        viewModel.getNextPropertyCode { code ->
                            propertyToEdit = null // Fresh property
                            showAddEditDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 0.dp)
                        .testTag("add_property_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزودن فایل",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                "home" -> {
                    // --- HOME TAB ---
                    // 1. StatsDashboard (Displays "اجاره رفت" and "بایگانی" only, rest removed)
                    StatsDashboard(stats = stats)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimalist Segmented File Type Selector (Fits entirely in dashboard)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val fileTypes = listOf(
                            "ALL" to "همه فایل‌ها",
                            "آپارتمان" to "آپارتمان",
                            "ویلایی" to "ویلایی",
                            "باغ ویلا" to "باغ ویلا"
                        )
                        fileTypes.forEach { (id, label) ->
                            val isSel = selectedTypeFilter == id
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.selectedTypeFilter.value = id }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- SORTING CONTROLS ON HOME PAGE ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚡ مرتب‌سازی فایل‌ها:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // Ascending/Descending Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable { viewModel.sortAscending.value = !sortAscending }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (sortAscending) "صعودی" else "نزولی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Sort Fields Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sortFields = listOf(
                                    "DATE" to "تاریخ ثبت",
                                    "AREA" to "متراژ",
                                    "BEDROOMS" to "خواب",
                                    "REGION" to "منطقه"
                                )
                                sortFields.forEach { (field, label) ->
                                    val isSel = sortBy == field
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.sortBy.value = field }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recent Files Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📂 فایل‌های اخیر (${PersianUtils.formatNumber(properties.size)} مورد)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // List of recent properties (Limit to 3)
                    if (properties.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هیچ فایلی یافت نشد.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        val recentProperties = properties.take(3)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(recentProperties, key = { it.id }) { property ->
                                PropertyCardItem(
                                    property = property,
                                    isSelectionMode = false,
                                    isSelected = false,
                                    onCardClick = {
                                        propertyToView = property
                                        showDetailsDialog = true
                                    },
                                    onSelectionToggle = {},
                                    onToggleSpecial = { viewModel.toggleSpecial(property) },
                                    onQuickCopy = {
                                        val copyFormat = prefs.getString("copy_format", "standard") ?: "standard"
                                        val text = PersianUtils.generateSingleTelegramOutput(context, property, copyFormat)
                                        PersianUtils.copyToClipboard(context, text)
                                    },
                                    onEdit = {
                                        propertyToEdit = property
                                        showAddEditDialog = true
                                    },
                                    onDelete = {
                                        viewModel.deleteProperty(property)
                                        Toast.makeText(context, "فایل حذف شد", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            
                            // "More" button to see all files
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { currentTab = "all_files" },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "مشاهده فایل‌های بیشتر 📊",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "all_files" -> {
                    // --- ALL FILES TAB ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 54.dp)
                                .testTag("quick_search_input"),
                            placeholder = { Text("جستجوی سریع (کد، منطقه، قیمت، مالک...)", fontSize = 11.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.searchQuery.value = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "پاک کردن",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        )
                        
                        IconButton(
                            onClick = { showSearchDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("advanced_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "فیلتر پیشرفته",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Group operation bar (Only visible when selection mode is active)
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        GroupSelectionBar(
                            selectedCount = selectedPropertyIds.size,
                            onSelectAll = { viewModel.selectAllFiltered(properties) },
                            onClearSelection = { viewModel.clearSelection() },
                            onCopySummary = {
                                val selectedProps = properties.filter { selectedPropertyIds.contains(it.id) }
                                if (selectedProps.isEmpty()) {
                                    Toast.makeText(context, "هیچ فایلی انتخاب نشده است", Toast.LENGTH_SHORT).show()
                                } else {
                                    val text = PersianUtils.generateGroupSummaryOutput(selectedProps)
                                    PersianUtils.copyToClipboard(context, text)
                                }
                            },
                            onCopyFull = {
                                val selectedProps = properties.filter { selectedPropertyIds.contains(it.id) }
                                if (selectedProps.isEmpty()) {
                                    Toast.makeText(context, "هیچ فایلی انتخاب نشده است", Toast.LENGTH_SHORT).show()
                                } else {
                                    val copyFormat = prefs.getString("copy_format", "standard") ?: "standard"
                                    val text = PersianUtils.generateGroupFullOutput(selectedProps, copyFormat, context)
                                    PersianUtils.copyToClipboard(context, text)
                                }
                            }
                        )
                    }

                    // Quick statistics / Export All section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نمایش ${PersianUtils.formatNumber(properties.size)} فایل از ${PersianUtils.formatNumber(rawProperties.size)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )

                        if (rawProperties.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val text = PersianUtils.generateGroupSummaryOutput(rawProperties)
                                    PersianUtils.copyToClipboard(context, text, "MelkUp All Properties")
                                },
                                modifier = Modifier.testTag("export_all_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی خلاصه همه فایل‌ها", fontSize = 12.sp)
                            }
                        }
                    }

                    // Main List or Empty State
                    if (properties.isEmpty()) {
                        EmptyStateView(
                            isFilterActive = searchQuery.isNotEmpty() || selectedStatusFilter != "ALL" || selectedTypeFilter != "ALL" || isSpecialFilter,
                            onClearFilters = {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedStatusFilter.value = "ALL"
                                viewModel.selectedTypeFilter.value = "ALL"
                                viewModel.isSpecialFilter.value = false
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("properties_list"),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(properties, key = { it.id }) { property ->
                                PropertyCardItem(
                                    property = property,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedPropertyIds.contains(property.id),
                                    onCardClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(property.id)
                                        } else {
                                            propertyToView = property
                                            showDetailsDialog = true
                                        }
                                    },
                                    onSelectionToggle = { viewModel.toggleSelection(property.id) },
                                    onToggleSpecial = { viewModel.toggleSpecial(property) },
                                    onQuickCopy = {
                                        val copyFormat = prefs.getString("copy_format", "standard") ?: "standard"
                                        val text = PersianUtils.generateSingleTelegramOutput(context, property, copyFormat)
                                        PersianUtils.copyToClipboard(context, text)
                                    },
                                    onEdit = {
                                        propertyToEdit = property
                                        showAddEditDialog = true
                                    },
                                    onDelete = {
                                        viewModel.deleteProperty(property)
                                        Toast.makeText(context, "فایل حذف شد", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                "prompt_generator" -> {
                    // --- AI PROMPT GENERATOR PAGE ---
                    AiPromptGeneratorScreen(
                        properties = rawProperties,
                        context = context
                    )
                }
                "settings" -> {
                    // --- SETTINGS PAGE ---
                    SettingsAndBackupScreen(
                        onExport = {
                            val json = viewModel.exportBackupJson()
                            PersianUtils.copyToClipboard(context, json, "MelkUp Backup")
                        },
                        onImport = { importText ->
                            viewModel.importBackupJson(importText) { success, count ->
                                if (success) {
                                    Toast.makeText(context, "اطلاعات با موفقیت بازیابی شد! $count فایل وارد شد. 🎉", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "خطا در فرمت کد پشتیبان!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // --- DIALOGS & OVERLAYS ---

    if (showAddEditDialog) {
        AddEditPropertyDialog(
            property = propertyToEdit,
            regionsList = regions,
            onDismiss = { showAddEditDialog = false },
            onSave = { updatedProperty ->
                viewModel.saveProperty(updatedProperty) {
                    showAddEditDialog = false
                    Toast.makeText(context, "فایل با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                }
            },
            viewModel = viewModel
        )
    }

    if (showDetailsDialog && propertyToView != null) {
        PropertyDetailsDialog(
            property = propertyToView!!,
            onDismiss = {
                showDetailsDialog = false
                propertyToView = null
            },
            onStatusChange = { status ->
                viewModel.updateStatus(propertyToView!!, status)
                propertyToView = propertyToView!!.copy(status = status)
            },
            onEdit = {
                propertyToEdit = propertyToView
                showDetailsDialog = false
                showAddEditDialog = true
            },
            onToggleSpecial = {
                viewModel.toggleSpecial(propertyToView!!)
                propertyToView = propertyToView!!.copy(isSpecial = !propertyToView!!.isSpecial)
            }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            onDismiss = { showBackupDialog = false },
            onExport = {
                val json = viewModel.exportBackupJson()
                PersianUtils.copyToClipboard(context, json, "MelkUp Backup")
            },
            onImport = { json ->
                viewModel.importBackupJson(json) { success, count ->
                    if (success) {
                        Toast.makeText(context, "پشتیبان با موفقیت بازیابی شد! $count فایل وارد شد.", Toast.LENGTH_LONG).show()
                        showBackupDialog = false
                    } else {
                        Toast.makeText(context, "خطا در وارد کردن پشتیبان. فرمت نامعتبر است.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showSearchDialog) {
        SearchAndFilterDialog(
            query = searchQuery,
            onQueryChange = { viewModel.searchQuery.value = it },
            selectedStatus = selectedStatusFilter,
            onStatusSelect = { viewModel.selectedStatusFilter.value = it },
            isSpecialFilter = isSpecialFilter,
            onSpecialFilterToggle = { viewModel.isSpecialFilter.value = !isSpecialFilter },
            sortBy = sortBy,
            onSortByChange = { viewModel.sortBy.value = it },
            sortAscending = sortAscending,
            onSortAscendingChange = { viewModel.sortAscending.value = it },
            onDismiss = { showSearchDialog = false }
        )
    }
}

// --- SUB-COMPOSABLES ---

@Composable
fun StatsDashboard(stats: PropertyStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
        
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
        val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer

        // Box 1: تعداد فایل‌ها
        ThemeStatCard(
            title = "تعداد فایل‌ها",
            count = stats.total,
            icon = Icons.Default.Folder,
            bgColor = primaryContainer,
            borderColor = primaryColor.copy(alpha = 0.2f),
            textColor = onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )

        // Box 2: فایل‌های ستاره‌دار
        ThemeStatCard(
            title = "فایل‌های ستاره‌دار",
            count = stats.special,
            icon = Icons.Default.Star,
            bgColor = secondaryContainer,
            borderColor = secondaryColor.copy(alpha = 0.2f),
            textColor = onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ThemeStatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = PersianUtils.formatNumber(count),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    borderColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = PersianUtils.formatNumber(count),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedStatus: String,
    onStatusSelect: (String) -> Unit,
    isSpecialFilter: Boolean,
    onSpecialFilterToggle: () -> Unit,
    sortBy: String,
    onSortByChange: (String) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 جستجو و فیلتر پیشرفته",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                // Search Box
                Text("۱. عبارت جستجو:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().testTag("search_dialog_input"),
                    placeholder = { Text("کد، منطقه، قیمت، نام مالک...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Redesigned Status Custom Segmented Selector
                Text("۲. فیلتر وضعیت فایل:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statuses = listOf(
                        "ALL" to "همه",
                        "ACTIVE" to "فعال",
                        "RENTED" to "واگذار شده",
                        "ARCHIVED" to "بایگانی شده"
                    )
                    statuses.forEach { (id, label) ->
                        val isSel = selectedStatus == id
                        val (bg, fg) = when (id) {
                            "ALL" -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            "ACTIVE" -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
                            "RENTED" -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
                            "ARCHIVED" -> Pair(Color(0xFFF1F5F9), Color(0xFF334155))
                            else -> Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) bg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) fg else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onStatusSelect(id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) fg else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Special Filter Options Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐ فقط فایل‌های ویژه نشان داده شوند؟", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isSpecialFilter,
                        onCheckedChange = { onSpecialFilterToggle() }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Sort Choices Section
                Text("۳. مرتب‌سازی بر اساس:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val sortOptions = listOf(
                    "DATE" to "تاریخ ثبت",
                    "AREA" to "متراژ ملک",
                    "BEDROOMS" to "خواب",
                    "REGION" to "منطقه الفبایی"
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sortOptions.forEach { (field, label) ->
                        val isSel = sortBy == field
                        FilterChip(
                            selected = isSel,
                            onClick = { onSortByChange(field) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جهت مرتب‌سازی:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !sortAscending,
                            onClick = { onSortAscendingChange(false) },
                            label = { Text("نزولی (بیشترین/جدیدترین)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = sortAscending,
                            onClick = { onSortAscendingChange(true) },
                            label = { Text("صعودی", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("اعمال و مشاهده نتایج", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GroupSelectionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCopySummary: () -> Unit,
    onCopyFull: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "انصراف")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${PersianUtils.formatNumber(selectedCount)} فایل انتخاب شده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Row {
                TextButton(onClick = onSelectAll) {
                    Text("انتخاب همه", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onCopySummary,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("کپی خلاصه", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onCopyFull,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("کپی کامل", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PropertyCardItem(
    property: Property,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onToggleSpecial: () -> Unit,
    onQuickCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onCardClick() }
            .testTag("property_card_${property.code}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left/Right accent indicator strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (property.isSpecial) AccentGold else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
            // Header Row (Type, Special Badge, Status Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Emoji/Icon representing the type
                    val typeEmoji = when (property.type) {
                        "آپارتمان" -> "🏠"
                        "ویلایی" -> "🏡"
                        else -> "🌳"
                    }
                    Text(text = typeEmoji, fontSize = 20.sp)

                    Text(
                        text = "${property.type} - ${property.region}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (property.isSpecial) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("⭐", fontSize = 10.sp)
                                Text(
                                    text = "ویژه",
                                    color = Color(0xFF92400E),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    StatusBadge(status = property.status)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spec grid (Area, Bedrooms, Address/Region details, Code)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Col 1
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📐 ${PersianUtils.formatArea(property.area)} متر",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "📍 ${property.region}",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Col 2
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🛏 ${PersianUtils.formatNumber(property.bedrooms)} خواب",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "🏷 ${property.code}",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📅 ثبت: ${PersianUtils.formatDate(property.createdAt)}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing details block with custom quick copy and operations
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (property.financialMode) {
                            "RENT_AND_MORTGAGE" -> {
                                val hasDep = property.depositAmount != null && property.depositAmount > 0
                                val hasRent = property.rentAmount != null && property.rentAmount > 0
                                if (hasDep && !hasRent) "رهن کامل"
                                else if (!hasDep && hasRent) "اجاره کامل"
                                else "رهن / اجاره"
                            }
                            "FULL_MORTGAGE" -> "رهن کامل"
                            "FULL_RENT" -> "اجاره کامل"
                            else -> "شرایط مالی"
                        },
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (property.financialMode) {
                            "RENT_AND_MORTGAGE" -> {
                                val hasDep = property.depositAmount != null && property.depositAmount > 0
                                val hasRent = property.rentAmount != null && property.rentAmount > 0
                                if (hasDep && hasRent) {
                                    "${PersianUtils.formatPrice(property.depositAmount)} رهن / ${PersianUtils.formatPrice(property.rentAmount)} اجاره"
                                } else if (hasDep) {
                                    "${PersianUtils.formatPrice(property.depositAmount)} رهن"
                                } else if (hasRent) {
                                    "${PersianUtils.formatPrice(property.rentAmount)} اجاره"
                                } else {
                                    "توافقی"
                                }
                            }
                            "FULL_MORTGAGE" -> PersianUtils.formatPrice(property.depositAmount)
                            "FULL_RENT" -> PersianUtils.formatPrice(property.rentAmount)
                            else -> "توافقی"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Copy & operations buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onQuickCopy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("quick_copy_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("کپی سریع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Star Toggle Button
                    IconButton(
                        onClick = onToggleSpecial,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .testTag("quick_star_button")
                    ) {
                        Icon(
                            imageVector = if (property.isSpecial) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "ویژه",
                            tint = if (property.isSpecial) AccentGold else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // More vert menu
                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .testTag("more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "بیشتر",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ویرایش") },
                                onClick = {
                                    expandedMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف فایل") },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
            // Closing the Column
        }
        // Closing the outer Row
    }
    // Closing the Card
}
// Closing PropertyCardItem
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "ACTIVE" -> Triple("فعال", Color(0xFFD1FAE5), Color(0xFF065F46))
        "RENTED" -> Triple("اجاره رفت", Color(0xFFFEE2E2), Color(0xFF991B1B))
        "ARCHIVED" -> Triple("بایگانی", Color(0xFFF1F5F9), Color(0xFF334155))
        else -> Triple("فعال", Color(0xFFD1FAE5), Color(0xFF065F46))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyStateView(isFilterActive: Boolean, onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isFilterActive) Icons.Default.FilterListOff else Icons.Default.HolidayVillage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFilterActive) "هیچ فایلی با این فیلترها پیدا نشد!" else "بنگاه املاک شما هنوز فایلی ندارد",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFilterActive) "میتوانید جستجو یا فیلترها را ریست کنید تا فایلها مجدد نمایش داده شوند." else "ثبت فایل جدید را از دکمه پایین لمس کنید تا فرآیند فروش و رهن و اجاره آغاز شود.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (isFilterActive) {
            Button(onClick = onClearFilters) {
                Text("حذف تمام فیلترها")
            }
        }
    }
}

// --- FULLSCREEN OR LARGE OVERLAY DIALOGS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailsDialog(
    property: Property,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onToggleSpecial: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    var showAiPromptDialog by remember { mutableStateOf(false) }
    var selectedDetailTab by remember { mutableStateOf("property") } // "property" or "owner"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 600.dp)
                .padding(vertical = 12.dp)
                .heightIn(max = 650.dp)
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Compact Header (Instead of full screen top bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جزئیات کامل فایل ${property.code}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Special Toggle Star
                        IconButton(onClick = onToggleSpecial) {
                            Icon(
                                imageVector = if (property.isSpecial) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "ویژه",
                                tint = if (property.isSpecial) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Edit Icon
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Sleek Tab Control for Property vs Owner details (RTL layout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val detailTabs = listOf(
                        "property" to "🏠 مشخصات ملک",
                        "owner" to "🔑 اطلاعات مالک"
                    )
                    detailTabs.forEach { (tabId, label) ->
                        val isSelected = selectedDetailTab == tabId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedDetailTab = tabId },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (selectedDetailTab == "property") {
                        // --- PROPERTY DETAILS SECTION ---
                        // Images Section (If any)
                        val images = property.getImagesList()
                        if (images.isNotEmpty()) {
                            Text(
                                text = "تصاویر ملک (${PersianUtils.formatNumber(images.size)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                items(images) { imgUrl ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.width(220.dp).fillMaxHeight()
                                    ) {
                                        AsyncImage(
                                            model = imgUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Main Specifications Card (Shared Publicly)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏠 مشخصات عمومی ملک",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                    StatusBadge(status = property.status)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                SpecRow("نوع ملک", property.type)
                                SpecRow("منطقه", property.region)
                                SpecRow("متراژ", "${PersianUtils.formatArea(property.area)} متر")
                                SpecRow("تعداد خواب", "${PersianUtils.formatNumber(property.bedrooms)} خواب")

                                if (property.type == "آپارتمان") {
                                    if (property.totalFloors != null) SpecRow("تعداد طبقات ساختمان", "${PersianUtils.formatNumber(property.totalFloors)} طبقه")
                                    if (property.unitFloor != null) SpecRow("طبقه واحد", PersianUtils.formatNumber(property.unitFloor))
                                }

                                SpecRow("پارکینگ", if (property.hasParking) "دارد" else "ندارد")
                                if (property.cabinetType.isNotEmpty()) SpecRow("جنس کابینت", property.cabinetType)
                                if (property.otherAmenities.isNotEmpty()) SpecRow("سایر امکانات", property.otherAmenities)
                            }
                        }

                        // Financial Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "💰 اطلاعات مالی فایل",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                when (property.financialMode) {
                                    "RENT_AND_MORTGAGE" -> {
                                        if (property.depositAmount != null && property.depositAmount > 0) {
                                            SpecRow("میزان رهن", "${PersianUtils.formatPrice(property.depositAmount)} تومان")
                                        }
                                        if (property.rentAmount != null && property.rentAmount > 0) {
                                            SpecRow("میزان اجاره", "${PersianUtils.formatPrice(property.rentAmount)} تومان")
                                        }
                                    }
                                    "FULL_MORTGAGE" -> {
                                        SpecRow("میزان رهن کامل", "${PersianUtils.formatPrice(property.depositAmount)} تومان")
                                    }
                                    "FULL_RENT" -> {
                                        SpecRow("میزان اجاره کامل", "${PersianUtils.formatPrice(property.rentAmount)} تومان")
                                    }
                                }
                            }
                        }

                        // Description Card
                        if (property.description.trim().isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📝 توضیحات فایل",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = property.description,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // --- OWNER DETAILS SECTION ---
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🔒 اطلاعات مالک (محرمانه)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SpecRow("نام مالک", property.ownerName.ifEmpty { "ثبت نشده" })
                                
                                // Owner phone with direct click actions
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "شماره تماس مالک",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    if (property.ownerPhone.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = PersianUtils.formatToPersianNumbers(property.ownerPhone),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${property.ownerPhone}"))
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = "تماس", tint = Color(0xFF4CAF50))
                                            }
                                        }
                                    } else {
                                        Text("ثبت نشده", fontWeight = FontWeight.Bold)
                                    }
                                }

                                SpecRow("مالکیت فایل", if (property.isCollaborative) "متعلق به همکار (بنگاه دیگر)" else "فایل خودی (بنگاه خودمان)")
                                SpecRow("تاریخ آخرین تماس", PersianUtils.formatDate(property.lastContactDate))
                            }
                        }

                        // Quick Actions / Status Changer
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "⚡ تغییر سریع وضعیت فایل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onStatusChange("ACTIVE") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("فعال", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onStatusChange("RENTED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("اجاره رفت", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onStatusChange("ARCHIVED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("بایگانی", color = Color(0xFF616161), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // AI Prompt Button
                            Button(
                                onClick = { showAiPromptDialog = true },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("details_ai_prompt"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("پرامپت هوشمند آگهی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Copy Telegram Output Button
                            Button(
                                onClick = {
                                    val copyFormat = prefs.getString("copy_format", "standard") ?: "standard"
                                    val text = PersianUtils.generateSingleTelegramOutput(context, property, copyFormat)
                                    PersianUtils.copyToClipboard(context, text)
                                },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("details_copy_telegram"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("کپی خروجی تلگرام", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Close button on its own line for generous width and readability
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("بستن جزئیات فایل", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAiPromptDialog) {
        AiPromptGeneratorDialog(
            property = property,
            onDismiss = { showAiPromptDialog = false }
        )
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPropertyDialog(
    property: Property?,
    regionsList: List<com.example.data.model.Region>,
    onDismiss: () -> Unit,
    onSave: (Property) -> Unit,
    viewModel: PropertyViewModel
) {
    var type by remember { mutableStateOf(property?.type ?: "آپارتمان") }
    var region by remember { mutableStateOf(property?.region ?: "") }
    var area by remember { mutableStateOf(property?.area?.toString() ?: "") }
    var bedrooms by remember { mutableStateOf(property?.bedrooms?.toString() ?: "2") }
    var description by remember { mutableStateOf(property?.description ?: "") }
    
    // Apartment Specific fields
    var totalFloors by remember { mutableStateOf(property?.totalFloors?.toString() ?: "") }
    var unitFloor by remember { mutableStateOf(property?.unitFloor?.toString() ?: "") }

    // Amenities
    var hasParking by remember { mutableStateOf(property?.hasParking ?: true) }
    var cabinetType by remember { mutableStateOf(property?.cabinetType ?: "MDF") }
    var selectedAmenitiesList by remember {
        mutableStateOf(
            property?.otherAmenities?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }
    var customAmenitiesText by remember { mutableStateOf("") }

    // Financial
    var financialMode by remember { mutableStateOf(property?.financialMode ?: "RENT_AND_MORTGAGE") }
    var depositAmount by remember { mutableStateOf(property?.depositAmount?.toLong()?.toString() ?: "") }
    var rentAmount by remember { mutableStateOf(property?.rentAmount?.toLong()?.toString() ?: "") }

    // Internal Agency Details
    var isCollaborative by remember { mutableStateOf(property?.isCollaborative ?: true) }
    var ownerName by remember { mutableStateOf(property?.ownerName ?: "") }
    var ownerPhone by remember { mutableStateOf(property?.ownerPhone ?: "") }
    var followUpStatus by remember { mutableStateOf(property?.followUpStatus ?: "تماس گرفته شد") }
    
    // Pictures
    var selectedImageUrlList by remember {
        mutableStateOf(
            property?.getImagesList() ?: emptyList()
        )
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val amenitiesOptions = listOf("آسانسور", "انباری", "تراس", "حیاط", "استخر", "پکیج", "کولر گازی")
    val followUpOptions = listOf("تماس گرفته شد", "منتظر پاسخ", "بازدید انجام شد", "در حال مذاکره")

    // Setup autocomplete for region
    var regionDropdownExpanded by remember { mutableStateOf(false) }
    val filteredRegionSuggestions = regionsList.filter {
        it.name.contains(region, ignoreCase = true)
    }

    // Step navigation and optional amenities toggle states
    var currentStep by remember { mutableStateOf(1) }
    var showOptionalAmenities by remember {
        mutableStateOf(
            property != null && (selectedAmenitiesList.isNotEmpty() || cabinetType != "MDF" || !hasParking)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 600.dp)
                .padding(vertical = 12.dp)
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Header (Replacing TopAppBar with a compact clean layout for floating dialog)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (property == null) "ثبت فایل رهن و اجاره جدید 🏢" else "ویرایش فایل ${property.code} ✏️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                // Step indicators (Linear Progress Bar and Label)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentStep) {
                                1 -> "مرحله ۱: مشخصات اصلی ملک 🏠"
                                2 -> "مرحله ۲: جزئیات و امکانات ملک 🏢"
                                3 -> "مرحله ۳: مشخصات مالی فایل 💰"
                                else -> "مرحله ۴: اطلاعات مالک و عکس‌ها 🔑"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "گام $currentStep از ۴",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    LinearProgressIndicator(
                        progress = currentStep / 4f,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                }

                // Step Screen Contents
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            // Step 1: Base Specs
                            Text("نوع ملک *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("آپارتمان", "ویلایی", "باغ ویلا").forEach { t ->
                                    val isSel = type == t
                                    Button(
                                        onClick = { type = t },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = t,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Region Autocomplete Input
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = region,
                                    onValueChange = {
                                        region = it
                                        regionDropdownExpanded = it.isNotEmpty()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("region_input"),
                                    label = { Text("منطقه / محله *") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (regionDropdownExpanded && filteredRegionSuggestions.isNotEmpty()) {
                                    DropdownMenu(
                                        expanded = regionDropdownExpanded,
                                        onDismissRequest = { regionDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        filteredRegionSuggestions.take(5).forEach { sug ->
                                            DropdownMenuItem(
                                                text = { Text(sug.name) },
                                                onClick = {
                                                    region = sug.name
                                                    regionDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Area & Bedrooms
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = area,
                                    onValueChange = { area = it },
                                    modifier = Modifier.weight(1f).testTag("area_input"),
                                    label = { Text("متراژ (متر مربع) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = bedrooms,
                                    onValueChange = { bedrooms = it },
                                    modifier = Modifier.weight(1f).testTag("bedrooms_input"),
                                    label = { Text("تعداد خواب *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        2 -> {
                            // Step 2: Building Specs & Optional Amenities
                            if (type == "آپارتمان") {
                                Text("مشخصات طبقات ساختمان", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = totalFloors,
                                        onValueChange = { totalFloors = it },
                                        modifier = Modifier.weight(1f).testTag("total_floors_input"),
                                        label = { Text("تعداد طبقات ساختمان") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = unitFloor,
                                        onValueChange = { unitFloor = it },
                                        modifier = Modifier.weight(1f).testTag("unit_floor_input"),
                                        label = { Text("طبقه واحد") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Optional Amenities Toggle Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ثبت امکانات فرعی و کابینت (اختیاری)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Switch(
                                            checked = showOptionalAmenities,
                                            onCheckedChange = { showOptionalAmenities = it }
                                        )
                                    }

                                    AnimatedVisibility(visible = showOptionalAmenities) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Divider()
                                            
                                            // Parking
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("پارکینگ دارد؟", fontSize = 13.sp)
                                                Switch(
                                                    checked = hasParking,
                                                    onCheckedChange = { hasParking = it },
                                                    modifier = Modifier.testTag("parking_switch")
                                                )
                                            }

                                            // Cabinet selection
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("جنس کابینت:", fontSize = 13.sp)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    listOf("MDF", "فلزی", "ندارد").forEach { cab ->
                                                        val isSel = cabinetType == cab
                                                        FilterChip(
                                                            selected = isSel,
                                                            onClick = { cabinetType = cab },
                                                            label = { Text(cab, fontSize = 11.sp) }
                                                        )
                                                    }
                                                }
                                            }

                                            // CustomFlowRow Optional Amenities List (box size matching, wrapping cleanly)
                                            Column {
                                                Text("انتخاب سایر امکانات ملک:", modifier = Modifier.padding(bottom = 6.dp), fontSize = 13.sp)
                                                CustomFlowRow(
                                                    mainAxisSpacing = 6.dp,
                                                    crossAxisSpacing = 6.dp,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    amenitiesOptions.forEach { opt ->
                                                        val isSel = selectedAmenitiesList.contains(opt)
                                                        FilterChip(
                                                            selected = isSel,
                                                            onClick = {
                                                                selectedAmenitiesList = if (isSel) {
                                                                    selectedAmenitiesList - opt
                                                                } else {
                                                                    selectedAmenitiesList + opt
                                                                }
                                                            },
                                                            label = { Text(opt, fontSize = 11.sp) }
                                                        )
                                                    }
                                                }
                                            }

                                            // Custom Text Amenities
                                            OutlinedTextField(
                                                value = customAmenitiesText,
                                                onValueChange = { customAmenitiesText = it },
                                                modifier = Modifier.fillMaxWidth().testTag("custom_amenities_input"),
                                                label = { Text("سایر امکانات دلخواه (با ویرگول جدا کنید)", fontSize = 12.sp) },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Step 3: Financial Specs
                            Text("اطلاعات مالی فایل *", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modes = listOf(
                                    "RENT_AND_MORTGAGE" to "رهن و اجاره",
                                    "FULL_MORTGAGE" to "رهن کامل",
                                    "FULL_RENT" to "اجاره کامل"
                                )
                                modes.forEach { (m, label) ->
                                    val isSel = financialMode == m
                                    Button(
                                        onClick = { financialMode = m },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (financialMode == "RENT_AND_MORTGAGE" || financialMode == "FULL_MORTGAGE") {
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = depositAmount,
                                            onValueChange = { depositAmount = it },
                                            modifier = Modifier.fillMaxWidth().testTag("deposit_input"),
                                            label = { Text(if (financialMode == "FULL_MORTGAGE") "مبلغ رهن کامل (تومان) *" else "مبلغ رهن (تومان) *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (depositAmount.isNotEmpty()) {
                                            val depLong = depositAmount.toLongOrNull()
                                            val wordsText = if (depLong != null) PersianUtils.numberToPersianWords(depLong) + " تومان" else ""
                                            Column {
                                                Text(
                                                    text = "معادل: " + PersianUtils.formatPrice(depositAmount.toDoubleOrNull()),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                )
                                                if (wordsText.isNotEmpty()) {
                                                    Text(
                                                        text = "($wordsText)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (financialMode == "RENT_AND_MORTGAGE" || financialMode == "FULL_RENT") {
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = rentAmount,
                                            onValueChange = { rentAmount = it },
                                            modifier = Modifier.fillMaxWidth().testTag("rent_input"),
                                            label = { Text(if (financialMode == "FULL_RENT") "مبلغ اجاره کامل (تومان) *" else "مبلغ اجاره (تومان) *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (rentAmount.isNotEmpty()) {
                                            val rentLong = rentAmount.toLongOrNull()
                                            val wordsText = if (rentLong != null) PersianUtils.numberToPersianWords(rentLong) + " تومان" else ""
                                            Column {
                                                Text(
                                                    text = "معادل: " + PersianUtils.formatPrice(rentAmount.toDoubleOrNull()),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                )
                                                if (wordsText.isNotEmpty()) {
                                                    Text(
                                                        text = "($wordsText)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Step 4: Owner specs, images, description
                            // Owner Info Card (محرمانه)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("🔒 اطلاعات مالک (فقط جهت بایگانی آژانس)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    
                                    OutlinedTextField(
                                        value = ownerName,
                                        onValueChange = { ownerName = it },
                                        modifier = Modifier.fillMaxWidth().testTag("owner_name_input"),
                                        label = { Text("نام مالک") },
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = ownerPhone,
                                        onValueChange = { ownerPhone = it },
                                        modifier = Modifier.fillMaxWidth().testTag("owner_phone_input"),
                                        label = { Text("شماره تماس مالک") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("فایل متعلق به بنگاه دیگری است؟")
                                        Switch(
                                            checked = isCollaborative,
                                            onCheckedChange = { isCollaborative = it }
                                        )
                                    }


                                }
                            }

                            // Description
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("description_input"),
                                label = { Text("توضیحات و نکات تکمیلی") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))

                // Step Navigation Row beautifully separated with divider and margin!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("مرحله قبلی", fontSize = 13.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(0.5f))
                        }

                        Button(
                            onClick = {
                                // Validate each step before moving forward
                                if (currentStep == 1) {
                                    if (region.trim().isEmpty() || area.trim().isEmpty() || bedrooms.trim().isEmpty()) {
                                        Toast.makeText(context, "لطفاً موارد ستاره‌دار را تکمیل کنید.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val doubleArea = area.toDoubleOrNull()
                                    val intBedrooms = bedrooms.toIntOrNull()
                                    if (doubleArea == null || intBedrooms == null) {
                                        Toast.makeText(context, "متراژ و تعداد خواب باید عدد معتبر باشند.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    currentStep++
                                } else if (currentStep == 2) {
                                    currentStep++
                                } else if (currentStep == 3) {
                                    val depVal = depositAmount.toDoubleOrNull()
                                    val rentVal = rentAmount.toDoubleOrNull()

                                    if (financialMode == "RENT_AND_MORTGAGE" && depVal == null && rentVal == null) {
                                        Toast.makeText(context, "لطفاً حداقل یکی از مقادیر رهن یا اجاره را وارد کنید.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (financialMode == "FULL_MORTGAGE" && depVal == null) {
                                        Toast.makeText(context, "لطفاً مقدار رهن کامل را وارد کنید.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (financialMode == "FULL_RENT" && rentVal == null) {
                                        Toast.makeText(context, "لطفاً مقدار اجاره کامل را وارد کنید.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    currentStep++
                                } else {
                                    // Save execution on final step
                                    val doubleArea = area.toDoubleOrNull() ?: 0.0
                                    val intBedrooms = bedrooms.toIntOrNull() ?: 0
                                    val depVal = depositAmount.toDoubleOrNull()
                                    val rentVal = rentAmount.toDoubleOrNull()

                                    // Generate or keep code
                                    viewModel.getNextPropertyCode { nextCode ->
                                        val finalCode = property?.code ?: nextCode

                                        // Combine selected amenities and custom amenities if shown
                                        val allAmenities = if (showOptionalAmenities) {
                                            (selectedAmenitiesList + customAmenitiesText.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }).joinToString(", ")
                                        } else {
                                            ""
                                        }

                                        val finalProperty = Property(
                                            id = property?.id ?: 0L,
                                            code = finalCode,
                                            type = type,
                                            region = region,
                                            area = doubleArea,
                                            bedrooms = intBedrooms,
                                            description = description,
                                            totalFloors = totalFloors.toIntOrNull(),
                                            unitFloor = unitFloor.toIntOrNull(),
                                            hasParking = if (showOptionalAmenities) hasParking else true,
                                            cabinetType = if (showOptionalAmenities) cabinetType else "MDF",
                                            otherAmenities = allAmenities,
                                            financialMode = financialMode,
                                            depositAmount = depVal,
                                            rentAmount = rentVal,
                                            imagesString = selectedImageUrlList.joinToString(","),
                                            isCollaborative = isCollaborative,
                                            ownerName = ownerName,
                                            ownerPhone = ownerPhone,
                                            lastContactDate = property?.lastContactDate ?: System.currentTimeMillis(),
                                            followUpStatus = "",
                                            status = property?.status ?: "ACTIVE",
                                            isSpecial = property?.isSpecial ?: false,
                                            createdAt = property?.createdAt ?: System.currentTimeMillis()
                                        )

                                        onSave(finalProperty)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(9f)
                                .height(42.dp)
                                .testTag("save_property_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (currentStep < 4) "ادامه و گام بعدی" else "ذخیره فایل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        if (currentStep == 1) {
                            Spacer(modifier = Modifier.weight(0.5f))
                        }
                    }
                }
            }
        }
}

@Composable
fun BackupRestoreDialog(
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    var agencyName by remember { mutableStateOf(prefs.getString("agency_name", "") ?: "") }
    var importText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ تنظیمات و پشتیبان‌گیری",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Agency Name Setting
                Text("تنظیمات آژانس املاک:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = agencyName,
                    onValueChange = {
                        agencyName = it
                        prefs.edit().putString("agency_name", it).apply()
                    },
                    label = { Text("نام بنگاه / آژانس (اختیاری)") },
                    placeholder = { Text("مثال: املاک پارس", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Text(
                    text = "در صورت ثبت، نام بنگاه شما به‌طور خودکار در پرامپت‌های تولیدی درج می‌شود.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Divider()

                Text(
                    text = "💾 پشتیبان‌گیری و بازیابی آفلاین",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "با استفاده از این بخش می‌توانید تمام اطلاعات ثبت‌شده ملکی را صادر کرده و کپی بگیرید یا از فایل پشتیبان کپی شده بازیابی کنید.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                // Export
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تهیه پشتیبان (کپی کد پشتیبان)")
                }

                Divider()

                // Import
                Text("بازیابی اطلاعات:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("کد پشتیبان را در اینجا قرار دهید (Paste)...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (importText.isNotEmpty()) onImport(importText) },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = importText.isNotEmpty()
                    ) {
                        Text("بازیابی پشتیبان")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("انصراف")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛠️ بخش تنظیمات ساخته شده توسط مصطفی مدبری",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// Custom flow row layout helper
@Composable
fun CustomFlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        
        var xPosition = 0
        var yPosition = 0
        var rowHeight = 0
        
        val positions = mutableListOf<Pair<Int, Int>>()
        
        placeables.forEach { placeable ->
            if (xPosition + placeable.width > layoutWidth) {
                xPosition = 0
                yPosition += rowHeight + crossAxisSpacing.roundToPx()
                rowHeight = 0
            }
            positions.add(xPosition to yPosition)
            xPosition += placeable.width + mainAxisSpacing.roundToPx()
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        
        layout(
            width = layoutWidth,
            height = if (positions.isEmpty()) 0 else yPosition + rowHeight
        ) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.placeRelative(x, y)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPromptGeneratorDialog(
    property: Property,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    val agencyName = remember { prefs.getString("agency_name", "") ?: "" }
    
    var selectedTemplateIndex by remember { mutableStateOf(0) }
    var selectedTone by remember { mutableStateOf("حرفه‌ای") }
    var selectedVersionsCount by remember { mutableStateOf(1) }
    
    var generatedPrompt by remember {
        mutableStateOf(
            prefs.getString("last_prompt_${property.id}", "") ?: ""
        )
    }

    val templates = listOf(
        "استوری رسمی",
        "استوری فروش فوری",
        "استوری لوکس",
        "استوری کوتاه",
        "استوری چند اسلایدی",
        "کپشن اینستاگرام",
        "متن تبلیغاتی تلگرام",
        "متن واتساپ",
        "متن آگهی حرفه‌ای",
        "متن خلاقانه و جذاب"
    )
    
    val tones = listOf("رسمی", "دوستانه", "لوکس", "حرفه‌ای", "هیجانی", "فروش فوری")
    val versionsOptions = listOf(1, 3, 5, 10)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
                TopAppBar(
                    title = { Text("تولید پرامپت هوشمند AI 🧠", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "با استفاده از این بخش می‌توانید اطلاعات کامل ملک را به یک پرامپت تخصصی و مهندسی‌شده تبدیل کنید تا با کپی و قرار دادن آن در چت‌بات‌های هوش مصنوعی (Gemini، ChatGPT و...) بهترین متن تبلیغاتی را تولید کنید.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    // Template selector
                    Text("1. نوع قالب و خروجی آگهی:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    CustomFlowRow(
                        mainAxisSpacing = 6.dp,
                        crossAxisSpacing = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        templates.forEachIndexed { index, name ->
                            val isSel = selectedTemplateIndex == index
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedTemplateIndex = index },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Tone selector
                    Text("۲. لحن متن تولیدی:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    CustomFlowRow(
                        mainAxisSpacing = 6.dp,
                        crossAxisSpacing = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tones.forEach { name ->
                            val isSel = selectedTone == name
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedTone = name },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Versions selector
                    Text("۳. تعداد نسخه‌های پیشنهادی:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        versionsOptions.forEach { count ->
                            val isSel = selectedVersionsCount == count
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedVersionsCount = count },
                                label = { Text("$count نسخه", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            val promptText = generatePromptText(
                                property = property,
                                agencyName = agencyName,
                                templateName = templates[selectedTemplateIndex],
                                tone = selectedTone,
                                versionsCount = selectedVersionsCount
                            )
                            generatedPrompt = promptText
                            prefs.edit().putString("last_prompt_${property.id}", promptText).apply()
                            Toast.makeText(context, "پرامپت هوشمند تولید شد! ✨", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تولید پرامپت اختصاصی", fontWeight = FontWeight.Bold)
                    }

                    // Generated Output Display
                    if (generatedPrompt.isNotEmpty()) {
                        Divider()
                        
                        Text("📋 پرامپت آماده کپی:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = generatedPrompt,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = {
                                        PersianUtils.copyToClipboard(context, generatedPrompt, "MelkUp AI Prompt")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("کپی پرامپت", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Web Bot Quick Launch Panel
                        Text("🚀 کپی کردی؟ ارسال مستقیم به چت‌بات‌ها:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val bots = listOf(
                                "Gemini" to "https://gemini.google.com",
                                "ChatGPT" to "https://chatgpt.com",
                                "Claude" to "https://claude.ai"
                            )
                            bots.forEach { (name, url) ->
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generatePromptText(
    property: Property,
    agencyName: String,
    templateName: String,
    tone: String,
    versionsCount: Int
): String {
    val detailsBuilder = StringBuilder()
    detailsBuilder.append("اطلاعات زیر مربوط به یک فایل ملکی است:\n\n")
    if (agencyName.isNotEmpty()) {
        detailsBuilder.append("نام بنگاه: $agencyName\n")
    }
    detailsBuilder.append("نوع ملک: ${property.type}\n")
    detailsBuilder.append("منطقه: ${property.region}\n")
    detailsBuilder.append("متراژ: ${PersianUtils.formatArea(property.area)} متر مربع\n")
    detailsBuilder.append("تعداد خواب: ${PersianUtils.formatNumber(property.bedrooms)}\n")
    detailsBuilder.append("پارکینگ: ${if (property.hasParking) "دارد" else "ندارد"}\n")
    if (property.cabinetType.isNotEmpty()) {
        detailsBuilder.append("نوع کابینت: ${property.cabinetType}\n")
    }
    if (property.otherAmenities.isNotEmpty()) {
        detailsBuilder.append("امکانات ملک: ${property.otherAmenities}\n")
    }
    if (property.description.trim().isNotEmpty()) {
        detailsBuilder.append("توضیحات: ${property.description.trim()}\n")
    }
    
    val priceText = when (property.financialMode) {
        "RENT_AND_MORTGAGE" -> {
            val depText = if (property.depositAmount != null && property.depositAmount > 0) "${PersianUtils.formatPrice(property.depositAmount)} رهن" else ""
            val rentText = if (property.rentAmount != null && property.rentAmount > 0) "${PersianUtils.formatPrice(property.rentAmount)} اجاره" else ""
            if (depText.isNotEmpty() && rentText.isNotEmpty()) "$depText و $rentText"
            else if (depText.isNotEmpty()) "$depText کامل"
            else if (rentText.isNotEmpty()) "$rentText کامل"
            else "توافقی"
        }
        "FULL_MORTGAGE" -> "${PersianUtils.formatPrice(property.depositAmount)} رهن کامل"
        "FULL_RENT" -> "${PersianUtils.formatPrice(property.rentAmount)} اجاره کامل"
        else -> "توافقی"
    }
    detailsBuilder.append("قیمت: $priceText\n")
    detailsBuilder.append("کد فایل: ${property.code}\n\n")

    val instructions = when (templateName) {
        "استوری رسمی" -> "لطفاً یک استوری رسمی و حرفه‌ای برای معرفی این ملک تولید کن. متن کوتاه، مؤدبانه و برای معرفی رسمی ملک باشد. در پایان کد فایل را نمایش بده."
        "استوری فروش فوری" -> "لطفاً یک استوری با لحن فوری، جذاب و ترغیب‌کننده برای جذب مشتری سریع بنویس. متن حس فرصت طلایی و فوری بودن را القا کند. در پایان کد فایل را نمایش بده."
        "استوری لوکس" -> "لطفاً یک استوری با لحن مجلل، لوکس و توصیفات شیک ویژه معرفی املاک گران‌قیمت تولید کن. در پایان کد فایل را نمایش بده."
        "استوری کوتاه" -> "لطفاً یک متن استوری بسیار کوتاه (حداکثر ۲ الی ۳ خط) برای انتشار خیلی سریع و مؤثر معرفی این ملک تولید کن. در پایان کد فایل را نمایش بده."
        "استوری چند اسلایدی" -> "لطفاً یک متن سناریو برای یک استوری چند اسلایدی (بین ۳ تا ۵ اسلاید متوالی) تولید کن که اطلاعات ملک را جذاب بخش‌بندی کند. در پایان کد فایل را نمایش بده."
        "کپشن اینستاگرام" -> "لطفاً یک کپشن کامل اینستاگرام همراه با ایموجی‌های مرتبط و هشتگ‌های پرکاربرد و پربازدید املاک برای این فایل تولید کن. در پایان کد فایل را نمایش بده."
        "متن تبلیغاتی تلگرام" -> "لطفاً یک متن تبلیغاتی منظم، زیبا و ساختاریافته مناسب کانال‌های تلگرامی املاک بنویس که با علائم بصری خوانایی بالایی داشته باشد. در پایان کد فایل را نمایش بده."
        "متن واتساپ" -> "لطفاً یک متن صمیمانه، کوتاه و دعوتی برای ارسال به مشتریان واتساپ تولید کن که آنها را مشتاق به بازدید ملک کند. در پایان کد فایل را نمایش بده."
        "متن آگهی حرفه‌ای" -> "لطفاً یک متن کامل، توصیفی، اصولی و حرفه‌ای برای درج در آگهی‌های پلتفرم‌های ملکی بنویس. در پایان کد فایل را نمایش بده."
        "متن خلاقانه و جذاب" -> "لطفاً یک متن تبلیغاتی فوق‌العاده خلاقانه با رویکرد بازاریابی نوین، داستانی یا غافلگیرکننده بنویس که حداکثر مخاطب را جذب کند. در پایان کد فایل را نمایش بده."
        else -> "لطفاً یک متن آگهی تبلیغاتی بنویس. در پایان کد فایل را نمایش بده."
    }

    detailsBuilder.append("$instructions\n")
    detailsBuilder.append("لحن درخواستی: $tone\n")
    if (versionsCount > 1) {
        detailsBuilder.append("لطفاً $versionsCount نسخه متفاوت با عناوین مشخص (مثلاً «نسخه ۱»، «نسخه ۲» و...) تولید کن تا انتخاب‌های متعددی داشته باشم.")
    }
    
    return detailsBuilder.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPromptGeneratorScreen(
    properties: List<Property>,
    context: android.content.Context
) {
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    val agencyName = remember { prefs.getString("agency_name", "") ?: "" }
    
    var selectedProperty by remember { mutableStateOf<Property?>(properties.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }
    
    var selectedTemplateIndex by remember { mutableStateOf(0) }
    var selectedTone by remember { mutableStateOf("حرفه‌ای") }
    var selectedVersionsCount by remember { mutableStateOf(1) }
    
    var currentStep by remember { mutableStateOf(1) }
    
    var generatedPrompt by remember {
        mutableStateOf(
            selectedProperty?.let { prefs.getString("last_prompt_${it.id}", "") } ?: ""
        )
    }

    LaunchedEffect(selectedProperty) {
        generatedPrompt = selectedProperty?.let { prefs.getString("last_prompt_${it.id}", "") } ?: ""
    }

    val templates = listOf(
        "استوری رسمی",
        "استوری فروش فوری",
        "استوری لوکس",
        "استوری کوتاه",
        "استوری چند اسلایدی",
        "کپشن اینستاگرام",
        "متن تبلیغاتی تلگرام",
        "متن واتساپ",
        "متن آگهی حرفه‌ای",
        "متن خلاقانه و جذاب"
    )
    
    val tones = listOf("رسمی", "دوستانه", "لوکس", "حرفه‌ای", "هیجانی", "فروش فوری")
    val versionsOptions = listOf(1, 3, 5, 10)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ماژول تولید پرامپت هوشمند AI 🧠",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "با استفاده از این بخش می‌توانید اطلاعات کامل هر کدام از فایل‌های ملکی را به یک پرامپت تخصصی و مهندسی‌شده تبدیل کنید تا با کپی و قرار دادن آن در چت‌بات‌های هوش مصنوعی (Gemini، ChatGPT و...) بهترین متن تبلیغاتی و استوری را تولید کنید.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        // Step indicator row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepCircle(stepNumber = 1, isActive = currentStep >= 1, isCompleted = currentStep > 1, title = "انتخاب فایل")
            StepLine(isActive = currentStep > 1)
            StepCircle(stepNumber = 2, isActive = currentStep >= 2, isCompleted = currentStep > 2, title = "انتخاب قالب")
            StepLine(isActive = currentStep > 2)
            StepCircle(stepNumber = 3, isActive = currentStep >= 3, isCompleted = currentStep > 3, title = "تنظیمات نهایی")
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (properties.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "هیچ فایل ملکی در سیستم ثبت نشده است. ابتدا یک فایل ملکی ثبت کنید تا بتوانید برای آن پرامپت تولید کنید.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            when (currentStep) {
                1 -> {
                    Text("👇 مرحله ۱: انتخاب یک فایل ملکی از لیست:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedProperty?.let { "فایل ${it.code} (${it.type} - منطقه ${it.region})" } ?: "انتخاب فایل ملکی...",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 300.dp)
                        ) {
                            properties.forEach { prop ->
                                DropdownMenuItem(
                                    text = { Text("کد ${prop.code} - ${prop.type} (${prop.region})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    onClick = {
                                        selectedProperty = prop
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    selectedProperty?.let { prop ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📍 جزئیات فایل انتخاب شده:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("نوع: ${prop.type}", fontSize = 11.sp)
                                    Text("منطقه: ${prop.region}", fontSize = 11.sp)
                                    Text("متراژ: ${PersianUtils.formatArea(prop.area)} متر", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { currentStep = 2 },
                            shape = RoundedCornerShape(10.dp),
                            enabled = selectedProperty != null
                        ) {
                            Text("مرحله بعدی")
                        }
                    }
                }
                2 -> {
                    Text("👇 مرحله ۲: قالب و خروجی آگهی مورد نظر را انتخاب کنید:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    var expandedTemplateDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedTemplateDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = templates[selectedTemplateIndex],
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedTemplateDropdown,
                            onDismissRequest = { expandedTemplateDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            templates.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    onClick = {
                                        selectedTemplateIndex = index
                                        expandedTemplateDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 1 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("قبلی")
                        }
                        Button(
                            onClick = { currentStep = 3 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("مرحله بعدی")
                        }
                    }
                }
                3 -> {
                    Text("👇 مرحله ۳: لحن متن و تعداد نسخه‌های پیشنهادی را تعیین کنید:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    
                    Text("لحن متن تولیدی:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var expandedToneDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedToneDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTone,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedToneDropdown,
                            onDismissRequest = { expandedToneDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            tones.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    onClick = {
                                        selectedTone = name
                                        expandedToneDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Text("تعداد نسخه‌های پیشنهادی:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var expandedVersionsDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedVersionsDropdown = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$selectedVersionsCount نسخه",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedVersionsDropdown,
                            onDismissRequest = { expandedVersionsDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            versionsOptions.forEach { count ->
                                DropdownMenuItem(
                                    text = { Text("$count نسخه", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    onClick = {
                                        selectedVersionsCount = count
                                        expandedVersionsDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 2 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("قبلی")
                        }
                        
                        Button(
                            onClick = {
                                selectedProperty?.let { prop ->
                                    val promptText = generatePromptText(
                                        property = prop,
                                        agencyName = agencyName,
                                        templateName = templates[selectedTemplateIndex],
                                        tone = selectedTone,
                                        versionsCount = selectedVersionsCount
                                    )
                                    generatedPrompt = promptText
                                    prefs.edit().putString("last_prompt_${prop.id}", promptText).apply()
                                    Toast.makeText(context, "پرامپت هوشمند تولید شد! ✨", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تولید پرامپت هوشمند", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (generatedPrompt.isNotEmpty()) {
                Divider()
                
                Text("📋 پرامپت آماده کپی:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = generatedPrompt,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                PersianUtils.copyToClipboard(context, generatedPrompt, "MelkUp AI Prompt")
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("کپی پرامپت در کلیپ‌برد", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("🚀 کپی کردی؟ ارسال مستقیم به چت‌بات‌ها:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val bots = listOf(
                        "Gemini" to "https://gemini.google.com",
                        "ChatGPT" to "https://chatgpt.com",
                        "Claude" to "https://claude.ai"
                    )
                    bots.forEach { (name, url) ->
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepCircle(stepNumber: Int, isActive: Boolean, isCompleted: Boolean, title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) MaterialTheme.colorScheme.primary
                    else if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = 1.5.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StepLine(isActive: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(2.dp)
            .background(
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

@Composable
fun SettingsAndBackupScreen(
    onExport: () -> Unit,
    onImport: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    var agencyName by remember { mutableStateOf(prefs.getString("agency_name", "") ?: "") }
    var copyFormat by remember { mutableStateOf(prefs.getString("copy_format", "standard") ?: "standard") }
    var importText by remember { mutableStateOf("") }

    var compactShowType by remember { mutableStateOf(prefs.getBoolean("compact_show_type", true)) }
    var compactShowRegion by remember { mutableStateOf(prefs.getBoolean("compact_show_region", true)) }
    var compactShowArea by remember { mutableStateOf(prefs.getBoolean("compact_show_area", true)) }
    var compactShowBedrooms by remember { mutableStateOf(prefs.getBoolean("compact_show_bedrooms", true)) }
    var compactShowPrice by remember { mutableStateOf(prefs.getBoolean("compact_show_price", true)) }
    var compactShowCode by remember { mutableStateOf(prefs.getBoolean("compact_show_code", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "تنظیمات و پشتیبان‌گیری",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "در این بخش می‌توانید اطلاعات نام آژانس املاک خود را تنظیم کنید یا از اطلاعات ثبت شده ملکی نسخه پشتیبان تهیه و بازیابی کنید.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Text("تنظیمات آژانس املاک:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = agencyName,
            onValueChange = {
                agencyName = it
                prefs.edit().putString("agency_name", it).apply()
            },
            label = { Text("نام بنگاه / آژانس (اختیاری)") },
            placeholder = { Text("مثال: املاک پارس", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        Text(
            text = "در صورت ثبت، نام بنگاه شما به‌طور خودکار در پرامپت‌های تولیدی درج می‌شود.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Text("📋 فرمت کپی اطلاعات ملک:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formats = listOf(
                "standard" to "استاندارد 🌟",
                "plain" to "متن ساده 📄",
                "compact" to "خلاصه تک‌خطی ⚡"
            )
            formats.forEach { (formatKey, formatLabel) ->
                val isSelected = copyFormat == formatKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .clickable {
                            copyFormat = formatKey
                            prefs.edit().putString("copy_format", formatKey).apply()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = "فرمت انتخاب‌شده برای کپی‌های تک فایل و کپی‌های گروهی اعمال خواهد شد.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (copyFormat == "compact") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚙️ فیلدهای فعال در کپی خلاصه تک‌خطی:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val items = listOf(
                        Triple("compact_show_type", "نوع ملک", compactShowType) to { v: Boolean -> compactShowType = v },
                        Triple("compact_show_region", "منطقه", compactShowRegion) to { v: Boolean -> compactShowRegion = v },
                        Triple("compact_show_area", "متراژ", compactShowArea) to { v: Boolean -> compactShowArea = v },
                        Triple("compact_show_bedrooms", "خواب", compactShowBedrooms) to { v: Boolean -> compactShowBedrooms = v },
                        Triple("compact_show_price", "مبلغ / رهن و اجاره", compactShowPrice) to { v: Boolean -> compactShowPrice = v },
                        Triple("compact_show_code", "کد فایل", compactShowCode) to { v: Boolean -> compactShowCode = v }
                    )
                    
                    val chunks = items.chunked(2)
                    chunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (triple, setter) ->
                                val (key, label, value) = triple
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val newVal = !value
                                            setter(newVal)
                                            prefs.edit().putBoolean(key, newVal).apply()
                                        }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Checkbox(
                                        checked = value,
                                        onCheckedChange = { newVal ->
                                            setter(newVal)
                                            prefs.edit().putBoolean(key, newVal).apply()
                                        }
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Text(
            text = "💾 پشتیبان‌گیری و بازیابی آفلاین",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "با استفاده از این بخش می‌توانید تمام اطلاعات ثبت‌شده ملکی را صادر کرده و کپی بگیرید یا از فایل پشتیبان کپی شده بازیابی کنید.",
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.ContentCopy, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تهیه پشتیبان (کپی کد پشتیبان)", fontWeight = FontWeight.Bold)
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Text("بازیابی اطلاعات:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            placeholder = { Text("کد پشتیبان را در اینجا قرار دهید (Paste)...", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = { if (importText.isNotEmpty()) onImport(importText) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            enabled = importText.isNotEmpty()
        ) {
            Text("بازیابی پشتیبان", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛠️ ساخته شده توسط مصطفی مدبری",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FloatingNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "bg_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        label = "content_color"
    )

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
                AnimatedVisibility(
                    visible = selected,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

