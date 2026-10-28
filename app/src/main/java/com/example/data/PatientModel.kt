package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Robust Kotlin data model mapping the HL7 FHIR R4 Patient Resource.
 *
 * Includes complete demographic definitions (identifiers, human names, contact points,
 * addresses, gender, birth date) alongside nested clinical observations specifically
 * tailored for vital signs such as Heart Rate (LOINC 8867-4) and Oxygen Saturation (LOINC 2708-6).
 */
@JsonClass(generateAdapter = true)
data class PatientModel(
    @Json(name = "resourceType")
    val resourceType: String = "Patient",

    @Json(name = "id")
    val id: String = "patient-${UUID.randomUUID().toString().take(8)}",

    @Json(name = "meta")
    val meta: PatientMeta? = PatientMeta(),

    @Json(name = "identifier")
    val identifier: List<PatientIdentifier> = emptyList(),

    @Json(name = "active")
    val active: Boolean = true,

    @Json(name = "name")
    val name: List<PatientHumanName> = emptyList(),

    @Json(name = "telecom")
    val telecom: List<PatientContactPoint> = emptyList(),

    @Json(name = "gender")
    val gender: String? = null, // "male" | "female" | "other" | "unknown"

    @Json(name = "birthDate")
    val birthDate: String? = null, // ISO-8601: "YYYY-MM-DD"

    @Json(name = "deceasedBoolean")
    val deceasedBoolean: Boolean? = null,

    @Json(name = "deceasedDateTime")
    val deceasedDateTime: String? = null,

    @Json(name = "address")
    val address: List<PatientAddress> = emptyList(),

    @Json(name = "maritalStatus")
    val maritalStatus: PatientCodeableConcept? = null,

    @Json(name = "communication")
    val communication: List<PatientCommunication> = emptyList(),

    @Json(name = "generalPractitioner")
    val generalPractitioner: List<PatientReference> = emptyList(),

    @Json(name = "managingOrganization")
    val managingOrganization: PatientReference? = null,

    /**
     * Nested clinical observations container for digital twin simulations and bedside telemetry.
     */
    @Json(name = "clinicalObservations")
    val clinicalObservations: PatientObservations? = null
) : Serializable {

    val primaryName: String
        get() = name.firstOrNull()?.text
            ?: name.firstOrNull()?.let {
                val givenNames = it.given.joinToString(" ")
                val family = it.family ?: ""
                "$givenNames $family".trim().ifEmpty { null }
            }
            ?: "Unknown Patient"

    val primaryIdentifier: String?
        get() = identifier.firstOrNull()?.value

    val isMale: Boolean
        get() = gender?.equals("male", ignoreCase = true) == true

    val isFemale: Boolean
        get() = gender?.equals("female", ignoreCase = true) == true

    /**
     * Quick accessors for vital observations.
     */
    val heartRate: HeartRateObservation?
        get() = clinicalObservations?.heartRate

    val oxygenSaturation: OxygenSaturationObservation?
        get() = clinicalObservations?.oxygenSaturation

    /**
     * Immutable mutation helper: updates or records a new Heart Rate observation.
     */
    fun withHeartRate(
        bpm: Double,
        effectiveDateTime: String = currentIsoTimestamp()
    ): PatientModel {
        val currentObs = clinicalObservations ?: PatientObservations()
        val newHr = HeartRateObservation.create(bpm, effectiveDateTime)
        return copy(clinicalObservations = currentObs.copy(heartRate = newHr))
    }

    /**
     * Immutable mutation helper: updates or records a new Oxygen Saturation observation.
     */
    fun withOxygenSaturation(
        spo2Percent: Double,
        effectiveDateTime: String = currentIsoTimestamp()
    ): PatientModel {
        val currentObs = clinicalObservations ?: PatientObservations()
        val newSpo2 = OxygenSaturationObservation.create(spo2Percent, effectiveDateTime)
        return copy(clinicalObservations = currentObs.copy(oxygenSaturation = newSpo2))
    }

    /**
     * Immutable mutation helper: updates both core vital observations simultaneously.
     */
    fun withVitals(
        heartRateBpm: Double,
        spo2Percent: Double,
        systolicBp: Double? = null,
        diastolicBp: Double? = null,
        effectiveDateTime: String = currentIsoTimestamp()
    ): PatientModel {
        val currentObs = clinicalObservations ?: PatientObservations()
        val bpObs = if (systolicBp != null && diastolicBp != null) {
            BloodPressureObservation.create(systolicBp, diastolicBp, effectiveDateTime)
        } else {
            currentObs.bloodPressure
        }
        val updatedObs = currentObs.copy(
            heartRate = HeartRateObservation.create(heartRateBpm, effectiveDateTime),
            oxygenSaturation = OxygenSaturationObservation.create(spo2Percent, effectiveDateTime),
            bloodPressure = bpObs
        )
        return copy(clinicalObservations = updatedObs)
    }

    /**
     * Serializes this PatientModel to a formatted FHIR JSON string.
     */
    fun toJson(): String = companionMoshiAdapter.toJson(this)

    companion object {
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        private val companionMoshiAdapter by lazy {
            moshi.adapter(PatientModel::class.java).indent("  ")
        }

        fun fromJson(json: String): PatientModel? {
            return try {
                companionMoshiAdapter.fromJson(json)
            } catch (e: Exception) {
                null
            }
        }

        fun currentIsoTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            return sdf.format(Date())
        }

        /**
         * Builder helper to instantiate a PatientModel with common demographics & initial observations.
         */
        fun create(
            id: String = "patient-${UUID.randomUUID().toString().take(8)}",
            mrn: String = "MRN-${(100000..999999).random()}",
            fullName: String,
            gender: String = "male",
            birthDate: String = "1980-01-01",
            phone: String? = null,
            email: String? = null,
            city: String? = null,
            initialHeartRate: Double? = null,
            initialSpO2: Double? = null
        ): PatientModel {
            val names = listOf(
                PatientHumanName(
                    use = "official",
                    text = fullName,
                    family = fullName.split(" ").lastOrNull(),
                    given = fullName.split(" ").dropLast(1)
                )
            )

            val telecomList = mutableListOf<PatientContactPoint>()
            if (!phone.isNullOrBlank()) {
                telecomList.add(PatientContactPoint(system = "phone", value = phone, use = "mobile"))
            }
            if (!email.isNullOrBlank()) {
                telecomList.add(PatientContactPoint(system = "email", value = email, use = "home"))
            }

            val addressList = if (!city.isNullOrBlank()) {
                listOf(PatientAddress(use = "home", city = city))
            } else {
                emptyList()
            }

            val obs = if (initialHeartRate != null || initialSpO2 != null) {
                val now = currentIsoTimestamp()
                PatientObservations(
                    heartRate = initialHeartRate?.let { HeartRateObservation.create(it, now) },
                    oxygenSaturation = initialSpO2?.let { OxygenSaturationObservation.create(it, now) }
                )
            } else {
                null
            }

            return PatientModel(
                id = id,
                identifier = listOf(
                    PatientIdentifier(
                        use = "official",
                        system = "urn:oid:medical-record-number",
                        value = mrn
                    )
                ),
                active = true,
                name = names,
                telecom = telecomList,
                gender = gender,
                birthDate = birthDate,
                address = addressList,
                clinicalObservations = obs
            )
        }
    }
}

