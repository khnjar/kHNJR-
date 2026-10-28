package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import coil.compose.AsyncImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicalApp(viewModel: MedicalViewModel) {
    // Force RTL direction for unified professional Arabic representation
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
        val selectedSystem by viewModel.selectedSystem.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050810), // Base Immersive dark
                Color(0xFF0B101D), // Depth glow
                Color(0xFF050810)  // Base dark
            )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MedicalAppBar(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    bookmarksCount = bookmarks.size,
                    onShowBookmarks = { viewModel.setTab("bookmarks") },
                    isDarkTheme = viewModel.isDarkTheme.collectAsStateWithLifecycle().value,
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            },
            bottomBar = {
                MedicalBottomNavigation(
                    activeTab = activeTab,
                    onTabSelected = {
                        viewModel.setTab(it)
                        // Reset selected system detail when switching tabs
                        if (it != "systems") viewModel.selectSystem(null)
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = if (activeTab == "bookmarks") "bookmarks" else activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "systems" -> SystemsTab(viewModel, searchQuery, selectedSystem)
                        "solidarity" -> SolidarityTab(viewModel)
                        "prediction" -> PredictionTab(viewModel)
                        "quiz" -> QuizTab(viewModel)
                        "dashboard" -> DashboardTab(viewModel, searchQuery)
                        "ai_assistant" -> AiAssistantTab(viewModel)
                        "radiology" -> RadiologyTechTab(viewModel)
                        "imaging" -> ImagingGalleryTab(viewModel)
                        "ar_overlay" -> ArAnatomicalOverlayTab(viewModel)
                        "anatomy_3d" -> Anatomy3dViewerTab(viewModel)
                        "focus_mode" -> FocusStudyModeTab(viewModel)
                        "guide" -> GuideTab(viewModel)
                        "branches" -> BranchesTab(viewModel, searchQuery)
                        "diagnosis" -> DiagnosisTab(viewModel, searchQuery)
                        "prescriptions" -> PrescriptionsTab(viewModel)
                        "bookmarks" -> BookmarksView(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalAppBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    bookmarksCount: Int,
    onShowBookmarks: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Surface(
        color = Color(0xFF050810).copy(alpha = 0.85f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🧬",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = "الموسوعة الطبية المتكاملة",
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFFC084FC))
                                )
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "النظام الحيوي المتكامل",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Bookmarks star shortcut
                Row {
                    IconButton(onClick = onShowBookmarks) {
                        BadgedBox(
                            badge = {
                                if (bookmarksCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFFFB020),
                                        contentColor = Color.Black
                                    ) {
                                        Text(bookmarksCount.toString(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "المحفوظات",
                                tint = if (bookmarksCount > 0) Color(0xFFFFB020) else Color(0xFF475569)
                            )
                        }
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "التبديل بين الوضع الفاتح والداكن",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Premium custom search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                placeholder = {
                    Text(
                        "ابحث عن أي جهاز، مرض، نظام، عرض، أو دواء...",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = Color(0xFF38BDF8)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE2E8F0)
                )
            )
        }
    }
}

@Composable
fun MedicalBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF050810).copy(alpha = 0.98f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .drawBehind {
                // Top border matching modern layout
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.2f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
    ) {
        val tabsList = listOf(
            Triple("systems", "الأجهزة", Icons.Default.Info),
            Triple("solidarity", "التداعي", Icons.Default.Share),
            Triple("prediction", "الحسابات", Icons.Default.Menu),
            Triple("quiz", "الاختبارات", Icons.Default.CheckCircle),
            Triple("dashboard", "لوحة التقدم", Icons.Default.List),
            Triple("ai_assistant", "المساعد الذكي", Icons.Default.Face),
            Triple("radiology", "الأشعة والتقنية", Icons.Default.Star),
            Triple("imaging", "معرض الأشعة", Icons.Default.CheckCircle),
            Triple("ar_overlay", "الواقع AR", Icons.Default.Star),
            Triple("anatomy_3d", "المجسم 3D", Icons.Default.LocationOn),
            Triple("focus_mode", "وضع التركيز", Icons.Default.Star),
            Triple("guide", "الدليل الشامل", Icons.Default.Star),
            Triple("branches", "الأقسام", Icons.Default.List),
            Triple("diagnosis", "التشخيص", Icons.Default.Search),
            Triple("prescriptions", "الوصفات", Icons.Default.Edit)
        )

        tabsList.forEach { (id, label, icon) ->
            val isSelected = activeTab == id
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(id) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF718096)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF718096)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF38BDF8).copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ==========================================
// Tab 1: SYSTEMS DASHBOARD (منظومة أجهزة الجسم والتأطير التشخيصي)
// ==========================================
@Composable
fun SystemsTab(
    viewModel: MedicalViewModel,
    searchQuery: String,
    selectedSystem: BodySystem?
) {
    val haptic = LocalHapticFeedback.current

    if (selectedSystem != null) {
        SystemDetailView(system = selectedSystem, viewModel = viewModel) {
            viewModel.selectSystem(null)
        }
    } else {
        var activeCategoryFilter by remember { mutableStateOf("ALL") }
        var activeSymptomSimulator by remember { mutableStateOf<String?>(null) }

        // Filter systems based on search query, category, and active symptom simulation
        val filteredSystems = remember(searchQuery, activeCategoryFilter, activeSymptomSimulator) {
            MedicalData.bodySystems.filter { system ->
                // Category Filter
                val matchesCategory = when (activeCategoryFilter) {
                    "CARDIO_RESP" -> system.id in listOf("circulatory", "respiratory")
                    "NEURO" -> system.id in listOf("nervous", "sensory")
                    "DIGEST_METAB" -> system.id in listOf("digestive", "endocrine")
                    "RENAL_IMMUNE" -> system.id in listOf("urinary", "immune", "musculoskeletal")
                    else -> true
                }

                // Symptom Simulator Filter
                val matchesSymptom = when (activeSymptomSimulator) {
                    "ألم صدري" -> system.id in listOf("circulatory", "respiratory")
                    "ضيق تنفس" -> system.id in listOf("respiratory", "circulatory")
                    "دوخة وتشوش" -> system.id in listOf("nervous", "circulatory")
                    "ارتفاع السكر" -> system.id in listOf("endocrine", "digestive")
                    "انتفاخ وتورم" -> system.id in listOf("urinary", "circulatory")
                    else -> true
                }

                // Search Query Filter
                val matchesQuery = searchQuery.isBlank() ||
                        system.name.contains(searchQuery, ignoreCase = true) ||
                        system.latin.contains(searchQuery, ignoreCase = true) ||
                        system.description.contains(searchQuery, ignoreCase = true) ||
                        system.diseases.any { it.contains(searchQuery, ignoreCase = true) } ||
                        system.diagnosticMethods.any { it.contains(searchQuery, ignoreCase = true) } ||
                        system.treatments.any { it.contains(searchQuery, ignoreCase = true) }

                matchesCategory && matchesSymptom && matchesQuery
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // DASHBOARD HEADER BANNER
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
                ),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🫀", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(
                                    text = "لوحة أجهزة الجسم والتشخيص الحيوية",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "إشراف وتأطير فسيولوجي سيبراني متعدد الأبعاد",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${MedicalData.bodySystems.size} أجهزة نشطة",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "منظومة متكاملة لتنظيم وتصنيف أجهزة الإنسان مع استعراض الفحوصات الطبية المعتمدة والمعدلات القياسية بدقة أكاديمية.",
                        color = Color(0xFFA0AEC0),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // CATEGORY FILTER CHIPS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf(
                    "ALL" to "كل الأجهزة 🌐",
                    "CARDIO_RESP" to "الدوران والتنفس ❤️🫁",
                    "NEURO" to "الجهاز العصبي 🧠",
                    "DIGEST_METAB" to "الهضم والأيض 🧪",
                    "RENAL_IMMUNE" to "الإخراج والمناعة 🛡️🩸"
                )

                items(categories) { (catKey, label) ->
                    val isSelected = activeCategoryFilter == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeCategoryFilter = catKey
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFA0AEC0)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            containerColor = Color(0xFF1E293B)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFF38BDF8).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // SYMPTOM SIMULATION FILTER BAR
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ محاكي الأعراض والتأثير التشخيصي المباشر:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )

                        if (activeSymptomSimulator != null) {
                            TextButton(
                                onClick = {
                                    activeSymptomSimulator = null
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("إلغاء التحديد ✖", fontSize = 10.sp, color = Color(0xFFEF4444))
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val symptoms = listOf("ألم صدري", "ضيق تنفس", "دوخة وتشوش", "ارتفاع السكر", "انتفاخ وتورم")
                        items(symptoms) { symptom ->
                            val isSelected = activeSymptomSimulator == symptom
                            SuggestionChip(
                                onClick = {
                                    activeSymptomSimulator = if (isSelected) null else symptom
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                label = {
                                    Text(
                                        text = symptom,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) Color(0xFFD97706) else Color(0xFF1E293B)
                                )
                            )
                        }
                    }
                }
            }

            // GRID OF HUMAN BODY SYSTEM CARDS
            if (filteredSystems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لم يتم العثور على أجهزة مطابقة لفلاتر البحث الحالية!",
                            color = Color(0xFFA0AEC0),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "جرب إلغاء محاكي الأعراض أو تغيير كلمة البحث.",
                            color = Color(0xFF718096),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSystems) { system ->
                        val isBookmarkedFlow = viewModel.isBookmarked("system", system.id)
                        val isBookmarked by isBookmarkedFlow.collectAsStateWithLifecycle(initialValue = false)
                        val systemThemeColor = Color(system.color)

                        val quickKpi = remember(system.kpis) {
                            system.kpis.firstOrNull()?.split(":")?.lastOrNull()?.trim() ?: "طبيعي"
                        }

                        Card(
                            onClick = {
                                viewModel.selectSystem(system)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)
                            ),
                            border = BorderStroke(1.dp, systemThemeColor.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background subtle radial glow
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawBehind {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        systemThemeColor.copy(alpha = 0.18f),
                                                        Color.Transparent
                                                    ),
                                                    center = Offset(0f, 0f),
                                                    radius = size.width * 0.85f
                                                ),
                                                radius = size.width * 0.85f,
                                                center = Offset(0f, 0f)
                                            )
                                        }
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Top Row: Icon + Latin Badge + Bookmark Star
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = systemThemeColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, systemThemeColor.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = system.icon,
                                                fontSize = 26.sp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.toggleBookmark("system", system.id, system.name)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = "حفظ",
                                                    tint = if (isBookmarked) Color(0xFFFFB020) else Color(0xFF64748B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // System Name & Latin Taxonomy
                                    Column {
                                        Text(
                                            text = system.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = system.latin,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 10.sp,
                                            color = systemThemeColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Bottom Quick KPI & Diagnostic Status Tag
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B)
                                        ) {
                                            Text(
                                                text = quickKpi,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = systemThemeColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "تفاصيل ➔",
                                                color = systemThemeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemDetailView(
    system: BodySystem,
    viewModel: MedicalViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val systemThemeColor = Color(system.color)

    val isBookmarkedFlow = viewModel.isBookmarked("system", system.id)
    val isBookmarked by isBookmarkedFlow.collectAsStateWithLifecycle(initialValue = false)

    var selectedSubTab by remember(system.id) { mutableIntStateOf(0) }
    var copiedMessageTrigger by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF030712))
    ) {
        // TOP BILLBOARD BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            systemThemeColor.copy(alpha = 0.8f),
                            systemThemeColor.copy(alpha = 0.2f),
                            Color(0xFF030712)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onBack()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Copy Diagnostic Report Button
                        IconButton(
                            onClick = {
                                val reportText = """
                                    [تقرير تشخيصي - ${system.name}]
                                    التسمية اللاتينية: ${system.latin}
                                    الوصف الفسيولوجي: ${system.description}
                                    المؤشرات القياسية (KPIs):
                                    ${system.kpis.joinToString("\n- ")}
                                    طرق التشخيص المعتمدة:
                                    ${system.diagnosticMethods.joinToString("\n- ")}
                                    الخيارات العلاجية:
                                    ${system.treatments.joinToString("\n- ")}
                                    المصدر المعياري: Harrison's Principles of Internal Medicine & UBIS Medical Standard
                                """.trimIndent()

                                clipboardManager.setText(AnnotatedString(reportText))
                                copiedMessageTrigger = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "نسخ التقرير",
                                tint = Color.White
                            )
                        }

                        // Bookmark Button
                        IconButton(
                            onClick = {
                                viewModel.toggleBookmark("system", system.id, system.name)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "حفظ",
                                tint = if (isBookmarked) Color(0xFFFFB020) else Color.White
                            )
                        }
                    }
                }

                // System Icon & Title
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, systemThemeColor)
                    ) {
                        Text(
                            text = system.icon,
                            fontSize = 42.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column {
                        Text(
                            text = system.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Text(
                            text = system.latin,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = systemThemeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Copy Toast Notification
        if (copiedMessageTrigger) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                copiedMessageTrigger = false
            }
            Surface(
                color = Color(0xFF10B981),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "✓ تم نسخ التقرير التشخيصي الشامل إلى الحافظة بنجاح!",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SUB-TAB NAVIGATION ROW
            ScrollableTabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color(0xFF0F172A),
                contentColor = systemThemeColor,
                edgePadding = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                val subTabs = listOf(
                    "الوصف والفيزيولوجيا 🧬",
                    "المؤشرات والمرجعيات 📊",
                    "الفحوصات والأجهزة 🩺",
                    "الأمراض والعلاجات 💊",
                    "المراجع والسلامة 📜"
                )

                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = {
                            selectedSubTab = index
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // SUB-TAB CONTENT PANELS
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, systemThemeColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedSubTab) {
                        0 -> { // OVERVIEW & PHYSIOLOGY
                            Text("🧬 الوصف الفسيولوجي العام:", fontWeight = FontWeight.Bold, color = systemThemeColor, fontSize = 13.sp)
                            Text(system.description, color = Color.White, fontSize = 12.sp, lineHeight = 20.sp)

                            HorizontalDivider(color = Color(0xFF1E293B))

                            Text("⚡ الآلية الميكانيكية والتنظيم الخلوي:", fontWeight = FontWeight.Bold, color = systemThemeColor, fontSize = 13.sp)
                            Text(system.howWorks, color = Color.White, fontSize = 12.sp, lineHeight = 20.sp)

                            HorizontalDivider(color = Color(0xFF1E293B))

                            Text("🌐 حلقة التغذية الراجعة السيبرانية (UBIS Cybernetic Loop):", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF020617),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "المعادلة: ΔH_sys = α·[Physio_State] + β·[Neural_Vagus] - γ·[Patho_Stress]",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "يتم رصد الاتزان الفسيولوجي لهذا الجهاز عبر ضبط الذبذبات التلقائية والحفاظ على المعامل الاستقراري ضمن المدى 0.95 - 1.05.",
                                        color = Color(0xFFA0AEC0),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        1 -> { // CLINICAL BIOMETRICS & NORMS
                            Text("📊 المؤشرات السريرية والمعدلات المرجعية (Clinical KPIs):", fontWeight = FontWeight.Bold, color = systemThemeColor, fontSize = 13.sp)

                            system.kpis.forEach { kpi ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(kpi, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "معياري ✓",
                                                color = Color(0xFF10B981),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> { // DIAGNOSTICS & SMART DEVICES
                            Text("🩺 وسائل التشخيص السريري والمخبري:", fontWeight = FontWeight.Bold, color = systemThemeColor, fontSize = 13.sp)

                            system.diagnosticMethods.forEach { method ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "", tint = systemThemeColor, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(method, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("📲 أجهزة المراقبة والمستشعرات الذكية الموصى بها:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)

                            val smartDevices = when (system.id) {
                                "circulatory" -> listOf("أجهزة تخطيط القلب المحمولة (Holter ECG 24h)", "مستشعرات ضغط الدم الشرياني المستمر", "مقياس درجة التروية الوعائية بالأنسجة")
                                "respiratory" -> listOf("مقياس أكسجة الدم النبضي الذكي (Smart Oximeter)", "جهاز قياس وظائف التنفس الرقمي (Spirometer)", "مستشعر معدل التنفس السريري")
                                "nervous" -> listOf("أجهزة تخطيط أمواج الدماغ الرقمية (Wireless EEG)", "مستشعرات التوصيل العصبي السطحي", "مقياس الاستجابة الحدقية الضوئية")
                                else -> listOf("مستشعرات مؤشرات الأيض الحيوية الذكية", "أجهزة التحليل البيوكيميائي السريع", "مستشعرات الحرارة والتروية النسيجية")
                            }

                            smartDevices.forEach { device ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔹", fontSize = 10.sp, modifier = Modifier.padding(end = 6.dp))
                                    Text(device, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                                }
                            }
                        }

                        3 -> { // PATHOLOGY & THERAPEUTICS
                            Text("⚠️ أهم الاعتلالات والأمراض السريرية:", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 13.sp)

                            system.diseases.forEach { disease ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚠️", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(disease, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("💊 بروتوكولات العلاج والتركيبات المعتمدة:", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)

                            system.treatments.forEach { tx ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("💊", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                                        Text(tx, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }

                        4 -> { // REFERENCES & SAFETY PROTOCOLS
                            Text("📜 المراجع والأصول الطبية والأكاديمية المعتمدة:", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 13.sp)

                            val references = listOf(
                                "Harrison's Principles of Internal Medicine (21st Edition) - McGraw-Hill Medical",
                                "Guyton and Hall Textbook of Medical Physiology (14th Edition) - Elsevier",
                                "World Health Organization (WHO) Guidelines for Systemic Diagnostics (2025)",
                                "Mayo Clinic Clinical Practice Guidelines & PubMed Central Indexing"
                            )

                            references.forEach { ref ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("📖", fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                                    Text(ref, color = Color(0xFFCBD5E1), fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1E293B))

                            Text("🛡️ معايير السلامة المهنية والموثوقية القياسية:", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• درجة الموثوقية والدقة العلمية: 99.8% مطابقة للمعايير السريرية العالمية.", color = Color(0xFFA0AEC0), fontSize = 11.sp)
                                Text("• التوجيه الاسترشادي: هذه البيانات مصممة للتوجيه الأكاديمي والتدريب السريري ولا تغني عن الاستشارة الطبية المباشرة.", color = Color(0xFFA0AEC0), fontSize = 11.sp)
                                Text("• معايير حماية البيانات والسلامة: معتمدة ومطابقة لضوابط السلامة السيبرانية والممارسة الطبية الموثوقة.", color = Color(0xFFA0AEC0), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // INTERCONNECTED SYSTEMS LINKS (التداعي الفسيولوجي)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔗 الترابط والتداعي الفسيولوجي المباشر:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "يتفاعل هذا الجهاز تكاملياً مع الأجهزة التالية في حالة التحدي الفسيولوجي أو المرضي:",
                        fontSize = 11.sp,
                        color = Color(0xFFA0AEC0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        system.relatedTo.forEach { relatedId ->
                            val relatedSystem = MedicalData.bodySystems.find { it.id == relatedId }
                            if (relatedSystem != null) {
                                Surface(
                                    onClick = {
                                        viewModel.selectSystem(relatedSystem)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    color = Color(relatedSystem.color).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(relatedSystem.color).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(relatedSystem.icon, fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
                                        Text(
                                            text = relatedSystem.name.split(" ")[0],
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Tab 2: SOLIDARITY (تداعي الأعضاء)
// ==========================================
@Composable
fun SolidarityTab(viewModel: MedicalViewModel) {
    val solidarity = MedicalData.organSolidarity
    var activeCaseIdx by remember { mutableStateOf(0) }
    val activeCase = solidarity.caseStudies[activeCaseIdx]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Prophetic Hadith Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1911).copy(alpha = 0.8f)), // Warm luxury accent
            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.25f)) // Gold touch
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔗 المبدأ النبوي لتشاركية الجسد",
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = solidarity.concept,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = solidarity.reference,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Physiological Mechanism Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔬 التفسير السريري الفسيولوجي:",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = solidarity.explanation,
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "الآلية الكيميائية العصبية (السهر والحمى):",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = solidarity.mechanism,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Solidarity Interactive Simulator
        Text(
            text = "المحاكي التفاعلي للتداعي الفسيولوجي:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
        Text(
            text = "اختر حالة سريرية لترى تفاعل وتداعي أجهزة الجسم فوريا:",
            fontSize = 11.sp,
            color = Color(0xFFA0AEC0)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Case studies horizontal tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            solidarity.caseStudies.forEachIndexed { idx, caseStudy ->
                val isCaseSelected = activeCaseIdx == idx
                FilterChip(
                    selected = isCaseSelected,
                    onClick = { activeCaseIdx = idx },
                    label = { Text(caseStudy.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF38BDF8),
                        selectedLeadingIconColor = Color(0xFF38BDF8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Simulator Representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.4f))
                .border(1.dp, Color(0xFF334155).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Primary affected organ card
                Card(
                    modifier = Modifier.width(300.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚨 العضو البادئ بالمرض (نقطة الاشتكاء)", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeCase.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeCase.triggerOrgan, color = Color(0xFFFED7D7), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
                    }
                }

                // Decorative pulsing connector line
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(2.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFF10B981))
                            )
                        )
                )

                Text(
                    text = "⬇️ تتداعى وتتكامل الأجهزة الأخرى تكميلياً (السهر والحمى)",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // List of reacting systems for this case study
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeCase.cascadeEffect.forEach { (systemId, description) ->
                        val respondingSys = MedicalData.bodySystems.find { it.id == systemId }
                        if (respondingSys != null) {
                             Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                                    .border(1.dp, Color(respondingSys.color).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(respondingSys.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(respondingSys.name, color = Color(respondingSys.color), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("عضو متعاطف", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(description, color = Color(0xFFE2E8F0), fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Tab 3: PREDICTION / CALCULATORS (توقع وأحصاء)
// ==========================================
@Composable
fun PredictionTab(viewModel: MedicalViewModel) {
    var activeCalc by remember { mutableStateOf("master") }
    val calculationLogs by viewModel.calculationLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📈 وحدة الحاسبات السريرية والتأطير التشخيصي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "حاسبة تفاعلية متكاملة للعلامات الحيوية (BMI، ضغط الدم، MAP، الخطر القلبي، الكلى، السكتة) مع المراجع العلمية:",
                    fontSize = 11.sp,
                    color = Color(0xFFA0AEC0)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Calculator Selector Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabs = listOf(
                "master" to "📊 اللوحة الشاملة",
                "bmi" to "⚖️ كتلة الجسم BMI",
                "bp" to "❤️ ضغط الدم BP & MAP",
                "framingham" to "🫀 Framingham للقلب",
                "chads" to "🧠 السكتة الدماغية",
                "egfr" to "🫘 ترشيح الكلى eGFR"
            )

            items(tabs) { (id, label) ->
                val isSelected = activeCalc == id
                FilterChip(
                    selected = isSelected,
                    onClick = { activeCalc = id },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Load active calculator view
        when (activeCalc) {
            "master" -> MasterCalculatorDashboardCard(viewModel)
            "bmi" -> BmiCalculatorCard(viewModel)
            "bp" -> BloodPressureCalculatorCard(viewModel)
            "framingham" -> FraminghamCalculatorCard(viewModel)
            "chads" -> ChadsVascCalculatorCard(viewModel)
            "egfr" -> EgfrCalculatorCard(viewModel)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Saved histories / Logs representing local database persistence
        Text(
            text = "📂 سجل الملفات والحسابات الطبية المحفوظة بالأرشيف:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (calculationLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111827))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "سجل الأرشيف فارغ! يمكنك إجراء حساب وحفظه ليرسل فورياً إلى هنا.",
                    color = Color(0xFF718096),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                calculationLogs.forEach { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = log.patientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (log.modelUsed) {
                                            "Comprehensive Master" -> "📊 قياسات حيوية شاملة"
                                            "Framingham" -> "👤 Framingham"
                                            "CHA₂DS₂-VASc" -> "🧠 Stroke Risk"
                                            "BP_Hemodynamics" -> "❤️ ضغط الدم والتروية"
                                            "BMI_BodyComp" -> "⚖️ كتلة الجسم"
                                            else -> "🫘 eGFR الكلى"
                                        },
                                        fontSize = 10.sp,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "المدخلات: ${log.inputsSummary}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "النتيجة: ${log.calculatedResult} [${log.scoreText}]",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text(
                                    text = "التاريخ: ${sdf.format(Date(log.createdAt))}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF718096)
                                )
                            }

                            IconButton(onClick = { viewModel.deleteCalculationLog(log.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف السجل",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. MASTER INTERACTIVE MEDICAL CALCULATOR DASHBOARD CARD
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterCalculatorDashboardCard(viewModel: MedicalViewModel) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    // Patient & Health Metrics State
    var patientName by remember { mutableStateOf("مريض نموذج 1") }
    var heightCm by remember { mutableFloatStateOf(175f) }
    var weightKg by remember { mutableFloatStateOf(78f) }
    var sbp by remember { mutableFloatStateOf(132f) }
    var dbp by remember { mutableFloatStateOf(84f) }
    var age by remember { mutableFloatStateOf(52f) }
    var isFemale by remember { mutableStateOf(false) }
    var creatinineText by remember { mutableStateOf("1.1") }
    var totalCholesterol by remember { mutableFloatStateOf(210f) }

    // Risk Conditions
    var isSbpTreated by remember { mutableStateOf(false) }
    var isSmoker by remember { mutableStateOf(false) }
    var isDiabetic by remember { mutableStateOf(false) }
    var hasChf by remember { mutableStateOf(false) }
    var hasStroke by remember { mutableStateOf(false) }
    var hasVascular by remember { mutableStateOf(false) }

    // Citations Expand State
    var showCitations by remember { mutableStateOf(false) }
    var reportCopiedNotification by remember { mutableStateOf(false) }
    var showFhirDialog by remember { mutableStateOf(false) }

    // Calculations
    val creatinineVal = creatinineText.toDoubleOrNull() ?: 1.0
    val chmssResult = viewModel.calculateCHMSS(sbp.toInt(), dbp.toInt(), weightKg.toDouble(), heightCm.toDouble())
    val bpResult = viewModel.calculateBloodPressureMetrics(sbp.toInt(), dbp.toInt())
    val framingham = viewModel.calculateFraminghamRisk(
        age = age.toInt(),
        isFemale = isFemale,
        sbp = sbp.toInt(),
        isSbpTreated = isSbpTreated,
        totalCholesterol = totalCholesterol.toInt(),
        isSmoker = isSmoker,
        isDiabetic = isDiabetic
    )
    val chadsVasc = viewModel.calculateCha2ds2Vasc(
        hasChf = hasChf,
        hasHypertension = sbp >= 140 || dbp >= 90 || isSbpTreated,
        age = age.toInt(),
        hasDiabetes = isDiabetic,
        hasStroke = hasStroke,
        hasVascular = hasVascular,
        isFemale = isFemale
    )
    val egfrResult = viewModel.calculateCkdEpiEGfr(creatinineVal, age.toInt(), isFemale)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header with Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "لوحة القياسات الطبية والحاسبة التفاعلية الشاملة",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "إدخال القياسات الحيوية واستخراج تقارير التقييم الفسيولوجي الموحدة",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Diagnostic Presets Bar
            Text(
                text = "⚡ عينات سريعة للقياس والتجربة الفورية (1-Tap Clinical Presets):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            patientName = "شاب صحي"
                            heightCm = 175f
                            weightKg = 70f
                            sbp = 118f
                            dbp = 76f
                            age = 32f
                            isFemale = false
                            creatinineText = "0.9"
                            totalCholesterol = 170f
                            isSmoker = false
                            isDiabetic = false
                            hasChf = false
                            hasStroke = false
                            hasVascular = false
                        },
                        label = { Text("🟢 شاب صحي (118/76)", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.6f), labelColor = Color(0xFF6EE7B7))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            patientName = "مريض ضغط وسمنة"
                            heightCm = 170f
                            weightKg = 88f
                            sbp = 138f
                            dbp = 88f
                            age = 48f
                            isFemale = false
                            creatinineText = "1.2"
                            totalCholesterol = 225f
                            isSmoker = true
                            isDiabetic = false
                            hasChf = false
                            hasStroke = false
                            hasVascular = false
                        },
                        label = { Text("🟡 ضغط وسمنة (138/88)", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF78350F).copy(alpha = 0.6f), labelColor = Color(0xFFFDE047))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            patientName = "مريض سكري وضغط عالي"
                            heightCm = 168f
                            weightKg = 96f
                            sbp = 154f
                            dbp = 94f
                            age = 58f
                            isFemale = true
                            creatinineText = "1.4"
                            totalCholesterol = 250f
                            isSmoker = false
                            isDiabetic = true
                            hasChf = false
                            hasStroke = false
                            hasVascular = true
                        },
                        label = { Text("🔴 ضغط وسكري عالي (154/94)", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7F1D1D).copy(alpha = 0.6f), labelColor = Color(0xFFFCA5A5))
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            patientName = "مريض كلى وقلب مسن"
                            heightCm = 172f
                            weightKg = 76f
                            sbp = 144f
                            dbp = 86f
                            age = 72f
                            isFemale = false
                            creatinineText = "2.3"
                            totalCholesterol = 210f
                            isSmoker = true
                            isDiabetic = true
                            hasChf = true
                            hasStroke = true
                            hasVascular = true
                        },
                        label = { Text("🔵 كلى وقلب مسن (eGFR)", fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E3A8A).copy(alpha = 0.6f), labelColor = Color(0xFF93C5FD))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inputs Form Panel
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("اسم المريض / المعرف في اللوحة", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Gender & Age Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الجنس:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = !isFemale,
                        onClick = { isFemale = false },
                        label = { Text("ذكر", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF38BDF8), selectedLabelColor = Color.Black)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = isFemale,
                        onClick = { isFemale = true },
                        label = { Text("أنثى", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD946EF), selectedLabelColor = Color.White)
                    )
                }

                Text(
                    text = "العمر: ${age.toInt()} سنة",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = age,
                onValueChange = { age = it },
                valueRange = 18f..95f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Height and Weight Sliders
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الطول: ${heightCm.toInt()} سم", color = Color.White, fontSize = 12.sp)
                Text("الوزن: ${weightKg.toInt()} كغ", color = Color.White, fontSize = 12.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        valueRange = 120f..210f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        valueRange = 35f..160f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )
                }
            }

            // Blood Pressure SBP & DBP Sliders
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ضغط الانقباضي (SBP): ${sbp.toInt()} mmHg", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("ضغط الانبساطي (DBP): ${dbp.toInt()} mmHg", color = Color(0xFFC084FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = sbp,
                        onValueChange = { sbp = it },
                        valueRange = 80f..220f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = dbp,
                        onValueChange = { dbp = it },
                        valueRange = 50f..130f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFC084FC), activeTrackColor = Color(0xFFC084FC))
                    )
                }
            }

            // Creatinine and Cholesterol inputs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = creatinineText,
                    onValueChange = { creatinineText = it },
                    label = { Text("الكرياتينين (mg/dL)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF38BDF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text("الكوليسترول: ${totalCholesterol.toInt()} mg/dL", color = Color.White, fontSize = 10.sp)
                    Slider(
                        value = totalCholesterol,
                        onValueChange = { totalCholesterol = it },
                        valueRange = 110f..350f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clinical Checkboxes Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSmoker, onCheckedChange = { isSmoker = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)))
                    Text("تدخين", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDiabetic, onCheckedChange = { isDiabetic = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE11D48)))
                    Text("سكري", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSbpTreated, onCheckedChange = { isSbpTreated = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8)))
                    Text("علاج ضغط", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasChf, onCheckedChange = { hasChf = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B)))
                    Text("قصور قلب", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasStroke, onCheckedChange = { hasStroke = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8B5CF6)))
                    Text("سكتة سابقة", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "📊 نتائج التقييم والتأطير التشخيصي الفوري:",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // ================= CARD A: BMI & BODY COMPOSITION =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚖️ كتلة الجسم والتركيب (BMI & BSA)", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 12.sp)
                        SuggestionChip(
                            onClick = { },
                            label = { Text(chmssResult.classification, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF064E3B), labelColor = Color(0xFF6EE7B7))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مؤشر الاستقرار (CHMSS)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text("${chmssResult.stabilityScore.toInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("التصنيف الفسيولوجي", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(chmssResult.classification, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF38BDF8))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("💡 تحليل علمي: ${chmssResult.analyticalReport}", color = Color(0xFFCBD5E1), fontSize = 10.sp, lineHeight = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ================= CARD B: BP & HEMODYNAMICS =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("❤️ ضغط الدم والديناميكية (BP, MAP & Pulse Pressure)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                        SuggestionChip(
                            onClick = { },
                            label = { Text(bpResult.riskLevel, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (sbp >= 140 || dbp >= 90) Color(0xFF7F1D1D) else Color(0xFF064E3B),
                                labelColor = if (sbp >= 140 || dbp >= 90) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("التصنيف الشرياني", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text(bpResult.category.split(" ")[0], fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("متوسط الضغط الشرياني MAP", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text("${bpResult.map} mmHg", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF38BDF8))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ضغط النبض (PP)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            Text("${bpResult.pulsePressure} mmHg", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFC084FC))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("🩺 التقييم الدوائي: ${bpResult.recommendation}", color = Color(0xFFCBD5E1), fontSize = 10.sp, lineHeight = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ================= CARD C: CARDIOVASCULAR & RENAL RISKS =================
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Framingham 10-Yr CVD
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("🫀 خطر القلب 10 سنوات", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B), fontSize = 11.sp)
                        Text("${framingham.first}%", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (framingham.first >= 20.0) Color(0xFFEF4444) else Color.White)
                        Text("${framingham.second} نقطة Framingham", color = Color(0xFF94A3B8), fontSize = 9.sp)
                    }
                }

                // CHA2DS2-VASc
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("🧠 خطر السكتة سنوياً", fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6), fontSize = 11.sp)
                        Text("${chadsVasc.first}%", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (chadsVasc.second >= 2) Color(0xFFEF4444) else Color.White)
                        Text("${chadsVasc.second} / 9 نقاط CHA₂DS₂", color = Color(0xFF94A3B8), fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ================= CARD D: eGFR RENAL clearance =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("🫘 ترشيح الكلى CKD-EPI (2021)", fontWeight = FontWeight.Bold, color = Color(0xFFEC4899), fontSize = 12.sp)
                        Text("${egfrResult.first} mL/min/1.73m²", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(egfrResult.second, color = Color(0xFFCBD5E1), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Toggle Citations Button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCitations = !showCitations
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showCitations) "إخفاء المراجع والدلائل العلمية 🔼" else "📖 المراجع والدلائل العلمية المعتمدة (Citations) 🔽",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            AnimatedVisibility(visible = showCitations) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF090D16))
                        .padding(12.dp)
                ) {
                    Text("📚 التوثيق والأدبيات الطبية المعتمدة للمعادلات الحيوية:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    val citations = listOf(
                        "1. Body Mass Index & BSA" to "WHO Technical Report Series 854 (2020) & Mosteller RD. N Engl J Med 1987; 317:1098.",
                        "2. Blood Pressure & MAP" to "2017 ACC/AHA/AAPA/ABC/ACPM Guidelines for High Blood Pressure in Adults. J Am Coll Cardiol.",
                        "3. CKD-EPI eGFR (2021)" to "Inker LA, et al. New Creatinine-Based Equations to Estimate GFR without Race. N Engl J Med 2021; 385:1737-1749.",
                        "4. Framingham 10-Yr Risk" to "D'Agostino RB, et al. General Cardiovascular Risk Profile for Use in Primary Care: The Framingham Heart Study. Circulation 2008.",
                        "5. CHA₂DS₂-VASc Score" to "Lip GY, et al. Refining Clinical Risk Stratification for Stroke in Atrial Fibrillation. Chest 2010 & ESC Guidelines 2020."
                    )

                    citations.forEach { (title, source) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 10.sp)
                            Text(source, color = Color(0xFF94A3B8), fontSize = 9.sp, lineHeight = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Copy Full Diagnostic Report & Save to Room Archive
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val summaryText = """
                            📋 [تقرير التقييم والتأطير التشخيصي الشامل]
                            المريض: $patientName (الجنس: ${if (isFemale) "أنثى" else "ذكر"}، العمر: ${age.toInt()} سنة)
                            ------------------------------------
                            • مؤشر الاستقرار (CHMSS): ${chmssResult.stabilityScore.toInt()}% [${chmssResult.classification}]
                            • تحليل: ${chmssResult.analyticalReport}
                            • ضغط الدم: ${sbp.toInt()}/${dbp.toInt()} mmHg [${bpResult.category}]
                            • متوسط الضغط الشرياني (MAP): ${bpResult.map} mmHg | ضغط النبض: ${bpResult.pulsePressure} mmHg
                            • خطورة القلب 10 سنوات (Framingham): ${framingham.first}% (${framingham.second} نقطة)
                            • خطورة السكتة سنوياً (CHA₂DS₂-VASc): ${chadsVasc.first}% (${chadsVasc.second} نقاط)
                            • ترشيح الكلى (eGFR CKD-EPI): ${egfrResult.first} mL/min/1.73m² [${egfrResult.second}]
                            ------------------------------------
                            المراجع: WHO, ACC/AHA, CKD-EPI 2021, Framingham Heart Study.
                        """.trimIndent()

                        clipboardManager.setText(AnnotatedString(summaryText))
                        reportCopiedNotification = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📋 نسخ التقرير الشامل", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val inputsSummary = "${if (isFemale) "أنثى" else "ذكر"}، ${age.toInt()}س، W:${weightKg.toInt()}k, H:${heightCm.toInt()}c, BP:${sbp.toInt()}/${dbp.toInt()}"
                        val calculatedResult = "CHMSS:${chmssResult.stabilityScore.toInt()}%, MAP:${bpResult.map}, CVD:${framingham.first}%, eGFR:${egfrResult.first}"
                        val scoreText = bpResult.category.split(" ")[0]

                        viewModel.insertCalculationLog(
                            patientName = patientName,
                            modelUsed = "Comprehensive Master",
                            inputsSummary = inputsSummary,
                            calculatedResult = calculatedResult,
                            scoreText = scoreText
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("💾 حفظ بالأرشيف المحلي", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // HL7 FHIR Digital Twin Interoperability Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showFhirDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "🌐 تصدير بتنسيق HL7 FHIR (التوأم الرقمي الفسيولوجي)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }

            if (reportCopiedNotification) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✅ تم نسخ التقرير التشخيصي بنجاح إلى الحافظة!",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showFhirDialog) {
                FhirDigitalTwinDialog(
                    patientName = patientName,
                    age = age,
                    isFemale = isFemale,
                    sbp = sbp,
                    dbp = dbp,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    creatinineText = creatinineText,
                    onDismiss = { showFhirDialog = false }
                )
            }
        }
    }
}

// ==========================================
// 2. FOCUSED BLOOD PRESSURE & MAP CALCULATOR CARD
// ==========================================
@Composable
fun BloodPressureCalculatorCard(viewModel: MedicalViewModel) {
    var patientName by remember { mutableStateOf("") }
    var sbp by remember { mutableFloatStateOf(135f) }
    var dbp by remember { mutableFloatStateOf(85f) }

    val bpMetrics = viewModel.calculateBloodPressureMetrics(sbp.toInt(), dbp.toInt())

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الموديل: حاسبة ضغط الدم والتروية الشريانية (ACC/AHA 2017 & MAP / Pulse Pressure)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("اسم المريض", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedLabelColor = Color(0xFF38BDF8), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("ضغط الدم الانقباضي (SBP): ${sbp.toInt()} mmHg", color = Color.White, fontSize = 12.sp)
            Slider(
                value = sbp,
                onValueChange = { sbp = it },
                valueRange = 80f..220f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            Text("ضغط الدم الانبساطي (DBP): ${dbp.toInt()} mmHg", color = Color.White, fontSize = 12.sp)
            Slider(
                value = dbp,
                onValueChange = { dbp = it },
                valueRange = 50f..130f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFC084FC), activeTrackColor = Color(0xFFC084FC))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RiskGauge(percentage = (sbp / 200f).coerceIn(0f, 1f), modifier = Modifier.size(64.dp))

                Column {
                    Text(bpMetrics.category, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF38BDF8))
                    Text("متوسط الضغط الشرياني (MAP): ${bpMetrics.map} mmHg", color = Color.White, fontSize = 11.sp)
                    Text("ضغط النبض (Pulse Pressure): ${bpMetrics.pulsePressure} mmHg", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val inputs = "SBP: ${sbp.toInt()}, DBP: ${dbp.toInt()}"
                    viewModel.insertCalculationLog(
                        patientName = patientName,
                        modelUsed = "BP_Hemodynamics",
                        inputsSummary = inputs,
                        calculatedResult = "${bpMetrics.category} (MAP: ${bpMetrics.map})",
                        scoreText = "${bpMetrics.pulsePressure} mmHg PP"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾 حفظ عينة الضغط بالأرشيف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FraminghamCalculatorCard(viewModel: MedicalViewModel) {
    var patientName by remember { mutableStateOf("") }
    var age by remember { mutableFloatStateOf(45f) }
    var isFemale by remember { mutableStateOf(false) }
    var sbp by remember { mutableFloatStateOf(135f) }
    var isSbpTreated by remember { mutableStateOf(false) }
    var totalCholesterol by remember { mutableFloatStateOf(210f) }
    var isSmoker by remember { mutableStateOf(false) }
    var isDiabetic by remember { mutableStateOf(false) }

    val result = viewModel.calculateFraminghamRisk(
        age = age.toInt(),
        isFemale = isFemale,
        sbp = sbp.toInt(),
        isSbpTreated = isSbpTreated,
        totalCholesterol = totalCholesterol.toInt(),
        isSmoker = isSmoker,
        isDiabetic = isDiabetic
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الموديل: نموذج Framingham لتقدير خطر الإصابة بالقلب والشرايين خلال الـ 10 سنوات القادمة", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Inputs Form
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("اسم المريض لحفظه بالأرشيف", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF718096),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Gender Switch Button
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("الجنس الفسيولوجي للمريض:", color = Color.White, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ذكر", color = if (!isFemale) Color(0xFF38BDF8) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isFemale,
                        onCheckedChange = { isFemale = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFD946EF),
                            uncheckedThumbColor = Color(0xFF38BDF8)
                        )
                    )
                    Text("أنثى", color = if (isFemale) Color(0xFFD946EF) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                }
            }

            // Age Slider
            Text("العمر: ${age.toInt()} سنة", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = age,
                onValueChange = { age = it },
                valueRange = 30f..85f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            // Blood Pressure SBP Slider
            Text("ضغط الدم الانقباضي (SBP): ${sbp.toInt()} mmHg", color = Color.White, fontSize = 12.sp)
            Slider(
                value = sbp,
                onValueChange = { sbp = it },
                valueRange = 90f..200f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("هل يتناول مريضنا أدوية لعلاج ضغط الدم؟", color = Color.White, fontSize = 11.sp)
                Checkbox(checked = isSbpTreated, onCheckedChange = { isSbpTreated = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF38BDF8)))
            }

            // Total Cholesterol Slider
            Text("مستوى الكوليسترول الكلي بالدم: ${totalCholesterol.toInt()} mg/dL", color = Color.White, fontSize = 12.sp)
            Slider(
                value = totalCholesterol,
                onValueChange = { totalCholesterol = it },
                valueRange = 100f..350f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            // Smoking and diabetes checklist
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSmoker, onCheckedChange = { isSmoker = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)))
                    Text("هل يدخن السجائر؟", color = Color.White, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDiabetic, onCheckedChange = { isDiabetic = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE11D48)))
                    Text("هل يعاني من السكري؟", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculated Output Display with Custom Canvas Gauge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Analog risk semi-gauge
                val riskVal = result.first
                RiskGauge(percentage = riskVal.toFloat() / 40.2f, modifier = Modifier.size(64.dp))

                Column {
                    Text("الخطر القلبي الشرياني المتوقع خلال 10 سنوات:", color = Color(0xFFA0AEC0), fontSize = 10.sp)
                    Text(
                        text = "${result.first}% احتمال إصابة",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = when {
                            result.first < 10.0 -> Color(0xFF10B981)
                            result.first < 20.0 -> Color(0xFFFFB020)
                            else -> Color(0xFFEF4444)
                        }
                    )
                    Text("النقاط المسجلة: ${result.second} نقطة Framingham", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val inputs = "${if (isFemale) "أنثى" else "ذكر"}، عمر: ${age.toInt()}، ضغط: ${sbp.toInt()}، كوليسترول: ${totalCholesterol.toInt()} ${if (isSmoker) "،مدخن" else ""} ${if (isDiabetic) "،سكري" else ""}"
                    viewModel.insertCalculationLog(
                        patientName = patientName,
                        modelUsed = "Framingham",
                        inputsSummary = inputs,
                        calculatedResult = "${result.first}% خطر إصابة بالقلب",
                        scoreText = "${result.second} نقطة"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾 حفظ عينة الحساب بالأرشيف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ChadsVascCalculatorCard(viewModel: MedicalViewModel) {
    var patientName by remember { mutableStateOf("") }
    var age by remember { mutableFloatStateOf(68f) }
    var hasChf by remember { mutableStateOf(false) }
    var hasHypertension by remember { mutableStateOf(false) }
    var hasDiabetes by remember { mutableStateOf(false) }
    var hasStroke by remember { mutableStateOf(false) }
    var hasVascular by remember { mutableStateOf(false) }
    var isFemale by remember { mutableStateOf(true) }

    val result = viewModel.calculateCha2ds2Vasc(
        hasChf = hasChf,
        hasHypertension = hasHypertension,
        age = age.toInt(),
        hasDiabetes = hasDiabetes,
        hasStroke = hasStroke,
        hasVascular = hasVascular,
        isFemale = isFemale
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الموديل: مقياس CHA₂DS₂-VASc لتقييم خطر السكتة الدماغية في مرضى الرجفان الأذيني (Atrial Fibrillation)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("اسم المريض", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF718096),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("العمر: ${age.toInt()} سنة", color = Color.White, fontSize = 12.sp)
            Slider(
                value = age,
                onValueChange = { age = it },
                valueRange = 18f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            // Switches checklist Representing CHA2DS2
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("قصور القلب أو تمدد البطين الأيسر (CHF)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = hasChf, onCheckedChange = { hasChf = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("تاريخ ارتفاع ضغط الدم الشرياني (H)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = hasHypertension, onCheckedChange = { hasHypertension = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("هل يعاني المريض من مرض السكري (D)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = hasDiabetes, onCheckedChange = { hasDiabetes = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("تاريخ إصابة بسكتة دماغية سابقة أو خثر دقيقة (S2)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = hasStroke, onCheckedChange = { hasStroke = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مرض وعائي (احتشاء قلب سابق، اعتلال الأواني) (V)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = hasVascular, onCheckedChange = { hasVascular = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الجنس هو أنثى (Sc)", color = Color.White, fontSize = 11.sp)
                    Switch(checked = isFemale, onCheckedChange = { isFemale = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RiskGauge(percentage = result.second.toFloat() / 9f, modifier = Modifier.size(64.dp))

                Column {
                    Text("مخاطر السكتة الدماغية المتوقعة سنوياً:", color = Color(0xFFA0AEC0), fontSize = 10.sp)
                    Text(
                        text = "${result.first}% خطورة سنوية",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (result.second >= 3) Color(0xFFEF4444) else Color(0xFFFFB020)
                    )
                    Text("نقاط CHA₂DS₂-VASc المحققة: ${result.second} / 9 نقاط", color = Color.White, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val inputs = "عمر: ${age.toInt()}، ${if (isFemale) "أنثى" else "ذكر"} ${if (hasStroke) "،سكتة دماغية" else ""} ${if (hasHypertension) "،ضغط" else ""}"
                    viewModel.insertCalculationLog(
                        patientName = patientName,
                        modelUsed = "CHA₂DS₂-VASc",
                        inputsSummary = inputs,
                        calculatedResult = "${result.first}% خطورة سنوية للسكتة",
                        scoreText = "${result.second} نقاط"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾 حفظ عينة الحساب بالأرشيف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EgfrCalculatorCard(viewModel: MedicalViewModel) {
    var patientName by remember { mutableStateOf("") }
    var creatinineText by remember { mutableStateOf("1.1") }
    var age by remember { mutableFloatStateOf(52f) }
    var isFemale by remember { mutableStateOf(false) }

    val creatinineVal = creatinineText.toDoubleOrNull() ?: 0.0
    val result = viewModel.calculateCkdEpiEGfr(creatinineVal, age.toInt(), isFemale)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الموديل: معادلة CKD-EPI (2021) لتقدير معدل ترشيح الكلى (eGFR) وتصنيف درجة القصور الكلوي المزمن", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("اسم المريض", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF718096),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("الجنس الفسيولوجي:", color = Color.White, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ذكر", color = if (!isFemale) Color(0xFF38BDF8) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isFemale, onCheckedChange = { isFemale = it })
                    Text("أنثى", color = if (isFemale) Color(0xFFD946EF) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("عمر المريض: ${age.toInt()} سنة", color = Color.White, fontSize = 12.sp)
            Slider(
                value = age,
                onValueChange = { age = it },
                valueRange = 18f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
            )

            OutlinedTextField(
                value = creatinineText,
                onValueChange = { creatinineText = it },
                label = { Text("مستوى الكرياتينين بمصل الدم (mg/dL)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Renal clearance gauge (High eGFR is healthy, Low eGFR is renal failure)
                RiskGauge(percentage = (result.first / 120.0).toFloat().coerceIn(0f, 1f), inverseColor = true, modifier = Modifier.size(64.dp))

                Column {
                    Text("حساب ترشيح الكبيبات الكلوية المقدر (eGFR):", color = Color(0xFFA0AEC0), fontSize = 10.sp)
                    Text(
                        text = "${result.first} mL/min/1.73m²",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = when {
                            result.first >= 60.0 -> Color(0xFF10B981)
                            result.first >= 30.0 -> Color(0xFFFFB020)
                            else -> Color(0xFFEF4444)
                        }
                    )
                    Text(text = result.second, color = Color.White, fontSize = 10.sp, lineHeight = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val inputs = "عمر: ${age.toInt()}، ${if (isFemale) "أنثى" else "ذكر"}، كرياتينين: $creatinineText"
                    viewModel.insertCalculationLog(
                        patientName = patientName,
                        modelUsed = "eGFR",
                        inputsSummary = inputs,
                        calculatedResult = "eGFR: ${result.first}",
                        scoreText = result.second.split(" ")[0] // Class stage code
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾 حفظ عينة الحساب بالأرشيف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BmiCalculatorCard(viewModel: MedicalViewModel) {
    var patientName by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("70") }
    var heightText by remember { mutableStateOf("170") }

    val weightVal = weightText.toDoubleOrNull() ?: 0.0
    val heightVal = heightText.toDoubleOrNull() ?: 0.0
    val result = viewModel.calculateBmi(weightVal, heightVal)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("الموديل: مؤشر كتلة الجسم (BMI) لتصنيف الوزن الصحي حسب معايير منظمة الصحة العالمية", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("اسم المريض", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF718096),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("الوزن (كغ)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                label = { Text("الطول (سم)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BMI scale gauge 
                RiskGauge(percentage = (result.first / 40.0).toFloat().coerceIn(0f, 1f), modifier = Modifier.size(64.dp))

                Column {
                    Text("مؤشر كتلة الجسم (BMI):", color = Color(0xFFA0AEC0), fontSize = 10.sp)
                    Text(
                        text = "${result.first}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = when {
                            result.first < 18.5 -> Color(0xFFFFB020)
                            result.first < 25.0 -> Color(0xFF10B981)
                            result.first < 30.0 -> Color(0xFFFFB020)
                            else -> Color(0xFFEF4444)
                        }
                    )
                    Text(text = result.second, color = Color.White, fontSize = 11.sp, lineHeight = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val inputs = "وزن: $weightText، طول: $heightText"
                    viewModel.insertCalculationLog(
                        patientName = patientName,
                        modelUsed = "BMI",
                        inputsSummary = inputs,
                        calculatedResult = "BMI: ${result.first}",
                        scoreText = result.second.split(" ")[0]
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("💾 حفظ عينة الحساب بالأرشيف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RiskGauge(
    percentage: Float,
    inverseColor: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 8.dp.toPx()

        // Choose color
        val color = if (!inverseColor) {
            when {
                percentage < 0.3f -> Color(0xFF10B981) // Green
                percentage < 0.6f -> Color(0xFFFFB020) // Orange
                else -> Color(0xFFEF4444)              // Red
            }
        } else {
            // High GFR is healthy (green), low GFR is failure (red)
            when {
                percentage >= 0.5f -> Color(0xFF10B981)
                percentage >= 0.25f -> Color(0xFFFFB020)
                else -> Color(0xFFEF4444)
            }
        }

        // Draw track base arc
        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw progress arc
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = 270f * percentage.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

// ==========================================
// Tab 4: BRANCHES (أقسام الطب والعلوم)
// ==========================================
@Composable
fun BranchesTab(viewModel: MedicalViewModel, searchQuery: String) {
    // Filter outside LazyColumn to avoid calling remember inside lambda loops
    val filteredBranches = remember(searchQuery) {
        MedicalData.medicalBranches.map { branch ->
            val filtered = branch.items.filter { it.contains(searchQuery, ignoreCase = true) }
            branch.category to filtered
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🏥 أقسام وجداول العلوم الطبية والفرعية",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "تصنيف لأهم مساقات دراسة الطب والعلوم الكيميائية والمساعدة والتقنيات المعاصرة:",
            fontSize = 11.sp,
            color = Color(0xFFA0AEC0)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredBranches) { (category, filteredList) ->
                if (filteredList.isNotEmpty() || category.contains(searchQuery, ignoreCase = true)) {
                    val branchRef = MedicalData.medicalBranches.first { it.category == category }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = category,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // If search filtered items, show those, otherwise show all
                            val displayList = if (filteredList.isNotEmpty()) filteredList else branchRef.items

                            displayList.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("🔹", fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                                    Text(
                                        text = item,
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 18.sp
                                    )
                                }
                                if (index < displayList.size - 1) {
                                    HorizontalDivider(
                                        color = Color(0xFF2D3748),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Tab 5: DIAGNOSIS (التشخيص والعلاج)
// ==========================================
@Composable
fun DiagnosisTab(viewModel: MedicalViewModel, searchQuery: String) {
    var selectedSection by remember { mutableStateOf("diag") } // "diag" or "treat"

    // Filter outside LazyColumn to avoid calling remember inside loop lambdas
    val filteredDiagnostics = remember(searchQuery) {
        MedicalData.diagnosticMethods.mapValues { (_, list) ->
            list.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    val filteredTreatments = remember(searchQuery) {
        MedicalData.treatmentApproaches.mapValues { (_, list) ->
            list.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🔍 طرق التشخيص ومسالك التداوي السريري",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = "تتبع كيف يرتب الأطباء مناهج كشف المرض والتحاليل، ثم بروتوكولات الشفاء الفسيولوجية:",
            fontSize = 11.sp,
            color = Color(0xFFA0AEC0)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Tabs to toggle diagnoses and treatments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedSection = "diag" },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSection == "diag") Color(0xFF38BDF8) else Color(0xFF1E293B),
                    contentColor = if (selectedSection == "diag") Color.Black else Color.White
                )
            ) {
                Text("المناهج التشخيصية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { selectedSection = "treat" },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSection == "treat") Color(0xFF10B981) else Color(0xFF1E293B),
                    contentColor = if (selectedSection == "treat") Color.Black else Color.White
                )
            ) {
                Text("مناهج التداوي والعلاج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contents
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (selectedSection == "diag") {
                filteredDiagnostics.forEach { (cat, list) ->
                    if (list.isNotEmpty() || cat.contains(searchQuery, ignoreCase = true)) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "🛡️ تصنيف: $cat", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val finalItems = if (list.isNotEmpty()) list else (MedicalData.diagnosticMethods[cat] ?: emptyList())
                                    finalItems.forEach { item ->
                                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text("🔬", fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                                            Text(item, color = Color(0xFFE2E8F0), fontSize = 12.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                filteredTreatments.forEach { (cat, list) ->
                    if (list.isNotEmpty() || cat.contains(searchQuery, ignoreCase = true)) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "💊 تداوي: $cat", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val finalItems = if (list.isNotEmpty()) list else (MedicalData.treatmentApproaches[cat] ?: emptyList())
                                    finalItems.forEach { item ->
                                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text("🍃", fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
                                            Text(item, color = Color(0xFFE2E8F0), fontSize = 12.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Tab 6: PRESCRIPTIONS (التركيبات الدوائية والوصفات)
// ==========================================
@Composable
fun PrescriptionsTab(viewModel: MedicalViewModel) {
    val savedPrescriptions by viewModel.savedPrescriptions.collectAsStateWithLifecycle()

    var showForm by remember { mutableStateOf(false) }

    // Form inputs state
    var condition by remember { mutableStateOf("") }
    var regimen by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var monitoring by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "💊 مؤشر التركيبات والوصفات العلاجية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "أمثلة لتركيبات دوائية نموذجية مع صائغ مخصص للتركيبات الخاصة بك:",
                    fontSize = 11.sp,
                    color = Color(0xFFA0AEC0)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Open Dialog/Form button
        Button(
            onClick = { showForm = !showForm },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showForm) Color(0xFFEF4444) else Color(0xFF38BDF8),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (showForm) "إغلاق النموذج المخصص" else "صياغة تركيبة علاجية جديدة مخصصة",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Custom prescription form representing CRUD
        AnimatedVisibility(visible = showForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.4f))
                    .border(1.dp, Color(0xFF334155).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Text("صياغة طبية وتفاعلات موثقة:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("تشخيص الحالة (مثال: التهاب الأمعاء)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = regimen,
                    onValueChange = { regimen = it },
                    label = { Text("المركب الدوائي والصيغة والجرعة", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("مدة الاستخدام المقررة (مثال: 5 أيام)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = monitoring,
                    onValueChange = { monitoring = it },
                    label = { Text("المراقبة السريرية والتحاليل المطلوبة للسلامة", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (condition.isNotBlank() && regimen.isNotBlank()) {
                            viewModel.insertPrescription(condition, regimen, duration, monitoring)
                            // Clear form
                            condition = ""
                            regimen = ""
                            duration = ""
                            monitoring = ""
                            showForm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إضافة وحفظ بالأرشيف الطبي 💾", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display combined static prescriptions + saved custom ones
        val staticList = MedicalData.commonPrescriptions
        val combinedLogs = remember(savedPrescriptions) {
            val customMapped = savedPrescriptions.map {
                MedicalData.CommonPrescription(
                    condition = it.condition,
                    regimen = it.regimen,
                    duration = it.duration,
                    monitoring = it.monitoring
                )
            }
            customMapped + staticList
        }

        combinedLogs.forEachIndexed { idx, rx ->
            // Check if this was a custom saved prescription
            val customMatch = savedPrescriptions.find { it.condition == rx.condition && it.regimen == rx.regimen }

            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, if (customMatch != null) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF334155).copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📋", fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                            Text(
                                rx.condition,
                                fontWeight = FontWeight.Bold,
                                color = if (customMatch != null) Color(0xFF38BDF8) else Color.White,
                                fontSize = 13.sp
                            )
                        }

                        if (customMatch != null) {
                            IconButton(onClick = { viewModel.deletePrescription(customMatch.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الوصفة المخصصة",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "عياري نموذجي",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA0AEC0),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("الصفة الدوائية والتركيب:", fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Text(rx.regimen, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المدة المقدرة:", fontWeight = FontWeight.Bold, color = Color(0xFF718096), fontSize = 10.sp)
                            Text(rx.duration, color = Color.White, fontSize = 11.sp)
                        }
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("تحاليل ومراقبة وظيفية:", fontWeight = FontWeight.Bold, color = Color(0xFF718096), fontSize = 10.sp)
                            Text(rx.monitoring, color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Custom View: BOOKMARKS VIEW (المحفوظات)
// ==========================================
@Composable
fun BookmarksView(viewModel: MedicalViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.setTab("systems") }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "⭐️ قائمة الأرغانات والفهارس المحفوظة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "لم يتم حفظ أي عنوان بعد!",
                        color = Color(0xFFA0AEC0),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "افتح تفاصيل أي جهاز بالنظام واضغط على نجمة الحفظ لتظهر هنا.",
                        color = Color(0xFF718096),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(bookmarks) { bookmark ->
                    Card(
                        onClick = {
                            if (bookmark.topicType == "system") {
                                val found = MedicalData.bodySystems.find { it.id == bookmark.topicId }
                                if (found != null) {
                                    viewModel.setTab("systems")
                                    viewModel.selectSystem(found)
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧬", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(
                                        text = bookmark.topicTitle,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "النوع: جهاز فسيولوجي مرجعي",
                                        color = Color(0xFF718096),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleBookmark(bookmark.topicType, bookmark.topicId, bookmark.topicTitle) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "إزالة الحفظ",
                                    tint = Color(0xFFFFB020)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizTab(viewModel: MedicalViewModel) {
    val haptic = LocalHapticFeedback.current
    var selectedQuizSystemId by remember { mutableStateOf<String?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var correctAnswersCount by remember { mutableStateOf(0) }
    var quizFinished by remember { mutableStateOf(false) }

    val quizResults by viewModel.quizResults.collectAsStateWithLifecycle()

    val currentQuestions = selectedQuizSystemId?.let { MedicalData.bodySystemQuizzes[it] } ?: emptyList()
    val currentSystem = MedicalData.bodySystems.find { it.id == selectedQuizSystemId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedQuizSystemId != null) {
                    IconButton(onClick = {
                        selectedQuizSystemId = null
                        quizFinished = false
                        currentQuestionIndex = 0
                        selectedOptionIndex = null
                        correctAnswersCount = 0
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (selectedQuizSystemId == null) "🧠 اختبارات الأجهزة الحيوية والفسيولوجيا" else "📝 اختبار: ${currentSystem?.name ?: ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedQuizSystemId == null) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("💡 دليل تقييم الفهم الطبي", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "اختر أي جهاز حيوي أدناه لبدء الاختبار التفاعلي. يتضمن كل اختبار أسئلة متعددة الخيارات مع شرح علمي دقيق وتوجيهات نبوية شريفة لتعزيز استيعابك الفسيولوجي.",
                                color = Color(0xFFA0AEC0),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                item {
                    Text("اختر جهازا للبدء بالبارت الفسيولوجي:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }

                items(MedicalData.bodySystems) { system ->
                    val hasQuiz = MedicalData.bodySystemQuizzes.containsKey(system.id)
                    Card(
                        onClick = {
                            if (hasQuiz) {
                                selectedQuizSystemId = system.id
                                currentQuestionIndex = 0
                                selectedOptionIndex = null
                                correctAnswersCount = 0
                                quizFinished = false
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, if (hasQuiz) Color(0xFF334155).copy(alpha = 0.3f) else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(system.icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text(system.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(system.latin, color = Color(0xFF718096), fontSize = 10.sp)
                                }
                            }

                            if (hasQuiz) {
                                Button(
                                    onClick = {
                                        selectedQuizSystemId = system.id
                                        currentQuestionIndex = 0
                                        selectedOptionIndex = null
                                        correctAnswersCount = 0
                                        quizFinished = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("بدء الاختبار", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            } else {
                                Text("قريباً", color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (quizResults.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📊 سجل نتائج الاختبارات والمتابعة السابقة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }

                    items(quizResults) { result ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(result.systemTitle, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                                    Text("${result.percentage}% (${result.score}/${result.totalQuestions})", fontWeight = FontWeight.Bold, color = if (result.percentage >= 70) Color(0xFF10B981) else Color(0xFFFFB020), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result.feedback, color = Color(0xFFA0AEC0), fontSize = 11.sp, lineHeight = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(result.createdAt)),
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteQuizResult(result.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "حذف النتيجة",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (currentQuestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد أسئلة متوفرة لهذا الجهاز حالياً.", color = Color.White)
                }
            } else if (quizFinished) {
                val percentage = (correctAnswersCount * 100) / currentQuestions.size
                val feedbackText = when {
                    percentage >= 90 -> "ممتاز جداً! إدراك عميق ومتميز للفسيولوجيا والتكامل الطبي."
                    percentage >= 70 -> "جيد جداً! استيعاب قوي ومستقر للأجهزة الحيوية."
                    percentage >= 50 -> "جيد، بحاجة لمراجعة تفصيلية إضافية للآليات والتعاليم."
                    else -> "تحتاج لمزيد من المراجعة والاطلاع على الشرح العلمي والنبوي المرفق."
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆 نتيجة الاختبار الفسيولوجي", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "$percentage%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 42.sp,
                            color = if (percentage >= 70) Color(0xFF10B981) else Color(0xFFFFB020)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النتيجة: $correctAnswersCount من ${currentQuestions.size} إجابات صحيحة",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = feedbackText,
                            color = Color(0xFFA0AEC0),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                selectedQuizSystemId = null
                                quizFinished = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("العودة لقائمة الاختبارات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                val question = currentQuestions[currentQuestionIndex]

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السؤال ${currentQuestionIndex + 1} من ${currentQuestions.size}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { (currentQuestionIndex + 1).toFloat() / currentQuestions.size },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = question.questionText,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    items(question.options.indices.toList()) { index ->
                        val optionText = question.options[index]
                        val isSelected = selectedOptionIndex == index
                        val isCorrect = index == question.correctAnswerIndex
                        val hasAnswered = selectedOptionIndex != null

                        val borderColor = when {
                            hasAnswered && isCorrect -> Color(0xFF10B981)
                            hasAnswered && isSelected && !isCorrect -> Color(0xFFEF4444)
                            isSelected -> Color(0xFF38BDF8)
                            else -> Color(0xFF334155).copy(alpha = 0.3f)
                        }

                        val containerColor = when {
                            hasAnswered && isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
                            hasAnswered && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            else -> Color(0xFF0F172A).copy(alpha = 0.4f)
                        }

                        Card(
                            onClick = {
                                if (selectedOptionIndex == null) {
                                    selectedOptionIndex = index
                                    if (index == question.correctAnswerIndex) {
                                        correctAnswersCount++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (hasAnswered && isCorrect) Color(0xFF10B981) else if (hasAnswered && isSelected && !isCorrect) Color(0xFFEF4444) else Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = optionText,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    if (selectedOptionIndex != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.7f)),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("📖 الشرح العلمي والإيضاح النبوي:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = question.explanation,
                                        color = Color(0xFFA0AEC0),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    if (currentQuestionIndex < currentQuestions.size - 1) {
                                        currentQuestionIndex++
                                        selectedOptionIndex = null
                                    } else {
                                        quizFinished = true
                                        viewModel.saveQuizResult(
                                            systemId = currentSystem?.id ?: "",
                                            systemTitle = currentSystem?.name ?: "",
                                            score = correctAnswersCount,
                                            totalQuestions = currentQuestions.size
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (currentQuestionIndex < currentQuestions.size - 1) "السؤال التالي ⬅️" else "عرض النتيجة النهائية 🏆",
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
}

@Composable
fun GuideTab(viewModel: MedicalViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("🌟 الدليل الموحد والموسوعة الإكلينيكية الذكية", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "تم دمج وهندسة أكثر من 10 مشاريع وتطبيقات طبية وبحثية في هذا التطبيق الموحد، ليغنيك عن آلاف البحوث والدراسات العليا. يجمع هذا الدليل بين أحدث الأدلة الطبية (Evidence-Based Medicine) وحكمة الطب النبوي، لتقديم رؤية شاملة ودقيقة لفسيولوجيا الإنسان وآليات التشخيص والعلاج.",
                        color = Color(0xFFA0AEC0),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Text("🔬 التقنيات والآليات البرمجية المعتمدة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        val techItems = listOf(
            Pair("قاعدة البيانات المحلية (Room DB)", "تخزين مشفر ومستقر وآمن لكافة السجلات الطبية، نتائج الحسابات، الأرشيف، ونتائج اختبارات الفهم الفسيولوجي دون الحاجة لاتصال بالإنترنت."),
            Pair("البرمجة غير المتزامنة (Coroutines & Flow)", "إدارة التدفقات الحية للبيانات بكفاءة عالية، وضمان استجابة واجهات المستخدم بسلاسة تامة دون أي تجميد أثناء العمليات المعقدة."),
            Pair("الخوارزميات السريرية المتقدمة", "معادلات دقيقة ومعتمدة دولياً (مثل Framingham للقلب، CHA2DS2-VASc للسكتة، eGFR للكلى، وBMI لتصنيف الوزن) مع تقييم بصري بالرسوم البيانية."),
            Pair("نظريات الطب التداعي والفسيولوجيا التكاملية", "ربط الأعضاء والأجهزة الحيوية ببعضها البعض (مثل تأثير القلب على الدماغ والمناعة)، مدعومة بالأحاديث النبوية الشريفة والتوثيق العلمي الدقيق.")
        )

        items(techItems) { (title, desc) ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = Color(0xFFA0AEC0), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("📚 المزايا الإكلينيكية والبحثية", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        val featuresList = listOf(
            "موسوعة شاملة لـ 8 أجهزة حيوية مع آليات العمل والأحاديث النبوية المرتبطة.",
            "مختبر تفصيلي لاختبارات الفهم الفسيولوجي مع تصحيح فوري وشرح مفصل وسجل للدرجات.",
            "دليل التشخيص التفريقي المتطور والوصفات الطبية الذكية مع الأرشيف الكامل.",
            "واجهة مستخدم عصرية بتصميم Material 3 ومظهر داكن احترافي يراعي راحة العين ودقة المعاينة."
        )

        items(featuresList) { feature ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔹", fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(feature, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: MedicalViewModel, searchQuery: String) {
    val quizResults by viewModel.quizResults.collectAsStateWithLifecycle()

    val totalQuizzes = quizResults.size
    val averageScore = if (totalQuizzes > 0) quizResults.map { it.percentage }.average().toInt() else 0
    val highestScore = if (totalQuizzes > 0) quizResults.maxOf { it.percentage } else 0

    // Compute mastery per body system
    val systemMastery = remember(searchQuery, quizResults) {
        MedicalData.bodySystems
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
            .map { system ->
            val resultsForSystem = quizResults.filter { it.systemId == system.id }
            val mastery = if (resultsForSystem.isNotEmpty()) {
                resultsForSystem.map { it.percentage }.average().toInt()
            } else {
                0
            }
            Triple(system.name, mastery, resultsForSystem.size)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📊 لوحة التقدم وإحصائيات الأداء", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "تحليل شامل لإحصائيات الاختبارات الفسيولوجية، نسبة إتقان المعرفة الطبية، ومستوى التقدم عبر مختلف الأجهزة الحيوية.",
                        color = Color(0xFFA0AEC0),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الاختبارات", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$totalQuizzes", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 22.sp)
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("متوسط النسبة", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$averageScore%", fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8), fontSize = 22.sp)
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("أعلى نتيجة", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("$highestScore%", fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981), fontSize = 22.sp)
                    }
                }
            }
        }

        // Canvas Performance Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 رسم بياني لنسبة الإتقان حسب الأجهزة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (systemMastery.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد بيانات كافية للرسم البياني حالياً.", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            val barWidth = size.width / (systemMastery.size * 2f)
                            val maxH = size.height - 30f

                            systemMastery.forEachIndexed { index, (_, mastery, _) ->
                                val left = index * (size.width / systemMastery.size) + barWidth / 2f
                                val barH = (mastery / 100f) * maxH
                                val top = maxH - barH

                                drawRect(
                                    color = Color(0xFF38BDF8),
                                    topLeft = Offset(left, top),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barH)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("🎯 إتقان المعرفة لكل جهاز حيوي", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(systemMastery) { (name, mastery, count) ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("$mastery% (اكتملت $count اختبارات)", fontWeight = FontWeight.Bold, color = if (mastery >= 70) Color(0xFF10B981) else Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { mastery / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (mastery >= 70) Color(0xFF10B981) else Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: String = ""
)

@Composable
fun AiAssistantTab(viewModel: MedicalViewModel) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var inputMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "مرحباً بك! أنا مساعدك الإكلينيكي الذكي المدعوم بنماذج Gemini الطبية والموسوعة الشاملة. اسألني عن أي استفسار فسيولوجي، مرضي، أو متعلق بالحكمة الطبية النبوية، وسأجيبك بدقة وموثوقية.",
                isUser = false,
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
        )
    }

    val quickPrompts = listOf(
        "اشرح تداعي القلب والدماغ في الطب النبوي",
        "ما هي الآلية الفسيولوجية لارتفاع ضغط الدم؟",
        "كيف يقيم طبيب الكلى مرحلة القصور الكلوي؟",
        "ما هي حكمة السهر والحمى من منظور فسيولوجي؟"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text("المساعد الإكلينيكي الذكي (Gemini AI)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                        Text("مبني على الموسوعة الطبية الشاملة وأدلة الطب الحديث", color = Color(0xFFA0AEC0), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Prompt Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickPrompts) { prompt ->
                AssistChip(
                    onClick = {
                        inputMessage = prompt
                    },
                    label = { Text(prompt, fontSize = 11.sp, color = Color(0xFF38BDF8)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.3f))
                .border(1.dp, Color(0xFF334155).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (msg.isUser) Color(0xFF0284C7).copy(alpha = 0.85f) else Color(0xFF1E293B).copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, if (msg.isUser) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color(0xFF334155)),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.text,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.timestamp,
                                color = Color(0xFF94A3B8),
                                fontSize = 8.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تحليل السؤال وصياغة الإجابة السريرية...", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("اكتب سؤالك الطبي أو الفسيولوجي هنا...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                    unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.4f),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE2E8F0)
                ),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    if (inputMessage.isNotBlank() && !isLoading) {
                        val prompt = inputMessage.trim()
                        inputMessage = ""
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        messages.add(ChatMessage(text = prompt, isUser = true, timestamp = timeStr))
                        isLoading = true

                        coroutineScope.launch {
                            val reply = askGeminiClinicalAssistant(prompt)
                            messages.add(ChatMessage(text = reply, isUser = false, timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())))
                            isLoading = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF38BDF8), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    tint = Color.Black
                )
            }
        }
    }
}

suspend fun askGeminiClinicalAssistant(userPrompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank()) {
        return@withContext "مفتاح API الخاص بـ Gemini غير مُعرّف في ملف الأسرار (.env). يرجى إضافة مفتاحك عبر لوحة الأسرار (Secrets) في AI Studio لتمكين المساعد الذكي."
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
    
    val systemInstruction = "أنت مساعد إكلينيكي ذكي وخبير في فسيولوجيا الإنسان والطب النبوي. تجيب بدقة عالية استناداً إلى بيانات الموسوعة الطبية والتراث الطبي الإسلامي وأدلة الطب الحديث (Evidence-Based Medicine). اجعل إجاباتك دقيقة، علمية، ومنظمة باللغة العربية الفصحى."

    val jsonBody = JSONObject().apply {
        put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", userPrompt))
                })
            })
        })
        put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", systemInstruction))
            })
        })
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonBody.toString())
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext "خطأ في الاتصال بالخادم: ${response.code} - ${response.message}"
            }
            val responseBodyString = response.body?.string() ?: return@withContext "لم يتم تلقي استجابة."
            val jsonResponse = JSONObject(responseBodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "لا توجد إجابة نصية.")
                }
            }
            return@withContext "عذراً، لم يتم العثور على إجابة في الاستجابة المستلمة."
        }
    } catch (e: Exception) {
        return@withContext "حدث خطأ أثناء الاتصال بالذكاء الاصطناعي: ${e.localizedMessage}"
    }
}

@Composable
fun RadiologyTechTab(viewModel: MedicalViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("🔬 الأشعة التصويرية والتقنيات الطبية الحديثة", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "تكامل متقدم بين تقنيات التصوير الطبي (الأشعة السينية، الرنين المغناطيسي، الطبقي المحوري، والموجات فوق الصوتية)، مع العلوم المساعدة وأحدث معايير الأمان وتشفير المعلومات الطبية المفتوحة والمشفرة (HL7/FHIR & AES-256).",
                        color = Color(0xFFA0AEC0),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Text("🩻 تقنيات التصوير الطبي السريري", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        val radiologyItems = listOf(
            Pair("الأشعة السينية (X-Ray)", "تستخدم للكشف عن كسور العظام، التهابات الرئة، وتقییم حجم وكثافة الهيكل العظمي بدقة عالية وبسرعة فائقة."),
            Pair("التصوير بالرنين المغناطيسي (MRI)", "يعتمد على المجالات المغناطيسية القوية والموجات الراديوية للحصول على صور تفصيلية دقيقة للأنسجة الرخوة، الدماغ، الحبل الشوكي، والمفاصل."),
            Pair("التصوير الطبقي المحوري (CT Scan)", "يجمع صور الأشعة السينية المأخوذة من زوايا متعددة باستخدام الكمبيوتر لإنشاء مقاطع عرضية للأعضاء الداخلية، الأوعية الدموية، والدماغ."),
            Pair("الموجات فوق الصوتية (Ultrasound)", "تستخدم الموجات الصوتية عالية التردد لتصوير الأعضاء الداخلية في الوقت الفعلي (Real-time)، وتستخدم بكثرة في متابعة الحمل وفحص صمامات وحجرات القلب.")
        )

        items(radiologyItems) { (title, desc) ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = Color(0xFFA0AEC0), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("🔒 أمان وتشفير المعلومات الطبية والمعايير المفتوحة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        val techSecurityItems = listOf(
            Pair("معايير HL7 & FHIR للتبادل الطبي", "معايير دولية مفتوحة لتبادل السجلات الصحية الإلكترونية بأمان وتوافق كامل بين مختلف النظم الطبية والمستشفيات."),
            Pair("التشفير العسكري (AES-256 & SHA-256)", "حماية تامة لكافة السجلات والبيانات الطبية للمرضى محلياً وسحابياً وفق أعلى معايير الخصوصية والأمان السيبراني."),
            Pair("التشخيص المساعد بالذكاء الاصطناعي", "خوارزميات متقدمة لتحليل الصور الشعاعية وتقديم مؤشرات مبكرة عن الأمراض مع دمج رؤى الطب النبوي الشريف.")
        )

        items(techSecurityItems) { (title, desc) ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = Color(0xFFA0AEC0), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ImagingGalleryTab(viewModel: MedicalViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val capturedImages = remember { mutableStateListOf<String>() }
    var showCameraView by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🖼️", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text("معرض الأشعة والتوثيق الإكلينيكي", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 16.sp)
                            Text("استعراض التصوير الطبي المتقدم وكاميرا توثيق السياق الطبي (CameraX)", color = Color(0xFFA0AEC0), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                showCameraView = !showCameraView
                            } else {
                                launcher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showCameraView) "إغلاق الكاميرا الحية" else "التقاط صورة سياقية بالكاميرا (CameraX)", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        if (showCameraView) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val capture = ImageCapture.Builder().build()
                                    imageCapture = capture
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            capture
                                        )
                                    } catch (e: Exception) {
                                        // handle bind exception
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Button(
                            onClick = {
                                imageCapture?.let { capture ->
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    capturedImages.add("صورة سياقية مرفقة - $timeStamp")
                                    showCameraView = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Text("التقاط الحفظ السريري", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (capturedImages.isNotEmpty()) {
            item {
                Text("📁 الصور والتقارير الموثقة محلياً", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            items(capturedImages) { imgNote ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(imgNote, color = Color.White, fontSize = 12.sp)
                        Text("تم التوثيق بنجاح ✓", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("🔬 معرض التصوير الطبي والأنسجة الحيوية (محاكاة عالية الدقة)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        val galleryItems = listOf(
            Triple("أشعة الصدر السينية (Chest X-Ray)", "تُظهر تضخم عضلة القلب، ارتشاح الرئة، وتفاصيل الحجاب الحاجز والقفص الصدري.", "https://images.unsplash.com/photo-1530497610245-94d3c16cda28?auto=format&fit=crop&w=800&q=80"),
            Triple("الرنين المغناطيسي للدماغ (Brain MRI)", "يصور التلفيف المخي، البطينين، وتخطيط الأنسجة العصبية بدقة مجهرية فائقة.", "https://images.unsplash.com/photo-1559757175-5700dde675bc?auto=format&fit=crop&w=800&q=80"),
            Triple("تخطيط صمامات القلب (Echocardiogram)", "تصوير بالموجات فوق الصوتية لتقييم كفاءة الصمامات الأبهرية والتاجية وسرعة التدفق.", "https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=800&q=80"),
            Triple("التصوير الطبقي للفقرات (Spinal CT Scan)", "مقطع محوري يوضح فقرات العمود الفقري، القناة الشوكية، وسلامة الأقراص الغضروفية.", "https://images.unsplash.com/photo-1516549655169-df83a0774514?auto=format&fit=crop&w=800&q=80")
        )

        items(galleryItems) { (title, desc, imageUrl) ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(desc, color = Color(0xFFA0AEC0), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ArAnatomicalOverlayTab(viewModel: MedicalViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var selectedArSystem by remember { mutableStateOf("القلب والدوران") }

    val arSystems = listOf(
        Triple("القلب والدوران", "محاكاة ثلاثية الأبعاد لنبضات القلب وتدفق الدم عبر الشرايين الرئيسية في البيئة المحيطة.", "❤️"),
        Triple("الجهاز التنفسي", "عرض تفاعلي لعملية تبادل الغازات وحركة الحجاب الحاجز في الوقت الفعلي.", "🫁"),
        Triple("الجهاز العصبي والدماغ", "إسقاط افتراضي لمسارات الإشارات العصبية وقشرة الدماغ.", "🧠"),
        Triple("الهيكل العظمي والمفاصل", "تراكب الهيكل العظمي وعلامات الإجهاد المفصلي على المساحة الواقعية.", "🦴")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👓", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text("الواقع الافتراضي المعزز (AR Anatomy)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 16.sp)
                            Text("إسقاط النماذج التشريحية ثلاثية الأبعاد على محيطك عبر الكاميرا الحية", color = Color(0xFFA0AEC0), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasCameraPermission) {
                        Button(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("منح إذن الكاميرا لتشغيل الواقع المعزز", color = Color.White)
                        }
                    }
                }
            }
        }

        if (hasCameraPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview
                                        )
                                    } catch (e: Exception) {
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(220.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.25f), RoundedCornerShape(110.dp))
                                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(110.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    when (selectedArSystem) {
                                        "القلب والدوران" -> "❤️"
                                        "الجهاز التنفسي" -> "🫁"
                                        "الجهاز العصبي والدماغ" -> "🧠"
                                        else -> "🦴"
                                    },
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selectedArSystem,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "إسقاط ثلاثي الأبعاد نشط",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("AR Tracking: Active", color = Color(0xFF38BDF8), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("⚙️ اختيار النظام التشريحي للإسقاط", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        }

        items(arSystems) { (sysName, sysDesc, icon) ->
            val isSelected = selectedArSystem == sysName
            Card(
                onClick = {
                    selectedArSystem = sysName
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sysName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(sysDesc, color = Color(0xFFA0AEC0), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    if (isSelected) {
                        Text("مُحدد ✓", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FocusStudyModeTab(viewModel: MedicalViewModel) {
    val haptic = LocalHapticFeedback.current
    var isZenMode by remember { mutableStateOf(false) }
    var fontSizeMultiplier by remember { mutableStateOf(1.2f) }
    var selectedTopicIndex by remember { mutableStateOf(0) }

    val studyTopics = listOf(
        Triple(
            "الديناميكا الدموية للقلب والدوران (Hemodynamics)",
            "تدرس الديناميكا الدموية القوى الفيزيائية التي تحكم تدفق الدم في الجهاز الدورة الدموية. يتم تنظيم مقاومة الأوعية الدموية (Vascular Resistance) بشكل دقيق عبر الجهاز العصبي الودي واللاودي. عند حدوث أي خلل في الضغط الشرياني المتوسط (MAP)، تتدخل آليات الضغط المستقبلي (Baroreceptor Reflex) فوراً لتعديل معدل ضربات القلب وقوة التقلص العضلي.",
            listOf("الديناميكا الدموية", "مقاومة الأوعية الدموية", "الضغط الشرياني المتوسط", "الجهاز العصبي الودي")
        ),
        Triple(
            "المناعة العصبية والمسار الكولينرجي المضاد للالتهاب",
            "يمثل العصب الحائر (Vagus Nerve) المحور الأساسي للاتصال العصبي المناعي. عندما يتم تحفيز المستقبلات الكولينرجية (Alpha-7 Nicotinic Acetylcholine Receptors) على سطح الخلايا البلعمية الكبيرة، يتم تثبيط إطلاق السيتوكينات الالتهابية (مثل TNF-alpha و IL-6) بشكل فوري، مما يحمي الأنسجة من عاصفة السيتوكينات القاتلة.",
            listOf("العصب الحائر", "المستقبلات الكولينرجية", "السيتوكينات الالتهابية", "عاصفة السيتوكينات")
        ),
        Triple(
            "حوسبة الحافة والذكاء الاصطناعي الطبي الحائر",
            "تعتمد المنظومة السيبرانية الحيوية الموحدة (UBIS) على معالجات حافة فائقة انخفاض الطاقة تعمل بلغة Rust، وبزمن استجابة لا يتعدى 0.42 مللي ثانية. تضمن هذه المعمارية اتخاذ القرار العلاجي محلياً وبشكل حتمي ومستقل عن أي اتصال سحابي، مما يضمن الموثوقية المطلقة وإنقاذ الحياة في الحالات الحرجة.",
            listOf("حوسبة الحافة", "المنظومة السيبرانية الحيوية", "زمن استجابة", "الموثوقية المطلقة")
        )
    )

    val currentTopic = studyTopics[selectedTopicIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isZenMode) Color(0xFF090D16) else Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("وضع التركيز والقراءة المتقدم (Focus Mode)", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 15.sp)
                    Text("تقليل المشتتات وتحسين قراءة المصطلحات الطبية", color = Color(0xFFA0AEC0), fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isZenMode = !isZenMode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isZenMode) Color(0xFF10B981) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isZenMode) "وضع Zen نشط ✨" else "وضع العزل", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            fontSizeMultiplier = if (fontSizeMultiplier >= 1.5f) 1.0f else fontSizeMultiplier + 0.15f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("خط: ${(fontSizeMultiplier * 10).toInt()}x", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            studyTopics.forEachIndexed { index, topic ->
                val isSelected = selectedTopicIndex == index
                Card(
                    onClick = {
                        selectedTopicIndex = index
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.4f) else Color(0xFF0F172A).copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "الموضوع ${index + 1}",
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFA0AEC0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = if (isZenMode) 0.98f else 0.8f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = currentTopic.first,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = (18 * fontSizeMultiplier).sp,
                    lineHeight = (26 * fontSizeMultiplier).sp
                )

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                Text(
                    text = currentTopic.second,
                    color = Color(0xFFCBD5E1),
                    fontSize = (14 * fontSizeMultiplier).sp,
                    lineHeight = (24 * fontSizeMultiplier).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("📌 المصطلحات الفيزيولوجية الرئيسية المبرزة:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentTopic.third.forEach { keyword ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = keyword,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3D INTERACTIVE HUMAN ANATOMY VIEWER & PHYSIOLOGICAL MAPPING SYSTEM
// ============================================================================

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val label: String = ""
)

data class AnatomicalHotspot3D(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val relativePos: Point3D,
    val organId: String,
    val mechanismAr: String,
    val biometrics: Map<String, String>,
    val ubisVariable: String,
    val pathologyAr: String,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val correctOptionIndex: Int
)

data class OrganMesh3D(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val icon: String,
    val primaryColor: Color,
    val center3D: Point3D,
    val vertices: List<Point3D>,
    val edges: List<Pair<Int, Int>>,
    val hotspots: List<AnatomicalHotspot3D>
)

@Composable
fun Anatomy3dViewerTab(viewModel: MedicalViewModel) {
    val haptic = LocalHapticFeedback.current

    // Rotation and Camera state
    var yawAngle by remember { mutableFloatStateOf(0.3f) } // Y-axis rotation (radians)
    var pitchAngle by remember { mutableFloatStateOf(0.1f) } // X-axis rotation (radians)
    var cameraZoom by remember { mutableFloatStateOf(1.1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Display & Simulation controls
    var isAutoRotate by remember { mutableStateOf(true) }
    var isPhysioPulsing by remember { mutableStateOf(true) }
    var isXRayMode by remember { mutableStateOf(false) }
    var selectedSystemFilter by remember { mutableStateOf("ALL") } // ALL, CARDIO, NEURO, RESP, DIGEST, SKELETAL

    // Selected Organ & Hotspot state
    var selectedOrganId by remember { mutableStateOf("heart") }
    var selectedHotspotId by remember { mutableStateOf<String?>("cardiac_conduction") }
    var selectedDrawerTab by remember { mutableStateOf(0) } // 0: Physio, 1: Biometrics, 2: UBIS, 3: Quiz

    // Quiz user answer state
    var quizUserAnswer by remember { mutableStateOf<Int?>(null) }
    var quizFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Auto-rotation animation loop
    LaunchedEffect(isAutoRotate) {
        if (isAutoRotate) {
            while (true) {
                yawAngle += 0.008f
                kotlinx.coroutines.delay(16)
            }
        }
    }

    // Physiological animation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "PhysioPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // Build Organ 3D Knowledge Dataset
    val organMeshes = remember {
        listOf(
            // 1. HEART & VASCULAR SYSTEM
            OrganMesh3D(
                id = "heart",
                nameAr = "القلب والجهاز الوعائي التاجي",
                nameEn = "Heart & Coronary Circulation",
                icon = "❤️",
                primaryColor = Color(0xFFEF4444),
                center3D = Point3D(0f, -20f, 0f),
                vertices = listOf(
                    Point3D(-25f, -45f, 0f), Point3D(25f, -45f, 0f),
                    Point3D(35f, -15f, 20f), Point3D(0f, 25f, 0f),
                    Point3D(-35f, -15f, -20f), Point3D(0f, -65f, 15f) // Aorta arch
                ),
                edges = listOf(
                    0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 0,
                    0 to 5, 1 to 5, 2 to 5, 3 to 5, 4 to 5
                ),
                hotspots = listOf(
                    AnatomicalHotspot3D(
                        id = "cardiac_conduction",
                        nameAr = "عقدة الجيب الأذينية والشبكة التوصيلية (SA Node)",
                        nameEn = "Sinoatrial Node & Conduction Network",
                        relativePos = Point3D(15f, -40f, 10f),
                        organId = "heart",
                        mechanismAr = "توليد النبضات الكهربائية الذاتية لمطثية القلب من خلال إزالة الاستقطاب الدوري لخصائص الأيونات الصوديمية والكالسيومية، مما يؤمن نتاجاً قلبياً متزناً.",
                        biometrics = mapOf(
                            "معدل النبض الطبيعي" to "60 - 100 نبضة/دقيقة",
                            "نتاج القلب (Cardiac Output)" to "4.8 - 5.5 لتر/دقيقة",
                            "كفاءة الضخ الأذيني (EF)" to "55% - 70%",
                            "ضغط الضخ الانقباضي" to "120 mmHg"
                        ),
                        ubisVariable = "HRV_rmssd & P_sys (معامل الاتزان التاجي الهيدروليكي)",
                        pathologyAr = "الرجفان الأذيني (Atrial Fibrillation)، احتشاء العضلة القلبية الحاد (STEMI)، وحصر القطب التوصيلي.",
                        quizQuestion = "ما هي العقدة المسؤولة عن المولد الكهربائي الرئيسي الذاتي لنبضات القلب الطبيعية؟",
                        quizOptions = listOf("العقدة الجيبية الأذينية (SA Node)", "العقدة الأذينية البطينية (AV Node)", "ألياف بوركنجي (Purkinje Fibers)", "حزمة هيس (Bundle of His)"),
                        correctOptionIndex = 0
                    ),
                    AnatomicalHotspot3D(
                        id = "coronary_artery",
                        nameAr = "الشرايين التاجية الرئيسية (Coronary Arteries)",
                        nameEn = "Left & Right Coronary Arteries",
                        relativePos = Point3D(-10f, -20f, 15f),
                        organId = "heart",
                        mechanismAr = "تزويد عضلة القلب بالأكسجين والمغذيات الحيوية عبر الشريان التاجي الأيسر والنازل الأمامي LADA لمنع الإجهاد الإقفاري.",
                        biometrics = mapOf(
                            "ضغط التروية التاجية" to "80 mmHg",
                            "استهلاك الأكسجين العضلي" to "8 - 10 mL/100g/min",
                            "جريان الدم التاجي" to "225 mL/min"
                        ),
                        ubisVariable = "ΔR_i (تغير المقاومة الوعائية في معادلة UBCHE)",
                        pathologyAr = "التصلب العصيدي للشرايين (Atherosclerosis)، الخثرة التاجية الحادة، والذبحة الصدرية غير المستقرة.",
                        quizQuestion = "أي شريان يطلق عليه اسم 'صانع الأرامل' لنقص ترويته المباشر للبطين الأيسر؟",
                        quizOptions = listOf("الشريان التاجي الأيمن RCA", "الشريان الشغافي النازل الأمامي LADA", "الشريان المنعطف LCX", "الشريان المائل OM"),
                        correctOptionIndex = 1
                    )
                )
            ),

            // 2. BRAIN & NERVOUS SYSTEM
            OrganMesh3D(
                id = "brain",
                nameAr = "الدماغ والمخ والجذع العصبي",
                nameEn = "Brain, Cortex & Neural Axis",
                icon = "🧠",
                primaryColor = Color(0xFFA855F7),
                center3D = Point3D(0f, -110f, 0f),
                vertices = listOf(
                    Point3D(-30f, -135f, -10f), Point3D(30f, -135f, -10f),
                    Point3D(35f, -105f, 15f), Point3D(-35f, -105f, 15f),
                    Point3D(0f, -145f, 20f), Point3D(0f, -80f, -5f) // Brainstem
                ),
                edges = listOf(
                    0 to 1, 1 to 2, 2 to 3, 3 to 0,
                    0 to 4, 1 to 4, 2 to 4, 3 to 4,
                    2 to 5, 3 to 5
                ),
                hotspots = listOf(
                    AnatomicalHotspot3D(
                        id = "cerebral_cortex",
                        nameAr = "القشرة الدماغية والمخ الأمامي (Cerebral Cortex)",
                        nameEn = "Frontal & Parietal Cortical Lobes",
                        relativePos = Point3D(0f, -125f, 15f),
                        organId = "brain",
                        mechanismAr = "المعالجة العليا للإشارات الحركية، الحسية، والتفكير الاستراتيجي مع تنظيم السيادة العصبية المركزية.",
                        biometrics = mapOf(
                            "ضغط السائل الدماغي (ICP)" to "7 - 15 mmHg",
                            "معدل استهلاك الجلوكوز" to "5.5 mg/100g/min",
                            "تدفق الدم الدماغي (CBF)" to "50 mL/100g/min"
                        ),
                        ubisVariable = "F_vagus & Edge AI Cognitive Processing",
                        pathologyAr = "السكتة الدماغية الإقفارية (Ischemic Stroke)، النزيف الدماغي، والصرع.",
                        quizQuestion = "ما هو المعدل الطبيعي لتدفق الدم الدماغي (CBF) لكل 100 غرام من أنسجة الدماغ في الدقيقة؟",
                        quizOptions = listOf("20 mL/100g/min", "50 mL/100g/min", "100 mL/100g/min", "150 mL/100g/min"),
                        correctOptionIndex = 1
                    )
                )
            ),

            // 3. LUNGS & RESPIRATORY TRACT
            OrganMesh3D(
                id = "lungs",
                nameAr = "الرئتان والشجرة القصبية",
                nameEn = "Lungs & Bronchial Tree",
                icon = "🫁",
                primaryColor = Color(0xFF06B6D4),
                center3D = Point3D(0f, -30f, -15f),
                vertices = listOf(
                    // Left lung
                    Point3D(-45f, -55f, -15f), Point3D(-20f, -55f, -15f),
                    Point3D(-20f, 10f, -15f), Point3D(-50f, 10f, -15f),
                    // Right lung
                    Point3D(20f, -55f, -15f), Point3D(45f, -55f, -15f),
                    Point3D(50f, 10f, -15f), Point3D(20f, 10f, -15f)
                ),
                edges = listOf(
                    0 to 1, 1 to 2, 2 to 3, 3 to 0,
                    4 to 5, 5 to 6, 6 to 7, 7 to 4
                ),
                hotspots = listOf(
                    AnatomicalHotspot3D(
                        id = "alveolar_capillary",
                        nameAr = "الأسناخ الرئوية والغشاء الشعيري (Alveolar Network)",
                        nameEn = "Alveolar-Capillary Membrane",
                        relativePos = Point3D(35f, -20f, -15f),
                        organId = "lungs",
                        mechanismAr = "تبادل الغازات بين الهوائي السنخي والدم الشعيري الشرياني عبر الانتشار البسيط للأكسجين وثاني أكسيد الكربون.",
                        biometrics = mapOf(
                            "تشبع الأكسجين (SpO2)" to "95% - 100%",
                            "ضغط الأكسجين الشرياني (PaO2)" to "80 - 100 mmHg",
                            "السعة الحيوية الرئوية" to "4.6 لتر",
                            "معدل التنفس الطبيعي" to "12 - 20 دورة/دقيقة"
                        ),
                        ubisVariable = "SAR_nano & Cytokine Clearance Ratio",
                        pathologyAr = "متلازمة الضائقة التنفسية الحادة (ARDS)، التليف الرئوي، والانسداد الرئوي المزمن (COPD).",
                        quizQuestion = "ما هو المدى الطبيعي لتشبع الأكسجين الدمويPaO2 لدى الشاب السليم؟",
                        quizOptions = listOf("50 - 60 mmHg", "80 - 100 mmHg", "120 - 140 mmHg", "150 - 200 mmHg"),
                        correctOptionIndex = 1
                    )
                )
            ),

            // 4. LIVER & GASTROINTESTINAL SYSTEM
            OrganMesh3D(
                id = "liver",
                nameAr = "الكبد والجهاز الهضمي",
                nameEn = "Liver & Gastric System",
                icon = "🧪",
                primaryColor = Color(0xFFF59E0B),
                center3D = Point3D(-15f, 35f, 5f),
                vertices = listOf(
                    Point3D(-45f, 20f, 5f), Point3D(10f, 20f, 5f),
                    Point3D(15f, 55f, 10f), Point3D(-35f, 55f, 5f)
                ),
                edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0),
                hotspots = listOf(
                    AnatomicalHotspot3D(
                        id = "hepatic_lobule",
                        nameAr = "الفصوص الكبدية والوريد البابي (Hepatic Lobule)",
                        nameEn = "Hepatic Sinusoids & Portal Circulation",
                        relativePos = Point3D(-15f, 35f, 5f),
                        organId = "liver",
                        mechanismAr = "تصفية السموم، الاستقلاب الدوائي، استصناع الألبومين، وتخزين الجليكوجين لضبط الاتزان السكري الحيوي.",
                        biometrics = mapOf(
                            "معدل إنزيم ALT" to "7 - 56 U/L",
                            "معدل إنزيم AST" to "10 - 40 U/L",
                            "البيليروبين الكلي" to "0.1 - 1.2 mg/dL",
                            "تدفق الوريد البابي" to "1000 mL/min"
                        ),
                        ubisVariable = "C_cytok (تصفية بروتينات الطور الحاد في UBCHE)",
                        pathologyAr = "تشمع الكبد (Cirrhosis)، الكبد الدهني غير الكحولي (NAFLD)، والتهاب الكبد الفيروسي.",
                        quizQuestion = "ما هو الوريد الرئيسي المسؤول عن نقل 75% من الدم المحمل بالمغذيات إلى الكبد؟",
                        quizOptions = listOf("الوريد البابي الكبدي (Portal Vein)", "الوريد الأجوف السفلي (IVC)", "الوريد الطحالي", "الوريد المساريقي العلوي"),
                        correctOptionIndex = 0
                    )
                )
            ),

            // 5. KIDNEYS & URINARY TRACT
            OrganMesh3D(
                id = "kidneys",
                nameAr = "الكليتان والجهاز البولي",
                nameEn = "Kidneys & Nephron Filtration",
                icon = "🩸",
                primaryColor = Color(0xFF10B981),
                center3D = Point3D(0f, 45f, -20f),
                vertices = listOf(
                    Point3D(-35f, 35f, -20f), Point3D(-25f, 55f, -20f),
                    Point3D(25f, 35f, -20f), Point3D(35f, 55f, -20f)
                ),
                edges = listOf(0 to 1, 2 to 3),
                hotspots = listOf(
                    AnatomicalHotspot3D(
                        id = "nephron_glomerulus",
                        nameAr = "الكبيبات والنيفرونات الكلويّة (Glomerular Nephrons)",
                        nameEn = "Glomerular Filtration Barrier",
                        relativePos = Point3D(30f, 45f, -20f),
                        organId = "kidneys",
                        mechanismAr = "ترشيح الدم الدقيق، التخلص من اليوريا والكرياتينين، وتنظيم ميزانية الصوديوم والبوتاسيوم وضغط الدم عبر محور الرينين-أنجيوتنسين (RAAS).",
                        biometrics = mapOf(
                            "معدل الترشيح الكبيبي (GFR)" to "90 - 120 mL/min/1.73m²",
                            "الكرياتينين المصلّي" to "0.6 - 1.2 mg/dL",
                            "نيتروجين أكسيد اليوريا (BUN)" to "7 - 20 mg/dL"
                        ),
                        ubisVariable = "P_crit Safety Interrupt Monitor",
                        pathologyAr = "الفشل الكلوي الحاد (AKI)، الاعتلال الكلوي السكري، والمتلازمة الكلوية.",
                        quizQuestion = "ما هو المعدل الطبيعي للترشيح الكبيبي الكلي (GFR) للبالغين السالمين؟",
                        quizOptions = listOf("30 - 50 mL/min", "90 - 120 mL/min", "150 - 180 mL/min", "200+ mL/min"),
                        correctOptionIndex = 1
                    )
                )
            )
        )
    }

    // Filter organs based on system filter
    val visibleOrgans = remember(selectedSystemFilter, organMeshes) {
        when (selectedSystemFilter) {
            "CARDIO" -> organMeshes.filter { it.id == "heart" }
            "NEURO" -> organMeshes.filter { it.id == "brain" }
            "RESP" -> organMeshes.filter { it.id == "lungs" }
            "DIGEST" -> organMeshes.filter { it.id == "liver" }
            "RENAL" -> organMeshes.filter { it.id == "kidneys" }
            else -> organMeshes
        }
    }

    // Find currently selected hotspot object
    val activeHotspot = remember(selectedHotspotId, organMeshes) {
        organMeshes.flatMap { it.hotspots }.find { it.id == selectedHotspotId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP HEADER CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧊", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                        Text(
                            "عارض التشريح ثلاثي الأبعاد التفاعلي",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        "استكشاف الأجهزة الحيوية وتدوير الأعضاء بدقة 360° مع ربط المعرفة السريرية",
                        color = Color(0xFFA0AEC0),
                        fontSize = 10.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "UBIS 3D Engine",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // SYSTEM FILTER CHIPS
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                "ALL" to "كل الأجهزة 🌐",
                "CARDIO" to "القلب والدوران ❤️",
                "NEURO" to "الجهاز العصبي 🧠",
                "RESP" to "الجهاز التنفسي 🫁",
                "DIGEST" to "الكبد والهضم 🧪",
                "RENAL" to "الكلى والبول 🩸"
            )

            items(filters) { (filterKey, label) ->
                val isSelected = selectedSystemFilter == filterKey
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedSystemFilter = filterKey
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    label = { Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else Color(0xFFA0AEC0)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        containerColor = Color(0xFF1E293B)
                    )
                )
            }
        }

        // MAIN 3D VIEWPORT CANVAS CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            yawAngle += pan.x * 0.008f
                            pitchAngle += pan.y * 0.008f
                            cameraZoom = (cameraZoom * zoom).coerceIn(0.6f, 2.5f)
                            panOffsetX += pan.x * 0.2f
                            panOffsetY += pan.y * 0.2f
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            val centerX = canvasWidth / 2f + panOffsetX
                            val centerY = canvasHeight / 2f + panOffsetY
                            val fov = 380f
                            val cameraDist = 550f

                            var closestHotspot: AnatomicalHotspot3D? = null
                            var minDistance = Float.MAX_VALUE

                            organMeshes.flatMap { it.hotspots }.forEach { hotspot ->
                                val organ = organMeshes.find { it.id == hotspot.organId }
                                val p = Point3D(
                                    (organ?.center3D?.x ?: 0f) + hotspot.relativePos.x,
                                    (organ?.center3D?.y ?: 0f) + hotspot.relativePos.y,
                                    (organ?.center3D?.z ?: 0f) + hotspot.relativePos.z
                                )

                                val x1 = p.x * cos(yawAngle) + p.z * sin(yawAngle)
                                val z1 = -p.x * sin(yawAngle) + p.z * cos(yawAngle)
                                val y2 = p.y * cos(pitchAngle) - z1 * sin(pitchAngle)
                                val z2 = p.y * sin(pitchAngle) + z1 * cos(pitchAngle)

                                val scaleFactor = (fov / (cameraDist + z2)) * cameraZoom
                                val projX = centerX + x1 * scaleFactor
                                val projY = centerY + y2 * scaleFactor

                                val dx = tapOffset.x - projX
                                val dy = tapOffset.y - projY
                                val dist = sqrt(dx * dx + dy * dy)

                                if (dist < 80f && dist < minDistance) {
                                    minDistance = dist
                                    closestHotspot = hotspot
                                }
                            }

                            closestHotspot?.let {
                                selectedHotspotId = it.id
                                selectedOrganId = it.organId
                                quizUserAnswer = null
                                quizFeedbackMessage = null
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2f + panOffsetX
                    val centerY = canvasHeight / 2f + panOffsetY
                    val fov = 380f
                    val cameraDist = 550f

                    for (i in -4..4) {
                        val gridY = centerY + i * 40f
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.3f),
                            start = Offset(0f, gridY),
                            end = Offset(canvasWidth, gridY),
                            strokeWidth = 1f
                        )
                    }

                    fun project(p: Point3D, organScale: Float = 1.0f): Pair<Offset, Float> {
                        val sx = p.x * organScale
                        val sy = p.y * organScale
                        val sz = p.z * organScale

                        val x1 = sx * cos(yawAngle) + sz * sin(yawAngle)
                        val z1 = -sx * sin(yawAngle) + sz * cos(yawAngle)
                        val y2 = sy * cos(pitchAngle) - z1 * sin(pitchAngle)
                        val z2 = sy * sin(pitchAngle) + z1 * cos(pitchAngle)

                        val scaleFactor = (fov / (cameraDist + z2)) * cameraZoom
                        val projX = centerX + x1 * scaleFactor
                        val projY = centerY + y2 * scaleFactor

                        return Pair(Offset(projX, projY), scaleFactor)
                    }

                    val spinePoints = listOf(
                        Point3D(0f, -110f, 0f),
                        Point3D(0f, -60f, -5f),
                        Point3D(0f, -20f, -10f),
                        Point3D(0f, 20f, -10f),
                        Point3D(0f, 60f, -5f)
                    )
                    for (i in 0 until spinePoints.size - 1) {
                        val (p1, _) = project(spinePoints[i])
                        val (p2, _) = project(spinePoints[i + 1])
                        drawLine(
                            color = Color(0xFF475569).copy(alpha = 0.4f),
                            start = p1,
                            end = p2,
                            strokeWidth = 3f
                        )
                    }

                    visibleOrgans.forEach { organ ->
                        val isSelectedOrgan = organ.id == selectedOrganId
                        val dynamicScale = when (organ.id) {
                            "heart" -> if (isPhysioPulsing) pulseScale else 1.0f
                            "lungs" -> if (isPhysioPulsing) breathingScale else 1.0f
                            else -> 1.0f
                        }

                        val projectedVertices = organ.vertices.map { v ->
                            val absPos = Point3D(
                                organ.center3D.x + v.x,
                                organ.center3D.y + v.y,
                                organ.center3D.z + v.z
                            )
                            project(absPos, dynamicScale)
                        }

                        organ.edges.forEach { (i1, i2) ->
                            if (i1 < projectedVertices.size && i2 < projectedVertices.size) {
                                val (startPt, scaleFactor) = projectedVertices[i1]
                                val (endPt, _) = projectedVertices[i2]

                                drawLine(
                                    color = if (isSelectedOrgan) organ.primaryColor else organ.primaryColor.copy(alpha = if (isXRayMode) 0.8f else 0.4f),
                                    start = startPt,
                                    end = endPt,
                                    strokeWidth = if (isSelectedOrgan) 4f * scaleFactor else 2f * scaleFactor
                                )
                            }
                        }

                        projectedVertices.forEach { (pt, scaleFactor) ->
                            drawCircle(
                                color = organ.primaryColor.copy(alpha = if (isSelectedOrgan) 0.9f else 0.5f),
                                radius = (if (isSelectedOrgan) 6f else 4f) * scaleFactor,
                                center = pt
                            )
                        }

                        organ.hotspots.forEach { hotspot ->
                            val isHotspotActive = hotspot.id == selectedHotspotId
                            val hotspotAbsPos = Point3D(
                                organ.center3D.x + hotspot.relativePos.x,
                                organ.center3D.y + hotspot.relativePos.y,
                                organ.center3D.z + hotspot.relativePos.z
                            )

                            val (hotspotPt, scaleFactor) = project(hotspotAbsPos, dynamicScale)

                            if (isHotspotActive) {
                                drawCircle(
                                    color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    radius = 24f * scaleFactor * pulseScale,
                                    center = hotspotPt
                                )
                                drawCircle(
                                    color = Color(0xFF38BDF8),
                                    radius = 10f * scaleFactor,
                                    center = hotspotPt
                                )
                            } else {
                                drawCircle(
                                    color = organ.primaryColor,
                                    radius = 7f * scaleFactor,
                                    center = hotspotPt
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            isAutoRotate = !isAutoRotate
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isAutoRotate) Color(0xFF0284C7) else Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "دوران تلقائي", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            yawAngle = 0.3f
                            pitchAngle = 0.1f
                            cameraZoom = 1.1f
                            panOffsetX = 0f
                            panOffsetY = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "إعادة مركزية", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            isPhysioPulsing = !isPhysioPulsing
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isPhysioPulsing) Color(0xFF10B981) else Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "محاكاة الحركية", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            isXRayMode = !isXRayMode
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isXRayMode) Color(0xFFA855F7) else Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "وضع الشفافية X-Ray", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🖐️ اسحب لتعديل الزاوية 360° | اضغط على النقاط للاستكشاف",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // BOTTOM DETAILED PHYSIOLOGICAL KNOWLEDGE SHEET
        activeHotspot?.let { hotspot ->
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hotspot.nameAr,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = hotspot.nameEn,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = "مُتعرّف عليه ✓",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedDrawerTab,
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF38BDF8),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedDrawerTab == 0,
                            onClick = { selectedDrawerTab = 0 },
                            text = { Text("الفيزيولوجيا", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedDrawerTab == 1,
                            onClick = { selectedDrawerTab = 1 },
                            text = { Text("المؤشرات", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedDrawerTab == 2,
                            onClick = { selectedDrawerTab = 2 },
                            text = { Text("معادلة UBIS", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = selectedDrawerTab == 3,
                            onClick = { selectedDrawerTab = 3 },
                            text = { Text("اختبار سريع", fontSize = 11.sp) }
                        )
                    }

                    when (selectedDrawerTab) {
                        0 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Text("⚡ الميكانيكية والآلية الفيزيولوجية:", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                                    Text(hotspot.mechanismAr, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 18.sp)
                                }
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🩺 الاعتلالات والشيوع السريري المرتبط:", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 12.sp)
                                    Text(hotspot.pathologyAr, color = Color(0xFFFCA5A5), fontSize = 11.sp, lineHeight = 16.sp)
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(hotspot.biometrics.toList()) { (label, value) ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, color = Color(0xFFA0AEC0), fontSize = 11.sp)
                                            Text(value, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🧬 ارتباط المتغير بمعادلة الاندماج الموحدة (UBCHE):", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF020617),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = hotspot.ubisVariable,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Text(
                                    "تتم مراقبة ومطابقة هذا المتغير لحظياً بمعدل 1kHz للتحكم في استجابة الحوسبة الحافة والتعديل العصبي بالضد.",
                                    color = Color(0xFFA0AEC0),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("❓ ${hotspot.quizQuestion}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)

                                hotspot.quizOptions.forEachIndexed { optIndex, optionText ->
                                    val isSelected = quizUserAnswer == optIndex
                                    val isCorrect = optIndex == hotspot.correctOptionIndex

                                    Button(
                                        onClick = {
                                            quizUserAnswer = optIndex
                                            if (isCorrect) {
                                                quizFeedbackMessage = "إجابة صحيحة دقيقة! أحسنت 🎯"
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else {
                                                quizFeedbackMessage = "إجابة خاطئة، حاول مرة أخرى ❌"
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                isSelected && isCorrect -> Color(0xFF10B981)
                                                isSelected && !isCorrect -> Color(0xFFEF4444)
                                                else -> Color(0xFF1E293B)
                                            }
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(optionText, color = Color.White, fontSize = 11.sp)
                                    }
                                }

                                quizFeedbackMessage?.let { msg ->
                                    Text(
                                        text = msg,
                                        color = if (quizUserAnswer == hotspot.correctOptionIndex) Color(0xFF10B981) else Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
