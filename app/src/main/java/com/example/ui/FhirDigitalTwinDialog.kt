package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.fhir.*

@Composable
fun FhirDigitalTwinDialog(
    patientName: String,
    age: Float,
    isFemale: Boolean,
    sbp: Float,
    dbp: Float,
    heightCm: Float,
    weightKg: Float,
    creatinineText: String,
    onDismiss: () -> Unit
) {
    val bridge = remember { FhirDigitalTwinBridge() }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var selectedViewMode by remember { mutableStateOf("card") } // "card" | "json"
    var copyNotification by remember { mutableStateOf(false) }

    // State for live patient & simulation snapshot
    var activePatient by remember {
        mutableStateOf(
            bridge.createPatient(
                familyArabic = if (patientName.contains(" ")) patientName.substringAfter(" ") else "الافتراضي",
                givenArabic = if (patientName.contains(" ")) patientName.substringBefore(" ") else patientName.ifBlank { "مريض" },
                gender = if (isFemale) "female" else "male",
                birthDate = "${2026 - age.toInt()}-01-01"
            )
        )
    }

    var activeSnapshot by remember {
        val creatinine = creatinineText.toDoubleOrNull() ?: 1.0
        val egfrCalculated = calculateQuickEgfr(age.toDouble(), isFemale, creatinine)
        mutableStateOf(
            PhysiologicalSnapshot(
                heartRate = 75.0,
                systolicBp = sbp.toDouble(),
                diastolicBp = dbp.toDouble(),
                oxygenSaturation = 98.0,
                respiratoryRate = 16.0,
                temperatureCelsius = 37.0,
                egfr = egfrCalculated
            )
        )
    }

    val observations = remember(activePatient, activeSnapshot) {
        bridge.createObservationsFromSnapshot(activePatient, activeSnapshot)
    }

    val fhirJson = remember(activePatient, observations) {
        val patientJson = bridge.exportPatientToJson(activePatient)
        val obsJsons = observations.map { bridge.exportObservationToJson(it) }
        buildString {
            append("{\n")
            append("  \"resourceType\": \"Bundle\",\n")
            append("  \"type\": \"collection\",\n")
            append("  \"timestamp\": \"2026-10-28T12:00:00Z\",\n")
            append("  \"total\": ${1 + observations.size},\n")
            append("  \"entry\": [\n")
            append("    {\n      \"fullUrl\": \"urn:uuid:${activePatient.id}\",\n      \"resource\": ")
            append(patientJson.replace("\n", "\n      "))
            append("\n    }")
            obsJsons.forEach { obsJson ->
                append(",\n    {\n      \"resource\": ")
                append(obsJson.replace("\n", "\n      "))
                append("\n    }")
            }
            append("\n  ]\n}")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌐", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "نموذج المريض والمعايير الطبية (HL7 FHIR)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "ربط التوأم الرقمي الفسيولوجي بمعايير LOINC & FHIR R4 العالمية",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Standard Badges Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BadgeChip("HL7 FHIR R4", Color(0xFF0284C7))
                    BadgeChip("LOINC Standard", Color(0xFF10B981))
                    BadgeChip("UCUM Units", Color(0xFF8B5CF6))
                    BadgeChip("Digital Twin", Color(0xFFF59E0B))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preset Scenario Selector
                Text(
                    text = "⚡ سيناريوهات التوأم الرقمي الفسيولوجي السريرية:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        PresetChip(
                            label = "🟢 مستقر طبيعي",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val (p, s) = bridge.getPresetScenario(FhirDigitalTwinBridge.ScenarioType.HEALTHY_BASELINE)
                                activePatient = p
                                activeSnapshot = s
                            }
                        )
                    }
                    item {
                        PresetChip(
                            label = "🔴 أزمة ضغط (175/110)",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val (p, s) = bridge.getPresetScenario(FhirDigitalTwinBridge.ScenarioType.HYPERTENSIVE_CRISIS)
                                activePatient = p
                                activeSnapshot = s
                            }
                        )
                    }
                    item {
                        PresetChip(
                            label = "🟣 قصور قلب ونقص أكسجة",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val (p, s) = bridge.getPresetScenario(FhirDigitalTwinBridge.ScenarioType.HEART_FAILURE_DECOMPENSATION)
                                activePatient = p
                                activeSnapshot = s
                            }
                        )
                    }
                    item {
                        PresetChip(
                            label = "🟠 صدمة إنتانية (حمى وهبوط)",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val (p, s) = bridge.getPresetScenario(FhirDigitalTwinBridge.ScenarioType.SEPSIS_ALERT)
                                activePatient = p
                                activeSnapshot = s
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher (Card View vs JSON View)
                TabRow(
                    selectedTabIndex = if (selectedViewMode == "card") 0 else 1,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedViewMode == "card",
                        onClick = { selectedViewMode = "card" },
                        text = { Text("🩺 البطاقة السريرية ومؤشرات LOINC", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedViewMode == "json",
                        onClick = { selectedViewMode = "json" },
                        text = { Text("📄 كود FHIR JSON الكامل", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedViewMode == "card") {
                        FhirCardView(activePatient, activeSnapshot, observations)
                    } else {
                        FhirJsonView(fhirJson)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboardManager.setText(AnnotatedString(fhirJson))
                            copyNotification = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ كود FHIR JSON", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.5f),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text("إغلاق", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }

                if (copyNotification) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✅ تم نسخ كود FHIR الكامل بنجاح إلى الحافظة!",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FhirCardView(
    patient: FhirPatient,
    snapshot: PhysiologicalSnapshot,
    observations: List<FhirObservation>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Patient Resource Overview Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "👤 المريض: ${patient.primaryName}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Patient ID: ${patient.id ?: "auto"}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("السجل الطبي: ${patient.primaryIdentifier ?: "-"}", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                    Text("الجنس: ${if (patient.gender == "female") "أنثى (female)" else "ذكر (male)"}", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                    Text("تاريخ الميلاد: ${patient.birthDate ?: "-"}", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "📊 القياسات الفسيولوجية المرمزة دولياً (LOINC Observations):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Observation items
        observations.forEach { obs ->
            ObservationItemCard(obs)
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ObservationItemCard(obs: FhirObservation) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
        border = BorderStroke(0.5.dp, Color(0xFF475569)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = obs.code.text ?: "Observation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
                val code = obs.code.coding.firstOrNull()?.code ?: ""
                Text(
                    text = "LOINC: $code",
                    fontSize = 9.sp,
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (obs.valueQuantity != null) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "القيمة: ${obs.valueQuantity.value} ${obs.valueQuantity.unit}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    obs.interpretation.firstOrNull()?.let { interp ->
                        Text(
                            text = interp.text ?: (interp.coding.firstOrNull()?.display ?: ""),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (interp.coding.any { it.code in listOf("H", "HH", "L", "LL") }) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                }
            } else if (obs.component.isNotEmpty()) {
                // Components (e.g. BP Systolic & Diastolic)
                obs.component.forEach { comp ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "• ${comp.code.text ?: "Component"}:",
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "${comp.valueQuantity?.value ?: 0.0} ${comp.valueQuantity?.unit ?: ""}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            obs.referenceRange.firstOrNull()?.text?.let { rangeText ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "المجال المرجعي: $rangeText",
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
private fun FhirJsonView(jsonString: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = jsonString,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF38BDF8),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1E293B),
        border = BorderStroke(0.5.dp, Color(0xFF475569)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun calculateQuickEgfr(age: Double, isFemale: Boolean, creatinine: Double): Double {
    val kappa = if (isFemale) 0.7 else 0.9
    val alpha = if (isFemale) -0.241 else -0.302
    val minVal = Math.min(creatinine / kappa, 1.0)
    val maxVal = Math.max(creatinine / kappa, 1.0)
    val femaleFactor = if (isFemale) 1.012 else 1.0
    return Math.round(142.0 * Math.pow(minVal, alpha) * Math.pow(maxVal, -1.200) * Math.pow(0.9938, age) * femaleFactor * 10.0) / 10.0
}
