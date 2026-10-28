package com.example.data.fhir

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Snapshot of physiological variables in the Digital Twin simulation.
 */
data class PhysiologicalSnapshot(
    val heartRate: Double,
    val systolicBp: Double,
    val diastolicBp: Double,
    val oxygenSaturation: Double,
    val respiratoryRate: Double = 16.0,
    val temperatureCelsius: Double = 37.0,
    val egfr: Double? = 95.0,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Bridge between physiological digital twin simulation and HL7 FHIR standard resources.
 */
class FhirDigitalTwinBridge {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val patientAdapter = moshi.adapter(FhirPatient::class.java).indent("  ")
    private val observationAdapter = moshi.adapter(FhirObservation::class.java).indent("  ")
    private val bundleAdapter = moshi.adapter(FhirBundle::class.java).indent("  ")

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

    /**
     * Creates a FHIR Patient resource with standard identifiers and demographics.
     */
    fun createPatient(
        id: String = "patient-${UUID.randomUUID().toString().take(8)}",
        medicalRecordNumber: String = "MRN-${(10000..99999).random()}",
        familyArabic: String = "الأحمد",
        givenArabic: String = "عبدالله",
        familyEnglish: String = "Al-Ahmad",
        givenEnglish: String = "Abdullah",
        gender: String = "male", // "male", "female", "other", "unknown"
        birthDate: String = "1978-06-15",
        phone: String = "+966501234567",
        city: String = "الرياض",
        country: String = "المملكة العربية السعودية"
    ): FhirPatient {
        return FhirPatient(
            id = id,
            meta = FhirMeta(
                versionId = "1",
                lastUpdated = formatIsoDate(System.currentTimeMillis()),
                profile = listOf("http://hl7.org/fhir/StructureDefinition/Patient")
            ),
            identifier = listOf(
                FhirIdentifier(
                    use = "official",
                    system = "http://hospital.org/fhir/mrn",
                    value = medicalRecordNumber
                )
            ),
            active = true,
            name = listOf(
                FhirHumanName(
                    use = "official",
                    text = "$givenArabic $familyArabic",
                    family = familyArabic,
                    given = listOf(givenArabic)
                ),
                FhirHumanName(
                    use = "usual",
                    text = "$givenEnglish $familyEnglish",
                    family = familyEnglish,
                    given = listOf(givenEnglish)
                )
            ),
            telecom = listOf(
                FhirContactPoint(
                    system = "phone",
                    value = phone,
                    use = "mobile"
                )
            ),
            gender = gender,
            birthDate = birthDate,
            address = listOf(
                FhirAddress(
                    use = "home",
                    city = city,
                    country = country
                )
            ),
            communication = listOf(
                FhirCommunication(
                    language = FhirCodeableConcept(
                        coding = listOf(
                            FhirCoding(
                                system = "urn:ietf:bcp:47",
                                code = "ar",
                                display = "العربية / Arabic"
                            )
                        ),
                        text = "العربية"
                    ),
                    preferred = true
                )
            )
        )
    }

    /**
     * Converts a physiological simulation snapshot into a list of FHIR Observation resources.
     */
    fun createObservationsFromSnapshot(
        patient: FhirPatient,
        snapshot: PhysiologicalSnapshot
    ): List<FhirObservation> {
        val patientRef = FhirReference(
            reference = "Patient/${patient.id ?: "unknown"}",
            display = patient.primaryName
        )
        val timeIso = formatIsoDate(snapshot.timestampMs)
        val observations = mutableListOf<FhirObservation>()

        // 1. Heart Rate Observation (LOINC 8867-4)
        val hrInterpretation = when {
            snapshot.heartRate < 50 -> FhirLoincCodes.INTERPRETATION_CRITICAL_LOW
            snapshot.heartRate < 60 -> FhirLoincCodes.INTERPRETATION_LOW
            snapshot.heartRate <= 100 -> FhirLoincCodes.INTERPRETATION_NORMAL
            snapshot.heartRate <= 120 -> FhirLoincCodes.INTERPRETATION_HIGH
            else -> FhirLoincCodes.INTERPRETATION_CRITICAL_HIGH
        }
        observations.add(
            FhirObservation(
                id = "obs-hr-${UUID.randomUUID().toString().take(8)}",
                status = "final",
                category = listOf(FhirLoincCodes.CATEGORY_VITAL_SIGNS),
                code = FhirCodeableConcept(
                    coding = listOf(
                        FhirCoding(
                            system = FhirLoincCodes.SYSTEM_LOINC,
                            code = FhirLoincCodes.CODE_HEART_RATE,
                            display = FhirLoincCodes.DISPLAY_HEART_RATE
                        )
                    ),
                    text = "معدل ضربات القلب / Heart Rate"
                ),
                subject = patientRef,
                effectiveDateTime = timeIso,
                valueQuantity = FhirQuantity(
                    value = snapshot.heartRate,
                    unit = FhirLoincCodes.UNIT_BEATS_PER_MIN,
                    code = "/min"
                ),
                interpretation = listOf(hrInterpretation),
                referenceRange = listOf(
                    FhirReferenceRange(
                        low = FhirQuantity(60.0, "/min", code = "/min"),
                        high = FhirQuantity(100.0, "/min", code = "/min"),
                        text = "60 - 100 نبضة/دقيقة"
                    )
                )
            )
        )

        // 2. Blood Pressure Panel Observation (LOINC 85354-9 with Systolic & Diastolic components)
        val sbpInterp = when {
            snapshot.systolicBp < 90 -> FhirLoincCodes.INTERPRETATION_LOW
            snapshot.systolicBp <= 120 -> FhirLoincCodes.INTERPRETATION_NORMAL
            snapshot.systolicBp <= 139 -> FhirLoincCodes.INTERPRETATION_HIGH
            else -> FhirLoincCodes.INTERPRETATION_CRITICAL_HIGH
        }
        val dbpInterp = when {
            snapshot.diastolicBp < 60 -> FhirLoincCodes.INTERPRETATION_LOW
            snapshot.diastolicBp <= 80 -> FhirLoincCodes.INTERPRETATION_NORMAL
            snapshot.diastolicBp <= 89 -> FhirLoincCodes.INTERPRETATION_HIGH
            else -> FhirLoincCodes.INTERPRETATION_CRITICAL_HIGH
        }

        observations.add(
            FhirObservation(
                id = "obs-bp-${UUID.randomUUID().toString().take(8)}",
                status = "final",
                category = listOf(FhirLoincCodes.CATEGORY_VITAL_SIGNS),
                code = FhirCodeableConcept(
                    coding = listOf(
                        FhirCoding(
                            system = FhirLoincCodes.SYSTEM_LOINC,
                            code = FhirLoincCodes.CODE_BP_PANEL,
                            display = FhirLoincCodes.DISPLAY_BP_PANEL
                        )
                    ),
                    text = "ضغط الدم الشرياني / Blood Pressure"
                ),
                subject = patientRef,
                effectiveDateTime = timeIso,
                component = listOf(
                    FhirObservationComponent(
                        code = FhirCodeableConcept(
                            coding = listOf(
                                FhirCoding(
                                    system = FhirLoincCodes.SYSTEM_LOINC,
                                    code = FhirLoincCodes.CODE_BP_SYSTOLIC,
                                    display = FhirLoincCodes.DISPLAY_BP_SYSTOLIC
                                )
                            ),
                            text = "الضغط الانقباضي / Systolic Blood Pressure"
                        ),
                        valueQuantity = FhirQuantity(
                            value = snapshot.systolicBp,
                            unit = FhirLoincCodes.UNIT_MMHG,
                            code = "mm[Hg]"
                        ),
                        interpretation = listOf(sbpInterp)
                    ),
                    FhirObservationComponent(
                        code = FhirCodeableConcept(
                            coding = listOf(
                                FhirCoding(
                                    system = FhirLoincCodes.SYSTEM_LOINC,
                                    code = FhirLoincCodes.CODE_BP_DIASTOLIC,
                                    display = FhirLoincCodes.DISPLAY_BP_DIASTOLIC
                                )
                            ),
                            text = "الضغط الانبساطي / Diastolic Blood Pressure"
                        ),
                        valueQuantity = FhirQuantity(
                            value = snapshot.diastolicBp,
                            unit = FhirLoincCodes.UNIT_MMHG,
                            code = "mm[Hg]"
                        ),
                        interpretation = listOf(dbpInterp)
                    )
                ),
                interpretation = listOf(if (snapshot.systolicBp > 130 || snapshot.diastolicBp > 85) sbpInterp else FhirLoincCodes.INTERPRETATION_NORMAL)
            )
        )

        // 3. SpO2 Oxygen Saturation Observation (LOINC 2708-6)
        val spo2Interpretation = when {
            snapshot.oxygenSaturation < 90 -> FhirLoincCodes.INTERPRETATION_CRITICAL_LOW
            snapshot.oxygenSaturation < 95 -> FhirLoincCodes.INTERPRETATION_LOW
            else -> FhirLoincCodes.INTERPRETATION_NORMAL
        }
        observations.add(
            FhirObservation(
                id = "obs-spo2-${UUID.randomUUID().toString().take(8)}",
                status = "final",
                category = listOf(FhirLoincCodes.CATEGORY_VITAL_SIGNS),
                code = FhirCodeableConcept(
                    coding = listOf(
                        FhirCoding(
                            system = FhirLoincCodes.SYSTEM_LOINC,
                            code = FhirLoincCodes.CODE_OXYGEN_SATURATION,
                            display = FhirLoincCodes.DISPLAY_OXYGEN_SATURATION
                        )
                    ),
                    text = "تشبع الأكسجين الشرياني / Oxygen Saturation"
                ),
                subject = patientRef,
                effectiveDateTime = timeIso,
                valueQuantity = FhirQuantity(
                    value = snapshot.oxygenSaturation,
                    unit = FhirLoincCodes.UNIT_PERCENT,
                    code = "%"
                ),
                interpretation = listOf(spo2Interpretation),
                referenceRange = listOf(
                    FhirReferenceRange(
                        low = FhirQuantity(95.0, "%", code = "%"),
                        high = FhirQuantity(100.0, "%", code = "%"),
                        text = "95% - 100%"
                    )
                )
            )
        )

        // 4. Respiratory Rate (LOINC 9279-1)
        observations.add(
            FhirObservation(
                id = "obs-resp-${UUID.randomUUID().toString().take(8)}",
                status = "final",
                category = listOf(FhirLoincCodes.CATEGORY_VITAL_SIGNS),
                code = FhirCodeableConcept(
                    coding = listOf(
                        FhirCoding(
                            system = FhirLoincCodes.SYSTEM_LOINC,
                            code = FhirLoincCodes.CODE_RESPIRATORY_RATE,
                            display = FhirLoincCodes.DISPLAY_RESPIRATORY_RATE
                        )
                    ),
                    text = "معدل التنفس / Respiratory Rate"
                ),
                subject = patientRef,
                effectiveDateTime = timeIso,
                valueQuantity = FhirQuantity(
                    value = snapshot.respiratoryRate,
                    unit = FhirLoincCodes.UNIT_BREATHS_PER_MIN,
                    code = "/min"
                ),
                referenceRange = listOf(
                    FhirReferenceRange(
                        low = FhirQuantity(12.0, "/min", code = "/min"),
                        high = FhirQuantity(20.0, "/min", code = "/min"),
                        text = "12 - 20 نفس/دقيقة"
                    )
                )
            )
        )

        // 5. Body Temperature (LOINC 8310-5)
        observations.add(
            FhirObservation(
                id = "obs-temp-${UUID.randomUUID().toString().take(8)}",
                status = "final",
                category = listOf(FhirLoincCodes.CATEGORY_VITAL_SIGNS),
                code = FhirCodeableConcept(
                    coding = listOf(
                        FhirCoding(
                            system = FhirLoincCodes.SYSTEM_LOINC,
                            code = FhirLoincCodes.CODE_BODY_TEMPERATURE,
                            display = FhirLoincCodes.DISPLAY_BODY_TEMPERATURE
                        )
                    ),
                    text = "درجة حرارة الجسم / Body Temperature"
                ),
                subject = patientRef,
                effectiveDateTime = timeIso,
                valueQuantity = FhirQuantity(
                    value = snapshot.temperatureCelsius,
                    unit = FhirLoincCodes.UNIT_CELSIUS,
                    code = "Cel"
                ),
                referenceRange = listOf(
                    FhirReferenceRange(
                        low = FhirQuantity(36.5, "Cel", code = "Cel"),
                        high = FhirQuantity(37.5, "Cel", code = "Cel"),
                        text = "36.5 - 37.5 °C"
                    )
                )
            )
        )

        // 6. Optional eGFR Observation (LOINC 33914-3)
        snapshot.egfr?.let { egfrVal ->
            val egfrInterp = when {
                egfrVal >= 90 -> FhirLoincCodes.INTERPRETATION_NORMAL
                egfrVal >= 60 -> FhirLoincCodes.INTERPRETATION_LOW
                else -> FhirLoincCodes.INTERPRETATION_CRITICAL_LOW
            }
            observations.add(
                FhirObservation(
                    id = "obs-egfr-${UUID.randomUUID().toString().take(8)}",
                    status = "final",
                    category = listOf(
                        FhirCodeableConcept(
                            coding = listOf(
                                FhirCoding(
                                    system = FhirLoincCodes.SYSTEM_OBSERVATION_CATEGORY,
                                    code = "laboratory",
                                    display = "Laboratory"
                                )
                            ),
                            text = "Laboratory / التحاليل المخبرية"
                        )
                    ),
                    code = FhirCodeableConcept(
                        coding = listOf(
                            FhirCoding(
                                system = FhirLoincCodes.SYSTEM_LOINC,
                                code = FhirLoincCodes.CODE_EGFR,
                                display = FhirLoincCodes.DISPLAY_EGFR
                            )
                        ),
                        text = "معدل الترشيح الكبيبي المقدر (CKD-EPI) / eGFR"
                    ),
                    subject = patientRef,
                    effectiveDateTime = timeIso,
                    valueQuantity = FhirQuantity(
                        value = egfrVal,
                        unit = FhirLoincCodes.UNIT_ML_MIN_173M2,
                        code = "mL/min/{1.73_m2}"
                    ),
                    interpretation = listOf(egfrInterp),
                    referenceRange = listOf(
                        FhirReferenceRange(
                            low = FhirQuantity(90.0, "mL/min/{1.73_m2}", code = "mL/min/{1.73_m2}"),
                            text = "> 90 mL/min/1.73m² (Normal renal function)"
                        )
                    )
                )
            )
        }

        return observations
    }

    /**
     * Serializes a single FhirPatient to standardized JSON string.
     */
    fun exportPatientToJson(patient: FhirPatient): String {
        return patientAdapter.toJson(patient)
    }

    /**
     * Serializes a single FhirObservation to standardized JSON string.
     */
    fun exportObservationToJson(observation: FhirObservation): String {
        return observationAdapter.toJson(observation)
    }

    /**
     * Deserializes a FHIR Patient JSON into FhirPatient model.
     */
    fun parsePatientFromJson(json: String): FhirPatient? {
        return try {
            patientAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates predefined clinical digital twin scenarios.
     */
    fun getPresetScenario(scenarioType: ScenarioType): Pair<FhirPatient, PhysiologicalSnapshot> {
        return when (scenarioType) {
            ScenarioType.HEALTHY_BASELINE -> {
                val patient = createPatient(
                    medicalRecordNumber = "MRN-10101",
                    familyArabic = "السعيد",
                    givenArabic = "طارق",
                    familyEnglish = "Al-Saeed",
                    givenEnglish = "Tariq",
                    birthDate = "1990-03-21"
                )
                val snapshot = PhysiologicalSnapshot(
                    heartRate = 72.0,
                    systolicBp = 118.0,
                    diastolicBp = 76.0,
                    oxygenSaturation = 98.5,
                    respiratoryRate = 16.0,
                    temperatureCelsius = 36.8,
                    egfr = 105.0
                )
                patient to snapshot
            }
            ScenarioType.HYPERTENSIVE_CRISIS -> {
                val patient = createPatient(
                    medicalRecordNumber = "MRN-20202",
                    familyArabic = "المنصور",
                    givenArabic = "خالد",
                    familyEnglish = "Al-Mansour",
                    givenEnglish = "Khaled",
                    birthDate = "1965-11-10"
                )
                val snapshot = PhysiologicalSnapshot(
                    heartRate = 104.0,
                    systolicBp = 175.0,
                    diastolicBp = 110.0,
                    oxygenSaturation = 96.0,
                    respiratoryRate = 22.0,
                    temperatureCelsius = 37.1,
                    egfr = 62.0
                )
                patient to snapshot
            }
            ScenarioType.HEART_FAILURE_DECOMPENSATION -> {
                val patient = createPatient(
                    medicalRecordNumber = "MRN-30303",
                    familyArabic = "العتيبي",
                    givenArabic = "فهد",
                    familyEnglish = "Al-Otaibi",
                    givenEnglish = "Fahad",
                    birthDate = "1958-08-05"
                )
                val snapshot = PhysiologicalSnapshot(
                    heartRate = 118.0,
                    systolicBp = 92.0,
                    diastolicBp = 58.0,
                    oxygenSaturation = 88.0,
                    respiratoryRate = 26.0,
                    temperatureCelsius = 36.5,
                    egfr = 38.0
                )
                patient to snapshot
            }
            ScenarioType.SEPSIS_ALERT -> {
                val patient = createPatient(
                    medicalRecordNumber = "MRN-40404",
                    familyArabic = "الغامدي",
                    givenArabic = "سعد",
                    familyEnglish = "Al-Ghamdi",
                    givenEnglish = "Saad",
                    birthDate = "1982-01-14"
                )
                val snapshot = PhysiologicalSnapshot(
                    heartRate = 125.0,
                    systolicBp = 85.0,
                    diastolicBp = 50.0,
                    oxygenSaturation = 92.0,
                    respiratoryRate = 28.0,
                    temperatureCelsius = 39.2,
                    egfr = 45.0
                )
                patient to snapshot
            }
        }
    }

    private fun formatIsoDate(millis: Long): String {
        return try {
            isoDateFormat.format(Date(millis))
        } catch (e: Exception) {
            "2026-10-28T12:00:00Z"
        }
    }

    enum class ScenarioType {
        HEALTHY_BASELINE,
        HYPERTENSIVE_CRISIS,
        HEART_FAILURE_DECOMPENSATION,
        SEPSIS_ALERT
    }
}