/**
 * Nested container for clinical observations associated with the patient.
 */
@JsonClass(generateAdapter = true)
data class PatientObservations(
    @Json(name = "heartRate")
    val heartRate: HeartRateObservation? = null,

    @Json(name = "oxygenSaturation")
    val oxygenSaturation: OxygenSaturationObservation? = null,

    @Json(name = "bloodPressure")
    val bloodPressure: BloodPressureObservation? = null,

    @Json(name = "respiratoryRate")
    val respiratoryRate: RespiratoryRateObservation? = null,

    @Json(name = "bodyTemperature")
    val bodyTemperature: BodyTemperatureObservation? = null,

    @Json(name = "allObservations")
    val allObservations: List<ClinicalObservation> = emptyList()
) : Serializable

/**
 * Clinical Observation for Heart Rate adhering to HL7 FHIR and LOINC 8867-4.
 */
@JsonClass(generateAdapter = true)
data class HeartRateObservation(
    @Json(name = "resourceType")
    val resourceType: String = "Observation",

    @Json(name = "id")
    val id: String = "obs-hr-${UUID.randomUUID().toString().take(8)}",

    @Json(name = "status")
    val status: String = "final",

    @Json(name = "code")
    val code: PatientCodeableConcept = PatientCodeableConcept(
        coding = listOf(
            PatientCoding(
                system = "http://loinc.org",
                code = LOINC_CODE,
                display = "Heart rate"
            )
        ),
        text = "Heart rate"
    ),

    @Json(name = "effectiveDateTime")
    val effectiveDateTime: String,

    @Json(name = "valueQuantity")
    val valueQuantity: PatientQuantity,

    @Json(name = "interpretation")
    val interpretation: List<PatientCodeableConcept> = emptyList(),

    @Json(name = "referenceRange")
    val referenceRange: List<ClinicalReferenceRange> = listOf(
        ClinicalReferenceRange(
            low = PatientQuantity(value = 60.0, unit = "beats/minute", code = "/min"),
            high = PatientQuantity(value = 100.0, unit = "beats/minute", code = "/min"),
            text = "Normal adult resting heart rate 60-100 bpm"
        )
    )
) : Serializable {

    val bpm: Double
        get() = valueQuantity.value

    val isBradycardia: Boolean
        get() = bpm < 60.0

    val isTachycardia: Boolean
        get() = bpm > 100.0

    val isNormal: Boolean
        get() = bpm in 60.0..100.0

    companion object {
        const val LOINC_CODE = "8867-4"
        const val UCUM_UNIT = "/min"

        fun create(
            bpm: Double,
            effectiveDateTime: String = PatientModel.currentIsoTimestamp()
        ): HeartRateObservation {
            val interpretationCode = when {
                bpm < 50.0 -> "critical-low"
                bpm < 60.0 -> "L" // Low (Bradycardia)
                bpm > 120.0 -> "critical-high"
                bpm > 100.0 -> "H" // High (Tachycardia)
                else -> "N" // Normal
            }
            val interpretationDisplay = when (interpretationCode) {
                "critical-low" -> "Severe Bradycardia"
                "L" -> "Bradycardia"
                "critical-high" -> "Severe Tachycardia"
                "H" -> "Tachycardia"
                else -> "Normal Heart Rate"
            }

            return HeartRateObservation(
                effectiveDateTime = effectiveDateTime,
                valueQuantity = PatientQuantity(
                    value = bpm,
                    unit = "beats/minute",
                    system = "http://unitsofmeasure.org",
                    code = UCUM_UNIT
                ),
                interpretation = listOf(
                    PatientCodeableConcept(
                        coding = listOf(
                            PatientCoding(
                                system = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
                                code = interpretationCode,
                                display = interpretationDisplay
                            )
                        ),
                        text = interpretationDisplay
                    )
                )
            )
        }
    }
}

