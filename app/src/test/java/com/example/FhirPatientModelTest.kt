package com.example

import com.example.data.fhir.FhirDigitalTwinBridge
import com.example.data.fhir.FhirLoincCodes
import com.example.data.fhir.PhysiologicalSnapshot
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests validating HL7 FHIR Patient and Observation models
 * for Physiological Digital Twin Simulation and Global Interoperability.
 */
class FhirPatientModelTest {

    private val bridge = FhirDigitalTwinBridge()

    @Test
    fun testPatientCreationAndJsonSerialization() {
        val patient = bridge.createPatient(
            id = "test-patient-001",
            medicalRecordNumber = "MRN-998877",
            familyArabic = "الغامدي",
            givenArabic = "عمر",
            familyEnglish = "Al-Ghamdi",
            givenEnglish = "Omar",
            gender = "male",
            birthDate = "1985-04-12"
        )

        assertEquals("Patient", patient.resourceType)
        assertEquals("test-patient-001", patient.id)
        assertEquals("عمر الغامدي", patient.primaryName)
        assertEquals("MRN-998877", patient.primaryIdentifier)
        assertEquals("male", patient.gender)

        // Serialize to FHIR JSON
        val json = bridge.exportPatientToJson(patient)
        assertNotNull(json)
        assertTrue(json.contains("\"resourceType\": \"Patient\""))
        assertTrue(json.contains("test-patient-001"))
        assertTrue(json.contains("MRN-998877"))
        assertTrue(json.contains("عمر الغامدي"))

        // Deserialize back
        val parsed = bridge.parsePatientFromJson(json)
        assertNotNull(parsed)
        assertEquals(patient.id, parsed?.id)
        assertEquals(patient.primaryName, parsed?.primaryName)
        assertEquals(patient.gender, parsed?.gender)
    }

