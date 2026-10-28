package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MedicalViewModel(private val repository: MedicalRepository) : ViewModel() {

    // UI Tab State
    private val _activeTab = MutableStateFlow("systems")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Dark Theme State
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Selected Body System Detail State
    private val _selectedSystem = MutableStateFlow<BodySystem?>(null)
    val selectedSystem: StateFlow<BodySystem?> = _selectedSystem.asStateFlow()

    // Shared Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Room Database Flows
    val savedPrescriptions: StateFlow<List<SavedPrescription>> = repository.allPrescriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calculationLogs: StateFlow<List<CalculationLog>> = repository.allCalculationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkedTopic>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizResults: StateFlow<List<QuizResultEntity>> = repository.allQuizResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveQuizResult(systemId: String, systemTitle: String, score: Int, totalQuestions: Int) {
        viewModelScope.launch {
            val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
            val feedback = when {
                percentage >= 90 -> "ممتاز جداً! إدراك عميق ومتميز للفسيولوجيا والتكامل الطبي."
                percentage >= 70 -> "جيد جداً! استيعاب قوي ومستقر للأجهزة الحيوية."
                percentage >= 50 -> "جيد، بحاجة لمراجعة تفصيلية إضافية للآليات والتعاليم."
                else -> "تحتاج لمزيد من المراجعة والاطلاع على الشرح العلمي والنبوي المرفق."
            }
            repository.insertQuizResult(
                QuizResultEntity(
                    systemId = systemId,
                    systemTitle = systemTitle,
                    score = score,
                    totalQuestions = totalQuestions,
                    percentage = percentage,
                    feedback = feedback
                )
            )
        }
    }

    fun deleteQuizResult(id: Int) {
        viewModelScope.launch {
            repository.deleteQuizResultById(id)
        }
    }

    // Update States
    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun selectSystem(system: BodySystem?) {
        _selectedSystem.value = system
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Bookmarks management
    fun toggleBookmark(type: String, topicId: String, topicTitle: String) {
        viewModelScope.launch {
            val isCurrentlyBookmarked = bookmarks.value.any { it.topicType == type && it.topicId == topicId }
            if (isCurrentlyBookmarked) {
                repository.deleteBookmark(type, topicId)
            } else {
                repository.insertBookmark(
                    BookmarkedTopic(
                        topicType = type,
                        topicId = topicId,
                        topicTitle = topicTitle
                    )
                )
            }
        }
    }

    fun isBookmarked(type: String, topicId: String): Flow<Boolean> {
        return repository.isBookmarked(type, topicId)
    }

    // Prescription triggers
    fun insertPrescription(condition: String, regimen: String, duration: String, monitoring: String) {
        viewModelScope.launch {
            val prescription = SavedPrescription(
                condition = condition,
                regimen = regimen,
                duration = duration,
                monitoring = monitoring,
                isCustom = true
            )
            repository.insertPrescription(prescription)
        }
    }

    fun deletePrescription(id: Int) {
        viewModelScope.launch {
            repository.deletePrescriptionById(id)
        }
    }

    // Calculation Logs management
    fun insertCalculationLog(patientName: String, modelUsed: String, inputsSummary: String, calculatedResult: String, scoreText: String) {
        viewModelScope.launch {
            val log = CalculationLog(
                patientName = patientName.ifBlank { "مريض مجهول" },
                modelUsed = modelUsed,
                inputsSummary = inputsSummary,
                calculatedResult = calculatedResult,
                scoreText = scoreText
            )
            repository.insertCalculationLog(log)
        }
    }

    fun deleteCalculationLog(id: Int) {
        viewModelScope.launch {
            repository.deleteCalculationLogById(id)
        }
    }

    // ==========================================
    // 1. Framingham Point Score Calculator
    // ==========================================
    fun calculateFraminghamRisk(
        age: Int,
        isFemale: Boolean,
        sbp: Int,
        isSbpTreated: Boolean,
        totalCholesterol: Int,
        isSmoker: Boolean,
        isDiabetic: Boolean
    ): Pair<Double, Int> {
        var points = 0

        // 1. Age Points
        if (!isFemale) {
            points += when {
                age in 30..34 -> 0
                age in 35..39 -> 2
                age in 40..44 -> 5
                age in 45..49 -> 7
                age in 50..54 -> 8
                age in 55..59 -> 10
                age in 60..64 -> 11
                age in 65..69 -> 12
                age in 70..74 -> 14
                age >= 75 -> 15
                else -> 0
            }
        } else {
            points += when {
                age in 30..34 -> 0
                age in 35..39 -> 2
                age in 40..44 -> 4
                age in 45..49 -> 5
                age in 50..54 -> 7
                age in 55..59 -> 8
                age in 60..64 -> 9
                age in 65..69 -> 10
                age in 70..74 -> 12
                age >= 75 -> 13
                else -> 0
            }
        }

        // 2. Total Cholesterol Points
        points += when {
            totalCholesterol < 160 -> 0
            totalCholesterol in 160..199 -> 1
            totalCholesterol in 200..239 -> 2
            else -> 3 // >=240
        }

        // 3. Smoking Points
        if (isSmoker) {
            points += if (!isFemale) 4 else 3
        }

        // 4. Systolic Blood Pressure (SBP) Points
        points += when {
            sbp < 120 -> 0
            sbp in 120..129 -> 1
            sbp in 130..139 -> 2
            sbp in 140..159 -> 3
            else -> 4 // >=160
        }
        if (isSbpTreated && sbp >= 120) {
            points += 1
        }

        // 5. Diabetes points
        if (isDiabetic) {
            points += if (!isFemale) 2 else 4
        }

        // Map points to 10-year risk percentage
        val riskPercentage = when {
            points <= 0 -> 1.0
            points in 1..4 -> 2.0
            points in 5..8 -> 4.0
            points in 9..11 -> 6.5
            points in 12..14 -> 11.0
            points in 15..16 -> 18.5
            points >= 17 -> 32.0
            else -> 35.0
        }

        return Pair(riskPercentage, points)
    }

    // ==========================================
    // 2. CHA₂DS₂-VASc Stroke Risk Calculator
    // ==========================================
    fun calculateCha2ds2Vasc(
        hasChf: Boolean,
        hasHypertension: Boolean,
        age: Int,
        hasDiabetes: Boolean,
        hasStroke: Boolean,
        hasVascular: Boolean,
        isFemale: Boolean
    ): Pair<Double, Int> {
        var score = 0
        if (hasChf) score += 1
        if (hasHypertension) score += 1
        if (age >= 75) score += 2 else if (age in 65..74) score += 1
        if (hasDiabetes) score += 1
        if (hasStroke) score += 2
        if (hasVascular) score += 1
        if (isFemale) score += 1

        val riskPercent = when (score) {
            0 -> 0.0
            1 -> 1.3
            2 -> 2.2
            3 -> 3.2
            4 -> 4.0
            5 -> 6.7
            6 -> 9.8
            7 -> 11.2
            8 -> 12.5
            else -> 15.0 // 9
        }

        return Pair(riskPercent, score)
    }

    // ==========================================
    // 3. eGFR CKD-EPI (2021 Race-Free Equation)
    // ==========================================
    fun calculateCkdEpiEGfr(
        creatinine: Double,
        age: Int,
        isFemale: Boolean
    ): Pair<Double, String> {
        if (creatinine <= 0.0) return Pair(0.0, "خطأ في عيار الكرياتينين")

        val kappa = if (isFemale) 0.7 else 0.9
        val alpha = if (isFemale) -0.241 else -0.302
        val genderCoeff = if (isFemale) 1.012 else 1.0

        val crRatio = creatinine / kappa
        val minRatio = min(crRatio, 1.0)
        val maxRatio = max(crRatio, 1.0)

        val egfrVal = 142.0 *
                minRatio.pow(alpha) *
                maxRatio.pow(-1.200) *
                0.9938.pow(age.toDouble()) *
                genderCoeff

        val roundedEgfr = (egfrVal * 10).toInt() / 10.0

        val stage = when {
            roundedEgfr >= 90.0 -> "المرحلة الأولى G1 (تنظيم طبيعي أو عالي: وظائف كلوية سليمة)"
            roundedEgfr >= 60.0 -> "المرحلة الثانية G2 (انخفاض طفيف: رصد ومتابعة كلوية)"
            roundedEgfr >= 45.0 -> "المرحلة الثالثة أ G3a (انخفاض طفيف إلى متوسط: رصد وعناية كلوية)"
            roundedEgfr >= 30.0 -> "المرحلة الثالثة ب G3b (انخفاض متوسط إلى شديد: معالجة وقائية مشددة)"
            roundedEgfr >= 15.0 -> "المرحلة الرابعة G4 (انخفاض شديد: تجهيز مسبق لغسيل/ديلزة الكلى)"
            else -> "المرحلة الخامسة G5 (فشل كلوي نهائي: ديلزة أو زرع كلى مستعجل)"
        }

        return Pair(roundedEgfr, stage)
    }

    fun calculateBmi(weightKg: Double, heightCm: Double): Pair<Double, String> {
        if (weightKg <= 0.0 || heightCm <= 0.0) return Pair(0.0, "يرجى إدخال قيم صحيحة")
        
        val heightM = heightCm / 100.0
        val bmiVal = weightKg / (heightM * heightM)
        val roundedBmi = (bmiVal * 10).toInt() / 10.0
        
        val category = when {
            roundedBmi < 18.5 -> "نقص في الوزن (Underweight)"
            roundedBmi < 25.0 -> "وزن طبيعي صحي (Normal weight)"
            roundedBmi < 30.0 -> "زيادة في الوزن (Overweight)"
            roundedBmi < 35.0 -> "سمنة من الدرجة الأولى (Obesity Class I)"
            roundedBmi < 40.0 -> "سمنة من الدرجة الثانية (Obesity Class II)"
            else -> "سمنة مفرطة من الدرجة الثالثة (Obesity Class III)"
        }
        
        return Pair(roundedBmi, category)
    }

    // ==========================================
    // 4. Blood Pressure, MAP & Hemodynamics
    // ==========================================
    data class BloodPressureResult(
        val category: String,
        val map: Double,
        val pulsePressure: Int,
        val riskLevel: String,
        val recommendation: String
    )

    fun calculateBloodPressureMetrics(sbp: Int, dbp: Int): BloodPressureResult {
        val mapVal = dbp + (sbp - dbp) / 3.0
        val roundedMap = (mapVal * 10).toInt() / 10.0
        val pp = sbp - dbp

        val (category, riskLevel, rec) = when {
            sbp >= 180 || dbp >= 120 -> Triple(
                "أزمة ارتفاع ضغط الدم (Hypertensive Crisis)",
                "خطر حاد يستدعي التدخل الفوري",
                "يطلب تقييم طبي طارئ لمنع الأذية العضوية الحادة (مخ، قلب، كلى)"
            )
            sbp >= 140 || dbp >= 90 -> Triple(
                "ارتفاع ضغط الدم - المرحلة الثانية (Stage 2 HTN)",
                "مرتفع مرتفع (عالي الخطورة)",
                "مطلوب استشارة طبية لبدء/تعديل العلاج الدوائي مع تعديل نمط الحياة"
            )
            sbp in 130..139 || dbp in 80..89 -> Triple(
                "ارتفاع ضغط الدم - المرحلة الأولى (Stage 1 HTN)",
                "متوسط الخطورة",
                "يوصى بتعديل النظام الغذائي وتقليل الصوديوم والمتابعة الدورية"
            )
            sbp in 120..129 && dbp < 80 -> Triple(
                "ضغط دم مرتفع طفيفاً (Elevated BP)",
                "منخفض إلى متوسط",
                "يوصى بالتمارين الرياضية وخفض الوزن وضبط الغذاء"
            )
            sbp in 90..119 && dbp in 60..79 -> Triple(
                "ضغط دم طبيعي ومثالي (Normal BP)",
                "طبيعي ومثالي",
                "المحافظة على العادات الصحية والتغذية المتوازنة"
            )
            else -> Triple(
                "انخفاض ضغط الدم (Hypotension)",
                "منخفض (بحاجة لتقييم الأعراض)",
                "مراجعة التروية الدموية وشرب السوائل واستشارة الطبيب إن وُجدت دوخة"
            )
        }

        return BloodPressureResult(
            category = category,
            map = roundedMap,
            pulsePressure = pp,
            riskLevel = riskLevel,
            recommendation = rec
        )
    }

    // ==========================================
    // 5. Body Composition & BSA
    // ==========================================
    data class BodyCompositionResult(
        val bmi: Double,
        val bmiCategory: String,
        val bsa: Double,
        val idealWeightKg: Double,
        val healthRecommendation: String
    )

    // ==========================================
    // 6. NEW: Comprehensive Hemodynamic & Metabolic Stability Score (CHMSS)
    // ==========================================
    data class CHMSSResult(
        val stabilityScore: Double, // 0 to 100
        val classification: String,
        val analyticalReport: String
    )

    fun calculateCHMSS(sbp: Int, dbp: Int, weightKg: Double, heightCm: Double): CHMSSResult {
        // 1. Normalize BP (Simple model for demo)
        val map = dbp + (sbp - dbp) / 3.0
        val normalizedBP = when {
            map in 70.0..105.0 -> 1.0
            map < 60.0 || map > 120.0 -> 0.4
            else -> 0.7
        }

        // 2. Normalize BMI
        val bmi = if (heightCm > 0) weightKg / ((heightCm / 100.0).pow(2.0)) else 25.0
        val normalizedBMI = when {
            bmi in 18.5..24.9 -> 1.0
            bmi < 16.0 || bmi > 35.0 -> 0.3
            else -> 0.6
        }

        // 3. Composite Weighted Score (Genius approach)
        val stabilityScore = (normalizedBP * 0.6 + normalizedBMI * 0.4) * 100
        
        val (classification, report) = when {
            stabilityScore >= 85.0 -> "استقرار مثالي" to "النظام الفسيولوجي يعمل ضمن المعايير المثالية. الحفاظ على العادات الحالية."
            stabilityScore >= 60.0 -> "استقرار مقبول" to "هناك انحرافات طفيفة عن المعايير الحيوية. ينصح بمراجعة نمط الحياة والنشاط البدني."
            else -> "حالة حذرة" to "انحراف كبير في المؤشرات الحيوية. يتطلب تقييماً طبياً مباشراً للوقاية من المضاعفات."
        }

        return CHMSSResult(stabilityScore, classification, report)
    }
}

class MedicalViewModelFactory(private val repository: MedicalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
