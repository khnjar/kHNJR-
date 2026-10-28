package com.example.data.fhir

/**
 * Standard Medical Coding Systems and LOINC Codes for Physiological Simulation
 * Adheres to HL7 FHIR US-Core and International Patient Summary (IPS) profiles.
 */
object FhirLoincCodes {
    const val SYSTEM_LOINC = "http://loinc.org"
    const val SYSTEM_SNOMED = "http://snomed.info/sct"
    const val SYSTEM_UCUM = "http://unitsofmeasure.org"
    const val SYSTEM_OBSERVATION_CATEGORY = "http://terminology.hl7.org/CodeSystem/observation-category"
    const val SYSTEM_OBSERVATION_INTERPRETATION = "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation"

    // Vital Signs Category
    val CATEGORY_VITAL_SIGNS = FhirCodeableConcept(
        coding = listOf(
            FhirCoding(
                system = SYSTEM_OBSERVATION_CATEGORY,
                code = "vital-signs",
                display = "Vital Signs"
            )
        ),
        text = "Vital Signs"
    )

    // Heart Rate (LOINC: 8867-4)
    const val CODE_HEART_RATE = "8867-4"
    const val DISPLAY_HEART_RATE = "Heart rate"
    const val UNIT_BEATS_PER_MIN = "/min"

    // Blood Pressure Panel (LOINC: 85354-9)
    const val CODE_BP_PANEL = "85354-9"
    const val DISPLAY_BP_PANEL = "Blood pressure panel with all children optional"
    const val CODE_BP_SYSTOLIC = "8480-6"
    const val DISPLAY_BP_SYSTOLIC = "Systolic blood pressure"
    const val CODE_BP_DIASTOLIC = "8462-4"
    const val DISPLAY_BP_DIASTOLIC = "Diastolic blood pressure"
    const val UNIT_MMHG = "mm[Hg]"

    // Oxygen Saturation / SpO2 (LOINC: 2708-6 / 59408-5)
    const val CODE_OXYGEN_SATURATION = "2708-6"
    const val DISPLAY_OXYGEN_SATURATION = "Oxygen saturation in Arterial blood"
    const val UNIT_PERCENT = "%"

    // Respiratory Rate (LOINC: 9279-1)
    const val CODE_RESPIRATORY_RATE = "9279-1"
    const val DISPLAY_RESPIRATORY_RATE = "Respiratory rate"
    const val UNIT_BREATHS_PER_MIN = "/min"

    // Body Temperature (LOINC: 8310-5)
    const val CODE_BODY_TEMPERATURE = "8310-5"
    const val DISPLAY_BODY_TEMPERATURE = "Body temperature"
    const val UNIT_CELSIUS = "Cel"

    // Glomerular Filtration Rate / eGFR (LOINC: 33914-3)
    const val CODE_EGFR = "33914-3"
    const val DISPLAY_EGFR = "Glomerular filtration rate/1.73 sq M.predicted"
    const val UNIT_ML_MIN_173M2 = "mL/min/{1.73_m2}"

    // Body Weight (LOINC: 29463-7)
    const val CODE_BODY_WEIGHT = "29463-7"
    const val DISPLAY_BODY_WEIGHT = "Body weight"
    const val UNIT_KG = "kg"

    // Body Height (LOINC: 8302-2)
    const val CODE_BODY_HEIGHT = "8302-2"
    const val DISPLAY_BODY_HEIGHT = "Body height"
    const val UNIT_CM = "cm"

    // Interpretations
    val INTERPRETATION_NORMAL = FhirCodeableConcept(
        coding = listOf(
            FhirCoding(
                system = SYSTEM_OBSERVATION_INTERPRETATION,
                code = "N",
                display = "Normal"
            )
        ),
        text = "طبيعي / Normal"
    )

    val INTERPRETATION_HIGH = FhirCodeableConcept(
        coding = listOf(
            FhirCoding(
                system = SYSTEM_OBSERVATION_INTERPRETATION,
                code = "H",
                display = "High"
            )
        ),
        text = "مرتفع / High"
    )

    val INTERPRETATION_CRITICAL_HIGH = FhirCodeableConcept(
        coding = listOf(
            FhirCoding(
                system = SYSTEM_OBSERVATION_INTERPRETATION,
                code = "HH",
                display = "Critical high"
            )
        ),
        text = "حرج مرتفع / Critical High"
    )

    val INTERPRETATION_LOW = FhirCodeableConcept(
        coding = listOf(
            FhirCoding(
                system = SYSTEM_OBSERVATION_INTERPRETATION,
                code = "L",
                display = "Low"
            )
        ),
        text = "منخفض / Low"
    )

    val INTERPRETATION_CRITICAL_LOW = FhirCodeableConcept(
        coding = listOf(
            FhirCoding(
                system = SYSTEM_OBSERVATION_INTERPRETATION,
                code = "LL",
                display = "Critical low"
            )
        ),
        text = "حرج منخفض / Critical Low"
    )
}