/**
 * Clinical Observation for Oxygen Saturation (SpO2) adhering to HL7 FHIR and LOINC 2708-6.
 */
@JsonClass(generateAdapter = true)
data class OxygenSaturationObservation(
    @Json(name = "resourceType")
    val resourceType: String = "Observation",

    @Json(name = "id")
    val id: String = "obs-spo2-${UUID.randomUUID().toString().take(8)}",

    @Json(name = "status")
    val status: String = "final",

    @Json(name = "code")
    val code: PatientCodeableConcept = PatientCodeableConcept(
        coding = listOf(
            PatientCoding(
                system = "http://loinc.org",
                code = LOINC_CODE,
                display = "Oxygen saturation in Arterial blood"
            )
        ),
        text = "Arterial Oxygen Saturation (SpO2)"
    ),

    @Json(name = "effectiveDateTime")
    val effectiveDateTime: String,

    @Json(name = "valueQuantity")
    val valueQuantity: PatientQuantity,

    @Json(name = "interpretation")
    val interpretation: List<PatientCodeableConcept> = emptyList(),

    @Json(name = "referenceRange")
    val referenceRange: List<ClinicalReferenceRange> = listOf(
        ClinicalReferenceRange(
            low = PatientQuantity(value = 95.0, unit = "%", code = "%"),
            high = PatientQuantity(value = 100.0, unit = "%", code = "%"),
            text = "Normal resting SpO2: 95% - 100%"
        )
    )
) : Serializable {

    val percentage: Double
        get() = valueQuantity.value

    val isHypoxemic: Boolean
        get() = percentage < 95.0

    val isCritical: Boolean
        get() = percentage < 90.0

    val isNormal: Boolean
        get() = percentage in 95.0..100.0

    companion object {
        const val LOINC_CODE = "2708-6"
        const val UCUM_UNIT = "%"

        fun create(
            percentage: Double,
            effectiveDateTime: String = PatientModel.currentIsoTimestamp()
        ): OxygenSaturationObservation {
            val (interpCode, interpDisplay) = when {
                percentage < 85.0 -> "critical-low" to "Severe Life-Threatening Hypoxemia"
                percentage < 90.0 -> "critical-low" to "Severe Hypoxemia"
                percentage < 95.0 -> "L" to "Mild Hypoxemia"
                else -> "N" to "Normal Saturation"
            }

            return OxygenSaturationObservation(
                effectiveDateTime = effectiveDateTime,
                valueQuantity = PatientQuantity(
                    value = percentage,
                    unit = "%",
                    system = "http://unitsofmeasure.org",
                    code = UCUM_UNIT
                ),
                interpretation = listOf(
                    PatientCodeableConcept(
                        coding = listOf(
                            PatientCoding(
                                system = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
                                code = interpCode,
                                display = interpDisplay
                            )
                        ),
                        text = interpDisplay
                    )
                )
            )
        }
    }
}

