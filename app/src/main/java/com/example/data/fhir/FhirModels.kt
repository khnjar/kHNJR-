package com.example.data.fhir

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

/**
 * HL7 FHIR R4 (Fast Healthcare Interoperability Resources) Core Data Models
 * Designed for Digital Twin Physiological Simulation & Global Medical Interoperability.
 */

@JsonClass(generateAdapter = true)
data class FhirPatient(
    @Json(name = "resourceType") val resourceType: String = "Patient",
    @Json(name = "id") val id: String? = null,
    @Json(name = "meta") val meta: FhirMeta? = null,
    @Json(name = "identifier") val identifier: List<FhirIdentifier> = emptyList(),
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "name") val name: List<FhirHumanName> = emptyList(),
    @Json(name = "telecom") val telecom: List<FhirContactPoint> = emptyList(),
    @Json(name = "gender") val gender: String? = null, // "male" | "female" | "other" | "unknown"
    @Json(name = "birthDate") val birthDate: String? = null, // "YYYY-MM-DD"
    @Json(name = "deceasedBoolean") val deceasedBoolean: Boolean? = null,
    @Json(name = "deceasedDateTime") val deceasedDateTime: String? = null,
    @Json(name = "address") val address: List<FhirAddress> = emptyList(),
    @Json(name = "maritalStatus") val maritalStatus: FhirCodeableConcept? = null,
    @Json(name = "communication") val communication: List<FhirCommunication> = emptyList(),
    @Json(name = "generalPractitioner") val generalPractitioner: List<FhirReference> = emptyList(),
    @Json(name = "managingOrganization") val managingOrganization: FhirReference? = null,
    @Json(name = "extension") val extension: List<FhirExtension> = emptyList()
) : Serializable {

    val primaryName: String
        get() = name.firstOrNull()?.text
            ?: name.firstOrNull()?.let { "${it.given.joinToString(" ")} ${it.family ?: ""}".trim() }
            ?: "Unknown Patient"

    val primaryIdentifier: String?
        get() = identifier.firstOrNull()?.value
}

@JsonClass(generateAdapter = true)
data class FhirMeta(
    @Json(name = "versionId") val versionId: String? = "1",
    @Json(name = "lastUpdated") val lastUpdated: String? = null,
    @Json(name = "profile") val profile: List<String> = emptyList()
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirIdentifier(
    @Json(name = "use") val use: String? = "official", // "usual" | "official" | "temp" | "secondary"
    @Json(name = "type") val type: FhirCodeableConcept? = null,
    @Json(name = "system") val system: String? = "http://hospital.example.org/patients",
    @Json(name = "value") val value: String
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirHumanName(
    @Json(name = "use") val use: String? = "official", // "usual" | "official" | "temp" | "nickname"
    @Json(name = "text") val text: String? = null,
    @Json(name = "family") val family: String? = null,
    @Json(name = "given") val given: List<String> = emptyList(),
    @Json(name = "prefix") val prefix: List<String> = emptyList(),
    @Json(name = "suffix") val suffix: List<String> = emptyList()
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirContactPoint(
    @Json(name = "system") val system: String = "phone", // "phone" | "email" | "pager" | "url" | "sms"
    @Json(name = "value") val value: String,
    @Json(name = "use") val use: String? = "home" // "home" | "work" | "temp" | "old" | "mobile"
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirAddress(
    @Json(name = "use") val use: String? = "home", // "home" | "work" | "temp" | "old"
    @Json(name = "type") val type: String? = "both", // "postal" | "physical" | "both"
    @Json(name = "line") val line: List<String> = emptyList(),
    @Json(name = "city") val city: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "country") val country: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirCodeableConcept(
    @Json(name = "coding") val coding: List<FhirCoding> = emptyList(),
    @Json(name = "text") val text: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirCoding(
    @Json(name = "system") val system: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "display") val display: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirCommunication(
    @Json(name = "language") val language: FhirCodeableConcept,
    @Json(name = "preferred") val preferred: Boolean? = true
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirReference(
    @Json(name = "reference") val reference: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "display") val display: String? = null
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirExtension(
    @Json(name = "url") val url: String,
    @Json(name = "valueString") val valueString: String? = null,
    @Json(name = "valueDecimal") val valueDecimal: Double? = null,
    @Json(name = "valueBoolean") val valueBoolean: Boolean? = null
) : Serializable

/**
 * FHIR Observation resource for physiological vital signs & biomarkers
 */
@JsonClass(generateAdapter = true)
data class FhirObservation(
    @Json(name = "resourceType") val resourceType: String = "Observation",
    @Json(name = "id") val id: String? = null,
    @Json(name = "status") val status: String = "final", // "registered" | "preliminary" | "final" | "amended"
    @Json(name = "category") val category: List<FhirCodeableConcept> = emptyList(),
    @Json(name = "code") val code: FhirCodeableConcept,
    @Json(name = "subject") val subject: FhirReference,
    @Json(name = "effectiveDateTime") val effectiveDateTime: String,
    @Json(name = "valueQuantity") val valueQuantity: FhirQuantity? = null,
    @Json(name = "valueString") val valueString: String? = null,
    @Json(name = "interpretation") val interpretation: List<FhirCodeableConcept> = emptyList(),
    @Json(name = "component") val component: List<FhirObservationComponent> = emptyList(),
    @Json(name = "referenceRange") val referenceRange: List<FhirReferenceRange> = emptyList()
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirObservationComponent(
    @Json(name = "code") val code: FhirCodeableConcept,
    @Json(name = "valueQuantity") val valueQuantity: FhirQuantity? = null,
    @Json(name = "interpretation") val interpretation: List<FhirCodeableConcept> = emptyList()
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirQuantity(
    @Json(name = "value") val value: Double,
    @Json(name = "unit") val unit: String,
    @Json(name = "system") val system: String = "http://unitsofmeasure.org",
    @Json(name = "code") val code: String
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirReferenceRange(
    @Json(name = "low") val low: FhirQuantity? = null,
    @Json(name = "high") val high: FhirQuantity? = null,
    @Json(name = "type") val type: FhirCodeableConcept? = null,
    @Json(name = "text") val text: String? = null
) : Serializable

/**
 * FHIR Bundle for packaging patient record and physiological observations together.
 */
@JsonClass(generateAdapter = true)
data class FhirBundle(
    @Json(name = "resourceType") val resourceType: String = "Bundle",
    @Json(name = "id") val id: String? = null,
    @Json(name = "type") val type: String = "collection", // "document" | "message" | "transaction" | "collection"
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "entry") val entry: List<FhirBundleEntry> = emptyList()
) : Serializable

@JsonClass(generateAdapter = true)
data class FhirBundleEntry(
    @Json(name = "fullUrl") val fullUrl: String? = null,
    @Json(name = "resource") val resource: Map<String, Any?>
) : Serializable