    @Test
    fun testPhysiologicalObservationsLoincMapping() {
        val patient = bridge.createPatient()
        val snapshot = PhysiologicalSnapshot(
            heartRate = 78.0,
            systolicBp = 122.0,
            diastolicBp = 82.0,
            oxygenSaturation = 98.0,
            respiratoryRate = 16.0,
            temperatureCelsius = 37.0,
            egfr = 95.0
        )

        val observations = bridge.createObservationsFromSnapshot(patient, snapshot)
        assertFalse(observations.isEmpty())

        // Verify Heart Rate Observation (LOINC 8867-4)
        val hrObs = observations.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_HEART_RATE } }
        assertNotNull("Heart rate observation must be present", hrObs)
        assertEquals(78.0, hrObs?.valueQuantity?.value ?: 0.0, 0.001)
        assertEquals(FhirLoincCodes.UNIT_BEATS_PER_MIN, hrObs?.valueQuantity?.unit)

        // Verify Blood Pressure Panel (LOINC 85354-9) with Systolic (8480-6) & Diastolic (8462-4)
        val bpObs = observations.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_BP_PANEL } }
        assertNotNull("Blood pressure panel observation must be present", bpObs)
        assertEquals(2, bpObs?.component?.size)
        val sbpComp = bpObs?.component?.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_BP_SYSTOLIC } }
        val dbpComp = bpObs?.component?.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_BP_DIASTOLIC } }
        assertNotNull(sbpComp)
        assertNotNull(dbpComp)
        assertEquals(122.0, sbpComp?.valueQuantity?.value ?: 0.0, 0.001)
        assertEquals(82.0, dbpComp?.valueQuantity?.value ?: 0.0, 0.001)

        // Verify SpO2 Observation (LOINC 2708-6)
        val spo2Obs = observations.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_OXYGEN_SATURATION } }
        assertNotNull(spo2Obs)
        assertEquals(98.0, spo2Obs?.valueQuantity?.value ?: 0.0, 0.001)

        // Verify eGFR Observation (LOINC 33914-3)
        val egfrObs = observations.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_EGFR } }
        assertNotNull(egfrObs)
        assertEquals(95.0, egfrObs?.valueQuantity?.value ?: 0.0, 0.001)
    }

    @Test
    fun testPresetScenarios() {
        val (patient, snapshot) = bridge.getPresetScenario(FhirDigitalTwinBridge.ScenarioType.HYPERTENSIVE_CRISIS)
        assertEquals("خالد المنصور", patient.primaryName)
        assertEquals(175.0, snapshot.systolicBp, 0.001)
        assertEquals(110.0, snapshot.diastolicBp, 0.001)

        val observations = bridge.createObservationsFromSnapshot(patient, snapshot)
        val bpObs = observations.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_BP_PANEL } }
        val sbpComp = bpObs?.component?.find { it.code.coding.any { c -> c.code == FhirLoincCodes.CODE_BP_SYSTOLIC } }

        // Hypertensive emergency should be flagged as critical high / high
        assertTrue(sbpComp?.interpretation?.any { it.coding.any { c -> c.code == "HH" || c.code == "H" } } == true)
    }

    @Test
    fun testPatientModelDemographicsAndNestedObservations() {
        // Create PatientModel using factory with demographics & initial vitals
        val patient = com.example.data.PatientModel.create(
            id = "patient-demo-01",
            mrn = "MRN-123456",
            fullName = "سارة العتيبي",
            gender = "female",
            birthDate = "1992-08-20",
            phone = "+966555123456",
            email = "sara@hospital.example.com",
            city = "جدة",
            initialHeartRate = 72.0,
            initialSpO2 = 99.0
        )

        assertEquals("Patient", patient.resourceType)
        assertEquals("patient-demo-01", patient.id)
        assertEquals("سارة العتيبي", patient.primaryName)
        assertEquals("MRN-123456", patient.primaryIdentifier)
        assertEquals("female", patient.gender)
        assertTrue(patient.isFemale)
        assertFalse(patient.isMale)

        // Validate nested observations
        val obs = patient.clinicalObservations
        assertNotNull("Nested observations must not be null", obs)

        // Heart Rate validation
        val hr = obs?.heartRate
        assertNotNull("Heart rate observation must be present", hr)
        assertEquals(72.0, hr?.bpm ?: 0.0, 0.001)
        assertEquals("8867-4", hr?.code?.coding?.firstOrNull()?.code)
        assertEquals("/min", hr?.valueQuantity?.code)
        assertTrue(hr?.isNormal == true)
        assertFalse(hr?.isBradycardia == true)
        assertFalse(hr?.isTachycardia == true)

        // Oxygen Saturation validation
        val spo2 = obs?.oxygenSaturation
        assertNotNull("Oxygen saturation observation must be present", spo2)
        assertEquals(99.0, spo2?.percentage ?: 0.0, 0.001)
        assertEquals("2708-6", spo2?.code?.coding?.firstOrNull()?.code)
        assertEquals("%", spo2?.valueQuantity?.code)
        assertTrue(spo2?.isNormal == true)
        assertFalse(spo2?.isHypoxemic == true)
        assertFalse(spo2?.isCritical == true)

        // Test immutable update of vitals
        val updatedPatient = patient.withVitals(
            heartRateBpm = 115.0,
            spo2Percent = 88.0,
            systolicBp = 145.0,
            diastolicBp = 95.0
        )

        val updatedHr = updatedPatient.heartRate
        assertNotNull(updatedHr)
        assertEquals(115.0, updatedHr?.bpm ?: 0.0, 0.001)
        assertTrue(updatedHr?.isTachycardia == true)

        val updatedSpo2 = updatedPatient.oxygenSaturation
        assertNotNull(updatedSpo2)
        assertEquals(88.0, updatedSpo2?.percentage ?: 0.0, 0.001)
        assertTrue(updatedSpo2?.isHypoxemic == true)
        assertTrue(updatedSpo2?.isCritical == true)

        // Test Blood pressure nested observation
        val bp = updatedPatient.clinicalObservations?.bloodPressure
        assertNotNull(bp)
        assertEquals(145.0, bp?.systolic ?: 0.0, 0.001)
        assertEquals(95.0, bp?.diastolic ?: 0.0, 0.001)

        // Test serialization and deserialization
        val json = patient.toJson()
        assertTrue(json.contains("\"resourceType\": \"Patient\""))
        assertTrue(json.contains("\"id\": \"patient-demo-01\""))
        assertTrue(json.contains("\"8867-4\""))
        assertTrue(json.contains("\"2708-6\""))

        val deserialized = com.example.data.PatientModel.fromJson(json)
        assertNotNull("Deserialization must succeed", deserialized)
        assertEquals(patient.id, deserialized?.id)
        assertEquals(patient.primaryName, deserialized?.primaryName)
        assertEquals(72.0, deserialized?.heartRate?.bpm ?: 0.0, 0.001)
        assertEquals(99.0, deserialized?.oxygenSaturation?.percentage ?: 0.0, 0.001)
    }
}