/**
 * Clinical Observation for Blood Pressure Panel (LOINC 85354-9) with Systolic & Diastolic components.
 */
@JsonClass(generateAdapter = true)
data class BloodPressureObservation(
    @Json(name = "resourceType")
    val resourceType: String = "Observation",

    @Json(name = "id")
    val id: String = "obs-bp-${UUID.randomUUID().toString().take(8)}",

    @Json(name = "status")
    val status: String = "final",

    @Json(name = "code")
    val code: PatientCodeableConcept = PatientCodeableConcept(
        coding = listOf(
            PatientCoding(system = "http://loinc.org", code = "85354-9", display = "Blood pressure panel")
        ),
        text = "Blood Pressure"
    ),

    @Json(name = "effectiveDateTime")
    val effectiveDateTime: String,

    @Json(name = "component")
    val component: List<PatientObservationComponent> = emptyList()
) : Serializable {

    val systolic: Double?
        get() = component.find { it.code.coding.any { c -> c.code == "8480-6" } }?.valueQuantity?.value

    val diastolic: Double?
        get() = component.find { it.code.coding.any { c -> c.code == "8462-4" } }?.valueQuantity?.value

    companion object {
        fun create(
            systolic: Double,
            diastolic: Double,
            effectiveDateTime: String = PatientModel.currentIsoTimestamp()
        ): BloodPressureObservation {
            val sbpComponent = PatientObservationComponent(
                code = PatientCodeableConcept(
                    coding = listOf(PatientCoding("http://loinc.org", "8480-6", "Systolic blood pressure"))
                ),
                valueQuantity = PatientQuantity(value = systolic, unit = "mmHg", code = "mm[Hg]")
            )
            val dbpComponent = PatientObservationComponent(
                code = PatientCodeableConcept(
                    coding = listOf(PatientCoding("http://loinc.org", "8462-4", "Diastolic blood pressure"))
                ),
                valueQuantity = PatientQuantity(value = diastolic, unit = "mmHg", code = "mm[Hg]")
            )
            return BloodPressureObservation(
                effectiveDateTime = effectiveDateTime,
                component = listOf(sbpComponent, dbpComponent)
            )
        }
    }
}

/**
 * Clinical Observation for Respiratory Rate (LOINC 9279-1).
 */
