package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.util.LicenseManager
import com.example.util.LicenseStatus
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.clickable

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
    var showShareFileNameDialog by remember { mutableStateOf(false) }
    var sharePropertiesList by remember { mutableStateOf<List<Property>>(emptyList()) }
    var propertyToEdit by remember { mutableStateOf<Property?>(null) }
    var propertyToView by remember { mutableStateOf<Property?>(null) }

    // Full screen image viewer state
    var fullScreenImageUrls by remember { mutableStateOf<List<String>?>(null) }
    var fullScreenImageInitialIndex by remember { mutableStateOf(0) }

    var showCustomizationDialog by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf("home") }
    var isCompactView by remember { mutableStateOf(false) }

    var licenseStatus by remember { mutableStateOf(LicenseManager.checkLicenseStatus(context)) }
    var showActivationLockDialog by remember { mutableStateOf(false) }

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
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.5.dp, Color(0xFF7C3AED), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logo_minimal),
                                contentDescription = "لوگو ملک آپ",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ملک آپ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 22.sp,
                                lineHeight = 22.sp,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Horizontal Add File button, visible only in home & all_files
                if (currentTab == "home" || currentTab == "all_files") {
                    Button(
                        onClick = {
                            if (!licenseStatus.isValid && rawProperties.size >= 3) {
                                showActivationLockDialog = true
                            } else {
                                propertyToEdit = null // Fresh property
                                showAddEditDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(46.dp)
                            .testTag("add_property_horizontal_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "افزودن فایل جدید",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
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
            }
        },
        floatingActionButton = {}
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val sortFields = listOf(
                                    "DATE" to "تاریخ ثبت",
                                    "CODE" to "کد یکتا",
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
                                            .padding(horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
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
                                    },
                                    onShare = {
                                        sharePropertiesList = listOf(property)
                                        showShareFileNameDialog = true
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
                                            text = "مشاهده فایل‌های بیشتر",
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
                                    val text = PersianUtils.generateGroupSummaryOutput(context, selectedProps)
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
                            },
                            onShareSelected = {
                                val selectedProps = properties.filter { selectedPropertyIds.contains(it.id) }
                                if (selectedProps.isEmpty()) {
                                    Toast.makeText(context, "هیچ فایلی انتخاب نشده است", Toast.LENGTH_SHORT).show()
                                } else {
                                    sharePropertiesList = selectedProps
                                    showShareFileNameDialog = true
                                }
                            }
                        )
                    }

                    // --- QUICK SORT PANEL FOR ALL FILES ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ascending/Descending Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .clickable { viewModel.sortAscending.value = !sortAscending }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
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

                        // Horizontal list of fields
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sortFields = listOf(
                                "DATE" to "تاریخ ثبت",
                                "CODE" to "کد یکتا",
                                "AREA" to "متراژ",
                                "BEDROOMS" to "خواب",
                                "REGION" to "منطقه"
                            )
                            sortFields.forEach { (field, label) ->
                                val isSel = sortBy == field
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.sortBy.value = field }
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quick statistics / Export All section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "نمایش ${PersianUtils.formatNumber(properties.size)} فایل",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )

                            var viewMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { viewMenuExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (isCompactView) "مشاهده خلاصه" else "مشاهده کارت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = viewMenuExpanded,
                                    onDismissRequest = { viewMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("مشاهده خلاصه", fontSize = 12.sp) },
                                        onClick = {
                                            isCompactView = true
                                            viewMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.ViewList, null, modifier = Modifier.size(16.dp)) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("مشاهده کارت", fontSize = 12.sp) },
                                        onClick = {
                                            isCompactView = false
                                            viewMenuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.GridView, null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }

                        if (rawProperties.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val text = PersianUtils.generateGroupSummaryOutput(context, rawProperties)
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
                                if (isCompactView) {
                                    CompactPropertyItem(
                                        property = property,
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
                                        isSelectionMode = isSelectionMode,
                                        onDelete = {
                                            viewModel.deleteProperty(property)
                                            Toast.makeText(context, "فایل حذف شد", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
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
                                        },
                                        onShare = {
                                            sharePropertiesList = listOf(property)
                                            showShareFileNameDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "prompt_generator" -> {
                    if (!licenseStatus.isValid) {
                        LicenseLockScreen(
                            androidId = LicenseManager.getAndroidId(context),
                            onActivate = { key ->
                                when (LicenseManager.registerLicense(context, key)) {
                                    com.example.util.RegisterResult.Success -> {
                                        licenseStatus = LicenseManager.checkLicenseStatus(context)
                                        Toast.makeText(context, "برنامه با موفقیت فعال شد! 🎉", Toast.LENGTH_LONG).show()
                                    }
                                    com.example.util.RegisterResult.AlreadyUsed -> {
                                        Toast.makeText(context, "این کد فعال‌سازی قبلاً استفاده شده است و قابل استفاده مجدد نیست!", Toast.LENGTH_LONG).show()
                                    }
                                    com.example.util.RegisterResult.Invalid -> {
                                        Toast.makeText(context, "کد فعال‌سازی نامعتبر است!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else {
                        // --- AI PROMPT GENERATOR PAGE ---
                        AiPromptGeneratorScreen(
                            properties = rawProperties,
                            context = context
                        )
                    }
                }
                "settings" -> {
                    // --- SETTINGS PAGE ---
                    SettingsAndBackupScreen(
                        properties = rawProperties,
                        onExport = { selectedList ->
                            val json = viewModel.exportBackupJson(selectedList)
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
                        },
                        licenseStatus = licenseStatus,
                        onLicenseChanged = {
                            licenseStatus = LicenseManager.checkLicenseStatus(context)
                        },
                        propertiesCount = rawProperties.size,
                        onShowCustomization = {
                            showCustomizationDialog = true
                        },
                        onShowBackupDialog = {
                            showBackupDialog = true
                        }
                    )
                }
            }
        }
    }

    // --- DIALOGS & OVERLAYS ---

    if (showActivationLockDialog) {
        val androidId = LicenseManager.getAndroidId(context)
        var dialogKeyInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showActivationLockDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("محدودیت تعداد فایل‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "شما از ۳ فایل رایگان خود استفاده کرده‌اید. برای ثبت فایل‌های بیشتر و نامحدود، لایسنس خود را وارد کرده و فعال‌سازی کنید.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    
                    Text(
                        text = "کد کاربری دستگاه شما:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                PersianUtils.copyToClipboard(context, androidId, "کد کاربری")
                                Toast.makeText(context, "کد کاربری کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(androidId, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedTextField(
                        value = dialogKeyInput,
                        onValueChange = { dialogKeyInput = it },
                        label = { Text("کد فعال‌سازی (لایسنس)") },
                        placeholder = { Text("مثال: ZZZ1-ABCD") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (LicenseManager.registerLicense(context, dialogKeyInput)) {
                            com.example.util.RegisterResult.Success -> {
                                licenseStatus = LicenseManager.checkLicenseStatus(context)
                                showActivationLockDialog = false
                                Toast.makeText(context, "برنامه با موفقیت فعال شد! 🎉", Toast.LENGTH_LONG).show()
                            }
                            com.example.util.RegisterResult.AlreadyUsed -> {
                                Toast.makeText(context, "این کد فعال‌سازی قبلاً استفاده شده است و قابل استفاده مجدد نیست!", Toast.LENGTH_LONG).show()
                            }
                            com.example.util.RegisterResult.Invalid -> {
                                Toast.makeText(context, "کد فعال‌سازی نامعتبر است!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = dialogKeyInput.trim().isNotEmpty()
                ) {
                    Text("فعال‌سازی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivationLockDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

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
            },
            onImageClick = { imgUrls, index ->
                fullScreenImageUrls = imgUrls
                fullScreenImageInitialIndex = index
            }
        )
    }

    if (fullScreenImageUrls != null) {
        FullScreenImageViewer(
            images = fullScreenImageUrls!!,
            initialIndex = fullScreenImageInitialIndex,
            onClose = {
                fullScreenImageUrls = null
            }
        )
    }

    if (showCustomizationDialog) {
        CustomizationDialog(
            onDismiss = { showCustomizationDialog = false },
            context = context
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            properties = rawProperties,
            onDismiss = { showBackupDialog = false },
            onExport = { selectedList ->
                val json = viewModel.exportBackupJson(selectedList)
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
            },
            onGetBackupJson = { selectedList ->
                viewModel.exportBackupJson(selectedList)
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

    if (showShareFileNameDialog) {
        ShareFileNameDialog(
            properties = sharePropertiesList,
            onDismiss = { showShareFileNameDialog = false },
            onShareComplete = { showShareFileNameDialog = false }
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
                    "CODE" to "کد یکتا",
                    "AREA" to "متراژ",
                    "BEDROOMS" to "خواب",
                    "REGION" to "منطقه"
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sortOptions.forEach { (field, label) ->
                        val isSel = sortBy == field
                        FilterChip(
                            selected = isSel,
                            onClick = { onSortByChange(field) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
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
    onCopyFull: () -> Unit,
    onShareSelected: () -> Unit
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
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onShareSelected,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ارسال 📤", fontSize = 11.sp)
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
    onDelete: () -> Unit,
    onShare: () -> Unit
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
                        text = "شرایط پرداخت / قیمت",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PersianUtils.generatePropertyPriceText(property),
                        fontSize = 13.sp,
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
                                text = { Text("ارسال فایل (اشتراک)") },
                                onClick = {
                                    expandedMenu = false
                                    onShare()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
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
    onToggleSpecial: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> }
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
                                        modifier = Modifier
                                            .width(220.dp)
                                            .fillMaxHeight()
                                            .clickable { onImageClick(images, images.indexOf(imgUrl)) }
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
                                    if (property.totalFloors != null) SpecRow("کل طبقات", "${PersianUtils.formatNumber(property.totalFloors)} طبقه")
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

                                var financialShown = false
                                if (property.fullDepositAmount != null && property.fullDepositAmount > 0) {
                                    SpecRow("میزان رهن کامل", "${PersianUtils.formatPrice(property.fullDepositAmount)} تومان")
                                    financialShown = true
                                }
                                if ((property.depositAmount != null && property.depositAmount > 0) || (property.rentAmount != null && property.rentAmount > 0)) {
                                    if (property.depositAmount != null && property.depositAmount > 0) {
                                        SpecRow("میزان رهن", "${PersianUtils.formatPrice(property.depositAmount)} تومان")
                                    }
                                    if (property.rentAmount != null && property.rentAmount > 0) {
                                        SpecRow("میزان اجاره", "${PersianUtils.formatPrice(property.rentAmount)} تومان")
                                    }
                                    financialShown = true
                                }
                                if (property.fullRentAmount != null && property.fullRentAmount > 0) {
                                    SpecRow("میزان اجاره کامل", "${PersianUtils.formatPrice(property.fullRentAmount)} تومان")
                                    financialShown = true
                                }
                                if (!financialShown) {
                                    SpecRow("مبلغ", "توافقی")
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
    var code by remember { mutableStateOf(property?.code ?: "") }
    var region by remember { mutableStateOf(property?.region ?: "") }
    var area by remember { mutableStateOf(property?.area?.toString() ?: "") }
    var bedrooms by remember { mutableStateOf(property?.bedrooms?.toString() ?: "2") }
    var description by remember { mutableStateOf(property?.description ?: "") }
    
    // Auto populate next code if empty (Requirement 6)
    val rawProperties by viewModel.allProperties.collectAsStateWithLifecycle()
    
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
    var fullDepositAmount by remember { mutableStateOf(property?.fullDepositAmount?.toLong()?.toString() ?: "") }
    var fullRentAmount by remember { mutableStateOf(property?.fullRentAmount?.toLong()?.toString() ?: "") }

    // Internal Agency Details
    var isCollaborative by remember { mutableStateOf(property?.isCollaborative ?: false) }
    var ownerName by remember { mutableStateOf(property?.ownerName ?: "") }
    var ownerPhone by remember { mutableStateOf(property?.ownerPhone ?: "") }
    var followUpStatus by remember { mutableStateOf(property?.followUpStatus ?: "تماس گرفته شد") }
    
    // Pictures
    var selectedImageUrlList by remember {
        mutableStateOf(
            property?.getImagesList() ?: emptyList()
        )
    }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Helpers for camera/gallery storage
    val context = LocalContext.current
    
    fun createCameraUri(ctx: Context): Uri {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val fileName = "JPEG_${timeStamp}_"
        val storageDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = java.io.File.createTempFile(fileName, ".jpg", storageDir)
        return androidx.core.content.FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            file
        )
    }

    fun copyUriToInternalStorage(ctx: Context, uri: Uri): String? {
        return try {
            val resolver = ctx.contentResolver
            val inputStream = resolver.openInputStream(uri) ?: return null
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US).format(java.util.Date())
            val fileName = "PropertyImg_${timeStamp}.jpg"
            val storageDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val file = java.io.File(storageDir, fileName)
            
            val outputStream = java.io.FileOutputStream(file)
            val buffer = ByteArray(4 * 1024)
            var bytesRead: Int
            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val copiedUris = uris.mapNotNull { uri ->
                copyUriToInternalStorage(context, uri)
            }
            selectedImageUrlList = selectedImageUrlList + copiedUris
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraTempUri?.let { uri ->
                val copiedUri = copyUriToInternalStorage(context, uri)
                if (copiedUri != null) {
                    selectedImageUrlList = selectedImageUrlList + copiedUri
                }
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val uri = createCameraUri(context)
                cameraTempUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در آماده‌سازی دوربین", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "برای گرفتن عکس به دسترسی دوربین نیاز است", Toast.LENGTH_SHORT).show()
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState()
    val scope = rememberCoroutineScope()

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

                            // Unique Property Code Input (Requirement 6)
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.trim() },
                                modifier = Modifier.fillMaxWidth().testTag("property_code_input"),
                                label = { Text("کد فایل اختصاصی *") },
                                leadingIcon = { Icon(Icons.Default.QrCode, null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                supportingText = {
                                    Text(
                                        text = "کد نمی‌تواند تکراری باشد",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            )

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
                                        label = { Text("کل طبقات") },
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
                            Text("اطلاعات مالی فایل (امکان پر کردن چند حالت)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("شما می‌توانید یک یا چند حالت مالی زیر را پر کنید. در خروجی هر کدام که پر شده باشد نشان داده می‌شود:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            // 1. رهن و اجاره (Mortgage and Rent)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("۱. رهن و اجاره", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = depositAmount,
                                                onValueChange = { depositAmount = it },
                                                modifier = Modifier.fillMaxWidth().testTag("deposit_input"),
                                                label = { Text("مبلغ رهن (تومان)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            if (depositAmount.isNotEmpty()) {
                                                val depLong = depositAmount.toLongOrNull()
                                                val wordsText = if (depLong != null) PersianUtils.numberToPersianWords(depLong) + " تومان" else ""
                                                if (wordsText.isNotEmpty()) {
                                                    Text(
                                                        text = wordsText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = rentAmount,
                                                onValueChange = { rentAmount = it },
                                                modifier = Modifier.fillMaxWidth().testTag("rent_input"),
                                                label = { Text("مبلغ اجاره (تومان)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            if (rentAmount.isNotEmpty()) {
                                                val rentLong = rentAmount.toLongOrNull()
                                                val wordsText = if (rentLong != null) PersianUtils.numberToPersianWords(rentLong) + " تومان" else ""
                                                if (wordsText.isNotEmpty()) {
                                                    Text(
                                                        text = wordsText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. رهن کامل (Full Mortgage)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("۲. رهن کامل", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
                                    OutlinedTextField(
                                        value = fullDepositAmount,
                                        onValueChange = { fullDepositAmount = it },
                                        modifier = Modifier.fillMaxWidth().testTag("full_deposit_input"),
                                        label = { Text("مبلغ رهن کامل (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    if (fullDepositAmount.isNotEmpty()) {
                                        val valLong = fullDepositAmount.toLongOrNull()
                                        val wordsText = if (valLong != null) PersianUtils.numberToPersianWords(valLong) + " تومان" else ""
                                        if (wordsText.isNotEmpty()) {
                                            Text(
                                                text = wordsText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. اجاره کامل (Full Rent)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("۳. اجاره کامل", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
                                    OutlinedTextField(
                                        value = fullRentAmount,
                                        onValueChange = { fullRentAmount = it },
                                        modifier = Modifier.fillMaxWidth().testTag("full_rent_input"),
                                        label = { Text("مبلغ اجاره کامل (تومان)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    if (fullRentAmount.isNotEmpty()) {
                                        val valLong = fullRentAmount.toLongOrNull()
                                        val wordsText = if (valLong != null) PersianUtils.numberToPersianWords(valLong) + " تومان" else ""
                                        if (wordsText.isNotEmpty()) {
                                            Text(
                                                text = wordsText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                            )
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

                            // --- IMAGES SECTION (Requirement 2) ---
                            Text("📸 تصاویر ملک (انتخاب از گالری یا دوربین)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("انتخاب از گالری", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ثبت با دوربین", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Selected Image Preview Row
                            if (selectedImageUrlList.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(selectedImageUrlList) { path ->
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        ) {
                                            AsyncImage(
                                                model = if (path.startsWith("/")) java.io.File(path) else path,
                                                contentDescription = "تصویر ملک",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { selectedImageUrlList = selectedImageUrlList.filter { it != path } },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right side button: Step Prev ("گام قبل") (First element in RTL is on the Right)
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "گام قبل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Left side button: Step Next ("گام بعد" / "ذخیره فایل") (Second element in RTL is on the Left)
                    Button(
                        onClick = {
                            // Validate each step before moving forward
                            if (currentStep == 1) {
                                if (region.trim().isEmpty() || area.trim().isEmpty() || bedrooms.trim().isEmpty() || code.trim().isEmpty()) {
                                    Toast.makeText(context, "لطفاً موارد ستاره‌دار از جمله کد فایل را تکمیل کنید.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val doubleArea = area.toDoubleOrNull()
                                val intBedrooms = bedrooms.toIntOrNull()
                                if (doubleArea == null || intBedrooms == null) {
                                    Toast.makeText(context, "متراژ و تعداد خواب باید عدد معتبر باشند.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Duplicate code check (Requirement 6)
                                val isDuplicate = rawProperties.any { it.code == code.trim() && it.id != (property?.id ?: 0L) }
                                if (isDuplicate) {
                                    Toast.makeText(context, "کد فایل تکراری است. لطفاً کد دیگری وارد کنید.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                currentStep++
                            } else if (currentStep == 2) {
                                currentStep++
                            } else if (currentStep == 3) {
                                val depVal = depositAmount.toDoubleOrNull()
                                val rentVal = rentAmount.toDoubleOrNull()
                                val fullDepVal = fullDepositAmount.toDoubleOrNull()
                                val fullRentVal = fullRentAmount.toDoubleOrNull()

                                if (depVal == null && rentVal == null && fullDepVal == null && fullRentVal == null) {
                                    Toast.makeText(context, "لطفاً حداقل یکی از حالت‌های مالی را تکمیل کنید.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                currentStep++
                            } else {
                                // Save execution on final step
                                val doubleArea = area.toDoubleOrNull() ?: 0.0
                                val intBedrooms = bedrooms.toIntOrNull() ?: 0
                                val depVal = depositAmount.toDoubleOrNull()
                                val rentVal = rentAmount.toDoubleOrNull()
                                val fullDepVal = fullDepositAmount.toDoubleOrNull()
                                val fullRentVal = fullRentAmount.toDoubleOrNull()

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
                                    code = code.trim(), // Use the manually entered/edited code
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
                                    fullDepositAmount = fullDepVal,
                                    fullRentAmount = fullRentVal,
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
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_property_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentStep < 4) "گام بعد" else "ذخیره فایل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                }
            }
        }
    }

@Composable
fun BackupRestoreDialog(
    properties: List<Property>,
    onDismiss: () -> Unit,
    onExport: (List<Property>) -> Unit,
    onImport: (String) -> Unit,
    onGetBackupJson: (List<Property>) -> String
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var selectedForBackup by remember(properties) { mutableStateOf(properties.toSet()) }

    var backupJsonToSave by remember { mutableStateOf("") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupJsonToSave.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "فایل پشتیبان با موفقیت ذخیره شد.", Toast.LENGTH_LONG).show()
            } catch (e: java.lang.Exception) {
                Toast.makeText(context, "خطا در ذخیره فایل پشتیبان: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (!content.isNullOrEmpty()) {
                    importText = content
                    Toast.makeText(context, "فایل پشتیبان بارگذاری شد. لطفاً دکمه بازیابی را بزنید.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فایل انتخابی خالی است.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: java.lang.Exception) {
                Toast.makeText(context, "خطا در خواندن فایل: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
                    text = "💾 پشتیبان‌گیری و بازیابی آفلاین",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "با استفاده از این بخش می‌توانید اطلاعات موارد دلخواه را صادر کرده و کپی بگیرید یا از فایل پشتیبان کپی شده بازیابی کنید.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = "انتخاب فایل‌ها برای پشتیبان‌گیری:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Select All / Deselect All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { selectedForBackup = properties.toSet() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("انتخاب همه", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { selectedForBackup = emptySet() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("حذف انتخاب‌ها", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "انتخاب شده: ${selectedForBackup.size} از ${properties.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Scrollable container for properties with checkboxes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                ) {
                    if (properties.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هیچ فایلی برای پشتیبان‌گیری وجود ندارد.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(4.dp)
                        ) {
                            properties.forEach { p ->
                                val isChecked = selectedForBackup.contains(p)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            selectedForBackup = if (isChecked) {
                                                selectedForBackup - p
                                            } else {
                                                selectedForBackup + p
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedForBackup = if (checked == true) {
                                                selectedForBackup + p
                                            } else {
                                                selectedForBackup - p
                                            }
                                        },
                                        modifier = Modifier.scale(0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = "کد ${p.code} • ${p.region}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${p.area.toInt()} متر • ${p.bedrooms} خوابه • ${when(p.type) {
                                                "APARTMENT" -> "آپارتمان"
                                                "VILLA" -> "ویلایی"
                                                "LAND" -> "زمین"
                                                "COMMERCIAL" -> "تجاری"
                                                else -> p.type
                                            }}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                            }
                        }
                    }
                }

                // Export Button
                Button(
                    onClick = {
                        if (selectedForBackup.isEmpty()) {
                            Toast.makeText(context, "لطفاً حداقل یک فایل را انتخاب کنید.", Toast.LENGTH_SHORT).show()
                        } else {
                            onExport(selectedForBackup.toList())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تهیه پشتیبان (کپی کد پشتیبان)")
                }

                OutlinedButton(
                    onClick = {
                        if (selectedForBackup.isEmpty()) {
                            Toast.makeText(context, "لطفاً حداقل یک فایل را انتخاب کنید.", Toast.LENGTH_SHORT).show()
                        } else {
                            val json = onGetBackupJson(selectedForBackup.toList())
                            backupJsonToSave = json
                            val dateCode = PersianUtils.getPersianDateCode()
                            createDocumentLauncher.launch("backup_$dateCode")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تولید فایل پشتیبان تکست (.txt) 📂")
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

                OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("text/plain"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتخاب و بارگذاری فایل پشتیبان (.txt) 📄")
                }

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

fun shareBackupFile(context: Context, fileName: String, fileContent: String) {
    try {
        val cleanName = if (fileName.endsWith(".json")) fileName else "$fileName.json"
        val cacheDir = context.cacheDir
        val file = java.io.File(cacheDir, cleanName)
        
        java.io.FileOutputStream(file).use { os ->
            os.write(fileContent.toByteArray(Charsets.UTF_8))
        }
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "ارسال فایل پشتیبان"))
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در اشتراک‌گذاری فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun ShareFileNameDialog(
    properties: List<Property>,
    onDismiss: () -> Unit,
    onShareComplete: () -> Unit
) {
    val context = LocalContext.current
    val simpleDate = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    val defaultName = "melkup_export_${simpleDate}.json"
    var fileName by remember { mutableStateOf(defaultName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "📤 ارسال فایل پشتیبان",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "شما در حال اشتراک‌گذاری ${properties.size} فایل ملکی به صورت فایل پشتیبان آفلاین هستید. گیرنده می‌تواند این فایل را در برنامه خود بازیابی کند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("نام فایل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", color = MaterialTheme.colorScheme.error)
                    }
                    
                    Button(
                        onClick = {
                            if (fileName.trim().isEmpty()) {
                                Toast.makeText(context, "لطفاً نام فایل را وارد کنید", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val json = com.example.util.BackupHelper.exportBackup(properties)
                            shareBackupFile(context, fileName.trim(), json)
                            onShareComplete()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اشتراک‌گذاری")
                    }
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
    
    val priceText = PersianUtils.generatePropertyPriceText(property)
    detailsBuilder.append("قیمت: $priceText\n")
    detailsBuilder.append("کد فایل: ${property.code}\n\n")

    var instructions = when (templateName) {
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

    if (agencyName.isNotEmpty()) {
        instructions += " در متن تبلیغاتی تولید شده، حتماً نام بنگاه «$agencyName» را به عنوان معرف فایل ذکر کن."
    }

    if (templateName.contains("استوری")) {
        instructions += " همچنین ابعاد تصویر این استوری باید 1080*1920 باشد؛ لطفاً یک تصویر مناسب و جذاب برای این استوری بساز."
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
    properties: List<Property>,
    onExport: (List<Property>) -> Unit,
    onImport: (String) -> Unit,
    licenseStatus: LicenseStatus,
    onLicenseChanged: () -> Unit,
    propertiesCount: Int,
    onShowCustomization: () -> Unit = {},
    onShowBackupDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    var agencyName by remember { mutableStateOf(prefs.getString("agency_name", "") ?: "") }
    var customHeader by remember { mutableStateOf(prefs.getString("custom_header", "") ?: "") }
    var customFooter by remember { mutableStateOf(prefs.getString("custom_footer", "") ?: "") }
    var copyFormat by remember { mutableStateOf(prefs.getString("copy_format", "standard") ?: "standard") }

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

        // --- ACTIVATION SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (licenseStatus.isValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.03f)
            ),
            border = BorderStroke(
                1.dp,
                if (licenseStatus.isValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (licenseStatus.isValid) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                        contentDescription = null,
                        tint = if (licenseStatus.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "فعال‌سازی و وضعیت لایسنس",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (licenseStatus.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                if (licenseStatus.isValid) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "وضعیت برنامه:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "فعال (نسخه ویژه) ✅",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اعتبار لایسنس:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (licenseStatus.isLifetime) "مادام‌العمر ♾️" else "${licenseStatus.remainingDays} روز باقی‌مانده ⏳",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val totalDays = remember(licenseStatus) { LicenseManager.getTotalDaysFromSavedLicense(context) }
                    if (!licenseStatus.isLifetime && totalDays > 0) {
                        val progress = (licenseStatus.remainingDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
                        val progressBarColor = when {
                            progress > 0.6f -> Color(0xFF10B981) // Green
                            progress > 0.25f -> Color(0xFFF59E0B) // Amber/Yellow
                            else -> Color(0xFFEF4444) // Red
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = progressBarColor,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "میزان مصرف لایسنس:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${PersianUtils.formatNumber(licenseStatus.remainingDays)} روز از ${PersianUtils.formatNumber(totalDays)} روز باقی‌مانده",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = progressBarColor
                                )
                            }
                        }
                    } else if (licenseStatus.isLifetime) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = 1.0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF10B981),
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "میزان مصرف لایسنس:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "بدون محدودیت زمانی ♾️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "وضعیت برنامه:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "نسخه محدود (غیرفعال) ⚠️",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تعداد فایل‌های ملکی:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$propertiesCount از ۳ فایل مجاز",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (propertiesCount >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                // Copy User Code block
                Text(
                    text = "کد کاربری شما (کد دستگاه):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val androidId = LicenseManager.getAndroidId(context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            PersianUtils.copyToClipboard(context, androidId, "کد کاربری")
                            Toast.makeText(context, "کد کاربری کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = androidId,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Activation input field
                var activationInputKey by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = activationInputKey,
                        onValueChange = { activationInputKey = it },
                        placeholder = { Text("کد فعال‌سازی (لایسنس)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    Button(
                        onClick = {
                            if (activationInputKey.isNotEmpty()) {
                                when (LicenseManager.registerLicense(context, activationInputKey)) {
                                    com.example.util.RegisterResult.Success -> {
                                        onLicenseChanged()
                                        activationInputKey = ""
                                        Toast.makeText(context, "برنامه با موفقیت فعال شد! 🎉", Toast.LENGTH_LONG).show()
                                    }
                                    com.example.util.RegisterResult.AlreadyUsed -> {
                                        Toast.makeText(context, "این کد فعال‌سازی قبلاً استفاده شده است و قابل استفاده مجدد نیست!", Toast.LENGTH_LONG).show()
                                    }
                                    com.example.util.RegisterResult.Invalid -> {
                                        Toast.makeText(context, "کد فعال‌سازی لایسنس نامعتبر است!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = activationInputKey.trim().isNotEmpty(),
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ثبت")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Telegram Channel button
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/mostafamodaberiapps"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطا در باز کردن تلگرام", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF24A1DE),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_send),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ورود به کانال تلگرام جهت خرید لایسنس 💬", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

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

        Text("سربرگ و ته برگ سفارشی متن کپی شده:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = customHeader,
            onValueChange = {
                customHeader = it
                prefs.edit().putString("custom_header", it).apply()
            },
            label = { Text("سربرگ سفارشی (بالای متن کپی)") },
            placeholder = { Text("مثال: ⚜️ گروه مشاورین املاک رویال ⚜️", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        OutlinedTextField(
            value = customFooter,
            onValueChange = {
                customFooter = it
                prefs.edit().putString("custom_footer", it).apply()
            },
            label = { Text("ته برگ سفارشی (پایین متن کپی)") },
            placeholder = { Text("مثال: 📞 تلفن تماس: ۰۹۱۲۳۴۵۶۷۸۹", fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        Text("📋 فرمت کپی اطلاعات ملک:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formats = listOf(
                "standard" to "استاندارد 🌟",
                "customized" to "شخصی سازی ⚙️",
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
                            if (formatKey == "customized") {
                                onShowCustomization()
                            }
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

        // Large Premium Card to launch Backup Dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowBackupDialog() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "💾 پشتیبان‌گیری و بازیابی اطلاعات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "کپی کد پشتیبان، تولید فایل متنی (.txt) یا بازیابی فایل‌ها",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

@Composable
fun CompactPropertyItem(
    property: Property,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    isSelectionMode: Boolean,
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Subtle left vertical indicator line to make it a distinct box!
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (property.isSpecial) AccentGold else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // First Row: Code tag and main specifications
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (property.code.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = property.code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            val floorText = if (property.type == "آپارتمان") {
                                when {
                                    property.unitFloor == 0 -> " | طبقه: همکف"
                                    property.unitFloor != null -> " | طبقه: ${PersianUtils.formatNumber(property.unitFloor)}"
                                    else -> ""
                                }
                            } else ""
                            
                            val specs = "${property.type} | ${property.region} | ${PersianUtils.formatArea(property.area)} متر | ${PersianUtils.formatNumber(property.bedrooms)} خواب$floorText"
                            Text(
                                text = specs,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Second Row: Price & Amenities & Description
                        val priceText = PersianUtils.generatePropertyPriceText(property)
                        
                        val amenitiesParts = mutableListOf<String>()
                        if (property.cabinetType.isNotEmpty()) {
                            amenitiesParts.add("کابینت: ${property.cabinetType}")
                        }
                        if (property.otherAmenities.isNotEmpty()) {
                            amenitiesParts.add("امکانات: ${property.otherAmenities}")
                        }
                        val amenitiesText = if (amenitiesParts.isNotEmpty()) " | ${amenitiesParts.joinToString(" - ")}" else ""
                        val descText = if (property.description.trim().isNotEmpty()) " | 📝 ${property.description.trim()}" else ""
                        
                        Text(
                            text = "💰 $priceText$amenitiesText$descText",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (property.isSpecial) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "ویژه",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (onDelete != null && !isSelectionMode) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "جزئیات",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LicenseLockScreen(
    androidId: String,
    onActivate: (String) -> Unit
) {
    val context = LocalContext.current
    var inputKey by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Gold Lock icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Text(
                    text = "دسترسی به پرامپت استوری محدود است",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "قابلیت تولید خودکار استوری و پرامپت‌های تبلیغاتی با هوش مصنوعی مخصوص نسخه ویژه (لایسنس‌دار) است. لطفاً برای فعال‌سازی کامل برنامه، لایسنس خود را وارد کنید.",
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                
                // Copy User Code Section
                Text(
                    text = "کد کاربری شما (کد دستگاه):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            PersianUtils.copyToClipboard(context, androidId, "کد کاربری")
                            Toast.makeText(context, "کد کاربری کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = androidId,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Enter Activation Key
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text("کد فعال‌سازی (لایسنس)") },
                    placeholder = { Text("مثال: ZZZ1-ABCD") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Button(
                    onClick = { if (inputKey.isNotEmpty()) onActivate(inputKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = inputKey.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فعال‌سازی برنامه", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { page ->
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                
                LaunchedEffect(pagerState.currentPage) {
                    scale = 1f
                    offset = Offset.Zero
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = images[page],
                        contentDescription = "تصویر ملک بزرگ‌نمایی شده",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = Color.White
                    )
                }

                Text(
                    text = "${PersianUtils.formatNumber(pagerState.currentPage + 1)} از ${PersianUtils.formatNumber(images.size)}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Navigation buttons
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "قبلی",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (pagerState.currentPage < images.size - 1) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "بعدی",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationDialog(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val prefs = remember { context.getSharedPreferences("melkup_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Emoji states
    var emojiApartment by remember { mutableStateOf(prefs.getString("custom_emoji_apartment", "🏢") ?: "🏢") }
    var emojiVilla by remember { mutableStateOf(prefs.getString("custom_emoji_villa", "🏡") ?: "🏡") }
    var emojiHouse by remember { mutableStateOf(prefs.getString("custom_emoji_house", "🏠") ?: "🏠") }
    var emojiRegion by remember { mutableStateOf(prefs.getString("custom_emoji_region", "📍") ?: "📍") }
    var emojiArea by remember { mutableStateOf(prefs.getString("custom_emoji_area", "📐") ?: "📐") }
    var emojiBedrooms by remember { mutableStateOf(prefs.getString("custom_emoji_bedrooms", "🛏") ?: "🛏") }
    var emojiParking by remember { mutableStateOf(prefs.getString("custom_emoji_parking", "🚗") ?: "🚗") }
    var emojiCabinet by remember { mutableStateOf(prefs.getString("custom_emoji_cabinet", "🚪") ?: "🚪") }
    var emojiAmenities by remember { mutableStateOf(prefs.getString("custom_emoji_amenities", "✨") ?: "✨") }
    var emojiPrice by remember { mutableStateOf(prefs.getString("custom_emoji_price", "💰") ?: "💰") }
    var emojiDescription by remember { mutableStateOf(prefs.getString("custom_emoji_description", "📝") ?: "📝") }
    
    // Separator states
    var separatorEnabled by remember { mutableStateOf(prefs.getBoolean("separator_enabled", false)) }
    var separatorText by remember { mutableStateOf(prefs.getString("separator_text", "---") ?: "---") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 500.dp)
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "شخصی‌سازی فرمت کپی اطلاعات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Section 1: Emojis
                Text(
                    text = "🎨 تغییر ایموجی‌ها در متن کپی:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Layout emoji input fields
                val emojiFields = listOf(
                    Triple("آپارتمان", emojiApartment) { s: String -> emojiApartment = s },
                    Triple("ویلا", emojiVilla) { s: String -> emojiVilla = s },
                    Triple("خانه/کلنگی", emojiHouse) { s: String -> emojiHouse = s },
                    Triple("محله/منطقه", emojiRegion) { s: String -> emojiRegion = s },
                    Triple("متراژ", emojiArea) { s: String -> emojiArea = s },
                    Triple("تعداد خواب", emojiBedrooms) { s: String -> emojiBedrooms = s },
                    Triple("پارکینگ", emojiParking) { s: String -> emojiParking = s },
                    Triple("کابینت", emojiCabinet) { s: String -> emojiCabinet = s },
                    Triple("امکانات", emojiAmenities) { s: String -> emojiAmenities = s },
                    Triple("قیمت/مالی", emojiPrice) { s: String -> emojiPrice = s },
                    Triple("توضیحات", emojiDescription) { s: String -> emojiDescription = s }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojiFields.chunked(2).forEach { rowFields ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowFields.forEach { (label, value, onValueChange) ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { if (it.length <= 4) onValueChange(it) }, // support 1-2 characters/emojis
                                        modifier = Modifier.width(54.dp).height(44.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                            if (rowFields.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Reset to default button
                TextButton(
                    onClick = {
                        emojiApartment = "🏢"
                        emojiVilla = "🏡"
                        emojiHouse = "🏠"
                        emojiRegion = "📍"
                        emojiArea = "📐"
                        emojiBedrooms = "🛏"
                        emojiParking = "🚗"
                        emojiCabinet = "🚪"
                        emojiAmenities = "✨"
                        emojiPrice = "💰"
                        emojiDescription = "📝"
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("بازنشانی به ایموجی‌های پیش‌فرض 🔄", fontSize = 11.sp)
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Section 2: Separator
                Text(
                    text = "🔗 جدا کننده بین دو فایل (در کپی گروهی):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فعال کردن جدا کننده بین دو فایل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "در صورت غیرفعال بودن، بین فایل‌ها یک سطر خالی خواهد بود.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = separatorEnabled,
                        onCheckedChange = { separatorEnabled = it }
                    )
                }

                if (separatorEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("عبارت یا کاراکتر جدا کننده:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        OutlinedTextField(
                            value = separatorText,
                            onValueChange = { separatorText = it },
                            placeholder = { Text("مثال: ---") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = "مثال با عبارت \"---\": پس از هر فایل، یک سطر خالی، یک سطر حاوی --- و مجدد یک سطر خالی قرار می‌گیرد.",
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            prefs.edit().apply {
                                putString("custom_emoji_apartment", emojiApartment)
                                putString("custom_emoji_villa", emojiVilla)
                                putString("custom_emoji_house", emojiHouse)
                                putString("custom_emoji_region", emojiRegion)
                                putString("custom_emoji_area", emojiArea)
                                putString("custom_emoji_bedrooms", emojiBedrooms)
                                putString("custom_emoji_parking", emojiParking)
                                putString("custom_emoji_cabinet", emojiCabinet)
                                putString("custom_emoji_amenities", emojiAmenities)
                                putString("custom_emoji_price", emojiPrice)
                                putString("custom_emoji_description", emojiDescription)
                                putBoolean("separator_enabled", separatorEnabled)
                                putString("separator_text", separatorText)
                            }.apply()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ذخیره تغییرات")
                    }
                }
            }
        }
    }
}