@JsonClass(generateAdapter = true)
data class RespiratoryRateObservation(
    @Json(name = "resourceType") val resourceType: String = "Observation",
    @Json(name = "id") val id: String = "obs-rr-${UUID.randomUUID().toString().take(8)}",
    @Json(name = "effectiveDateTime") val effectiveDateTime: String,
    @Json(name = "valueQuantity") val valueQuantity: PatientQuantity
) : Serializable

/**
 * Clinical Observation for Body Temperature (LOINC 8310-5).
 */
@JsonClass(generateAdapter = true)
data class BodyTemperatureObservation(
    @Json(name = "resourceType") val resourceType: String = "Observation",
    @Json(name = "id") val id: String = "obs-temp-${UUID.randomUUID().toString().take(8)}",
    @Json(name = "effectiveDateTime") val effectiveDateTime: String,
    @Json(name = "valueQuantity") val valueQuantity: PatientQuantity
) : Serializable

/**
 * General FHIR Observation representation.
 */
@JsonClass(generateAdapter = true)
data class ClinicalObservation(
    @Json(name = "resourceType") val resourceType: String = "Observation",
    @Json(name = "id") val id: String,
    @Json(name = "status") val status: String = "final",
    @Json(name = "code") val code: PatientCodeableConcept,
    @Json(name = "subject") val subject: PatientReference? = null,
    @Json(name = "effectiveDateTime") val effectiveDateTime: String,
    @Json(name = "valueQuantity") val valueQuantity: PatientQuantity? = null,
    @Json(name = "interpretation") val interpretation: List<PatientCodeableConcept> = emptyList()
) : Serializable

// --- FHIR R4 Core Demographic Supporting Data Types ---

@JsonClass(generateAdapter = true)
data class PatientMeta(
    @Json(name = "versionId") val versionId: String? = "1",
    @Json(name = "lastUpdated") val lastUpdated: String? = null,
    @Json(name = "profile") val profile: List<String> = listOf("http://hl7.org/fhir/StructureDefinition/Patient")
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientIdentifier(
    @Json(name = "use") val use: String? = "official",
    @Json(name = "system") val system: String? = "http://hospital.example.org/patients",
    @Json(name = "value") val value: String
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientHumanName(
    @Json(name = "use") val use: String? = "official",
    @Json(name = "text") val text: String? = null,
    @Json(name = "family") val family: String? = null,
    @Json(name = "given") val given: List<String> = emptyList(),
    @Json(name = "prefix") val prefix: List<String> = emptyList(),
    @Json(name = "suffix") val suffix: List<String> = emptyList()
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientContactPoint(
    @Json(name = "system") val system: String = "phone",
    @Json(name = "value") val value: String,
    @Json(name = "use") val use: String? = "mobile"
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientAddress(
    @Json(name = "use") val use: String? = "home",
    @Json(name = "type") val type: String? = "both",
    @Json(name = "line") val line: List<String> = emptyList(),
    @Json(name = "city") val city: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "country") val country: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientCodeableConcept(
    @Json(name = "coding") val coding: List<PatientCoding> = emptyList(),
    @Json(name = "text") val text: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientCoding(
    @Json(name = "system") val system: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "display") val display: String? = null,
    @Json(name = "version") val version: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientReference(
    @Json(name = "reference") val reference: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "display") val display: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientCommunication(
    @Json(name = "language") val language: PatientCodeableConcept,
    @Json(name = "preferred") val preferred: Boolean? = true
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientQuantity(
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "system") val system: String = "http://unitsofmeasure.org",
    @Json(name = "code") val code: String
) : Serializable

@JsonClass(generateAdapter = true)
data class PatientObservationComponent(
    @Json(name = "code") val code: PatientCodeableConcept,
    @Json(name = "valueQuantity") val valueQuantity: PatientQuantity? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class ClinicalReferenceRange(
    @Json(name = "low") val low: PatientQuantity? = null,
    @Json(name = "high") val high: PatientQuantity? = null,
    @Json(name = "text") val text: String? = null
) : Serializable
